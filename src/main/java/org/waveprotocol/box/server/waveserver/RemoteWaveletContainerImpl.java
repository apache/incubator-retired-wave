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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.codec.binary.Base64;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.waveserver.CertificateManager.SignerInfoPrefetchResultListener;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.UnknownSignerException;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationException;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.WaveletFederationProvider.HistoryResponseListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Remote wavelets differ from local ones in that deltas are not submitted for OT,
 * rather they are updated when a remote wave service provider has applied and sent
 * a delta.
 */
class RemoteWaveletContainerImpl extends WaveletContainerImpl implements RemoteWaveletContainer {
  private static final Log LOG = Log.get(RemoteWaveletContainerImpl.class);

  /**
   * Stores all pending deltas for this wavelet, whose insertions would cause
   * discontiguous blocks of deltas. This must only be accessed under writeLock.
   */
  private final NavigableMap<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>>
      pendingDeltas = Maps.newTreeMap();

  /**
   * Tracks the highest version commit notice received, which can not be performed
   * due to not yet having the required deltas. This must only be access under writeLock.
   */
  private boolean pendingCommit = false;
  private HashedVersion pendingCommitVersion;

  /**
   * Create a new RemoteWaveletContainerImpl. Just pass through to the parent
   * constructor.
   */
  public RemoteWaveletContainerImpl(WaveletName waveletName, WaveletNotificationSubscriber notifiee,
      ListenableFuture<? extends WaveletState> waveletStateFuture,
      Executor storageContinuationExecutor) {
    // We pass here null for waveDomain because you have to be explicit
    // participant on remote wavelet to have access permission.
    super(waveletName, notifiee, waveletStateFuture, null, storageContinuationExecutor);
  }

  @Override
  public ListenableFuture<Void> update(final List<ByteString> deltas,
      final String domain, final WaveletFederationProvider federationProvider,
      final CertificateManager certificateManager) {
    SettableFuture<Void> futureResult = SettableFuture.create();
    internalUpdate(deltas, domain, federationProvider, certificateManager, futureResult);
    return futureResult;
  }

  @Override
  public void commit(HashedVersion version) {
    try {
      awaitLoad();
    }
    catch(WaveletStateException ex) {
      LOG.warning("Failed to load " + getWaveletName() + " to perform commit.", ex);
      acquireWriteLock();
      markStateCorrupted();
      releaseWriteLock();
      return;
    }

    acquireWriteLock();
    try {
      attemptCommit(version);
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Attempts to commit at the given version.
   * This will only succeed if we are actually up to date.
   * If not, then the history is assumed to be coming, and so we can just skip the whole task.
   * */
  private void attemptCommit(HashedVersion version) {
    HashedVersion expectedVersion = getCurrentVersion();
    if(expectedVersion == null || version.getVersion() == expectedVersion.getVersion()) {
      LOG.info("Committed " + getWaveletName() + " at version " + version.getVersion());
      persist(version, ImmutableSet.<String>of());
      if(pendingCommitVersion == null || (version.getVersion() >= pendingCommitVersion.getVersion())) {
        pendingCommit = false;
      }
    } else {
      LOG.info("Ignoring commit request at " + version.getVersion() +
          " since only at " + expectedVersion.getVersion());
      if(pendingCommitVersion == null ||
          (pendingCommitVersion != null && pendingCommitVersion.getVersion() < version.getVersion())) {
        pendingCommitVersion = version;
      }
      LOG.info("pendingCommitVersion is now " + pendingCommitVersion.getVersion());
      pendingCommit = true;
    }
  }

  private void internalUpdate(final List<ByteString> deltas,
      final String domain, final WaveletFederationProvider federationProvider,
      final CertificateManager certificateManager, final SettableFuture<Void> futureResult) {
    // Turn raw serialised ByteStrings in to a more useful representation
    final List<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas = Lists.newArrayList();
    for (ByteString delta : deltas) {
      try {
        appliedDeltas.add(ByteStringMessage.parseProtocolAppliedWaveletDelta(delta));
      } catch (InvalidProtocolBufferException e) {
        LOG.info("Invalid applied delta protobuf for incoming " + getWaveletName(), e);
        acquireWriteLock();
        try {
          markStateCorrupted();
        } finally {
          releaseWriteLock();
        }
        futureResult.setException(new FederationException(
            FederationErrors.badRequest("Invalid applied delta protocol buffer")));
        return;
      }
    }
    LOG.info("Got update: " + appliedDeltas);

    // Fetch any signer info that we don't already have and then run internalUpdate
    final AtomicInteger numSignerInfoPrefetched = new AtomicInteger(1); // extra 1 for sentinel
    final Runnable countDown = new Runnable() {
      @Override
      public void run() {
        if (numSignerInfoPrefetched.decrementAndGet() == 0) {
          internalUpdateAfterSignerInfoRetrieval(
              appliedDeltas, domain, federationProvider, certificateManager, futureResult);
        }
      }
    };
    SignerInfoPrefetchResultListener prefetchListener = new SignerInfoPrefetchResultListener() {
      @Override
      public void onFailure(FederationError error) {
        LOG.warning("Signer info prefetch failed: " + error);
        countDown.run();
      }

      @Override
      public void onSuccess(ProtocolSignerInfo signerInfo) {
        LOG.info("Signer info prefetch success for " + signerInfo.getDomain());
        countDown.run();
      }
    };
    for (ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta : appliedDeltas) {
      ProtocolSignedDelta toVerify = appliedDelta.getMessage().getSignedOriginalDelta();
      HashedVersion deltaEndVersion;
      try {
        deltaEndVersion = AppliedDeltaUtil.calculateResultingHashedVersion(appliedDelta);
      } catch (InvalidProtocolBufferException e) {
        LOG.warning("Skipping illformed applied delta " + appliedDelta, e);
        continue;
      }
      for (ProtocolSignature sig : toVerify.getSignatureList()) {
        if (certificateManager.retrieveSignerInfo(sig.getSignerId()) == null) {
          LOG.info("Fetching signer info " + Base64.encodeBase64(sig.getSignerId().toByteArray()));
          numSignerInfoPrefetched.incrementAndGet();
          certificateManager.prefetchDeltaSignerInfo(federationProvider, sig.getSignerId(),
              getWaveletName(), deltaEndVersion, prefetchListener);
        }
      }
    }
    // If we didn't fetch any signer info, run internalUpdate immediately
    countDown.run();
  }

  private void internalUpdateAfterSignerInfoRetrieval(
      List<ByteStringMessage<ProtocolAppliedWaveletDelta>> appliedDeltas,
      final String domain, final WaveletFederationProvider federationProvider,
      final CertificateManager certificateManager, final SettableFuture<Void> futureResult) {

    try {
      awaitLoad();
    }
    catch(WaveletStateException ex) {
      LOG.warning("Failed to load " + getWaveletName() + " to perform update.", ex);
      acquireWriteLock();
      markStateCorrupted();
      releaseWriteLock();
      return;
    }

    LOG.info("Passed signer info check, now applying all " + appliedDeltas.size() + " deltas");
    acquireWriteLock();
    try {
      checkStateOk(); // TODO(soren): if CORRUPTED, throw away wavelet and start again
      HashedVersion expectedVersion = getCurrentVersion();
      boolean haveRequestedHistory = false;

      // Verify signatures of all deltas
      for (ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta : appliedDeltas) {
        try {
          certificateManager.verifyDelta(appliedDelta.getMessage().getSignedOriginalDelta());
        } catch (SignatureException e) {
          LOG.warning("Verification failure for " + domain + " incoming " + getWaveletName(), e);
          throw new WaveServerException("Verification failure", e);
        } catch (UnknownSignerException e) {
          LOG.severe("Unknown signer for " + domain + " incoming " + getWaveletName() +
              ", this is BAD! We were supposed to have prefetched it!", e);
          throw new WaveServerException("Unknown signer", e);
        }
      }

      // Insert all available deltas into pendingDeltas.
      for (ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta : appliedDeltas) {
        LOG.info("Delta incoming: " + appliedDelta);

        // Log any illformed signed original deltas. TODO: Check if this can be removed.
        try {
          ProtocolWaveletDelta actualDelta = ProtocolWaveletDelta.parseFrom(
              appliedDelta.getMessage().getSignedOriginalDelta().getDelta());
          LOG.info("actual delta: " + actualDelta);
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }

        HashedVersion appliedAt;
        try {
          appliedAt = AppliedDeltaUtil.getHashedVersionAppliedAt(appliedDelta);
        } catch (InvalidProtocolBufferException e) {
          markStateCorrupted();
          throw new WaveServerException(
              "Authoritative server sent delta with badly formed original wavelet delta", e);
        }

        pendingDeltas.put(appliedAt, appliedDelta);
      }

      // Traverse pendingDeltas while we have any to process.
      ImmutableList.Builder<WaveletDeltaRecord> resultingDeltas = ImmutableList.builder();
      while (pendingDeltas.size() > 0) {
        Map.Entry<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>> first =
            pendingDeltas.firstEntry();
        HashedVersion appliedAt = first.getKey();
        ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta = first.getValue();

        if(LOG.isInfoLoggable()) {
          LOG.info("pendingDeltas.size(): " + Integer.toString(pendingDeltas.size()));
          LOG.info("current appliedAt: " + appliedAt.getVersion() + " expected: " + expectedVersion.getVersion());
        }

        // If we don't have the right version it implies there is a history we need, so set up a
        // callback to request it and fall out of this update
        if (appliedAt.getVersion() > expectedVersion.getVersion()) {
          LOG.info("Missing history from " + expectedVersion.getVersion() + "-"
              + appliedAt.getVersion() + ", requesting from upstream for " + getWaveletName());

          if (federationProvider != null) {
            // TODO: only one request history should be pending at any one time?
            // We should derive a new one whenever the active one is finished,
            // based on the current state of pendingDeltas.
            federationProvider.requestHistory(getWaveletName(), domain,
                CoreWaveletOperationSerializer.serialize(expectedVersion),
                CoreWaveletOperationSerializer.serialize(appliedAt),
                -1,
                new HistoryResponseListener() {
                    @Override
                    public void onFailure(FederationError error) {
                      LOG.severe("Callback failure: " + error);
                    }

                    @Override
                    public void onSuccess(List<ByteString> deltaList,
                        ProtocolHashedVersion lastCommittedVersion, long versionTruncatedAt) {
                      LOG.info("Got response callback: " + getWaveletName() + ", lcv "
                          + lastCommittedVersion + " deltaList length = " + deltaList.size());

                      // Try updating again with the new history
                      internalUpdate(deltaList, domain, federationProvider, certificateManager,
                          futureResult);
                    }
                });
            haveRequestedHistory = true;
          } else {
            LOG.severe("History request resulted in non-contiguous deltas!");
          }
          break;
        }

        // This delta is at the correct (current) version - apply it.
        if (appliedAt.getVersion() == expectedVersion.getVersion()) {
          // Confirm that the applied at hash matches the expected hash.
          if (!appliedAt.equals(expectedVersion)) {
            markStateCorrupted();
            throw new WaveServerException("Incoming delta applied at version "
                + appliedAt.getVersion() + " is not applied to the correct hash");
          }

          LOG.info("Applying delta for version " + appliedAt.getVersion());
          try {
            WaveletDeltaRecord applicationResult = transformAndApplyRemoteDelta(appliedDelta);
            long opsApplied = applicationResult.getResultingVersion().getVersion()
                    - expectedVersion.getVersion();
            if (opsApplied != appliedDelta.getMessage().getOperationsApplied()) {
              throw new OperationException("Operations applied here do not match the authoritative"
                  + " server claim (got " + opsApplied + ", expected "
                  + appliedDelta.getMessage().getOperationsApplied() + ".");
            }
            // Add transformed result to return list.
            resultingDeltas.add(applicationResult);
            LOG.fine("Applied delta: " + appliedDelta);
          } catch (OperationException e) {
            markStateCorrupted();
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          } catch (InvalidProtocolBufferException e) {
            markStateCorrupted();
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          } catch (InvalidHashException e) {
            markStateCorrupted();
            throw new WaveServerException("Couldn't apply authoritative delta", e);
          }

          // TODO: does waveletData update?
          expectedVersion = getCurrentVersion();
        } else {
          LOG.warning("Got delta from the past: " + appliedDelta);
        }

        pendingDeltas.remove(appliedAt);
      }

      commitAndNotifyResultingDeltas(resultingDeltas, futureResult);

    } catch (WaveServerException e) {
      LOG.warning("Update failure", e);
      // TODO(soren): make everyone throw FederationException instead
      // of WaveServerException so we don't have to translate between them here
      futureResult.setException(
          new FederationException(FederationErrors.badRequest(e.getMessage())));
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Commits the resulting deltas, notifying the server of them.
   * Assumes that everything in resultingDeltas is now in-order, since
   * even if the original stream was non-contiguous, we have requestedHistory.
   * Even if not, it is still safe to commit up to the fragmented point.
   */
  private void commitAndNotifyResultingDeltas(
      ImmutableList.Builder<WaveletDeltaRecord> resultingDeltas,
      final SettableFuture<Void> futureResult) {
    if(!resultingDeltas.build().isEmpty()) {
      notifyOfDeltas(resultingDeltas.build(), ImmutableSet.<String>of());
      futureResult.set(null);

      //Attempt to run any pending commit
      if(pendingCommit) {
        releaseWriteLock();
        commit(pendingCommitVersion);
        acquireWriteLock();
      }
    } else {
      LOG.info("No deltas in list (fetching history?), ignoring callback");
    }
  }

  /**
   * Apply a serialised applied delta to a remote wavelet. This assumes the
   * caller has validated that the delta is at the correct version and can be
   * applied to the wavelet. Must be called with writelock held.
   *
   * @param appliedDelta that is to be applied to the wavelet in its serialised form
   * @return the transformed and applied delta.
   * @throws AccessControlException if the supplied Delta's historyHash does not
   *         match the canonical history.
   * @throws WaveServerException if the delta transforms away.
   */
  private WaveletDeltaRecord transformAndApplyRemoteDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta) throws OperationException,
      AccessControlException, InvalidHashException, InvalidProtocolBufferException,
      WaveServerException {
    // The serialised hashed version should actually match the currentVersion at this point, since
    // the caller of transformAndApply delta will have made sure the applied deltas are ordered
    HashedVersion hashedVersion = AppliedDeltaUtil.getHashedVersionAppliedAt(appliedDelta);
    Preconditions.checkState(hashedVersion.equals(getCurrentVersion()),
        "Applied delta must apply to current version");

    // Extract the serialised wavelet delta
    ByteStringMessage<ProtocolWaveletDelta> protocolDelta =
        ByteStringMessage.parseProtocolWaveletDelta(
            appliedDelta.getMessage().getSignedOriginalDelta().getDelta());
    WaveletDelta delta = CoreWaveletOperationSerializer.deserialize(protocolDelta.getMessage());

    // Transform operations against earlier deltas, if necessary
    WaveletDelta transformed = maybeTransformSubmittedDelta(delta);
    if (transformed.getTargetVersion().equals(delta.getTargetVersion())) {
      // No transformation took place.
      // As a sanity check, the hash from the applied delta should NOT be set (an optimisation, but
      // part of the protocol).
      if (appliedDelta.getMessage().hasHashedVersionAppliedAt()) {
        LOG.warning("Hashes are the same but applied delta has hashed_version_applied_at");
        // TODO: re-enable this exception for version 0.3 of the spec
//        throw new InvalidHashException("Applied delta and its contained delta have same hash");
      }
    }

    if (transformed.size() == 0) {
      // The host shouldn't be forwarding empty deltas!
      markStateCorrupted();
      throw new WaveServerException("Couldn't apply authoritative delta, " +
          "it transformed away at version " + transformed.getTargetVersion().getVersion());
    }

    if (!transformed.getTargetVersion().equals(hashedVersion)) {
      markStateCorrupted();
      throw new WaveServerException("Couldn't apply authoritative delta, " +
          "it transformed to wrong version. Expected " + hashedVersion +
          ", actual " + transformed.getTargetVersion().getVersion());
    }

    // Apply the delta to the local wavelet state.
    // This shouldn't fail since the delta is from the authoritative server, so if it fails
    // then the wavelet is corrupted (and the caller of this method will sort it out).
    return applyDelta(appliedDelta, transformed);
  }
}
