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

package org.waveprotocol.wave.client.wavepanel.impl.indicator;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.WaveMouseDownHandler;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession;
import org.waveprotocol.wave.client.wavepanel.impl.edit.EditSession.Listener;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;

/**
 * Interprets click on the reply indicators.
 */
public final class ReplyIndicatorController implements WaveMouseDownHandler, Listener {
  private final DomAsViewProvider panel;
  private final Actions actions;
  private final EditSession editSession;

  /**
   * Creates a reply indicator handler.
   *
   * @param actions
   * @param panel
   */
  private ReplyIndicatorController(Actions actions, EditSession editSession,
      DomAsViewProvider panel) {
    this.actions = actions;
    this.panel = panel;
    this.editSession = editSession;
    this.editSession.addListener(this);
  }

  /**
   * Installs the reply indicator feature in a wave panel.
   */
  public static void install(Actions handler, EditSession editSession, WavePanel panel) {
    ReplyIndicatorController controller = new ReplyIndicatorController(handler, editSession,
        panel.getViewProvider());
    panel.getHandlers().registerMouseDownHandler(TypeCodes.kind(Type.REPLY_BOX),
        controller);
    panel.getHandlers().registerMouseDownHandler(TypeCodes.kind(Type.CONTINUATION_INDICATOR),
        controller);
  }

  @Override
  public boolean onMouseDown(MouseDownEvent event, Element context) {
    if (event.getNativeButton() != NativeEvent.BUTTON_LEFT) {
      return false;
    }

    if (TypeCodes.kind(Type.REPLY_BOX).equals(
        context.getAttribute(BuilderHelper.KIND_ATTRIBUTE))) {
      ReplyBoxView indicatorView = panel.asReplyBox(context);
      ThreadView threadView = indicatorView.getParent();
      actions.addContinuation(threadView);
    } else if (TypeCodes.kind(Type.CONTINUATION_INDICATOR).equals(
        context.getAttribute(BuilderHelper.KIND_ATTRIBUTE))) {
      ContinuationIndicatorView indicatorView = panel.asContinuationIndicator(context);
      ThreadView threadView = indicatorView.getParent();
      actions.addContinuation(threadView);
    }

    event.preventDefault();
    return true;
  }

  @Override
  public void onSessionStart(Editor e, BlipView blipUi) {
    ThreadView threadUi = blipUi.getParent();
    if (threadUi.getBlipAfter(blipUi) == null) {
      // Editing has commenced on the last blip in thread.
      threadUi.getReplyIndicator().disable();
    }
  }

  @Override
  public void onSessionEnd(Editor e, BlipView blipView) {
    blipView.getParent().getReplyIndicator().enable();
  }
}