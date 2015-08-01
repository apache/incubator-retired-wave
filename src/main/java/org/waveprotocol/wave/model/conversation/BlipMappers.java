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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.util.Predicate;


/**
 * Mappers for conversations.
 *
 * @author hearnden@google.com (David Hearnden)
 * @see BlipMappers
 */
public final class BlipMappers {

  private static final class Exit extends RuntimeException {
    public static final Exit INSTANCE = new Exit();
    private Exit() {}
  }

  /**
   * A mapper that applies depth-first.
   */
  private static class DepthFirst {
    private final Predicate<? super ConversationBlip> p;

    DepthFirst(Predicate<? super ConversationBlip> p) {
      this.p = p;
    }

    void apply(ConversationView wave) {
      for (Conversation conversation : wave.getConversations()) {
        apply(conversation);
      }
    }

    void apply(Conversation conversation) {
      apply(conversation.getRootThread());
    }

    void apply(ConversationThread thread) {
      for (ConversationBlip blip : thread.getBlips()) {
        apply(blip);
      }
    }

    void apply(ConversationBlip blip) {
      if (!p.apply(blip)) {
        throw Exit.INSTANCE;
      }
      for (ConversationThread thread : blip.getReplyThreads()) {
        apply(thread);
      }
    }
  }

  public static void depthFirst(Predicate<? super ConversationBlip> p, ConversationView wave) {
    try {
      new DepthFirst(p).apply(wave);
    } catch (Exit e) {
    }
  }

  public static void depthFirst(Predicate<? super ConversationBlip> p, Conversation conversation) {
    try {
      new DepthFirst(p).apply(conversation);
    } catch (Exit e) {
    }
  }

  public static void depthFirst(Predicate<? super ConversationBlip> p, ConversationThread thread) {
    try {
      new DepthFirst(p).apply(thread);
    } catch (Exit e) {
    }
  }

  public static void depthFirst(Predicate<? super ConversationBlip> p, ConversationBlip blip) {
    try {
      new DepthFirst(p).apply(blip);
    } catch (Exit e) {
    }
  }
}
