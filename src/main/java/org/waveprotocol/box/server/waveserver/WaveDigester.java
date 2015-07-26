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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.BlipIterators;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.PrimitiveSupplement;
import org.waveprotocol.wave.model.supplement.PrimitiveSupplementImpl;
import org.waveprotocol.wave.model.supplement.SupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Generates digests for the search service.
 *
 * @author yurize@apache.org
 */
public class WaveDigester {

  private final ConversationUtil conversationUtil;
  private static final int DIGEST_SNIPPET_LENGTH = 140;
  private static final int PARTICIPANTS_SNIPPET_LENGTH = 5;
  private static final String EMPTY_WAVELET_TITLE = "";

  @Inject
  public WaveDigester(ConversationUtil conversationUtil) {
    this.conversationUtil = conversationUtil;
  }

  public SearchResult generateSearchResult(ParticipantId participant, String query,
      Collection<WaveViewData> results) {
    // Generate exactly one digest per wave. This includes conversational and
    // non-conversational waves. The position-based API for search prevents the
    // luxury of extra filtering here. Filtering can only be done in the
    // searchProvider. All waves returned by the search provider must be
    // included in the search result.
    SearchResult result = new SearchResult(query);
    if (results == null) {
      return result;
    }
    for (WaveViewData wave : results) {
      result.addDigest(build(participant, wave));
    }

    assert result.getDigests().size() == results.size();
    return result;
  }

  public Digest build(ParticipantId participant, WaveViewData wave) {

    Digest digest;

    // Note: the indexing infrastructure only supports single-conversation
    // waves, and requires raw wavelet access for snippeting.
    ObservableWaveletData root = null;
    ObservableWaveletData other = null;
    ObservableWaveletData udw = null;
    for (ObservableWaveletData waveletData : wave.getWavelets()) {
      WaveletId waveletId = waveletData.getWaveletId();
      if (IdUtil.isConversationRootWaveletId(waveletId)) {
        root = waveletData;
      } else if (IdUtil.isConversationalId(waveletId)) {
        other = waveletData;
      } else if (IdUtil.isUserDataWavelet(waveletId)
          && waveletData.getCreator().equals(participant)) {
        udw = waveletData;
      }
    }

    ObservableWaveletData convWavelet = root != null ? root : other;
    SupplementedWave supplement = null;
    ObservableConversationView conversations = null;
    if (convWavelet != null) {
      OpBasedWavelet wavelet = OpBasedWavelet.createReadOnly(convWavelet);
      if (WaveletBasedConversation.waveletHasConversation(wavelet)) {
        conversations = conversationUtil.buildConversation(wavelet);
        supplement = buildSupplement(participant, conversations, udw);
      }
    }
    if (conversations != null) {
      // This is a conversational wave. Produce a conversational digest.
      digest = generateDigest(conversations, supplement, convWavelet);
    } else {
      // It is unknown how to present this wave.
      digest = generateEmptyorUnknownDigest(wave);
    }

    return digest;
  }

  /**
   * Produces a digest for a set of conversations. Never returns null.
   *
   * @param conversations the conversation.
   * @param supplement the supplement that allows to easily perform various
   *        queries on user related state of the wavelet.
   * @param rawWaveletData the waveletData from which the digest is generated.
   *        This wavelet is a copy.
   * @return the server representation of the digest for the query.
   */
  Digest generateDigest(ObservableConversationView conversations, SupplementedWave supplement,
      WaveletData rawWaveletData) {
    ObservableConversation rootConversation = conversations.getRoot();
    ObservableConversationBlip firstBlip = null;
    if ((rootConversation != null) && (rootConversation.getRootThread() != null)
        && (rootConversation.getRootThread().getFirstBlip() != null)) {
      firstBlip = rootConversation.getRootThread().getFirstBlip();
    }
    String title;
    if (firstBlip != null) {
      Document firstBlipContents = firstBlip.getContent();
      title = TitleHelper.extractTitle(firstBlipContents).trim();
    } else {
      title = EMPTY_WAVELET_TITLE;
    }

    String snippet = Snippets.renderSnippet(rawWaveletData, DIGEST_SNIPPET_LENGTH).trim();
    if (snippet.startsWith(title) && !title.isEmpty()) {
      // Strip the title from the snippet if the snippet starts with the title.
      snippet = snippet.substring(title.length());
    }
    String waveId = ApiIdSerializer.instance().serialiseWaveId(rawWaveletData.getWaveId());
    List<String> participants = CollectionUtils.newArrayList();
    for (ParticipantId p : rawWaveletData.getParticipants()) {
      if (participants.size() < PARTICIPANTS_SNIPPET_LENGTH) {
        participants.add(p.getAddress());
      } else {
        break;
      }
    }
    int unreadCount = 0;
    int blipCount = 0;
    long lastModified = -1;
    for (ConversationBlip blip : BlipIterators.breadthFirst(rootConversation)) {
      if (supplement.isUnread(blip)) {
        unreadCount++;
      }
      lastModified = Math.max(blip.getLastModifiedTime(), lastModified);
      blipCount++;
    }
    return new Digest(title, snippet, waveId, participants, lastModified,
        rawWaveletData.getCreationTime(), unreadCount, blipCount);
  }

  /** @return a digest for an empty wave. */
  private Digest emptyDigest(WaveViewData wave) {
    String title = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
    String id = ApiIdSerializer.instance().serialiseWaveId(wave.getWaveId());
    return new Digest(title, "(empty)", id, Collections.<String> emptyList(), -1L, -1L, 0, 0);
  }

  /** @return a digest for an unrecognised type of wave. */
  private Digest unknownDigest(WaveViewData wave) {
    String title = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
    String id = ApiIdSerializer.instance().serialiseWaveId(wave.getWaveId());
    long lmt = -1L;
    long created = -1L;
    int docs = 0;
    List<String> participants = new ArrayList<String>();
    for (WaveletData data : wave.getWavelets()) {
      lmt = Math.max(lmt, data.getLastModifiedTime());
      created = Math.max(lmt, data.getCreationTime());
      docs += data.getDocumentIds().size();

      for (ParticipantId p : data.getParticipants()) {
        if (participants.size() < PARTICIPANTS_SNIPPET_LENGTH) {
          participants.add(p.getAddress());
        } else {
          break;
        }
      }
    }
    return new Digest(title, "(unknown)", id, participants, lmt, created, 0, docs);
  }

  /**
   * Generates an empty digest in case the wave is empty, or an unknown digest
   * otherwise.
   *
   * @param wave the wave.
   * @return the generated digest.
   */
  Digest generateEmptyorUnknownDigest(WaveViewData wave) {
    boolean empty = !wave.getWavelets().iterator().hasNext();
    Digest digest = empty ? emptyDigest(wave) : unknownDigest(wave);
    return digest;
  }

  /**
   * Builds the supplement model from a wave. Never returns null.
   *
   * @param viewer the participant for which the supplement is constructed.
   * @param conversations conversations in the wave
   * @param udw the user data wavelet for the logged user.
   * @return the wave supplement.
   */
  @VisibleForTesting
  SupplementedWave buildSupplement(ParticipantId viewer, ObservableConversationView conversations,
      ObservableWaveletData udw) {
    // Use mock state if there is no UDW.
    PrimitiveSupplement udwState =
        udw != null ? WaveletBasedSupplement.create(OpBasedWavelet.createReadOnly(udw))
            : new PrimitiveSupplementImpl();
        return SupplementedWaveImpl.create(udwState, conversations, viewer, DefaultFollow.ALWAYS);
  }
}
