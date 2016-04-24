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

package org.waveprotocol.wave.client.wavepanel.view;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Maps between model objects and model ids.
 *
 * A model id uniquely identifies a model object within other model objects of
 * the same type, and within the current model session. The model ids for two
 * distinct blips will be different. However, the model id for a blip may be the
 * same as the model id for some thread.
 *
 */
public interface ModelIdMapper {
  /** @return the model id of a group of conversations {@code c}. */
  String conversationsId(ConversationView c);

  /** @return the model id of a conversation {@code c}. */
  String convId(Conversation c);

  /** @return the model id of a thread {@code c}. */
  String threadId(ConversationThread t);

  /** @return the model id of a blip {@code c}. */
  String blipId(ConversationBlip b);

  /** @return the model id of a participant {@code p}. */
  String participantId(Conversation c, ParticipantId p);

  //
  // Reverse mappings.
  //

  /** @return the blip identified by {@code modelId}, if there is one. */
  ConversationBlip locateBlip(String modelId);

  /** @return the thread identified by {@code modelId}, if there is one. */
  ConversationThread locateThread(String modelId);

  /** @return the conversation identified by {@code modelId}, if there is one. */
  Conversation locateConversation(String modelId);

  /** @return the participant identified by {@code modelId}, if there is one. */
  Pair<Conversation, ParticipantId> locateParticipant(String modelId);
}
