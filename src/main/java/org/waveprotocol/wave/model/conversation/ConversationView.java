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

import java.util.Collection;


/**
 * A forest of {@link Conversation} trees, usually viewed under some filter such
 * as participation.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ConversationView {
  /**
   * Gets the globally unique identifier of this group of conversations.
   *
   * @return the globally unique identifier of this group of conversations.
   */
  String getId();

  /**
   * Gets a view of the conversations in this view. The collection is not
   * modifiable.
   */
  Collection<? extends Conversation> getConversations();

  /**
   * Retrieves a Conversation from the Conversation view by id.
   *
   * @param conversationId id of Conversation to return
   * @return the Conversation with the given id, or null if no such conversation exists.
   */
  Conversation getConversation(String conversationId);

  // TODO(anorth/zdwang): remove the concept of a distinguished root.

  /**
   * Gets the root conversation from this view, if one exists.
   *
   * @return a conversation, or null
   */
  Conversation getRoot();

  /**
   * Creates a root conversation in this view.
   *
   * @return the created conversation
   * @throws IllegalStateException if this view already has a root conversation
   */
  Conversation createRoot();

  /**
   * Creates a new conversation in this view.
   */
  Conversation createConversation();
}
