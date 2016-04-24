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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Get the view associated with the given model element.
 *
 */
public class ModelAsViewProviderImpl implements ModelAsViewProvider {
  private final DomAsViewProvider viewProvider;
  private final ViewIdMapper viewIdMapper;

  /**
   */
  public ModelAsViewProviderImpl(ViewIdMapper viewIdMapper, DomAsViewProvider viewProvider) {
    this.viewProvider = viewProvider;
    this.viewIdMapper = viewIdMapper;
  }

  @Override
  public BlipView getBlipView(ConversationBlip source) {
    Element e = Document.get().getElementById(viewIdMapper.blipOf(source));
    return viewProvider.asBlip(e);
  }

  @Override
  public BlipMetaView getBlipMetaView(ConversationBlip source) {
    Element e = Document.get().getElementById(viewIdMapper.metaOf(source));
    return viewProvider.asBlipMeta(e);
  }

  @Override
  public InlineThreadView getInlineThreadView(ConversationThread source) {
    Element e = Document.get().getElementById(viewIdMapper.threadOf(source));
    return viewProvider.asInlineThread(e);
  }

  @Override
  public RootThreadView getRootThreadView(ConversationThread source) {
    Element e = Document.get().getElementById(viewIdMapper.threadOf(source));
    return viewProvider.asRootThread(e);
  }

  @Override
  public AnchorView getDefaultAnchor(ConversationThread source) {
    Element e = Document.get().getElementById(viewIdMapper.defaultAnchorOf(source));
    return viewProvider.asAnchor(e);
  }

  @Override
  public AnchorView getInlineAnchor(ConversationThread source) {
    String domId = viewIdMapper.inlineAnchorOf(source.getParentBlip(), source.getId());
    Element e = Document.get().getElementById(domId);
    return viewProvider.asAnchor(e);
  }

  @Override
  public ParticipantView getParticipantView(Conversation conv, ParticipantId source) {
    Element e = Document.get().getElementById(viewIdMapper.participantOf(conv, source));
    return viewProvider.asParticipant(e);
  }

  @Override
  public ParticipantsView getParticipantsView(Conversation conv) {
    Element e = Document.get().getElementById(viewIdMapper.participantsOf(conv));
    return viewProvider.asParticipants(e);
  }

  @Override
  public ConversationView getConversationView(Conversation conv) {
    Element e = Document.get().getElementById(viewIdMapper.conversationOf(conv));
    return viewProvider.asConversation(e);
  }

  @Override
  public ConversationBlip getBlip(BlipView blipUi) {
    return viewIdMapper.blipOf(blipUi.getId());
  }

  @Override
  public ConversationThread getThread(ThreadView threadUi) {
    return viewIdMapper.threadOf(threadUi.getId());
  }

  @Override
  public Conversation getParticipants(ParticipantsView participantsUi) {
    return viewIdMapper.participantsOf(participantsUi.getId());
  }

  @Override
  public Pair<Conversation, ParticipantId> getParticipant(ParticipantView participantUi) {
    return viewIdMapper.participantOf(participantUi.getId());
  }
}
