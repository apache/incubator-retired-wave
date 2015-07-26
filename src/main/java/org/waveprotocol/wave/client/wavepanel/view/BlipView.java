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
import org.waveprotocol.wave.model.conversation.ConversationThread;


/**
 * A view interface for a blip.
 *
 */
public interface BlipView extends View, IntrinsicBlipView {

  //
  // Structure.
  //

  BlipMetaView getMeta();

  AnchorView insertDefaultAnchorBefore(AnchorView ref, ConversationThread t);
  AnchorView insertDefaultAnchorAfter(AnchorView ref, ConversationThread t);

  AnchorView getDefaultAnchorBefore(AnchorView ref);
  AnchorView getDefaultAnchorAfter(AnchorView ref);

  InlineConversationView getConversationBefore(InlineConversationView ref);
  InlineConversationView getConversationAfter(InlineConversationView ref);
  InlineConversationView insertConversationBefore(InlineConversationView ref, Conversation c);

  BlipLinkPopupView createLinkPopup();

  /**
   * @return the thread that contains this blip.
   */
  ThreadView getParent();
}
