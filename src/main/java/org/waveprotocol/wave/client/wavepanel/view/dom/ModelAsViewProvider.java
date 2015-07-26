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

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 */
public interface ModelAsViewProvider {
  BlipView getBlipView(ConversationBlip source);

  BlipMetaView getBlipMetaView(ConversationBlip source);

  InlineThreadView getInlineThreadView(ConversationThread source);

  RootThreadView getRootThreadView(ConversationThread source);

  /** @return the inline anchor for {@code source}, if it exists. */
  AnchorView getInlineAnchor(ConversationThread source);

  /** @return the default anchor for {@code source}, if it exists. */
  AnchorView getDefaultAnchor(ConversationThread source);

  /** @return the participant for {@code source}, if it exists. */
  ParticipantView getParticipantView(Conversation conv, ParticipantId source);

  /** @return the participants for {@code source}, if they exist. */
  ParticipantsView getParticipantsView(Conversation conv);

  /** @return the view of {@code source}, if it exists. */
  ConversationView getConversationView(Conversation conv);

  //
  // Inverse mapping.
  //

  /** @return the blip represented by {@code blipUi}. */
  ConversationBlip getBlip(BlipView blipUi);

  /** @return the thread represented by {@code threadUi}. */
  ConversationThread getThread(ThreadView threadUi);

  /**
   * @return the (mutable) participant collection represented by {@code
   *         participantsUi}.
   */
  Conversation getParticipants(ParticipantsView participantsUi);

  /** @return the participant represented by {@code participantUi}. */
  Pair<Conversation, ParticipantId> getParticipant(ParticipantView participantUi);
}
