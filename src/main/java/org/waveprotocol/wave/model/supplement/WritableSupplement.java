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


import org.waveprotocol.wave.model.conversation.InboxState;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Exposes actions associated with a conversation.
 * Specifically,
 * <ul>
 *   <li>read/unread actions;</li>
 *   <li>folder actions; and</li>
 *   <li>inboxing actions.<li>
 * </ul>
 *
 */
public interface WritableSupplement {

  //
  // Thread State Concerns.
  //

  /**
   * Sets a thread's state, such as collapsed or expanded.
   *
   * @param waveletId  id of the thread's wavelet
   * @param threadId   id of the thread to change
   * @param newState   new state to change the thread to
   */
  void setThreadState(WaveletId waveletId, String threadId, ThreadState newState);

  //
  // Read/unread concerns.
  //

  /**
   * Marks a blip has having been read at a particular version.
   *
   * @param waveletId  id of the blip's wavelet
   * @param blipId     id of the blip that has been read
   * @param version    wavelet version at which the blip has been read
   */
  void markBlipAsRead(WaveletId waveletId, String blipId, int version);

  /**
   * Marks the participant set has having been read at a particular version.
   *
   * @param waveletId  id of the participant-set
   * @param version    wavelet version at which the participants have been read
   */
  void markParticipantsAsRead(WaveletId waveletId, int version);

  /**
   * Marks the tags document has having been read at a particular version.
   *
   * @param waveletId  id of the participant-set
   * @param version    wavelet version at which the tags have been read
   */
  void markTagsAsRead(WaveletId waveletId, int version);

  /**
   * Marks all aspects of a wavelet (participant set and all blips) has having
   * been read at a particular version.  This method will generally be more
   * efficient than repeated calls to
   * {@link #markBlipAsRead(WaveletId, String, int)} and
   * {@link #markParticipantsAsRead(WaveletId, int)}.
   *
   * @param waveletId  id of the wavelet that has been read
   * @param version    version at at which all parts of a wavelet are read
   */
  void markWaveletAsRead(WaveletId waveletId, int version);

  /**
   * Marks all aspects of a wave as totally unread (i.e., all previously
   * tracked read state is cleared all wavelets in the wave).
   */
  void markAsUnread();

  //
  // Folder concerns.
  //

  /**
   * Removes all folder allocations for this wave, replacing them with a single
   * folder.  Note, this applies even if the wave is already in that folder.
   *
   * @param id  folder id
   * @throws IllegalArgumentException if {@code id} is not a non-inbox folder.
   */
  void moveToFolder(int id);

  /**
   * Removes all folder allocations for this wave.
   */
  void removeAllFolders();

  //
  // Inboxing concerns.
  //

  /**
   * Clears archive state.
   */
  void clearArchive();

  /**
   * Puts the given wavelet into the {@link InboxState#ARCHIVE} state until it reaches
   * version higher than the specified value.
   * The wave will be in the {@link InboxState#ARCHIVE} state only if all its wavelet are.
   */
  void archive(WaveletId waveletId, int version);

  /**
   * Follows this wave.
   */
  void follow();

  /**
   * Un-follows this wave.
   */
  void unfollow();

  //
  // Verified observation.
  //

  /**
   * Marks the specified wavelet as seen at the given hashed version.
   */
  void setSeenVersion(WaveletId waveletId, HashedVersion signature);

  //
  // Abuse concerns
  //

  /**
   * Adds a new wanted evaluation.
   */
  void addWantedEvaluation(WantedEvaluation evaluation);

  //
  // Notifications.
  //

  /**
   * Marks all aspects of a wavelet (participant set and all blips) has having
   * been notified at a particular version.
   *
   * @param waveletId  id of the wavelet that has been notified
   * @param version    version at at which all parts of a wavelet are notified
   */
  void markWaveletAsNotified(WaveletId waveletId, int version);

  /**
   * Clears pending notification state. This is for clearing the
   * obsolete pending notification boolean.
   */
  void clearPendingNotification();

  /**
   * Saves a gadget state key-value pair.
   *
   * @param gadgetId ID of the gadget that owns the state.
   * @param key The key.
   * @param value The value for the key. If null, the key is removed.
   */
  void setGadgetState(String gadgetId, String key, String value);
}
