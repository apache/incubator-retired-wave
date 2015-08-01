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


package org.waveprotocol.wave.client.wavepanel.block.xml;

import org.waveprotocol.wave.client.render.ConversationRenderer;
import org.waveprotocol.wave.client.render.RendererHelper;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.NodeType;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * Generator that renders a conversation collection into an XML block structure.
 *
 */
public final class XmlRenderer implements RendererHelper {
  public final static String ROOT_ID = "wave";

  /** Buffer used to build up rendering string. */
  private StringBuffer s;

  // Development hack.
  // Used to select the Nth editor as a focus target.
  private String selected;
  private int count;

  private final ViewIdMapper viewIdMapper;

  private XmlRenderer(ViewIdMapper viewIdMapper) {
    this.viewIdMapper = viewIdMapper;
  }

  /**
   * Renders the block structure of a collection of conversations into XML.
   *
   * @param model model to render
   * @return XML of block structure.
   */
  public static String render(ViewIdMapper viewIdMapper, ConversationView model) {
    return new XmlRenderer(viewIdMapper).getRendering(model);
  }

  /**
   * Invokes the conversation renderer through this generator, returning the
   * generated XML string.
   */
  private String getRendering(ConversationView model) {
    ConversationRenderer.renderWith(this, model);
    return s.toString();
  }

  /** Opens an element. */
  private void enter(String id, NodeType type) {
    s.append("<x id=\"X" + id + "\" k=\"" + type.ordinal() + "\">");
  }

  /** Closes the current element. */
  private void leave() {
    s.append("</x>");
  }

  @Override
  public void startView(ConversationView conv) {
    s = new StringBuffer();
    selected = null;
    count = 10;  // Select the 10th editor.

    enter(ROOT_ID, NodeType.ROOT);
  }

  @Override
  public void endView(ConversationView conv) {
    leave();
    // Append selecting element
    if (selected != null) {
      s.append("<y id=\"Xselected\" ref=\"" + selected + "\"></y>");
    }
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

    if (count-- == 0) {
      selected = viewIdMapper.metaOf(blip);
    }
  }


  @Override
  public void endBlip(ConversationBlip blip) {
    leave();
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
}
