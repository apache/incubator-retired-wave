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


package org.waveprotocol.wave.client.wave;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

/**
 * Manages the collection of blip documents and data documents for all
 * conversations in a wave, serving as a factory for new documents and a
 * registry for accessing blip documents.
 *
 * @param <BlipDocument> the document type for blip documents
 */
public final class WaveDocuments<BlipDocument extends DocumentOperationSink>
    implements DocumentFactory<DocumentOperationSink>, DocumentRegistry<BlipDocument> {

  /** The factory used to create blip documents. */
  private final DocumentFactory<BlipDocument> blipDocFactory;

  /** The factory used to create non-blip documents. */
  private final DocumentFactory<?> dataDocFactory;

  /** All the blip documents, indexed by conversation id then blip id. */
  private final StringMap<StringMap<BlipDocument>> blips = CollectionUtils.createStringMap();

  /**
   * Creates a wave's document collection.
   */
  private WaveDocuments(DocumentFactory<BlipDocument> blip, DocumentFactory<?> data) {
    this.blipDocFactory = blip;
    this.dataDocFactory = data;
  }

  /**
   * Creates a wave's document collection.
   *
   * @param blipDocFactory factory for blip documents
   * @param dataDocFactory factory for data documents.
   */
  public static <B extends DocumentOperationSink> WaveDocuments<B> create(
      DocumentFactory<B> blipDocFactory, DocumentFactory<?> dataDocFactory) {
    return new WaveDocuments<B>(blipDocFactory, dataDocFactory);
  }

  @Override
  public DocumentOperationSink create(
      final WaveletId waveletId, final String blipId, final DocInitialization content) {

    String waveletIdStr = ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId);
    if (IdUtil.isBlipId(blipId)) {
      BlipDocument document = blipDocFactory.create(waveletId, blipId, content);
      StringMap<BlipDocument> convDocuments = getConversationDocuments(waveletIdStr);
      Preconditions.checkState(!convDocuments.containsKey(blipId));
      convDocuments.put(blipId, document);
      return document;
    } else {
      return dataDocFactory.create(waveletId, blipId, content);
    }
  }

  /**
   * Gets the document map for a particular conversation.
   *
   * @param id conversation id
   */
  private StringMap<BlipDocument> getConversationDocuments(String id) {
    StringMap<BlipDocument> convDocuments = blips.get(id);
    if (convDocuments == null) {
      convDocuments = CollectionUtils.createStringMap();
      blips.put(id, convDocuments);
    }
    return convDocuments;
  }

  @Override
  public BlipDocument get(ConversationBlip blip) {
    return getBlipDocument(blip.getConversation().getId(), blip.getId());
  }

  /**
   * Reveals access to the special document implementation for conversational
   * blips.
   *
   * @param waveletId
   * @param docId
   * @return the special document implementation (content-document) for the
   *         document, if it has one. This returns null either if the document
   *         does not exist, or it is a regular non-special (data) document.
   */
  public BlipDocument getBlipDocument(String waveletId, String docId) {
    StringMap<BlipDocument> convDocuments = blips.get(waveletId);
    return convDocuments != null ? convDocuments.get(docId) : null;
  }
}
