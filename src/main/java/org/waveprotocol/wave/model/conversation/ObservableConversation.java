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

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Extends {@link Conversation} to provide events.
 *
 * @author anorth@google.com (Alex North)
 */
public interface ObservableConversation extends Conversation,
    SourcesEvents<ObservableConversation.Listener> {

  // Anchor listener and conversation listener are separated since listeners
  // to these two are likely to be disjoint. Anchor listener doesn't need all
  // the structure events.

  /**
   * Receives events about the anchoring of a conversation.
   */
  interface AnchorListener {
    /**
     * Notifies this listener that the conversation anchor has changed.
     *
     * @param oldAnchor the old anchor, may be null
     * @param newAnchor new anchor, may be null
     */
    void onAnchorChanged(Anchor oldAnchor, Anchor newAnchor);
  }

  /** Receives events on a conversation. */
  interface Listener {
    /**
     * Notifies this listener that a participant was added.
     * @param participant the added participant
     */
    void onParticipantAdded(ParticipantId participant);

    /**
     * Notifies this listener that a participant was removed.
     * @param participant the removed participant
     */
    void onParticipantRemoved(ParticipantId participant);

    /**
     * Notifies this listener that a blip was added.
     *
     * @param blip the new blip
     */
    void onBlipAdded(ObservableConversationBlip blip);

    /**
     * Notifies this listener that a blip was removed. The only valid methods on
     * the blip are those which query its state, not modify it.
     *
     * @param blip the deleted blip
     */
    void onBlipDeleted(ObservableConversationBlip blip);

    /**
     * Notifies this listener that a thread was added.
     *
     * @param thread the new thread
     */
    void onThreadAdded(ObservableConversationThread thread);

    /**
     * Notifies this listener that an inline thread was added.
     *
     * @param thread the new thread
     * @param location the new threads location in its parent document
     */
    void onInlineThreadAdded(ObservableConversationThread thread, int location);

    /**
     * Notifies this listener that a thread was removed. The thread is no longer
     * usable.
     *
     * @param thread the removed thread.
     */
    void onThreadDeleted(ObservableConversationThread thread);

    /**
     * Notifies this listener that a contributor was added to a blip.
     */
    void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId contributor);

    /**
     * Notifies this listener that a contributor was removed from a blip.
     */
    void onBlipContributorRemoved(ObservableConversationBlip blip, ParticipantId contributor);

    /**
     * Notifies the listener that a blip was submitted.
     */
    void onBlipSumbitted(ObservableConversationBlip blip);

    /**
     * Notifies the listener that a blip timestamp changed.
     */
    void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
        long newTimestamp);
  }

  @Override
  ObservableConversationBlip getBlip(String id);

  @Override
  ObservableConversationThread getThread(String id);

  @Override
  ObservableConversationThread getRootThread();

  @Override
  ObservableDocument getDataDocument(String name);

  // Listener registration for the AnchorListener, since this interface
  // cannot extend SourcesEvents twice.

  /**
   * Adds an anchor listener to this conversation.
   */
  void addListener(AnchorListener listener);

  /**
   * Removes an anchor listener from this conversation.
   */
  void removeListener(AnchorListener listener);

}
