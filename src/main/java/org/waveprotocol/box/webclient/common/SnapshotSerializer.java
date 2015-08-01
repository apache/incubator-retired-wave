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

package org.waveprotocol.box.webclient.common;

import org.waveprotocol.box.common.comms.DocumentSnapshot;
import org.waveprotocol.box.common.comms.WaveViewSnapshot;
import org.waveprotocol.box.common.comms.WaveletSnapshot;
import org.waveprotocol.box.common.comms.jso.DocumentSnapshotJsoImpl;
import org.waveprotocol.box.common.comms.jso.WaveletSnapshotJsoImpl;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.Collection;


/**
 * Utility class for serialising/deserialising model objects (and their
 * components) to/from their protocol buffer representations.
 *
 * NOTE: This class is mirrored in the server. Any changes here should also be
 * made in
 * {@link org.waveprotocol.box.server.common.SnapshotSerializer}
 *
 * @author Joseph Gentle (josephg@gmail.com)
 */
public class SnapshotSerializer {
  private SnapshotSerializer() {
  }

  /**
   * Serializes a snapshot for a wavelet.
   *
   * @param wavelet wavelet to snapshot
   * @param hashedVersion hashed version of the wavelet
   * @return a wavelet snapshot that contains all the information in the
   *         original wavelet.
   */
  public static WaveletSnapshot serializeWavelet(ReadableWaveletData wavelet,
      HashedVersion hashedVersion) {
    WaveletSnapshot builder = WaveletSnapshotJsoImpl.create();

    builder.setWaveletId(ModernIdSerialiser.INSTANCE.serialiseWaveletId(wavelet.getWaveletId()));
    for (ParticipantId participant : wavelet.getParticipants()) {
      builder.addParticipantId(participant.toString());
    }
    for (String id : wavelet.getDocumentIds()) {
      ReadableBlipData data = wavelet.getDocument(id);
      builder.addDocument(serializeDocument(data));
    }

    builder.setVersion(WaveletOperationSerializer.serialize(hashedVersion));
    builder.setLastModifiedTime(wavelet.getLastModifiedTime());
    builder.setCreator(wavelet.getCreator().getAddress());
    builder.setCreationTime(wavelet.getCreationTime());

    return builder;
  }

  /**
   * Deserializes the snapshot contained in the {@link WaveletSnapshot}
   * into a {@link WaveletData}.
   *
   * @param snapshot the {@link WaveletSnapshot} to deserialize.
   * @throws OperationException if the ops in the snapshot can not be applied.
   * @throws InvalidParticipantAddress
   * @throws InvalidIdException
   */
  public static ObservableWaveletData deserializeWavelet(WaveletSnapshot snapshot, WaveId waveId,
      DocumentFactory<?> docFactory) throws OperationException, InvalidParticipantAddress,
      InvalidIdException {
    ObservableWaveletData.Factory<? extends ObservableWaveletData> factory
        = WaveletDataImpl.Factory.create(docFactory);

    ParticipantId author = ParticipantId.of(snapshot.getCreator());
    WaveletId waveletId =
        ModernIdSerialiser.INSTANCE.deserialiseWaveletId(snapshot.getWaveletId());
    long creationTime = snapshot.getCreationTime();

    ObservableWaveletData wavelet = factory.create(new EmptyWaveletSnapshot(waveId, waveletId,
            author, WaveletOperationSerializer.deserialize(snapshot.getVersion()),
            creationTime));

    for (String participant : snapshot.getParticipantId()) {
      wavelet.addParticipant(ParticipantId.of(participant));
    }

    for (DocumentSnapshot document : snapshot.getDocument()) {
      addDocumentSnapshotToWavelet(document, wavelet);
    }

    wavelet.setVersion((long) snapshot.getVersion().getVersion());
    wavelet.setLastModifiedTime(snapshot.getLastModifiedTime());
    // The creator and creation time are set when the empty wavelet template is
    // created above.

    return wavelet;
  }

  /**
   * Serializes a document to a document snapshot.
   *
   * @param document The document to serialize
   * @return A snapshot of the given document
   */
  public static DocumentSnapshot serializeDocument(ReadableBlipData document) {
    DocumentSnapshot builder = DocumentSnapshotJsoImpl.create();

    builder.setDocumentId(document.getId());
    builder.setDocumentOperation(WaveletOperationSerializer.serialize(
        document.getContent().asOperation()));

    builder.setAuthor(document.getAuthor().getAddress());
    for (ParticipantId participant : document.getContributors()) {
      builder.addContributor(participant.getAddress());
    }
    builder.setLastModifiedVersion(document.getLastModifiedVersion());
    builder.setLastModifiedTime(document.getLastModifiedTime());

    return builder;
  }

  private static void addDocumentSnapshotToWavelet(
      DocumentSnapshot snapshot, WaveletData container) throws InvalidParticipantAddress {
    DocOp op = WaveletOperationSerializer.deserialize(snapshot.getDocumentOperation());
    DocInitialization docInit = DocOpUtil.asInitialization(op);

    Collection<ParticipantId> contributors = CollectionUtils.newArrayList();
    for (String p : snapshot.getContributor()) {
      contributors.add(ParticipantId.of(p));
    }
    container.createDocument(
        snapshot.getDocumentId(),
        new ParticipantId(snapshot.getAuthor()),  // We trust the server's snapshot
        contributors,
        docInit,
        snapshot.getLastModifiedTime(),
        snapshot.getLastModifiedVersion());
  }

  /**
   * Deserialize a wave view snapshot into a WaveViewData object
   *
   * @param snapshot the snapshot to deserialize
   * @return the deserialized snapshot
   * @throws OperationException
   * @throws InvalidParticipantAddress
   * @throws InvalidIdException
   */
  public static WaveViewData deserializeWave(WaveViewSnapshot snapshot,
      DocumentFactory<?> docFactory) throws OperationException, InvalidParticipantAddress,
      InvalidIdException {
    WaveId waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(snapshot.getWaveId());
    Collection<ObservableWaveletData> wavelets = CollectionUtils.newArrayList();
    for (WaveletSnapshot s : snapshot.getWavelet()) {
      wavelets.add(deserializeWavelet(s, waveId, docFactory));
    }

    return WaveViewDataImpl.create(waveId, wavelets);
  }
}
