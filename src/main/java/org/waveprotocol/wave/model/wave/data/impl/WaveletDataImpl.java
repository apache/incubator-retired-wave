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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link WaveletData} which uses {@link BlipDataImpl}
 * to represent blips.
 *
 */
public final class WaveletDataImpl extends AbstractWaveletData<BlipDataImpl> {

  /**
   * Factory for constructing wavelet data copies.
   */
  public static final class Factory implements ObservableWaveletData.Factory<WaveletDataImpl> {
    /**
     * @param contentFactory
     *
     * @return A new WaveletDataImpl.Factory using the given content factory.
     * @throws NullPointerException if contentFactory is null.
     */
    public static Factory create(DocumentFactory<?> contentFactory) {
      return new Factory(contentFactory);
    }

    private final DocumentFactory<?> contentFactory;

    private Factory(DocumentFactory<?> contentFactory) {
      Preconditions.checkNotNull(contentFactory, "null DocumentFactory");
      this.contentFactory = contentFactory;
    }

    @Override
    public WaveletDataImpl create(ReadableWaveletData data) {
      WaveletDataImpl waveletData = new WaveletDataImpl(data, contentFactory);
      waveletData.copyParticipants(data);
      waveletData.copyDocuments(data);
      return waveletData;
    }
  }

  /** The list of participants in this wavelet. */
  private final LinkedHashSet<ParticipantId> participants = new LinkedHashSet<ParticipantId>();

  /** The set of documentss in this wave, indexed by their identifier. */
  private final Map<String, BlipDataImpl> documents = CollectionUtils.newHashMap();

  /**
   * Creates a new wavelet.
   *
   * @param id                id of the wavelet
   * @param creator           creator of the wavelet
   * @param creationTime      timestamp of wavelet creation
   * @param version           initial version of the wavelet
   * @param distinctVersion   initial distinct server version of the wavelet
   * @param lastModifiedTime  initial last-modified time for the wavelet
   * @param waveId            id of the wave containing the wavelet
   * @param contentFactory    factory for creating new documents
   */
  public WaveletDataImpl(WaveletId id, ParticipantId creator, long creationTime, long version,
      HashedVersion distinctVersion, long lastModifiedTime, WaveId waveId,
      DocumentFactory<?> contentFactory) {
    super(id, creator, creationTime, version, distinctVersion, lastModifiedTime, waveId,
        contentFactory);
  }

  /**
   * Creates a copy of the given wavelet data retaining the meta data. No
   * documets or participants are copied.
   *
   * @param data data to copy
   * @param contentFactory factory for creating new documents
   */
  private WaveletDataImpl(ReadableWaveletData data, DocumentFactory<?> contentFactory) {
    super(data, contentFactory);
  }

  @Override
  protected Set<ParticipantId> getMutableParticipants() {
    return participants;
  }

  @Override
  protected BlipDataImpl internalCreateDocument(String docId, ParticipantId author,
      Collection<ParticipantId> contributors, DocumentOperationSink contentSink,
      long lastModifiedTime, long lastModifiedVersion) {
    Preconditions.checkArgument(!documents.containsKey(docId), "Duplicate doc id: %s", docId);

    BlipDataImpl blip = new BlipDataImpl(docId, this, author, contributors, contentSink,
        lastModifiedTime, lastModifiedVersion);
    documents.put(docId, blip);
    return blip;
  }

  @Override
  public BlipDataImpl getDocument(String documentName) {
    return documents.get(documentName);
  }

  @Override
  public Set<String> getDocumentIds() {
    return Collections.unmodifiableSet(documents.keySet());
  }
}
