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

import org.waveprotocol.box.common.Receiver;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.common.ListReceiver;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.Transform;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

/**
 * Contains the history of a wavelet - applied and transformed deltas plus the
 * content of the wavelet.
 *
 * TODO(soren): Unload the wavelet (remove it from WaveMap) if it becomes
 * corrupt or fails to load from storage.
 */
abstract class WaveletContainerImpl implements WaveletContainer {

  private static final Log LOG = Log.get(WaveletContainerImpl.class);

  private static final int AWAIT_LOAD_TIMEOUT_SECONDS = 1000;

  protected enum State {
    /** Everything is working fine. */
    OK,

    /** Wavelet state is being loaded from storage. */
    LOADING,

    /** Wavelet has been deleted, the instance will not contain any data. */
    DELETED,

    /**
     * For some reason this instance is broken, e.g. a remote wavelet update
     * signature failed.
     */
    CORRUPTED
  }

  private final Executor storageContinuationExecutor;

  private final Lock readLock;
  private final ReentrantReadWriteLock.WriteLock writeLock;
  private final WaveletName waveletName;
  private final WaveletNotificationSubscriber notifiee;
  private final ParticipantId sharedDomainParticipantId;
  /** Is counted down when initial loading from storage completes. */
  private final CountDownLatch loadLatch = new CountDownLatch(1);
  /** Is set at most once, before loadLatch is counted down. */
  private WaveletState waveletState;
  private State state = State.LOADING;

  /**
   * Constructs an empty WaveletContainer for a wavelet.
   * WaveletData is not set until a delta has been applied.
   *
   * @param notifiee the subscriber to notify of wavelet updates and commits.
   * @param waveletState the wavelet's delta history and current state.
   * @param waveDomain the wave server domain.
   * @param storageContinuationExecutor the executor used to perform post wavelet loading logic.
   */
  public WaveletContainerImpl(WaveletName waveletName, WaveletNotificationSubscriber notifiee,
      final ListenableFuture<? extends WaveletState> waveletStateFuture, String waveDomain,
      Executor storageContinuationExecutor) {
    this.waveletName = waveletName;
    this.notifiee = notifiee;
    this.sharedDomainParticipantId =
        waveDomain != null ? ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(waveDomain)
            : null;
    this.storageContinuationExecutor = storageContinuationExecutor;
    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();

    waveletStateFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            acquireWriteLock();
            try {
              Preconditions.checkState(waveletState == null,
                  "Repeat attempts to set wavelet state");
              Preconditions.checkState(state == State.LOADING, "Unexpected state %s", state);
              waveletState = FutureUtil.getResultOrPropagateException(
                  waveletStateFuture, PersistenceException.class);
              Preconditions.checkState(waveletState.getWaveletName().equals(getWaveletName()),
                  "Wrong wavelet state, named %s, expected %s",
                  waveletState.getWaveletName(), getWaveletName());
              state = State.OK;
            } catch (PersistenceException e) {
              LOG.warning("Failed to load wavelet " + getWaveletName(), e);
              state = State.CORRUPTED;
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              LOG.warning("Interrupted loading wavelet " + getWaveletName(), e);
              state = State.CORRUPTED;
            } catch (RuntimeException e) {
              // TODO(soren): would be better to terminate the process in this case
              LOG.severe("Unexpected exception loading wavelet " + getWaveletName(), e);
              state = State.CORRUPTED;
            } finally {
              releaseWriteLock();
            }
            loadLatch.countDown();
          }
        },
        storageContinuationExecutor);
  }

  protected void acquireReadLock() {
    readLock.lock();
  }

  protected void releaseReadLock() {
    readLock.unlock();
  }

  protected void acquireWriteLock() {
    writeLock.lock();
  }

  protected void releaseWriteLock() {
    writeLock.unlock();
  }

  protected void notifyOfDeltas(ImmutableList<WaveletDeltaRecord> deltas,
      ImmutableSet<String> domainsToNotify) {
    Preconditions.checkState(writeLock.isHeldByCurrentThread(), "must hold write lock");
    Preconditions.checkArgument(!deltas.isEmpty(), "empty deltas");
    HashedVersion endVersion = deltas.get(deltas.size() - 1).getResultingVersion();
    HashedVersion currentVersion = getCurrentVersion();
    Preconditions.checkArgument(endVersion.equals(currentVersion),
        "cannot notify of deltas ending in %s != current version %s", endVersion, currentVersion);
    notifiee.waveletUpdate(waveletState.getSnapshot(), deltas, domainsToNotify);
  }

  protected void notifyOfCommit(HashedVersion version, ImmutableSet<String> domainsToNotify) {
    Preconditions.checkState(writeLock.isHeldByCurrentThread(), "must hold write lock");
    notifiee.waveletCommitted(getWaveletName(), version, domainsToNotify);
  }

  /**
   * Blocks until the initial load of the wavelet state from storage completes.
   * Should be called without the read or write lock held.
   *
   * @throws WaveletStateException if the wavelet fails to load,
   *         either because of a storage access failure or timeout,
   *         or because the current thread is interrupted.
   */
  protected void awaitLoad() throws WaveletStateException {
    Preconditions.checkState(!writeLock.isHeldByCurrentThread(), "should not hold write lock");
    try {
      if (!loadLatch.await(AWAIT_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new WaveletStateException("Timed out waiting for wavelet to load");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WaveletStateException("Interrupted waiting for wavelet to load");
    }
  }

  /**
   * Verifies that the wavelet is in an operational state (not loading,
   * not corrupt).
   *
   * Should be preceded by a call to awaitLoad() so that the initial load from
   * storage has completed. Should be called with the read or write lock held.
   *
   * @throws WaveletStateException if the wavelet is loading or marked corrupt.
   */
  protected void checkStateOk() throws WaveletStateException {
    if (state != State.OK) {
      throw new WaveletStateException("The wavelet is in an unusable state: " + state);
    }
  }

  /**
   * Flags the wavelet corrupted so future calls to checkStateOk() will fail.
   */
  protected void markStateCorrupted() {
    Preconditions.checkState(writeLock.isHeldByCurrentThread(), "must hold write lock");
    state = State.CORRUPTED;
  }

  protected void persist(final HashedVersion version, final ImmutableSet<String> domainsToNotify) {
    Preconditions.checkState(writeLock.isHeldByCurrentThread(), "must hold write lock");
    final ListenableFuture<Void> result = waveletState.persist(version);
    result.addListener(
        new Runnable() {
          @Override
          public void run() {
            try {
              result.get();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
              LOG.severe("Version " + version, e);
            }
            acquireWriteLock();
            try {
              waveletState.flush(version);
              notifyOfCommit(version, domainsToNotify);
            } finally {
              releaseWriteLock();
            }
          }
        },
        storageContinuationExecutor);
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public boolean checkAccessPermission(ParticipantId participantId) throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      // ParticipantId will be null if the user isn't logged in. A user who isn't logged in should
      // have access to public waves once they've been implemented.
      // If the wavelet is empty, everyone has access (to write the first delta).
      // TODO(soren): determine if off-domain participants should be denied access if empty
      ReadableWaveletData snapshot = waveletState.getSnapshot();
      return WaveletDataUtil.checkAccessPermission(snapshot, participantId, sharedDomainParticipantId);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public HashedVersion getLastCommittedVersion() throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      return waveletState.getLastPersistedVersion();
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public ObservableWaveletData copyWaveletData() throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      return WaveletDataUtil.copyWavelet(waveletState.getSnapshot());
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public CommittedWaveletSnapshot getSnapshot() throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      return new CommittedWaveletSnapshot(waveletState.getSnapshot(),
          waveletState.getLastPersistedVersion());
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public <T> T applyFunction(Function<ReadableWaveletData, T> function)
      throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      return function.apply(waveletState.getSnapshot());
    } finally {
      releaseReadLock();
    }
  }

  /**
   * Transform a wavelet delta if it has been submitted against a different head (currentVersion).
   * Must be called with write lock held.
   *
   * @param delta to possibly transform
   * @return the transformed delta and the version it was applied at
   *   (the version is the current version of the wavelet, unless the delta is
   *   a duplicate in which case it is the version at which it was originally
   *   applied)
   * @throws InvalidHashException if submitting against same version but different hash
   * @throws OperationException if transformation fails
   */
  protected WaveletDelta maybeTransformSubmittedDelta(WaveletDelta delta)
      throws InvalidHashException, OperationException {
    HashedVersion targetVersion = delta.getTargetVersion();
    HashedVersion currentVersion = getCurrentVersion();
    if (targetVersion.equals(currentVersion)) {
      // Applied version is the same, we're submitting against head, don't need to do OT
      return delta;
    } else {
      // Not submitting against head, we need to do OT, but check the versions really are different
      if (targetVersion.getVersion() == currentVersion.getVersion()) {
        LOG.warning("Mismatched hash, expected " + currentVersion + ") but delta targets (" +
            targetVersion + ")");
        throw new InvalidHashException(currentVersion, targetVersion);
      } else {
        return transformSubmittedDelta(delta);
      }
    }
  }

  /**
   * Finds range of server deltas needed to transform against, then transforms all client
   * ops against the server ops.
   */
  private WaveletDelta transformSubmittedDelta(WaveletDelta submittedDelta)
      throws OperationException, InvalidHashException {
    HashedVersion targetVersion = submittedDelta.getTargetVersion();
    HashedVersion currentVersion = getCurrentVersion();
    Preconditions.checkArgument(!targetVersion.equals(currentVersion));
    ListReceiver<TransformedWaveletDelta> receiver = new ListReceiver<TransformedWaveletDelta>();
    waveletState.getTransformedDeltaHistory(targetVersion, currentVersion, receiver);
    DeltaSequence serverDeltas = DeltaSequence.of(receiver);
    Preconditions.checkState(!serverDeltas.isEmpty(),
        "No deltas between valid versions %s and %s", targetVersion, currentVersion);

    ParticipantId clientAuthor = submittedDelta.getAuthor();
    // TODO(anorth): remove this copy somehow; currently, it's necessary to
    // ensure that clientOps.equals() works correctly below (because
    // WaveletDelta breaks the List.equals() contract)
    List<WaveletOperation> clientOps = Lists.newArrayList(submittedDelta);
    for (TransformedWaveletDelta serverDelta : serverDeltas) {
      // If the client delta transforms to nothing before we've traversed all
      // the server deltas, return the version at which the delta was
      // obliterated (rather than the current version) to ensure that delta
      // submission is idempotent.
      if (clientOps.isEmpty()) {
        return new WaveletDelta(clientAuthor, targetVersion, clientOps);
      }
      ParticipantId serverAuthor = serverDelta.getAuthor();
      if (clientAuthor.equals(serverAuthor) && clientOps.equals(serverDelta)) {
        // This is a duplicate of the server delta.
        return new WaveletDelta(clientAuthor, targetVersion, clientOps);
      }
      clientOps = transformOps(clientOps, serverDelta);
      targetVersion = serverDelta.getResultingVersion();
    }
    Preconditions.checkState(targetVersion.equals(currentVersion));
    return new WaveletDelta(clientAuthor, targetVersion, clientOps);
  }

  /**
   * Transforms the specified client operations against the specified server operations,
   * returning the transformed client operations in a new list.
   *
   * @param clientOps may be unmodifiable
   * @param serverOps may be unmodifiable
   * @return transformed client ops
   */
  private List<WaveletOperation> transformOps(List<WaveletOperation> clientOps,
      List<WaveletOperation> serverOps) throws OperationException {
    List<WaveletOperation> transformedClientOps = Lists.newArrayList();

    for (WaveletOperation c : clientOps) {
      for (WaveletOperation s : serverOps) {
        OperationPair<WaveletOperation> pair;
        try {
          pair = Transform.transform(c, s);
        } catch (TransformException e) {
          throw new OperationException(e);
        }
        c = pair.clientOp();
      }
      transformedClientOps.add(c);
    }
    return transformedClientOps;
  }

  /**
   * Builds a {@link WaveletDeltaRecord} and applies it to the wavelet container.
   * The delta must be non-empty.
   */
  protected WaveletDeltaRecord applyDelta(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta, WaveletDelta transformed)
      throws InvalidProtocolBufferException, OperationException {
    TransformedWaveletDelta transformedDelta =
        AppliedDeltaUtil.buildTransformedDelta(appliedDelta, transformed);

    WaveletDeltaRecord deltaRecord = new WaveletDeltaRecord(transformed.getTargetVersion(),
        appliedDelta, transformedDelta);
    waveletState.appendDelta(deltaRecord);

    return deltaRecord;
  }

  /**
   * @param versionActuallyAppliedAt the version to look up
   * @return the applied delta applied at the specified hashed version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDelta(
      HashedVersion versionActuallyAppliedAt) {
    return waveletState.getAppliedDelta(versionActuallyAppliedAt);
  }

  /**
   * @param endVersion the version to look up
   * @return the applied delta with the given resulting version
   */
  protected ByteStringMessage<ProtocolAppliedWaveletDelta> lookupAppliedDeltaByEndVersion(
      HashedVersion endVersion) {
    return waveletState.getAppliedDeltaByEndVersion(endVersion);
  }

  protected TransformedWaveletDelta lookupTransformedDelta(HashedVersion appliedAtVersion) {
    return waveletState.getTransformedDelta(appliedAtVersion);
  }

  /**
   * @throws AccessControlException with the given message if version does not
   *         match a delta boundary in the wavelet history.
   */
  private void checkVersionIsDeltaBoundary(HashedVersion version, String message)
      throws AccessControlException {
    HashedVersion actual = waveletState.getHashedVersion(version.getVersion());
    if (!version.equals(actual)) {
      LOG.info("Unrecognized " + message + " at version " + version + ", actual " + actual);
      // We omit the hash from the message to avoid leaking it.
      throw new AccessControlException(
          "Unrecognized " + message + " at version " + version.getVersion());
    }
  }

  @Override
  public void requestHistory(HashedVersion startVersion, HashedVersion endVersion,
      Receiver<ByteStringMessage<ProtocolAppliedWaveletDelta>> receiver)
      throws AccessControlException, WaveletStateException {
    acquireReadLock();
    try {
      checkStateOk();
      checkVersionIsDeltaBoundary(startVersion, "start version");
      checkVersionIsDeltaBoundary(endVersion, "end version");
      waveletState.getAppliedDeltaHistory(startVersion, endVersion, receiver);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public void requestTransformedHistory(HashedVersion startVersion, HashedVersion endVersion,
      Receiver<TransformedWaveletDelta> receiver) throws AccessControlException, WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      checkVersionIsDeltaBoundary(startVersion, "start version");
      checkVersionIsDeltaBoundary(endVersion, "end version");
      waveletState.getTransformedDeltaHistory(startVersion, endVersion, receiver);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public boolean hasParticipant(ParticipantId participant) throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      ReadableWaveletData snapshot = waveletState.getSnapshot();
      return snapshot != null && snapshot.getParticipants().contains(participant);
    } finally {
      releaseReadLock();
    }
  }

  @Override
  public ParticipantId getSharedDomainParticipant() {
    return sharedDomainParticipantId;
  }

  @Override
  public ParticipantId getCreator() {
    ReadableWaveletData snapshot = waveletState.getSnapshot();
    return snapshot != null ? snapshot.getCreator() : null;
  }

  @Override
  public boolean isEmpty() throws WaveletStateException {
    awaitLoad();
    acquireReadLock();
    try {
      checkStateOk();
      return waveletState.getSnapshot() == null;
    } finally {
      releaseReadLock();
    }
  }

  @Nullable
  protected HashedVersion getCurrentVersion() {
    if(waveletState == null)
      return null;

    return waveletState.getCurrentVersion();
  }

  protected ReadableWaveletData accessSnapshot() {
    return waveletState.getSnapshot();
  }
}
