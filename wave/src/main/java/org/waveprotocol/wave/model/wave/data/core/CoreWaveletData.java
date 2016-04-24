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

package org.waveprotocol.wave.model.wave.data.core;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.Map;

/**
 * Defines the abstract data type used to describe the content of a wavelet.
 *
 *
 *
 *
 */
public interface CoreWaveletData {
  /**
   * Constructs an unmodifiable map view mapping the ids of all non-empty
   * documents in this wavelet to the corresponding document.
   */
  // TODO: Make this code use BufferedDocInitializations.
  Map<String, DocOp> getDocuments();

  /**
   * An immutable list of this wave's participants, in the order in which
   * they were added.
   *
   * @return the wave's participants.
   */
  List<ParticipantId> getParticipants();

  /**
   * Gets the {@link WaveletName} that uniquely identifies this wavelet.
   *
   * @return the name of this wavelet
   */
  WaveletName getWaveletName();

  /**
   * Adds a participant to this wave, ensuring it is in the participants
   * collection. The new participant is added to the end of the collection if it
   * was not already present.
   *
   * @param participant participant to add
   * @return false if the given participant was already a participant of this
   *         wavelet.
   */
  boolean addParticipant(ParticipantId participant);

  /**
   * Removes a participant from this wave, ensuring it is no longer reflected in
   * the participants collection.
   *
   * @param participant participant to remove
   * @return false if the given participant was not a participant of this
   *         wavelet.
   */
  boolean removeParticipant(ParticipantId participant);

  /**
   * Modify the document contained in the wavelet. If a document with the
   * given id did not previously exist, it will be created.
   */
  boolean modifyDocument(String documentId, DocOp op) throws OperationException;
}
