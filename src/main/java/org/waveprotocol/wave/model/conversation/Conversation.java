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

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Set;

/**
 * A set of participants and a tree-like structure of threads of blips in which
 * they participate. A conversation also contains data documents, some of which
 * are interpreted to implement the blip structure, tags, etc.
 *
 * Conversations may reference other conversations in a tree structure as part
 * of a {@link ConversationView}.
 *
 * @author anorth@google.com (Alex North)
 */
public interface Conversation {
  /**
   * An anchor point inside a conversation.
   */
  final class Anchor {
    private final Pair<Conversation, ConversationBlip> anchor;

    /**
     * Creates a new anchor to a blip in a conversation. At least one of {@code
     * blip} or {@code innerBlip} must be non-null. The client currently
     * provides and uses only the innerBlip, but will provide/use the
     * conversation blip after it's hooked up to this model.
     *
     * TODO(anorth): remove the innerBlip after properly hooking up the client.
     */
    public Anchor(Conversation conversation, ConversationBlip blip) {
      Preconditions.checkNotNull(conversation, "anchor conversation cannot be null");
      Preconditions.checkNotNull(blip, "anchro blip cannot be null");
      this.anchor = Pair.of(conversation, blip);
    }

    public Conversation getConversation() {
      return anchor.getFirst();
    }

    public ConversationBlip getBlip() {
      return anchor.getSecond();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Anchor)) {
        return false;
      }
      Anchor other = (Anchor) obj;
      return anchor.equals(other.anchor);
    }

    @Override
    public int hashCode() {
      return anchor.hashCode();
    }
  }

  /**
   * True if this conversation is anchored in another and that anchoring
   * conversation is accessible.
   */
  boolean hasAnchor();

  /**
   * Gets the anchor point of this conversation inside its parent. Null iff
   * {@link #hasAnchor()} is false.
   */
  Anchor getAnchor();

  /**
   * Sets the anchor point for this conversation. The anchor must not refer to
   * this conversation.
   *
   * @param newAnchor new anchor description, or null to clear anchor
   */
  void setAnchor(Anchor newAnchor);

  /**
   * Creates an anchor into this conversation.
   */
  Anchor createAnchor(ConversationBlip blip);

  /**
   * Gets the root thread of this conversation.
   */
  ConversationThread getRootThread();

  /**
   * Gets a blip from this conversation, if it exists.
   *
   * @param blipId id of the blip to get
   * @return a blip, or null if the named blip does not exist.
   */
  ConversationBlip getBlip(String blipId);

  /**
   * Gets a thread from this conversation, if it exists.
   *
   * @param threadId id of the thread to get
   * @return a thread, or null if the named thread does not exist.
   */
  ConversationThread getThread(String threadId);

  /**
   * Gets a named data document from the conversation.
   *
   * The name must not be a blip id or the name of a document used to represent
   * conversation structure.
   *
   * @param name name of the document to fetch
   * @return the named document, never null
   * @throws IllegalArgumentException if a blip or conversation metadata
   *         document is named
   */
  Document getDataDocument(String name);

  /**
   * Deletes all threads and blips from this conversation. After this call the
   * conversation may still be queried but not mutated. Participants are not
   * modified.
   */
  void delete();

  /**
   * Gets the set of participant ids on this conversation.
   * The returned set is backed by an ordered set and is not modifiable.
   *
   * @return this conversation's participants.
   */
  Set<ParticipantId> getParticipantIds();

  /**
   * Adds a set of participant ids to this conversation. Does nothing for
   * participants that are already participant on this conversation. Does
   * nothing if the participants set is {@code null}.
   *
   * @param participants the participant ids to add
   */
  void addParticipantIds(Set<ParticipantId> participants);

  /**
   * Adds a participant to this conversation. Does nothing if the participant is
   * already a participant on this conversation.
   *
   * @param participant participant to add
   */
  void addParticipant(ParticipantId participant);

  /**
   * Removes a participant from this conversation. Does nothing if the
   * participant is not a participant on this wavelet.
   *
   * @param participant participant to remove
   */
  void removeParticipant(ParticipantId participant);

  /**
   * Gets a unique id for this conversation
   *
   * @return this conversation's id
   */
  String getId();
}
