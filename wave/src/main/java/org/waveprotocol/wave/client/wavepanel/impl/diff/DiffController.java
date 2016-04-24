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


package org.waveprotocol.wave.client.wavepanel.impl.diff;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.wave.DocumentRegistry;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.BlipMappers;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationListenerImpl;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.Predicate;

/**
 * Inteprets focus-frame movement as reading actions, and also provides an
 * ordering for focus frame movement, based on unread content.
 *
 */
public final class DiffController extends ConversationListenerImpl implements EditSession.Listener,
    ObservableConversationView.Listener {
  private final ObservableConversationView wave;
  private final ObservableSupplementedWave supplement;
  private final DocumentRegistry<? extends InteractiveDocument> documents;
  private final ModelAsViewProvider models;

  private final IdentitySet<ConversationBlip> fullyUnread = CollectionUtils.createIdentitySet();
  private final Predicate<ConversationBlip> suppress = new Predicate<ConversationBlip>() {
    @Override
    public boolean apply(ConversationBlip blip) {
      suppressFullyUnread(blip);
      return true;
    }
  };
  private final Predicate<ConversationBlip> remove = new Predicate<ConversationBlip>() {
    @Override
    public boolean apply(ConversationBlip blip) {
      fullyUnread.remove(blip);
      return true;
    }
  };
  private final ObservableSupplementedWave.Listener supplementListener =
      new ObservableSupplementedWave.ListenerImpl() {
        @Override
        public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
          // Incorrectly assume that maybe-events are actually definite events.
          // In simple use-cases, they always are. In complex concurrent cases,
          // they are not.
          // TODO(hearnden): fix supplement event model to expose definite
          // events, or use the blip/thread monitors which have definite events,
          // then delete the comment above.
          onBlipReadChanged(blip);
        }
      };


  @VisibleForTesting
  DiffController(ObservableConversationView wave, ObservableSupplementedWave supplement,
      DocumentRegistry<? extends InteractiveDocument> documents, ModelAsViewProvider models) {
    this.wave = wave;
    this.supplement = supplement;
    this.documents = documents;
    this.models = models;
  }

  /**
   * Builds and installs the reading feature.
   *
   * @param documents
   *
   * @return the feature.
   */
  public static DiffController create(ObservableConversationView wave,
      ObservableSupplementedWave supplement,
      DocumentRegistry<? extends InteractiveDocument> documents, ModelAsViewProvider models) {
    return new DiffController(wave, supplement, documents, models);
  }

  /**
   * Initializes diff control.
   */
  public void install() {
    // Suppress diffs on fully-unread blips.
    BlipMappers.depthFirst(suppress, wave);

    // Attach listeners to observed components.
    wave.addListener(this);
    for (ObservableConversation conversation : wave.getConversations()) {
      conversation.addListener(this);
    }
    supplement.addListener(supplementListener);
  }

  /**
   * Upgrades diff control to interact with the editing feature.
   */
  public void upgrade(EditSession edit) {
    edit.addListener(this);
  }

  public void destroy() {
    supplement.removeListener(supplementListener);
    wave.removeListener(this);
  }

  private void suppressFullyUnread(ConversationBlip blip) {
    InteractiveDocument doc = documents.get(blip);
    if (doc.isCompleteDiff()) {
      fullyUnread.add(blip);
      doc.startDiffSuppression();
      assert !doc.isCompleteDiff();
    }
  }

  @Override
  public void onSessionStart(Editor e, BlipView blipUi) {
    documents.get(models.getBlip(blipUi)).startDiffSuppression();
  }

  @Override
  public void onSessionEnd(Editor e, BlipView blipUi) {
    documents.get(models.getBlip(blipUi)).stopDiffSuppression();
  }

  private void onBlipReadChanged(ObservableConversationBlip blip) {
    if (!supplement.isUnread(blip)) {
      InteractiveDocument doc = documents.get(blip);
      if (fullyUnread.contains(blip)) {
        doc.stopDiffSuppression();
        fullyUnread.remove(blip);
      } else {
        doc.clearDiffs();
      }
    }
  }

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    BlipMappers.depthFirst(suppress, conversation);
    conversation.addListener(this);
  }

  @Override
  public void onConversationRemoved(ObservableConversation conversation) {
    conversation.removeListener(this);
    BlipMappers.depthFirst(remove, conversation);
  }

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
    BlipMappers.depthFirst(suppress, blip);
  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
    BlipMappers.depthFirst(remove, blip);
  }

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
    BlipMappers.depthFirst(suppress, thread);
  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
    BlipMappers.depthFirst(remove, thread);
  }
}
