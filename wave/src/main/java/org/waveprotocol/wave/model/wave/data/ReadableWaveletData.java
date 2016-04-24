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

package org.waveprotocol.wave.model.wave.data;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

/**
 * A readable version of WaveletData.
 */
public interface ReadableWaveletData {

  /**
   * Factory for constructing wavelet data copies.
   *
   * @param <T> type constructed by this factory
   */
  interface Factory<T extends ReadableWaveletData> {
    /**
     * @return a copy of {@code data}.
     */
    T create(ReadableWaveletData data);
  }

  /**
   * Gets this wavelet's document identified by the provided name.
   *
   * @return the requested document.
   */
  ReadableBlipData getDocument(String documentName);

  /**
   * Gets a set of the ids of all non-empty documents in this wavelet.
   */
  Set<String> getDocumentIds();

  /**
   * Gets the participant that created this wavelet. The creator is immutable.
   * This method is safe to call even in the presence of concurrent calls
   * modifying the same {@link ReadableWaveletData} instance.
   * 
   * @return the wavelet's creator.
   */
  ParticipantId getCreator();

  /**
   * An immutable list of this wave's participants, in the order in which
   * they were added.
   *
   * @return the wavelet's participants.
   */
  Set<ParticipantId> getParticipants();

  /**
   * Gets the latest version number of this wavelet known to exist at the server.
   *
   * @return the wavelet's version.
   */
  long getVersion();

  /**
   * Gets the epoch time at which the wavelet was created.
   *
   * @return the creation time.
   */
  long getCreationTime();

  /**
   * Gets the epoch time of the last modification made to this wavelet
   * (including any of its documents).
   *
   * @return the wavelet's last-modified time.
   */
  long getLastModifiedTime();

  /**
   * Gets the latest distinct version number of this wavelet known to exist at the
   * server. The version number may be earlier than {@link #getVersion()}.
   *
   * @return the wavelet's distinct version
   */
  HashedVersion getHashedVersion();

  /**
   * Gets the id of the wave containing this wavelet.
   *
   * @return the id of the containing wave.
   */
  WaveId getWaveId();

  /**
   * Gets the identifier of this wavelet. Wavelet ids are unique within a wave.
   *
   * @return the unique identifier of this wavelet.
   */
  WaveletId getWaveletId();
}
