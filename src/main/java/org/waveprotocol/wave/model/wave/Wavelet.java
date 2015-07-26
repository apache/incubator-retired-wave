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

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Set;

public interface Wavelet {
  /**
   * Gets the blips from this wavelet. The order of iteration is undefined but will
   * not change between calls if the wavelet is unchanged.
   */
  Iterable<? extends Blip> getBlips();

  /**
   * Gets a blip from this wavelet.
   *
   * @param id a blip id
   * @return blip identified by {@code id}, or null
   */
  Blip getBlip(String id);

  /**
   * Creates a new blip in this wavelet. It is expected that callers have a
   * globally-unique id generator, since, unlike {@link #getDocument(String)},
   * this method does not work correctly with concurrent creations of the same
   * blip.
   *
   * @param id id for the new blip
   * @return the new blip
   * @throws IllegalArgumentException if there is already a blip identified by
   *         {@code id}.
   */
  Blip createBlip(String id);

  /**
   * Retrieves the document contained within this wavelet with the given name,
   * if it supports observation. It is assumed that callers know which documents
   * support this capability. Creates an empty document if no such document has
   * yet been created.
   *
   * @param id id of the document to fetch
   * @return the appropriate document. Never returns null.
   * @throws IllegalArgumentException if document {@code id} does not support
   *         the {@link ObservableDocument} interface.
   */
  ObservableDocument getDocument(String id);

  /**
   * Retrieves a set of the names of all non-empty documents (including blip
   * documents) in this wavelet.
   */
  Set<String> getDocumentIds();

    /**
   * Adds a participant to this wavelet.  If {@code participant} is already a
   * participant on this wavelet, this method has no effect.
   *
   * @param participant  participant to add
   */
  void addParticipant(ParticipantId participant);

  /**
   * Removes a participant from this wavelet.  If {@code participant} is not a
   * participant on this wavelet, this method has no effect.  Otherwise, if the
   * removed participant is the user through which this wavelet is viewed, then
   * future modifications through this interface are invalid and will throw
   * exceptions.
   *
   * @param participant  participant to remove
   */
  void removeParticipant(ParticipantId participant);

  /**
   * Gets the epoch time at which this wavelet was created.
   *
   * @return the creation time
   */
  long getCreationTime();

  /**
   * Gets the id of the creator of the wavelet.
   * Note that the creator is not necessarily a participant.
   *
   * @return the id of the creator
   */
  ParticipantId getCreatorId();

  /**
   * Gets the epoch time of the last modification to this wavelet.
   *
   * @return the last-modified time
   */
  long getLastModifiedTime();

  /**
   * Gets the participants on this wavelet.
   * The returned set is ordered and not modifiable.
   *
   * @return this wavelet's participants
   */
  Set<ParticipantId> getParticipantIds();

  /**
   * Adds a set of participant ids to this conversation. Does nothing for
   * participants that are already participant on this conversation. Does
   * nothing if the participants set is {@code null}.
   *
   * @param participants the participant ids to add
   */
  void addParticipantIds(Set<ParticipantId> participants);

  /**
   * Gets the version number of this wavelet.
   *
   * @return this wavelet's version
   */
  long getVersion();

  /**
   * Gets the most recent hashed version of this wavelet. This is not guaranteed
   * to match the latest {@link #getVersion() version}.
   */
  HashedVersion getHashedVersion();

  /**
   * Gets the id of this wavelet.
   *
   * @return this wavelet's id
   */
  WaveletId getId();

  // TODO(hearnden/anorth): add getWaveView(). A Wavelet
  // (as opposed to a WaveletData) is ALWAYS in the context of a WaveView
  // and can not be reused across views.

  /**
   * Gets the id of the wave to which this wavelet belongs.
   *
   * @return this wavelet's wave id
   */
  WaveId getWaveId();

  ObservableWaveletData getWaveletData();
}
