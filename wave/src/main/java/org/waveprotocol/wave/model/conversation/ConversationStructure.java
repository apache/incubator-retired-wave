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

import org.waveprotocol.wave.model.conversation.Conversation.Anchor;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;

import java.util.Collection;
import java.util.Collections;

/**
 * Exposes the structure in a conversation view. This structure is not live; it
 * does not remain synchronized with the conversation structure as it changes.
 */
public final class ConversationStructure {

  /** Maps blips to conversations anchored at that blip. Never null. */
  private final IdentityMap<ConversationBlip, Collection<Conversation>> anchoring;

  /** Collection of non-root conversations not attached to any blip. Never null. */
  private final Collection<Conversation> unanchored;

  /** The main conversation, if there is one. */
  private final Conversation mainConversation;

  /**
   * Creates a conversation structure.
   */
  private ConversationStructure(IdentityMap<ConversationBlip, Collection<Conversation>> anchoring,
      Collection<Conversation> unanchored, Conversation mainConversation) {
    this.anchoring = anchoring;
    this.unanchored = unanchored;
    this.mainConversation = mainConversation;
  }

  /** @return the structure of a conversations in {@code wave}. */
  public static ConversationStructure of(ConversationView wave) {
    IdentityMap<ConversationBlip, Collection<Conversation>> anchoring = null;
    Collection<Conversation> unanchored = null;
    Conversation mainConversation = getMainConversation(wave);

    for (Conversation conversation : wave.getConversations()) {
      if (conversation == mainConversation) {
        continue;
      }

      Anchor anchor = conversation.getAnchor();
      ConversationBlip blip = anchor != null ? anchor.getBlip() : null;
      if (blip != null) {
        addLazily((anchoring = createIfNull(anchoring)), blip, conversation);
      } else {
        (unanchored = createIfNull(unanchored)).add(conversation);
      }
    }

    if (anchoring == null) {
      anchoring = CollectionUtils.emptyIdentityMap();
    }
    if (unanchored == null) {
      unanchored = Collections.emptySet();
    }
    return new ConversationStructure(anchoring, unanchored, mainConversation);
  }

  /**
   * Adds an item to the value collection in a map, creating the collection if
   * it does not yet exist.
   */
  private static <K, V> void addLazily(IdentityMap<K, Collection<V>> map, K key, V value) {
    Collection<V> list = map.get(key);
    if (list == null) {
      list = CollectionUtils.createQueue();
      map.put(key, list);
    }
    list.add(value);
  }

  /** @return a non-null version of {@code list}. */
  private static <T> Collection<T> createIfNull(Collection<T> list) {
    return list != null ? list : CollectionUtils.<T> createQueue();
  }

  /** @return a non-null version of {@code map}. */
  private static <K, V> IdentityMap<K, V> createIfNull(IdentityMap<K, V> map) {
    return map != null ? map : CollectionUtils.<K, V> createIdentityMap();
  }

  /** @return the conversations anchored at {@code blip}. Never null. */
  public Collection<Conversation> getAnchoredConversations(ConversationBlip blip) {
    Collection<Conversation> anchored = anchoring.get(blip);
    return anchored != null ? anchored : Collections.<Conversation> emptySet();
  }

  /** @return the non-root conversations not anchored to any blip. */
  public Collection<Conversation> getUnanchored() {
    return unanchored;
  }

  /** @return the main conversation in this wave. */
  public Conversation getMainConversation() {
    return mainConversation;
  }

  /**
   * Finds the main conversation in a wave. The main conversation is defined as
   * the root if there is one, or the first unanchored conversation otherwise.
   */
  public static Conversation getMainConversation(ConversationView view) {
    Conversation root = view.getRoot();
    if (root == null) {
      for (Conversation curr : view.getConversations()) {
        if (!curr.hasAnchor()) {
          root = curr;
          break;
        }
      }
    }
    return root;
  }
}
