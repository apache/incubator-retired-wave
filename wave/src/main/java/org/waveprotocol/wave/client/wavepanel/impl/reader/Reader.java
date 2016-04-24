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

package org.waveprotocol.wave.client.wavepanel.impl.reader;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.wave.DocumentRegistry;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.LocalSupplementedWave;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter;
import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter.FocusOrder;
import org.waveprotocol.wave.client.wavepanel.impl.focus.ViewTraverser;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * Inteprets focus-frame movement as reading actions, and also provides an
 * ordering for focus frame movement, based on unread content.
 *
 */
public final class Reader implements FocusFramePresenter.Listener, FocusOrder {
  private final LocalSupplementedWave supplement;
  private final ModelAsViewProvider models;
  private final ViewTraverser traverser;
  private final DocumentRegistry<? extends InteractiveDocument> documents;

  @VisibleForTesting
  Reader(LocalSupplementedWave supplement, ModelAsViewProvider models,
      DocumentRegistry<? extends InteractiveDocument> documents, ViewTraverser traverser) {
    this.supplement = supplement;
    this.models = models;
    this.documents = documents;
    this.traverser = traverser;
  }

  /**
   * Builds and installs the reading feature.
   *
   * @return the feature.
   */
  public static Reader install(LocalSupplementedWave supplement, FocusFramePresenter focus,
      ModelAsViewProvider models, DocumentRegistry<? extends InteractiveDocument> documents) {
    ViewTraverser traverser = new ViewTraverser();
    final Reader reader = new Reader(supplement, models, documents, traverser);
    focus.setOrder(reader);
    focus.addListener(reader);
    return reader;
  }

  @Override
  public void onFocusMoved(BlipView oldUi, BlipView newUi) {
    if (oldUi != null) {
      ConversationBlip oldBlip = models.getBlip(oldUi);
      InteractiveDocument document = documents.get(oldBlip);
      if (oldBlip != null) {
        supplement.stopReading(oldBlip);
        document.stopDiffRetention();
        document.clearDiffs();
      }
    }

    if (newUi != null) {
      // UI hack: normally, becoming read triggers diff clearing, except when
      // the cause of becoming read is focus-frame placement.
      ConversationBlip newBlip = models.getBlip(newUi);
      InteractiveDocument document = documents.get(newBlip);
      if (newBlip != null) {
        document.startDiffRetention();
        supplement.startReading(newBlip);
      }
    }
  }

  public boolean isRead(BlipView blipUi) {
    ConversationBlip blip = models.getBlip(blipUi);
    if(blip != null)
      return !supplement.isUnread(blip);
    return false;
  }

  //
  // Next/Previous blips based on read/unread state.
  //

  @Override
  public BlipView getNext(BlipView start) {
    BlipView blipUi = traverser.getNext(start);
    while (blipUi != null && isRead(blipUi)) {
      blipUi = traverser.getNext(blipUi);
    }
    return blipUi;
  }

  @Override
  public BlipView getPrevious(BlipView start) {
    BlipView blipUi = traverser.getPrevious(start);
    while (blipUi != null && isRead(blipUi)) {
      blipUi = traverser.getPrevious(blipUi);
    }
    return blipUi;
  }
}
