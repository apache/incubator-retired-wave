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
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * Generator interface for processing a conversation rendering. The structure is
 * a recursive tree structure of conversations, threads, participants, and
 * blips. This generator is invoked with the structure of the rendering in depth
 * first order, and produces a rendering artifact.
 *
 * A rendering has the following structure:
 *
 * <pre>
 *   View: Conversation*
 *   Conversation: Participants Thread
 *   Thread: (Conversation | Blip)*
 *   Blip: Thread*
 * </pre>
 *
 * A typical call sequence is as following:
 *
 * <pre>
 *   begin
 *   beginView
 *     beginConversation(rootConversation)
 *       beginThread(rootThread)
 *         beginBlip(rootBlip)
 *           beginThread(replyThread)
 *             beginBlip(childBlip)
 *             endBlip(childBlip)
 *           endThread(replyThread)
 *         endBlip(rootBlip)
 *         beginConversation(privateReply)
 *           beginParticipants(privateReply)
 *           endParticipants(privateReply)
 *           beginThread(privateThread)
 *             beginBlip(privateBlip)
 *             endBlip(privateBlip)
 *           endThread(privateThread)
 *         endConversation(privateReply)
 *       endThread(rootThread)
 *     endConversation(rootConversation)
 *   endView
 *   end
 * </pre>
 *
 */
public interface RendererHelper {

  /** Starts processing a conversation view. */
  void startView(ConversationView view);

  /** Ends processing a conversation view. */
  void endView(ConversationView view);

  /** Starts processing a conversation. */
  void startConversation(Conversation conv);

  /** Ends processing a conversation. */
  void endConversation(Conversation conv);

  /** Starts processing a thread. */
  void startThread(ConversationThread thread);

  /** Ends processing a thread. */
  void endThread(ConversationThread thread);

  /** Starts processing of a thread inlined in a blip. */
  void startInlineThread(ConversationThread thread);

  /** Ends processing of a thread inlined in a blip. */
  void endInlineThread(ConversationThread thread);

  /** Starts processing a blip. */
  void startBlip(ConversationBlip blip);

  /** Ends processing a blip. */
  void endBlip(ConversationBlip blip);
}
