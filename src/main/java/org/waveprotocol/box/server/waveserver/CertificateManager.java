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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;

import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.crypto.UnknownSignerException;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Stand-in interface for the certificate manager.
 *
 *
 */
public interface CertificateManager {

  ImmutableSet<String> getLocalDomains();

  /**
   * @return the signer info for the local wave signer.
   */
  SignatureHandler getLocalSigner();

  /**
   * Verify the signature in the Signed Delta. Use the local WSP's certificate
   * to sign the delta.
   *
   * @param delta as a byte string (the serialised representation of a ProtocolWaveletDelta)
   * @return signed delta
   */
  ProtocolSignedDelta signDelta(ByteStringMessage<ProtocolWaveletDelta> delta);

  /**
   * Verify the signature in the Signed Delta. Use the delta's author's WSP
   * address to identify the certificate.
   *
   * @param signedDelta to verify
   * @return verified serialised ProtocolWaveletDelta, if signatures can be verified
   * @throws SignatureException if the signatures cannot be verified.
   */
  ByteStringMessage<ProtocolWaveletDelta> verifyDelta(ProtocolSignedDelta signedDelta)
      throws SignatureException, UnknownSignerException;

  /**
   * Stores information about a signer (i.e., its certificate chain) in a
   * permanent store. In addition to a certificate chain, a {@link SignerInfo}
   * also contains an identifier of hash algorithm. Signers will use the hash
   * of the cert chain to refer to this signer info in their signatures.
   *
   * @param signerInfo
   * @throws SignatureException if the {@link SignerInfo} doesn't check out
   */
  void storeSignerInfo(ProtocolSignerInfo signerInfo) throws SignatureException;

  /**
   * Retrieves information about a signer.
   *
   * @param signerId identifier of the signer (the hash of its certificate chain)
   * @return the signer information, if found, null otherwise
   */
  ProtocolSignerInfo retrieveSignerInfo(ByteString signerId);

  /**
   * Callback interface for {@code prefetchSignerInfo}.
   */
  interface SignerInfoPrefetchResultListener {
    void onSuccess(ProtocolSignerInfo signerInfo);
    void onFailure(FederationError error);
  }

  /**
   * Prefetch the signer info for a signed delta, calling back when the signer info is available.
   * Note that the signer info may be immediately available, in which case the callback is
   * immediately called in the same thread.
   *
   * @param provider of signer information
   * @param signerId to prefetch the signer info for
   * @param deltaEndVersion of delta to use for validating a getDeltaSignerInfo call, if necessary
   * @param waveletName of the wavelet to prefetch the signer info for
   * @param callback when the signer info is available, or on failure
   */
  void prefetchDeltaSignerInfo(WaveletFederationProvider provider, ByteString signerId,
      WaveletName waveletName, HashedVersion deltaEndVersion,
      SignerInfoPrefetchResultListener callback);
}
