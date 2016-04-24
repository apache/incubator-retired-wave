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
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.FileNotFoundPersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Wave store backed by a {@link DeltaStore}.
 *
 * @author soren@google.com (Soren Lassen)
 */
class DeltaStoreBasedSnapshotStore implements DeltaAndSnapshotStore {

  /**
   * Reads the transformed deltas from a {@link WaveletDeltaRecordReader}.
   */
  private static class TransformedWaveletDeltaIterator
      implements Iterator<TransformedWaveletDelta> {
    private final WaveletDeltaRecordReader reader;
    private long nextVersion = 0;

    public TransformedWaveletDeltaIterator(WaveletDeltaRecordReader reader) {
      this.reader = reader;
    }

    @Override
    public boolean hasNext() {
      return !reader.isEmpty() && nextVersion < reader.getEndVersion().getVersion();
    }

    /**
     * {@inheritDoc}
     *
     * @throws RuntimeIOException if the underlying reader throws
     *         {@link IOException}
     * @throws IllegalStateException if there are gaps between deltas or
     *         the next delta is empty
     */
    @Override
    public TransformedWaveletDelta next() {
      try {
        TransformedWaveletDelta delta = reader.getTransformedDelta(nextVersion);
        Preconditions.checkState(delta != null, "no delta at version %s", nextVersion);
        Preconditions.checkState(
            delta.getAppliedAtVersion() < delta.getResultingVersion().getVersion(),
            "delta [%s, %s) is empty", delta.getAppliedAtVersion(), delta.getResultingVersion());
        nextVersion = delta.getResultingVersion().getVersion();
        return delta;
      } catch (IOException e) {
        throw new RuntimeIOException(e);
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Reads all deltas and applies them all to construct the end wavelet state.
   */
  private static ReadableWaveletData buildWaveletFromDeltaReader(WaveletDeltaRecordReader reader)
      throws PersistenceException {
    try {
      // TODO(soren): better error handling of IllegalStateExceptions and
      // OperationExceptions thrown from here
      ReadableWaveletData wavelet =
          WaveletDataUtil.buildWaveletFromDeltas(reader.getWaveletName(),
              new TransformedWaveletDeltaIterator(reader));
      Preconditions.checkState(wavelet.getHashedVersion().equals(reader.getEndVersion()));
      return wavelet;
    } catch (OperationException e) {
      throw new PersistenceException(e);
    } catch (RuntimeIOException e) {
      throw new PersistenceException(e.getIOException());
    }
  }

  /**
   * Creates a {@link DeltaAndSnapshotStore.WaveletAccess} instance which wraps
   * {@code deltasAccess}.
   *
   * @throws IllegalStateException if the delta history is bad
   */
  private static WaveletAccess createWaveletAccess(DeltaStore.DeltasAccess deltasAccess)
      throws PersistenceException {
    ReadableWaveletData wavelet;
    wavelet = deltasAccess.isEmpty() ? null : buildWaveletFromDeltaReader(deltasAccess);
    return new DeltasAccessBasedWaveletAccess(deltasAccess, wavelet);
  }

  /**
   * Wraps {@link DeltaStore.DeltasAccess}.
   */
  static class DeltasAccessBasedWaveletAccess extends ForwardingWaveletDeltaRecordReader
      implements WaveletAccess {

    private final DeltaStore.DeltasAccess deltasAccess;

    // TODO(soren): figure out thread safe access
    // (synchronize access to snapshot, isClosed? or make them atomic types?)

    private ReadableWaveletData snapshot; // is null when there are no deltas
    private boolean isClosed = false;

    private DeltasAccessBasedWaveletAccess(DeltaStore.DeltasAccess deltasAccess,
        ReadableWaveletData snapshot) {
      this.deltasAccess = deltasAccess;
      this.snapshot = snapshot;
    }

    @Override
    protected WaveletDeltaRecordReader delegate() {
      Preconditions.checkState(!isClosed, "Illegal access after closure");
      return deltasAccess;
    }

    @Override
    public boolean isEmpty() {
      // Don't use the underlying deltasAccess method, rather let this
      // be controlled by our own state.
      return getSnapshot() == null;
    }

    @Override
    public HashedVersion getEndVersion() {
      // Don't use the underlying deltasAccess method, rather let this
      // be controlled by our own state.
      return getSnapshot().getHashedVersion();
    }

    @Override
    public ReadableWaveletData getSnapshot() {
      Preconditions.checkState(!isClosed, "Illegal access after closure");
      return snapshot;
    }

    @Override
    public void appendDeltas(Collection<WaveletDeltaRecord> deltas,
        ReadableWaveletData resultingSnapshot) throws PersistenceException {
      Preconditions.checkState(!isClosed, "Illegal access after closure");
      // First append the deltas.
      deltasAccess.append(deltas);
      // Once the deltas have been stored, we update the wavelet data, which
      // affects the result of any calls to getEndVersion() or getSnapshot().
      snapshot = resultingSnapshot;
    }

    @Override
    public void close() throws IOException {
      // We don't check if we're already closed, it's ok to call close() twice.
      isClosed = true;
      deltasAccess.close();
    }
  }

  private final DeltaStore deltaStore;

  /**
   * Constructs a {@link DeltaAndSnapshotStore} instance which wraps {@code deltaStore}.
   *
   * @param deltaStore The underlying {@link DeltaStore}.
   */
  @Inject
  public DeltaStoreBasedSnapshotStore(DeltaStore deltaStore) {
    this.deltaStore = deltaStore;
  }

  @Override
  public WaveletAccess open(WaveletName waveletName) throws PersistenceException {
    return createWaveletAccess(deltaStore.open(waveletName));
  }

  @Override
  public void delete(WaveletName waveletName) throws PersistenceException,
      FileNotFoundPersistenceException {
    deltaStore.delete(waveletName);
  }

  @Override
  public ImmutableSet<WaveletId> lookup(WaveId waveId) throws PersistenceException {
    return deltaStore.lookup(waveId);
  }

  @Override
  public ExceptionalIterator<WaveId, PersistenceException> getWaveIdIterator()
      throws PersistenceException {
    return deltaStore.getWaveIdIterator();
  }
}
