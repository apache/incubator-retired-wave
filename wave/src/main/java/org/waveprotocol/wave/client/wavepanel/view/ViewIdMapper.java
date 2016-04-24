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
 * Provides a projective mapping from model identifiers to view components.
 *
 */
public final class ViewIdMapper {
  private final ModelIdMapper modelMapper;

  public ViewIdMapper(ModelIdMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  public String frameOf(ConversationView c) {
    return modelMapper.conversationsId(c) + "F";
  }

  public String blipOf(ConversationBlip b) {
    return modelMapper.blipId(b) + "B";
  }

  public String metaOf(ConversationBlip b) {
    return modelMapper.blipId(b) + "M";
  }

  public String conversationOf(Conversation c) {
    return modelMapper.convId(c) + "C";
  }

  public String participantsOf(Conversation c) {
    return modelMapper.convId(c) + "P";
  }

  public String participantOf(Conversation c, ParticipantId p) {
    return modelMapper.participantId(c, p) + "P";
  }

  public String threadOf(ConversationThread t) {
    return modelMapper.threadId(t) + "T";
  }

  public String replyIndicatorOf(ConversationThread t) {
    return modelMapper.threadId(t) + "I";
  }

  /** Identifies the default anchor of a thread. */
  public String defaultAnchorOf(ConversationThread t) {
    return defaultAnchor(anchorRoot(t.getParentBlip(), t.getId()));
  }

  /** Identifies the inline anchor of a hypothetical thread. */
  public String inlineAnchorOf(ConversationBlip blip, String threadId) {
    // Anchor ids must not depend on the thread object - parent blip and thread
    // id only. (It should still be possible to construct the id of an anchor if
    // its referenced thread does not exist).
    return inlineAnchor(anchorRoot(blip, threadId));
  }

  /** Converts an inline-anchor id to a default-anchor id. */
  public static String defaultOfInlineAnchor(String id) {
    return defaultAnchor(baseOf(id));
  }

  /** Converts an default-anchor id to an inlie-anchor id. */
  public static String inlineOfDefaultAnchor(String id) {
    return inlineAnchor(baseOf(id));
  }

  /** @return the base id for a thread's anchors. */
  private String anchorRoot(ConversationBlip blip, String threadId) {
    // HACK(hearnden/reuben): this is not safe (can be incorrect on IE).
    // TODO(user): Do IDs properly, but doing the shortening in the view
    // space, not model space. That will also mean that all view IDs are already
    // HTML escaped (currently they are not).
    return modelMapper.blipId(blip) + threadId;
  }

  /** @return the DOM id of a default anchor, given the anchor base id. */
  private static String defaultAnchor(String baseId) {
    return baseId + "D";
  }

  /** @return the DOM id of an inline anchor, given the anchor base id. */
  private static String inlineAnchor(String baseId) {
    return baseId + "I";
  }

  /** @return the base id of a view id. */
  private static String baseOf(String id) {
    return id.substring(0, id.length() - 1);
  }

  //
  // Reverse mappings.
  //

  public ConversationBlip blipOf(String domId) {
    return modelMapper.locateBlip(baseOf(domId));
  }

  public ConversationThread threadOf(String domId) {
    return modelMapper.locateThread(baseOf(domId));
  }

  public Conversation participantsOf(String domId) {
    return modelMapper.locateConversation(baseOf(domId));
  }

  public Pair<Conversation, ParticipantId> participantOf(String domId) {
    return modelMapper.locateParticipant(baseOf(domId));
  }
}
