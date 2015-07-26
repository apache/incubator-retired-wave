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


package org.waveprotocol.wave.client.wavepanel.block.pojo;

import org.waveprotocol.wave.client.render.ConversationRenderer;
import org.waveprotocol.wave.client.render.RendererHelper;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.NodeType;
import org.waveprotocol.wave.client.wavepanel.block.pojo.PojoStructure.NodeImpl;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * Generator that renders a conversation collection into a
 * {@link PojoStructure}.
 *
 */
public final class PojoRenderer implements RendererHelper {

  private final PojoStructure structure;
  private final ViewIdMapper viewIdMapper;
  private NodeImpl current;

  private PojoRenderer(PojoStructure structure, ViewIdMapper viewIdMapper) {
    this.structure = structure;
    this.viewIdMapper = viewIdMapper;
  }

  /**
   * Renders a collection of conversations into a pojo view structure.
   *
   * @param viewIdMapper Mapper for mapping model object to view id.
   * @param model conversations to render
   * @return view
   */
  public static PojoStructure render(ViewIdMapper viewIdMapper, ConversationView model) {
    return new PojoRenderer(PojoStructure.create(), viewIdMapper).of(model);
  }

  /**
   * Invokes the conversation renderer through this generator, returning the
   * generated structure.
   */
  private PojoStructure of(ConversationView model) {
    ConversationRenderer.renderWith(this, model);
    return structure;
  }

  private void enter(String id, NodeType type) {
    current = current.createChild(id, type);
  }

  private void leave() {
    current = current.getParent();
  }

  @Override
  public void startView(ConversationView view) {
    current = structure.getRoot();
  }

  @Override
  public void startConversation(Conversation conv) {
    enter(viewIdMapper.conversationOf(conv), NodeType.CONVERSATION);
    enter(viewIdMapper.participantsOf(conv), NodeType.PARTICIPANTS);
    leave();
  }

  @Override
  public void startThread(ConversationThread thread) {
    enter(viewIdMapper.threadOf(thread), NodeType.THREAD);
  }

  @Override
  public void startInlineThread(ConversationThread thread) {
    enter(viewIdMapper.threadOf(thread), NodeType.THREAD);
  }

  @Override
  public void startBlip(ConversationBlip blip) {
    enter(viewIdMapper.blipOf(blip), NodeType.BLIP);
    enter(viewIdMapper.metaOf(blip), NodeType.META);
    leave();
  }

  @Override
  public void endView(ConversationView view) {
    current = null;
  }

  @Override
  public void endConversation(Conversation conv) {
    leave();
  }

  @Override
  public void endThread(ConversationThread thread) {
    leave();
  }

  @Override
  public void endInlineThread(ConversationThread thread) {
    leave();
  }

  @Override
  public void endBlip(ConversationBlip blip) {
    leave();
  }
}
