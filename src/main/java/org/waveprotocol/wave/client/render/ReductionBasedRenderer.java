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
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Builds a rendering of conversations in a wave, by folding together renderings
 * from production rules in {@link RenderingRules}.
 *
 * @see ConversationRenderer for an alternative (SAX) style of producing a
 *      rendering.
 */
public final class ReductionBasedRenderer<R> implements WaveRenderer<R> {

  /** Nesting structure of conversations. */
  private final ConversationStructure structure;

  /** Production rules. */
  private final RenderingRules<R> builders;

  /** Creates a rendering builder. */
  private ReductionBasedRenderer(RenderingRules<R> builders, ConversationStructure structure) {
    this.builders = builders;
    this.structure = structure;
  }

  /** @return a renderer of {@code wave}, using {@code builders}. */
  public static <R> ReductionBasedRenderer<R> of(
      RenderingRules<R> builders, ConversationView wave) {
    return new ReductionBasedRenderer<R>(builders, ConversationStructure.of(wave));
  }

  /** @return a rendering of {@code wave}, using {@code builders}. */
  public static <R> R renderWith(RenderingRules<R> builders, ConversationView wave) {
    return of(builders, wave).render(wave);
  }

  @Override
  public R render(ConversationView wave) {
    IdentityMap<Conversation, R> conversations = CollectionUtils.createIdentityMap();
    Conversation c = structure.getMainConversation();
    if (c != null) {
      conversations.put(c, render(c));
    }
    return builders.render(wave, conversations);
  }

  @Override
  public R render(Conversation conversation) {
    StringMap<R> participants = CollectionUtils.createStringMap();
    for (ParticipantId participant : conversation.getParticipantIds()) {
      participants.put(participant.getAddress(), render(conversation, participant));
    }

    return builders.render(conversation, builders.render(conversation, participants),
        renderInner(conversation.getRootThread()));
  }

  @Override
  public R render(Conversation conversation, ParticipantId participant) {
    return builders.render(conversation, participant);
  }

  /** @return the rendering of {@code thread}, without a surrounding anchor. */
  private R renderInner(ConversationThread thread) {
    IdentityMap<ConversationBlip, R> blips = null;
    for (ConversationBlip blip : thread.getBlips()) {
      if (blips == null) {
        blips = CollectionUtils.createIdentityMap();
      }
      blips.put(blip, render(blip));
    }
    return builders.render(thread, nonNull(blips));
  }

  @Override
  public R render(ConversationThread thread) {
    return builders.render(thread, renderInner(thread));
  }

  @Override
  public R render(ConversationBlip blip) {
    // Threads.
    IdentityMap<ConversationThread, R> threadRs = null;
    for (ConversationThread reply : blip.getReplyThreads()) {
      if (threadRs == null) {
        threadRs = CollectionUtils.createIdentityMap();
      }
      threadRs.put(reply, renderInner(reply));
    }
    threadRs = nonNull(threadRs);

    // Nested conversations.
    IdentityMap<Conversation, R> nestedRs = null;
    for (Conversation conversation : structure.getAnchoredConversations(blip)) {
      if (nestedRs == null) {
        nestedRs = CollectionUtils.createIdentityMap();
      }
      nestedRs.put(conversation, render(conversation));
    }
    nestedRs = nonNull(nestedRs);

    // Document.
    R documentR = builders.render(blip, threadRs);

    // Default-anchored threads.
    IdentityMap<ConversationThread, R> defaultAnchorRs = null;
    for (ConversationThread reply : blip.getReplyThreads()) {
      if (defaultAnchorRs == null) {
        defaultAnchorRs = CollectionUtils.createIdentityMap();
      }
      defaultAnchorRs.put(reply, builders.render(reply, threadRs.get(reply)));
    }
    defaultAnchorRs = nonNull(defaultAnchorRs);

    // Render blip.
    return builders.render(blip, documentR, defaultAnchorRs, nestedRs);
  }

  private static <K, V> IdentityMap<K, V> nonNull(IdentityMap<K, V> source) {
    return source != null ? source : CollectionUtils.<K, V>emptyIdentityMap();
  }
}
