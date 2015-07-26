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

package org.waveprotocol.wave.client.wavepanel.view.dom;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMenuItemView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;

/**
 * Exposes DOM elements as views.
 *
 */
public interface DomAsViewProvider {
  /** @return {@code source} exposed as a blip. */
  BlipView asBlip(Element source);

  /** @return {@code source} exposed as the meta section of a blip. */
  BlipMetaView asBlipMeta(Element source);

  /** @return {@code source} exposed as an item in a blip menu. */
  BlipMenuItemView asBlipMenuItem(Element source);

  /** @return {@code source} exposed as an inline thread. */
  InlineThreadView asInlineThread(Element source);

  /** @return {@code source} exposed as an inline thread indicator. */
  ContinuationIndicatorView asContinuationIndicator(Element source);

  /** @return the inline thread that surrounds the toggle {@code source}. */
  InlineThreadView fromToggle(Element source);

  /** @return {@code source} exposed as a root thread. */
  RootThreadView asRootThread(Element source);

  /** @return {@code source} exposed as a reply box. */
  ReplyBoxView asReplyBox(Element source);

  /** @return {@code source} exposed as a participant view. */
  ParticipantView asParticipant(Element source);

  /** @return {@code source} exposed as a participants view. */
  ParticipantsView asParticipants(Element source);

  /** @return the participants view that surrounds the button {@code source}. */
  ParticipantsView fromAddButton(Element source);

  /** @return the participants view that surrounds the button {@code source}. */
  ParticipantsView fromNewWaveWithParticipantsButton(Element source);

  /** @return {@code source} exposed as a top-conversation view. */
  TopConversationView asTopConversation(Element source);

  /** @return {@code source} exposed as an inline conversation view. */
  InlineConversationView asInlineConversation(Element source);

  /** @return {@code source} exposed as a thread anchor. */
  AnchorView asAnchor(Element source);

  /** @return {@code source} exposed as a conversation view. */
  ConversationView asConversation(Element e);

  // etc.
}
