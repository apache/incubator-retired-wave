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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentityMap.Reduce;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.EnumSet;
import java.util.Stack;

/**
 * A renderer helper that uses {@link RenderingRules production rules} to
 * perform reductions, resulting in a rendering.
 *
 */
public final class ReducingRendererHelper<R> implements ResultProducingRenderHelper<R> {
  enum Type {
    BLIP, THREAD, CONVERSATION, WAVE
  }

  /**
   * A rendering scope. It contains a map of the objects that have been rendered
   * in this scope.
   */
  private final static class Scope<R> {
    private final EnumSet<Type> expected;
    private IdentityMap<ConversationBlip, R> blips;
    private IdentityMap<ConversationThread, R> threads;
    private IdentityMap<Conversation, R> conversations;
    private IdentityMap<ConversationView, R> waves;

    public Scope(EnumSet<Type> expected) {
      this.expected = expected;
    }

    private void checkScopeExpects(Type t) {
      if (!expected.contains(t)) {
        throw new IllegalStateException("Encountered a " + t + " in a scope that only expected: "
            + expected);
      }
    }

    /** Adds a rendering of blip to this scope. */
    void add(ConversationBlip blip, R rendering) {
      checkScopeExpects(Type.BLIP);
      if (blips == null) {
        blips = CollectionUtils.createIdentityMap();
      }
      blips.put(blip, rendering);
    }

    /** Adds a rendering of blip to this scope. */
    void add(ConversationThread thread, R rendering) {
      checkScopeExpects(Type.THREAD);
      if (threads == null) {
        threads = CollectionUtils.createIdentityMap();
      }
      threads.put(thread, rendering);
    }

    /** Adds a rendering of conversation to this scope. */
    void add(Conversation conversation, R rendering) {
      checkScopeExpects(Type.CONVERSATION);
      if (conversations == null) {
        conversations = CollectionUtils.createIdentityMap();
      }
      conversations.put(conversation, rendering);
    }

    /** Adds a rendering of wave to this scope. */
    void add(ConversationView wave, R rendering) {
      checkScopeExpects(Type.WAVE);
      if (waves == null) {
        waves = CollectionUtils.createIdentityMap();
      }
      waves.put(wave, rendering);
    }

    IdentityMap<ConversationBlip, R> getBlips() {
      return blips != null ? blips : CollectionUtils.<ConversationBlip, R> emptyIdentityMap();
    }

    IdentityMap<ConversationThread, R> getThreads() {
      return threads != null ? threads : CollectionUtils.<ConversationThread, R> emptyIdentityMap();
    }

    IdentityMap<Conversation, R> getConversations() {
      return conversations != null ? conversations // \u2620
          : CollectionUtils.<Conversation, R> emptyIdentityMap();
    }

    IdentityMap<ConversationView, R> getWaves() {
      return waves != null ? waves : CollectionUtils.<ConversationView, R> emptyIdentityMap();
    }
  }

  /** Scope stack. */
  private final Stack<Scope<R>> scopes = new Stack<Scope<R>>();

  /** Production rules. */
  private final RenderingRules<R> builders;

  private Scope<R> result;

  private ReducingRendererHelper(RenderingRules<R> builders) {
    this.builders = builders;
  }

  /**
   * Creates a rendering builder, driven by a {@link ConversationRenderer}, that
   * drives a set of production rules.
   *
   * @param builders implementation of production rules
   */
  public static <R> ReducingRendererHelper<R> of(RenderingRules<R> builders) {
    return new ReducingRendererHelper<R>(builders);
  }

  /**
   * Creates a rendering builder, driven by a {@link ConversationRenderer}, that
   * drives a set of production rules.
   *
   * @param builders implementation of production rules
   */
  public static <R> ReducingRendererHelper<R> ofStarted(RenderingRules<R> builders) {
    ReducingRendererHelper<R> helper = of(builders);
    return helper;
  }

  /**
   * Starts a rendering.
   */
  public void begin() {
    // Accept anything.
    enter(EnumSet.allOf(Type.class));
    result = null;
  }

  public void end() {
    result = leave();
    Preconditions.checkState(scopes.isEmpty());
  }

  @Override
  public R getResult() {
    Reduce<Object, R, R> aggregator = new Reduce<Object, R, R>() {
      @Override
      public R apply(R soFar, Object key, R item) {
        if (soFar != null) {
          throw new IllegalStateException("scope contains multiple renderings");
        } else {
          return item;
        }
      }
    };
    // Thread aggregator through the rendering maps, ensuring there is at most
    // one rendering in any of them.
    R rendering = null;
    rendering = result.getBlips().reduce(rendering, aggregator);
    rendering = result.getThreads().reduce(rendering, aggregator);
    rendering = result.getConversations().reduce(rendering, aggregator);
    rendering = result.getWaves().reduce(rendering, aggregator);
    return rendering;
  }

  /**
   * Enters a new rendering scope.
   */
  private void enter(EnumSet<Type> expected) {
    scopes.push(new Scope<R>(expected));
  }

  /**
   * Leaves a rendering scope.
   *
   * @return the renderings of objects that were rendered in this scope.
   */
  private Scope<R> leave() {
    return scopes.pop();
  }

  /**
   * Queries for the current scope.
   */
  private Scope<R> currentScope() {
    return scopes.peek();
  }

  //
  // The pattern of these methods is as follows:
  // * start() methods just enter() a new scope.
  // * end() methods leave() a scope, then (optionally) add a rendering of the
  // object whose scope has just been left.
  //

  @Override
  public void startView(ConversationView wave) {
    enter(EnumSet.of(Type.CONVERSATION));
  }

  @Override
  public void endView(ConversationView wave) {
    IdentityMap<Conversation, R> convs = leave().getConversations();
    currentScope().add(wave, builders.render(wave, convs));
  }

  @Override
  public void startConversation(Conversation conv) {
    enter(EnumSet.of(Type.THREAD));
  }

  @Override
  public void endConversation(Conversation conv) {
    StringMap<R> allParticipants = CollectionUtils.createStringMap();
    for (ParticipantId participant : conv.getParticipantIds()) {
      allParticipants.put(participant.getAddress(), builders.render(conv, participant));
    }
    R participants = builders.render(conv, allParticipants);
    R rootThread = leave().getThreads().get(conv.getRootThread());
    currentScope().add(conv, builders.render(conv, participants, rootThread));
  }

  @Override
  public void startBlip(final ConversationBlip blip) {
    enter(EnumSet.of(Type.THREAD, Type.CONVERSATION));
  }

  @Override
  public void endBlip(ConversationBlip blip) {
    Scope<R> scope = leave();
    IdentityMap<ConversationThread, R> threads = scope.getThreads();
    IdentityMap<Conversation, R> nestedConversations = scope.getConversations();
    R document = builders.render(blip, threads);

    // Replace thread renderings with anchor renderings.
    IdentityMap<ConversationThread, R> defaultAnchors = CollectionUtils.createIdentityMap();
    for (ConversationThread reply : blip.getReplyThreads()) {
      defaultAnchors.put(reply, builders.render(reply, threads.get(reply)));
    }

    currentScope().add(blip, builders.render(blip, document, defaultAnchors, nestedConversations));
  }

  @Override
  public void startThread(ConversationThread thread) {
    enter(EnumSet.of(Type.BLIP));
  }

  @Override
  public void endThread(ConversationThread thread) {
    IdentityMap<ConversationBlip, R> blips = leave().getBlips();

    // Only include thread rendering if there was some component rendering.
    currentScope().add(thread, builders.render(thread, blips));
  }

  @Override
  public void startInlineThread(ConversationThread thread) {
    // Pretend it's a regular thread.
    startThread(thread);
  }

  @Override
  public void endInlineThread(ConversationThread thread) {
    // Pretend it's a regular thread.
    endThread(thread);
  }
}
