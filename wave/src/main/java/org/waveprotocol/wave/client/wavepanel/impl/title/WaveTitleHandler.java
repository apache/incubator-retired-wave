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

package org.waveprotocol.wave.client.wavepanel.impl.title;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Handles automatic setting of the wave title if the following conditions are true:
 * <ol>
 * <li>The edited blip is a root blip.</li>
 * <li>The edited line is the first line.</li>
 * <li>No explicit title for this wave is set.</li>
 * </ol>
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class WaveTitleHandler implements EditorUpdateListener, EditSession.Listener {

  private final EditSession editSession;
  private final ModelAsViewProvider views;

  private BlipView blipUi;
  private EditorContext editor;

  public static WaveTitleHandler install(EditSession editSession, ModelAsViewProvider views) {
    return new WaveTitleHandler(editSession, views);
  }

  private WaveTitleHandler(EditSession editSession, ModelAsViewProvider views) {
    this.views = views;
    this.editSession = editSession;
    init();
  }

  private void init() {
    editSession.addListener(this);
  }

  // Listeners methods.
  @Override
  public void onSessionStart(Editor editor, BlipView blipUi) {
    editor.addUpdateListener(this);
    this.blipUi = blipUi;
  }

  @Override
  public void onSessionEnd(Editor editor, BlipView blipUi) {
    editor.removeUpdateListener(this);
    this.blipUi = null;
    this.editor = null;
  }

  @Override
  public void onUpdate(EditorUpdateEvent event) {
    editor = event.context();
    if (event.contentChanged() && editor.isEditing()) {
      maybeSetOrUpdateTitle();
    }
  }

  /**
   * Sets or replaces an automatic title for the wave by annotating the first
   * line of the root blip with <code>conv/title</code> annotation. Has
   * effect only when the first line of the root blip is edited and no explicit
   * title is set.
   */
  private void maybeSetOrUpdateTitle() {
    if (blipUi != null && editor != null) {
      CMutableDocument document = editor.getDocument();
      ConversationBlip editBlip = views.getBlip(blipUi);
      if (editBlip.isRoot() && !TitleHelper.hasExplicitTitle(document)) {
        Range titleRange = TitleHelper.findImplicitTitle(document);
        TitleHelper.setImplicitTitle(document, titleRange.getStart(), titleRange.getEnd());
      }
    }
  }
}
