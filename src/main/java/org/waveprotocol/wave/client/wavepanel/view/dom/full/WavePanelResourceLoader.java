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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;

import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.wavepanel.view.dom.CssProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.BlipMessages;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.ReplyBoxMessages;
import org.waveprotocol.wave.client.widget.dialog.Dialog;

/**
 * This class is responsible for loading all the Css resources needed by the
 * wave panel.
 *
 */
public final class WavePanelResourceLoader {

  private final static BlipViewBuilder.Resources blip = 
      GWT.create(BlipViewBuilder.Resources.class);
  private final static CollapsibleBuilder.Resources collapsible =
      GWT.create(CollapsibleBuilder.Resources.class);
  private final static RootThreadViewBuilder.Resources rootThread =
      GWT.create(RootThreadViewBuilder.Resources.class);
  private final static ReplyBoxViewBuilder.Resources replyBox =
      GWT.create(ReplyBoxViewBuilder.Resources.class);
  private final static ContinuationIndicatorViewBuilder.Resources inlineContinuation =
      GWT.create(ContinuationIndicatorViewBuilder.Resources.class);
  private final static TopConversationViewBuilder.Resources conversation =
      GWT.create(TopConversationViewBuilder.Resources.class);
  private final static ParticipantsViewBuilder.Resources participants =
      GWT.create(ParticipantsViewBuilder.Resources.class);
  private final static Dialog.Resources dialog =
      GWT.create(Dialog.Resources.class);

  private final static BlipMessages blipMessages =
      GWT.create(BlipMessages.class);

  private final static ReplyBoxMessages replyBoxMessages =
      GWT.create(ReplyBoxMessages.class);

  static {
    // Inject all CSS synchronously. CSS must be injected synchronously, so that
    // any layout queries, that may happen to occur in the same event cycle,
    // operate on the correct state (GWT's default injection mode is
    // asynchronous). CSS is injected together in one bundle to minimize layout
    // invalidation, and to leave open the possibility of merging stylesheets
    // together for efficiency.
    boolean isSynchronous = true;
    StyleInjector.inject(blip.css().getText(), isSynchronous);
    StyleInjector.inject(collapsible.css().getText(), isSynchronous);
    StyleInjector.inject(rootThread.css().getText(), isSynchronous);
    StyleInjector.inject(replyBox.css().getText(), isSynchronous);
    StyleInjector.inject(inlineContinuation.css().getText(), isSynchronous);
    StyleInjector.inject(conversation.css().getText(), isSynchronous);
    StyleInjector.inject(participants.css().getText(), isSynchronous);
    StyleInjector.inject(dialog.css().getText(), isSynchronous);
  }

  private WavePanelResourceLoader() {
  }

  public static BlipViewBuilder.Resources getBlip() {
    return blip;
  }

  public static CollapsibleBuilder.Resources getCollapsible() {
    return collapsible;
  }

  public static RootThreadViewBuilder.Resources getRootThread() {
    return rootThread;
  }

  public static ReplyBoxViewBuilder.Resources getReplyBox() {
    return replyBox;
  }

  public static ContinuationIndicatorViewBuilder.Resources getContinuationIndicator() {
    return inlineContinuation;
  }

  public static TopConversationViewBuilder.Resources getConversation() {
    return conversation;
  }

  public static ParticipantsViewBuilder.Resources getParticipants() {
    return participants;
  }

  public static Dialog.Resources getDialog() {
    return dialog;
  }

  public static BlipMessages getBlipMessages() {
    return blipMessages;
  }

  public static ReplyBoxMessages getReplyBoxMessages() {
    return replyBoxMessages;
  }

  /**
   * Loads all the CSS required by the wave panel.
   */
  public static void loadCss() {
    // Static initializer loads all the wave panel views.
    // Need a few more:
    EditorImpl.init();
  }
  
  /** @return The provider of just the CSS layer of styles. */
  public static CssProvider createCssProvider() {
    return new CssProvider(getBlip().css(), 
        getCollapsible().css(), 
        getContinuationIndicator().css(),
        getConversation().css(),
        getParticipants().css(),
        getReplyBox().css(),
        getRootThread().css());
  }
}
