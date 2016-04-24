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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.wave.Wavelet;

/**
 * Exposes supplementary actions on a wave.
 *
 */
public interface WritableSupplementedWave {

  /**
   * Sets the state (collapsed etc.) of a Conversation thread.
   *
   * @param thread  thread to change
   * @param state   new state for the thread
   */
  void setThreadState(ConversationThread thread, ThreadState state);

  /**
   * Marks a blip as having been read.
   *
   * @param b  blip to mark as read
   */
  void markAsRead(ConversationBlip b);

  /**
   * Marks the whole wave as read.
   */
  void markAsRead();

  /**
   * Marks the participant list as read.
   *
   * @param wavelet
   */
  void markParticipantAsRead(Wavelet wavelet);

  /**
   * Marks the tags document as read.
   *
   * @param wavelet
   */
  void markTagsAsRead(Wavelet wavelet);

  /**
   * Marks the whole wave as unread.
   */
  void markAsUnread();

  /**
   * Moves this wave into the inbox.
   */
  void inbox();

  /**
   * Archives this wave (moves it out of the inbox).  The wave will re-appear
   * in the inbox on the next unread change (unless muted).
   */
  void archive();

  /**
   * Records that all the conversational wavelets in this wave were seen by
   * the user at their respective versions.
   */
  void see();

  /**
   * Records that the wavelet was seen by the user at the current version.
   *
   * @param wavelet The wavelet which has been seen
   */
  void see(Wavelet wavelet);

  /**
   * Mutes this wave (keeps it out of the inbox, regardless of changes).
   */
  void mute();

  /**
   * Follows this wave (changes since last archive will cause this wave to be
   * moved to the inbox).
   */
  void follow();

  /**
   * Un-follows this wave (changes to this wave will not cause it to move to be
   * moved to the inbox).
   */
  void unfollow();

  /**
   * Moves this wave into a folder (removes it from all current folders first).
   *
   * @param folderId  folder to which the wave is to be moved
   */
  void moveToFolder(int folderId);

  /**
   * Inserts a new evaluation, which will cause wanted evaluations to update.
   */
  void addWantedEvaluation(WantedEvaluation evaluation);

  /**
   * Marks the wave as notified at the current version.
   */
  void markAsNotified();

  /**
   * Sets or modifies the gadget state stored in the supplement. The value can
   * be null. If value is null the key is removed from the state.
   *
   * @param gadgetId ID of the gadget to modify the state for.
   * @param key State key.
   * @param value State value. If null the key will be removed from the state.
   */
  void setGadgetState(String gadgetId, String key, String value);
}
