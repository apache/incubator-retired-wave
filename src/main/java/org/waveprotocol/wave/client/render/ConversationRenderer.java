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


package org.waveprotocol.wave.client.render;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationStructure;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * Traverses a ConversationView to the view model.
 *
 * @see ReductionBasedRenderer for an alternative (parser) style of producing a
 *      rendering.
 */
public final class ConversationRenderer {
  // TODO(user): Make final after callers of the deprecated constructor are
  // updated.

  /** Visitor through which view structure is pumped. */
  private RendererHelper generator;

  /** Reveals anchoring relationships. */
  private final ConversationStructure structure;

  // Only to support deprecated API.
  private final ConversationView model;

  /**
   * Creates a renderer.
   *
   * @deprected Use {@link #renderWith(RendererHelper, ConversationView)}
   *            instead.
   */
  @Deprecated
  public ConversationRenderer(ConversationView model) {
    this.generator = null;
    this.structure = ConversationStructure.of(model);
    this.model = model;
  }

  /**
   * Creates a renderer.
   */
  private ConversationRenderer(ConversationStructure structure, RendererHelper generator) {
    this.generator = generator;
    this.structure = structure;
    this.model = null;
  }

  /**
   * Creates a renderer.
   *
   * @param generator rendering generator
   * @param model model to render
   */
  public static ConversationRenderer create(RendererHelper generator, ConversationView model) {
    return new ConversationRenderer(ConversationStructure.of(model), generator);
  }

  /**
   * Renders a group of conversations.
   *
   * @param generator rendering generator
   * @param model model to render
   */
  public static void renderWith(RendererHelper generator, ConversationView model) {
    create(generator, model).processConversationView(model);
  }

  /**
   * Renders a thread.
   *
   * @param generator rendering generator
   * @param model conversational context
   * @param thread thread to render
   */
  public static void renderWith(RendererHelper generator, ConversationView model,
      ConversationThread thread) {
    new ConversationRenderer(ConversationStructure.of(model), generator).processThread(thread);
  }

  /**
   * Renders a blip.
   *
   * @param generator rendering generator
   * @param model conversational context
   * @param blip blip to render
   */
  public static void renderWith(RendererHelper generator, ConversationView model,
      ConversationBlip blip) {
    new ConversationRenderer(ConversationStructure.of(model), generator).processBlip(blip);
  }


  /**
   * @deprecated Use {@link #renderWith(RendererHelper, ConversationView)}
   *             instead.
   */
  @Deprecated
  public void walkContainers(RendererHelper generator) {
    this.generator = generator;
    processConversationView(model);
  }

  public void processConversationView(ConversationView view) {
    generator.startView(view);
    Conversation root = structure.getMainConversation();
    if (root != null) {
      processConversation(root);
    }
    generator.endView(view);
  }

  public void processConversation(Conversation conversation) {
    generator.startConversation(conversation);
    processThread(conversation.getRootThread());
    generator.endConversation(conversation);
  }

  public void processThread(ConversationThread thread) {
    generator.startThread(thread);
    for (ConversationBlip blip : thread.getBlips()) {
      processBlip(blip);
    }
    generator.endThread(thread);
  }

  public void processInlineThread(ConversationThread thread) {
    generator.startInlineThread(thread);
    for (ConversationBlip blip : thread.getBlips()) {
      processBlip(blip);
    }
    generator.endInlineThread(thread);
  }

  public void processBlip(ConversationBlip blip) {
    generator.startBlip(blip);

    for (ConversationBlip.LocatedReplyThread<? extends ConversationThread> inlineReply : blip
        .locateReplyThreads()) {
      processInlineThread(inlineReply.getThread());
    }

    // process all the anchor to this blip
    for (Conversation privateReply : structure.getAnchoredConversations(blip)) {
      processConversation(privateReply);
    }

    generator.endBlip(blip);
  }
}
