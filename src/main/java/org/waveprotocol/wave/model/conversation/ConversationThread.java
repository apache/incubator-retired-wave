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

import org.waveprotocol.wave.model.document.operation.DocInitialization;


/**
 * A sequence of blips in a conversation.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ConversationThread {
  /**
   * Gets the conversation of which this thread is a part.
   */
  Conversation getConversation();

  /**
   * Gets the blip to which this thread is a reply. This is null for the root
   * thread.
   *
   * @return the blip to which this thread is a reply, or null
   */
  ConversationBlip getParentBlip();

  /**
   * Gets the blips in this thread in sequential order.
   */
  Iterable<? extends ConversationBlip> getBlips();

  /**
   * A convenience method returning the first blip in this thread, if it has
   * one, else null.
   */
  ConversationBlip getFirstBlip();

  /**
   * Creates a new blip and appends it to this thread.
   *
   * @return the new blip
   */
  ConversationBlip appendBlip();

  /**
   * Creates a new blip with specified content, and appends it to this thread.
   *
   * @param content specification for the initial state of the new blip
   * @return the new blip
   */
  ConversationBlip appendBlip(DocInitialization content);

  /**
   * Creates a new blip and inserts it before another in this thread.
   *
   * @param successor blip before which to insert the new blip
   * @return the new blip
   */
  ConversationBlip insertBlip(ConversationBlip successor);

  /**
   * @return id of the thread.
   */
  String getId();

  /**
   * Deletes all blips in the thread and the thread itself if it's not the root
   * thread.
   */
  void delete();
}
