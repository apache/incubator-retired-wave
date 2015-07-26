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
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.Set;

/**
 * Exposes per-user state associated with a conversation.
 * Specifically,
 * <ul>
 *   <li>read/unread state;</li>
 *   <li>folder allocations; and</li>
 *   <li>inboxing setting.<li>
 * </ul>
 *
 * The inboxing state depends on the versions of each wavelet
 * (same as the read/unread state).
 * <ul>
 * <li>
 *   The state is {@link InboxState#INBOX} if {@link #isFollowed(boolean)}
 *   returns true and for at least one wavelet
 *   {@link #isArchived(WaveletId, int)} returns false for the current version
 *   of that wavelet.
 * </li>
 * <li>
 *   The state is {@link InboxState#ARCHIVE} if {@link #isFollowed(boolean)}
 *   returns true and and {@link #isArchived(WaveletId, int)} returns true
 *   for the current version of each wavelet on the wave.
 * </li>
 * <li>
 *   The state is {@link InboxState#MUTE} when {@link #isFollowed(boolean)}
 *   returns false.
 * </li>
 * </ul>
 *
 */
public interface ReadableSupplement {

  //
  // Thread State Concerns.
  //

  /**
   * Retrieves the state of a thread, such as collapsed or expanded.
   *
   * @param waveletId  id of the thread's wavelet
   * @param threadId   id of the thread to change
   * @return the state of the specified thread.
   */
  ThreadState getThreadState(WaveletId waveletId, String threadId);

  /**
   * Tests whether a blip is unread.
   *
   * @param waveletId
   * @param blipId
   * @param version
   * @return true if the blip is unread at the given version.
   */
  boolean isBlipUnread(WaveletId waveletId, String blipId, int version);

  /**
   * Tests whether the set of participants in a wavelet is unread.
   *
   * @param waveletId
   * @param version
   * @return true if the set of participants is unread at the given version.
   */
  boolean isParticipantsUnread(WaveletId waveletId, int version);

  /**
   * Tests whether a wavelet's tags document is unread.
   *
   * @param waveletId The ID of the wavelet to check whether it's tags are unread.
   * @param version The version of the wavelet's tags document.
   * @return True if the wavelet's tags document is unread.
   */
  boolean isTagsUnread(WaveletId waveletId, int version);

  /**
   * @param waveletId
   *
   * @return true if the set of participants has ever been read.
   */
  boolean haveParticipantsEverBeenRead(WaveletId waveletId);

  /**
   * @return the folders to which this wave has been assigned.
   */
  Set<Integer> getFolders();

  /**
   * @return whether the wavelet is archived at the given version.
   */
  boolean isArchived(WaveletId waveletId, int version);

  /**
   * @return seen version and signature proving the seen version of
   * the given conversational wavelet (or zero)
   */
  HashedVersion getSeenVersion(WaveletId waveletId);

  /**
   * @return notified version (or zero)
   */
  int getNotifiedVersion(WaveletId waveletId);

  /**
   * @return true if at least one wavelet has been recorded as being notified.
   */
  boolean hasNotifiedVersion();

  /**
   * @return true if at least one wavelet has been recorded as being seen.
   */
  boolean hasSeenVersion();

  /**
   * @return Unmodifiable set of WaveletIds that have been recorded as being
   * seen.
   */
  Set<WaveletId> getSeenWavelets();

  /**
   * @return whether the wave is being followed.
   */
  boolean isFollowed(boolean defaultFollowed);

  /**
   * Gets a wanted evaluation set for the given wavelet id.
   */
  WantedEvaluationSet getWantedEvaluationSet(WaveletId waveletId);

  /**
   * @return whether the wave has a pending notification.
   */
  boolean hasPendingNotification();

  /**
   * @param waveletId the wavelet to check for pending notification.
   * @return whether the wavelet has a pending notification.
   */
  boolean hasPendingNotification(WaveletId waveletId);

  /**
   * Returns the gadget state as a String-to-String map.
   *
   * @param gadgetId ID of the gadget that owns the state.
   * @return the gadget state as a String-to-String map.
   */
  public ReadableStringMap<String> getGadgetState(String gadgetId);
}
