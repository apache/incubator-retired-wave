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


package org.waveprotocol.wave.client.wavepanel.impl.edit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.common.util.WaveRefConstants;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.client.wavepanel.impl.edit.i18n.ActionMessages;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.id.DualIdSerialiser;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;

/**
 * Defines the UI actions that can be performed as part of the editing feature.
 * This includes editing, replying, and deleting blips in a conversation.
 *
 */
public final class ActionsImpl implements Actions {
  private static final ActionMessages messages = GWT.create(ActionMessages.class);

  private final ModelAsViewProvider views;
  private final WaveDocuments<? extends InteractiveDocument> documents;
  private final BlipQueueRenderer blipQueue;
  private final FocusFramePresenter focus;
  private final EditSession edit;

  ActionsImpl(ModelAsViewProvider views, WaveDocuments<? extends InteractiveDocument> documents,
      BlipQueueRenderer blipQueue, FocusFramePresenter focus, EditSession edit) {
    this.views = views;
    this.documents = documents;
    this.blipQueue = blipQueue;
    this.focus = focus;
    this.edit = edit;
  }

  /**
   * Creates an action performer.
   *
   * @param views view provider
   * @param documents collection of documents in the wave
   * @param blipQueue blip renderer
   * @param focus focus-frame feature
   * @param edit blip-content editing feature
   */
  public static ActionsImpl create(ModelAsViewProvider views,
      WaveDocuments<? extends InteractiveDocument> documents, BlipQueueRenderer blipQueue,
      FocusFramePresenter focus, EditSession edit) {
    return new ActionsImpl(views, documents, blipQueue, focus, edit);
  }

  @Override
  public void startEditing(BlipView blipUi) {
    focusAndEdit(blipUi);
  }

  @Override
  public void stopEditing() {
    edit.stopEditing();
  }

  @Override
  public void reply(BlipView blipUi) {
    ConversationBlip blip = views.getBlip(blipUi);
    ContentDocument doc = documents.get(blip).getDocument();
    // Insert the reply at a good spot near the current selection, or use the
    // end of the document as a fallback.
    int location = DocumentUtil.getLocationNearSelection(doc);
    if (location == -1) {
      location = blip.getContent().size() - 1;
    }
    ConversationBlip reply = blip.addReplyThread(location).appendBlip();
    blipQueue.flush();
    focusAndEdit(views.getBlipView(reply));
  }

  @Override
  public void addContinuation(ThreadView threadUi) {
    ConversationThread thread = views.getThread(threadUi);
    ConversationBlip continuation = thread.appendBlip();
    blipQueue.flush();
    focusAndEdit(views.getBlipView(continuation));
  }

  @Override
  public void delete(BlipView blipUi) {
    // If focus is on the blip that is being deleted, move focus somewhere else.
    // If focus is on a blip inside the blip being deleted, don't worry about it
    // (checking could get too expensive).
    if (blipUi.equals(focus.getFocusedBlip())) {
      // Move to next blip in thread if there is one, otherwise previous blip in
      // thread, otherwise previous blip in traversal order.
      ThreadView parentUi = blipUi.getParent();
      BlipView nextUi = parentUi.getBlipAfter(blipUi);
      if (nextUi == null) {
        nextUi = parentUi.getBlipBefore(blipUi);
      }
      if (nextUi != null) {
        focus.focus(nextUi);
      } else {
        focus.moveUp();
      }
    }

    views.getBlip(blipUi).delete();
  }

  @Override
  public void delete(ThreadView threadUi) {
    views.getThread(threadUi).delete();
  }

  /**
   * Moves focus to a blip, and starts editing it.
   */
  private void focusAndEdit(BlipView blipUi) {
    edit.stopEditing();
    focus.focus(blipUi);
    edit.startEditing(blipUi);
  }

  @Override
  public void popupLink(BlipView blipUi) {
    ConversationBlip blip = views.getBlip(blipUi);
    // TODO(Yuri Z.) Change to use the conversation model when the Conversation
    // exposes a reference to its ConversationView.
    WaveId waveId = blip.hackGetRaw().getWavelet().getWaveId();
    WaveletId waveletId;
    try {
      waveletId = DualIdSerialiser.MODERN.deserialiseWaveletId(blip.getConversation().getId());
    } catch (InvalidIdException e) {
      Window.alert(messages.invalidWaveletId(blip.getConversation().getId()));
      return;
    }
    WaveRef waveRef = WaveRef.of(waveId, waveletId, blip.getId());
    final String waveRefStringValue =
        WaveRefConstants.WAVE_URI_PREFIX + GwtWaverefEncoder.encodeToUriPathSegment(waveRef);
    BlipLinkPopupView blipLinkPopupView = blipUi.createLinkPopup();
    blipLinkPopupView.setLinkInfo(waveRefStringValue);
    blipLinkPopupView.show();
  }
}
