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

package org.waveprotocol.wave.model.wave.data.core.impl;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the abstract data type used to describe the content of a wavelet.
 */
public class CoreWaveletDataImpl implements CoreWaveletData {

  /** A document (operation) with no contents. */
  private static final DocOp EMPTY_DOC_OP = new DocOpBuffer().finish();

  /** Id of the wave to which this wavelet belongs. */
  private final WaveId waveId;

  /** The identifier of this wavelet. */
  private final WaveletId waveletId;

  /** The list of participants in this wavelet. */
  private final List<ParticipantId> participants;

  /** The set of documents in this wave, indexed by their identifier. */
  private final Map<String, DocOp> documents;

  /**
   * Creates a new wavelet.
   *
   * @param waveId id of the wave containing the wavelet
   * @param waveletId id of the wavelet
   * @throws IllegalArgumentException if the wavelet name is bad.
   */
  public CoreWaveletDataImpl(WaveId waveId, WaveletId waveletId) {
    if (waveletId == null) {
      throw new IllegalArgumentException("wavelet id cannot be null");
    } else if (waveId == null) {
      throw new IllegalArgumentException("wave id cannot be null");
    }

    this.waveletId = waveletId;
    this.waveId = waveId;
    this.participants = new ArrayList<ParticipantId>();
    this.documents = new HashMap<String, DocOp>();
  }

  private DocOp getOrCreateDocument(String documentId) {
    DocOp doc = documents.get(documentId);
    if (doc == null) {
      doc = EMPTY_DOC_OP;
      documents.put(documentId, doc);
    }
    return doc;
  }

  @Override
  public Map<String, DocOp> getDocuments() {
    return Collections.unmodifiableMap(documents);
  }

  @Override
  public List<ParticipantId> getParticipants() {
    return Collections.unmodifiableList(participants);
  }

  @Override
  public WaveletName getWaveletName() {
    return WaveletName.of(waveId, waveletId);
  }

  @Override
  public boolean addParticipant(ParticipantId p) {
    return (participants.contains(p) ? false : participants.add(p));
  }

  @Override
  public boolean removeParticipant(ParticipantId p) {
    return participants.remove(p);
  }

  @Override
  public boolean modifyDocument(String documentId, DocOp operation)
      throws OperationException {
    DocOp newDoc = Composer.compose(getOrCreateDocument(documentId), operation);
    if (OpComparators.SYNTACTIC_IDENTITY.equal(newDoc, EMPTY_DOC_OP)) {
      documents.remove(documentId);
    } else {
      documents.put(documentId, newDoc);
    }
    return true;
  }

  @Override
  public String toString() {
    return "Wavelet State = " + waveId + " " + waveletId + " " + documents;
  }
}
