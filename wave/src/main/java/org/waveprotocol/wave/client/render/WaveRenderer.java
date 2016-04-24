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
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A wave renderer can render any part of a wave. A rendering of a wave part
 * includes renderings of its sub-parts.
 * <p>
 * Note that a wave renderer can be constructed from modular definitions of how
 * to render each component of a wave, by injecting the modular definitions (
 * {@link RenderingRules}) into a {@link ReductionBasedRenderer}.
 */
public interface WaveRenderer<T> {

  /** Renders a wave.  Returns null if there are no conversations to render. */
  T render(ConversationView wave);

  /** Renders a conversation. */
  T render(Conversation conversation);

  /** Renders an anchor for a thread. */
  T render(ConversationThread thread);

  /** Renders a blip. */
  T render(ConversationBlip blip);

  /** Renders a participant. */
  T render(Conversation conversation, ParticipantId participant);

}
