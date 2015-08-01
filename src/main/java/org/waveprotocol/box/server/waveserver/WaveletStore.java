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

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.FileNotFoundPersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Stores wavelets.
 *
 * <p>
 * Callers must serialize all calls to {@link #open(WaveletName)} and
 * {@link #delete(WaveletName)} on the same wavelet.
 *
 * @param <T> wavelet handle data type
 *
 * @author soren@google.com (Soren Lassen)
 */
public interface WaveletStore<T extends WaveletDeltaRecordReader> {

  /**
   * Opens a wavelet, which can be used to store wavelet state. If the wavelet
   * doesn't exist, it is implicitly created when the first op is appended to
   * it.
   *
   * @throws PersistenceException if anything goes wrong with the underlying
   *         storage.
   */
  T open(WaveletName waveletName) throws PersistenceException;

  /**
   * Deletes a non-empty wavelet.
   * 
   * @throws PersistenceException if anything goes wrong with the underlying
   *         storage.
   * @throws FileNotFoundPersistenceException if the the wavelet does not exist
   *         in the delta store.
   */
  void delete(WaveletName waveletName) throws PersistenceException,
      FileNotFoundPersistenceException;

  /**
   * Looks up all non-empty wavelets in the store.
   *
   * @throws PersistenceException if anything goes wrong with the underlying
   *         storage.
   */
  ImmutableSet<WaveletId> lookup(WaveId waveId) throws PersistenceException;

  /**
   * Return an {@link ExceptionalIterator} that throws a {@link PersistenceException} if something
   * goes wrong while iterating over the store's wave IDs. This will only return the ids of waves
   * that have at least one non-empty wavelet.
   * 
   * The results returned from the iterator may or may not reflect concurrent modifications.
   * the iterator itself is NOT thread safe and should only be used by one thread.
   * 
   * @throws PersistenceException if anything goes wrong with the underlying
   *         storage while creating the iterator.
   */
  ExceptionalIterator<WaveId, PersistenceException> getWaveIdIterator() throws PersistenceException;
}
