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

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.io.Closeable;
import java.util.Collection;

/**
 * Stores wavelet deltas and snapshots.
 *
 * @author soren@google.com (Soren Lassen)
 */
public interface DeltaAndSnapshotStore extends WaveletStore<DeltaAndSnapshotStore.WaveletAccess> {

  /**
   * Accesses the state for a wavelet.
   * Permits reading a snapshot of the wavelet state and historical deltas,
   * and appending deltas to the history.
   *
   * Callers must serialize all calls to
   * {@link #appendDeltas(Collection,ReadableWaveletData)}.
   */
  interface WaveletAccess extends WaveletDeltaRecordReader, Closeable {

    /**
     * @return Immutable copy of the last known committed state of the wavelet.
     */
    ReadableWaveletData getSnapshot();

    /**
     * Blocking call to append deltas to the end of the delta history.
     * If the call returns normally (doesn't throw an exception), then
     * the deltas have been successfully and "durably" stored, that is,
     * the method forces the data to disk.
     *
     * @param deltas Contiguous deltas, beginning from the DeltaAccess object's
     *        end version. It is the caller's responsibility to ensure that
     *        everything matches up (applied and transformed deltas in the
     *        records match, that the hashes are correctly computed, etc).
     * @param resultingSnapshot A snapshot of the state of the wavelet after
     *        {@code deltas} are applied.
     * @throws PersistenceException if anything goes wrong with the underlying
     *         storage.
     */
    void appendDeltas(Collection<WaveletDeltaRecord> deltas,
        ReadableWaveletData resultingSnapshot) throws PersistenceException;
  }
}
