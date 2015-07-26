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

package org.waveprotocol.wave.client.wavepanel.impl.focus;

import org.waveprotocol.wave.client.wavepanel.impl.reader.Reader;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collections;
import java.util.Map;

/**
 * Selects the blip that should should receive the focus.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class FocusBlipSelector {

  /** The conversation. */
  private final ConversationView wave;

  /** The model with views. */
  private final ModelAsViewProvider views;

  /** The blip ordering. */
  private final  ViewTraverser traverser;

  /** The root blip. */
  private BlipView rootBlip;

  /**
   * Creates a {@link FocusBlipSelector}.
   *
   * @param wave the he conversation.
   * @param views the model with views.
   * @param traverser the blip ordering.
   * @param reader the ordering of focus frame movement.
   * @return the focus blip selector.
   */
  public static FocusBlipSelector create(ConversationView wave,
      ModelAsViewProvider views, Reader reader, ViewTraverser traverser) {
    return new FocusBlipSelector(wave, views, reader, traverser);
  }

  FocusBlipSelector(ConversationView wave,
      ModelAsViewProvider models, Reader reader, ViewTraverser traverser) {
    this.wave = wave;
    this.views = models;
    this.traverser = traverser;
  }

  /**
   * @return the most recently modified blip.
   */
  public BlipView selectMostRecentlyModified() {
    Conversation conversation  = wave.getRoot();
    if (conversation == null) {
      return null;
    } else {
      ConversationBlip blip = wave.getRoot().getRootThread().getFirstBlip();
      BlipView rootBlipUi = views.getBlipView(blip);
      if (rootBlipUi == null) {
        return null;
      }
      return findMostRecentlyModified(rootBlipUi);
    }
  }

  /**
   * @return the root blip of the currently displayed wave.
   */
  public BlipView getOrFindRootBlip() {
    if (rootBlip == null) {
      Conversation conversation  = wave.getRoot();
      if (conversation == null) {
        return null;
      } else {
        ConversationBlip blip = wave.getRoot().getRootThread().getFirstBlip();
        BlipView rootBlipUi = views.getBlipView(blip);
        if (rootBlipUi == null) {
          return null;
        }
        rootBlip =  rootBlipUi;
      }
    }
    return rootBlip;
  }

  /**
   * Locates and returns the UI blip view by waveRef.
   *
   * @param waveRef the reference to the current wave that includes the blip id.
   */
  public BlipView selectBlipByWaveRef(WaveRef waveRef) {
    // Determine if waveRef has a documentId in it - if so, the referenced blip
    // should receive the focus on wave load.
    // First find conversation
    Conversation conversation;
    String documentId = null;
    if (waveRef != null && waveRef.hasWaveletId()) {
      String id = ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveRef.getWaveletId());
      documentId = waveRef.getDocumentId();
      conversation = wave.getConversation(id);
    } else {
      // Unspecified wavelet means root.
      conversation = wave.getRoot();
    }
    if (conversation == null) {
      return null;
    } else {
      ConversationBlip blip = null;
      // If there's blip reference then focus on that blip.
      // Find selected blip.
      if (documentId != null) {
        blip = wave.getRoot().getBlip(documentId);
        if (blip != null) {
          return views.getBlipView(blip);
        }
      }
      return null;
    }
  }

  private BlipView findMostRecentlyModified(BlipView start) {
    BlipView blipUi = start;
    Map<Long, BlipView> blips = CollectionUtils.newHashMap();
    while (blipUi != null) {
      ConversationBlip blip = views.getBlip(blipUi);
      blips.put(blip.getLastModifiedTime() , blipUi);
      blipUi = traverser.getNext(blipUi);
    }
    long lmt = Collections.max(blips.keySet());
    return blips.get(lmt);
  }
}
