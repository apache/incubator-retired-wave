/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.crypto.*;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.WaveletFederationProvider.DeltaSignerInfoResponseListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link CertificateManager}.
 */
public class CertificateManagerImpl implements CertificateManager {

  private static final Log LOG = Log.get(CertificateManagerImpl.class);

  private final SignatureHandler waveSigner;
  private final ImmutableSet<String> localDomains;
  private final WaveSignatureVerifier verifier;
  private final CertPathStore certPathStore;
  private final boolean disableVerfication;

  /**
   * Map of signer ids to requests for the signer info for those ids.  Each signer id is mapped to
   * a multimap: a domain mapped to a list of callbacks for that domain, called when the signer info
   * is available for the signer id.  It is arranged by domain to facilitate the optimisation where
   * exactly 1 signer request is sent per domain.
   */
  private final Map<ByteString, Multimap<String, SignerInfoPrefetchResultListener>>
      signerInfoRequests;

  @Inject
  public CertificateManagerImpl(Config config, SignatureHandler signer,
    WaveSignatureVerifier verifier, CertPathStore certPathStore) {
    this.disableVerfication = config.getBoolean("federation.waveserver_disable_verification");
    this.waveSigner = signer;
    // for now, we just support a single signer
    this.localDomains = ImmutableSet.of(signer.getDomain());
    this.verifier = verifier;
    this.certPathStore = certPathStore;
    this.signerInfoRequests = Maps.newHashMap();

    if (disableVerfication) {
      LOG.warning("** SIGNATURE VERIFICATION DISABLED ** "
          + "see configuration federation.waveserver_disable_verification");
    }
  }

  @Override
  public ImmutableSet<String> getLocalDomains() {
    return localDomains;
  }

  @Override
  public SignatureHandler getLocalSigner() {
    return waveSigner;
  }

  @Override
  public ProtocolSignedDelta signDelta(ByteStringMessage<ProtocolWaveletDelta> delta) {
    // TODO: support extended address paths. For now, there will be exactly
    // one signature, and we don't support federated groups.
    Preconditions.checkState(delta.getMessage().getAddressPathCount() == 0);

    ProtocolSignedDelta.Builder signedDelta = ProtocolSignedDelta.newBuilder();

    signedDelta.setDelta(delta.getByteString());
    signedDelta.addAllSignature(waveSigner.sign(delta));
    return signedDelta.build();
  }

  @Override
  public ByteStringMessage<ProtocolWaveletDelta> verifyDelta(ProtocolSignedDelta signedDelta)
      throws SignatureException, UnknownSignerException {
    ByteStringMessage<ProtocolWaveletDelta> delta;
    try {
      delta = ByteStringMessage.parseProtocolWaveletDelta(signedDelta.getDelta());
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("signed delta does not contain valid delta", e);
    }

    if (disableVerfication) {
      return delta;
    }

    List<String> domains = getParticipantDomains(delta.getMessage());

    if (domains.size() != signedDelta.getSignatureCount()) {
      throw new SignatureException("found " + domains.size() + " domains in " +
          "extended address path, but " + signedDelta.getSignatureCount() +
          " signatures.");
    }

    for (int i = 0; i < domains.size(); i++) {
      String domain = domains.get(i);
      ProtocolSignature signature = signedDelta.getSignature(i);
      verifySingleSignature(delta, signature, domain);
    }

    return delta;
  }

  /**
   * Verifies a single signature.
   * @param delta the payload that we're verifying the signature on.
   * @param signature the signature on the payload
   * @param domain the authority (domain name) that should have signed the
   *   payload.
   * @throws SignatureException if the signature doesn't verify.
   */
  private void verifySingleSignature(ByteStringMessage<ProtocolWaveletDelta> delta,
      ProtocolSignature signature, String domain)
      throws SignatureException, UnknownSignerException {
    verifier.verify(delta.getByteString().toByteArray(), signature, domain);
  }

  /**
   * Returns the domains of all the addresses in the extended address path.
   */
  private List<String> getParticipantDomains(ProtocolWaveletDelta delta) {
    Iterable<String> addresses = getExtendedAddressPath(delta);
    return getDeDupedDomains(addresses);
  }

  /**
   * Extracts the domains from user addresses, and removes duplicates.
   */
  private List<String> getDeDupedDomains(Iterable<String> addresses) {
    List<String> domains = Lists.newArrayList();
    for (String address : addresses) {
      String participantDomain = new ParticipantId(address).getDomain();
      if (!domains.contains(participantDomain)) {
        domains.add(participantDomain);
      }
    }
    return domains;
  }

  /**
   * Returns the extended address path, i.e., the addresses in the delta's
   * address path, plus the author of the delta.
   */
  private Iterable<String> getExtendedAddressPath(ProtocolWaveletDelta delta) {
    return Iterables.concat(delta.getAddressPathList(),
        ImmutableList.of(delta.getAuthor()));
  }

  @Override
  public synchronized void storeSignerInfo(ProtocolSignerInfo signerInfo)
      throws SignatureException {
    verifier.verifySignerInfo(new SignerInfo(signerInfo));
    certPathStore.putSignerInfo(signerInfo);
  }

  @Override
  public synchronized ProtocolSignerInfo retrieveSignerInfo(ByteString signerId) {
    SignerInfo signerInfo;
    try {
      signerInfo = certPathStore.getSignerInfo(signerId.toByteArray());
      // null is acceptable for retrieveSignerInfo.  The user of the certificate manager should call
      // prefetchDeltaSignerInfo for the mechanism to actually populate the certificate manager.
      return signerInfo == null ? null : signerInfo.toProtoBuf();
    } catch (SignatureException e) {
      /*
       * TODO: This may result in the server endlessly requesting the signer info from the
       * remote server, a more graceful failure needs to be implemented.
       */
      LOG.severe("Failed to retreive signer info for "
          + new String(Hex.encodeHex(signerId.toByteArray())), e);
      return null;
    }
  }

  @Override
  public synchronized void prefetchDeltaSignerInfo(WaveletFederationProvider provider,
      ByteString signerId, WaveletName waveletName, HashedVersion deltaEndVersion,
      SignerInfoPrefetchResultListener callback) {
    ProtocolSignerInfo signerInfo = retrieveSignerInfo(signerId);

    if (signerInfo != null) {
      callback.onSuccess(signerInfo);
    } else {
      enqueueSignerInfoRequest(provider, signerId, waveletName, deltaEndVersion, callback);
    }
  }

  /**
   * Enqueue a signer info request for a signed delta on a given domain.
   */
  private synchronized void enqueueSignerInfoRequest(final WaveletFederationProvider provider,
      final ByteString signerId, final WaveletName waveletName,
      HashedVersion deltaEndVersion, SignerInfoPrefetchResultListener callback) {
    final String domain = waveletName.waveletId.getDomain();
    Multimap<String, SignerInfoPrefetchResultListener> domainCallbacks =
        signerInfoRequests.get(signerId);

    if (domainCallbacks == null) {
      domainCallbacks = ArrayListMultimap.create();
      signerInfoRequests.put(signerId, domainCallbacks);
    }

    // The thing is, we need to add multiple callbacks for the same domain, but we only want to
    // have one outstanding request per domain
    domainCallbacks.put(domain, callback);

    if (domainCallbacks.get(domain).size() == 1) {
        provider.getDeltaSignerInfo(signerId, waveletName,
            (deltaEndVersion == null)
                ? null : CoreWaveletOperationSerializer.serialize(deltaEndVersion),
            new DeltaSignerInfoResponseListener() {
              @Override public void onFailure(FederationError error) {
                LOG.warning("getDeltaSignerInfo failed: " + error);
                // Fail all requests on this domain
                dequeueSignerInfoRequestForDomain(signerId, error, domain);
              }

              @Override public void onSuccess(ProtocolSignerInfo signerInfo) {
                try {
                  storeSignerInfo(signerInfo);
                  dequeueSignerInfoRequest(signerId, null);
                } catch (SignatureException e) {
                  LOG.warning("Failed to verify signer info", e);
                  dequeueSignerInfoRequest(signerId, FederationErrors.badRequest(e.toString()));
                }
              }});
    }
  }

  /**
   * Dequeue all signer info requests for a given signer id.
   *
   * @param signerId to dequeue requests for
   * @param error if there was an error, null for success
   */
  private synchronized void dequeueSignerInfoRequest(ByteString signerId, FederationError error) {
    List<String> domains = ImmutableList.copyOf(signerInfoRequests.get(signerId).keySet());
    for (String domain : domains) {
      dequeueSignerInfoRequestForDomain(signerId, error, domain);
    }
  }

  /**
   * Dequeue all signer info requests for a given signer id and a specific domain.
   *
   * @param signerId to dequeue requests for
   * @param error if there was an error, null for success
   * @param domain to dequeue the signer requests for
   */
  private synchronized void dequeueSignerInfoRequestForDomain(ByteString signerId,
      FederationError error, String domain) {
    Multimap<String, SignerInfoPrefetchResultListener> domainListeners =
        signerInfoRequests.get(signerId);
    if (domainListeners == null) {
      LOG.info("There are no domain listeners for signer " + signerId + " domain "+ domain);
      return;
    } else {
      LOG.info("Dequeuing " + domainListeners.size() + " listeners for domain " + domain);
    }

    for (SignerInfoPrefetchResultListener listener : domainListeners.get(domain)) {
      if (error == null) {
        listener.onSuccess(retrieveSignerInfo(signerId));
      } else {
        listener.onFailure(error);
      }
    }

    domainListeners.removeAll(domain);
    if (domainListeners.isEmpty()) {
      // No listeners for any domains, delete the signer id for the overall map
      signerInfoRequests.remove(signerId);
    }
  }
}
