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
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Set;

/**
 * Exposes supplementary queries on a wave.
 *
 */
public interface ReadableSupplementedWave {

  /**
   *  Retrieves the state (collapsed etc.) of a conversation thread.
   *
   * @param thread  the thread to examine
   * @return the state of the thread.
   */
  ThreadState getThreadState(ConversationThread thread);

  /**
   * Tests whether a blip is unread.
   *
   * @return true if the blip is unread at the given version.
   */
  boolean isUnread(ConversationBlip blip);

  /**
   * Tests whether the set of participants in a wavelet is unread.
   *
   * @return true if the set of participants is unread at the given version.
   */
  boolean isParticipantsUnread(Wavelet wavelet);

  /**
   * @param wavelet
   *
   * @return true if the participant list has been read before.
   */
  boolean haveParticipantsEverBeenRead(Wavelet wavelet);

  /**
   * @param wavelet The wavelet to check if has unread tags.
   * @return True if the wavelet has tags changes which have not been read.
   */
  boolean isTagsUnread(Wavelet wavelet);

  /**
   * @return the folders to which this wave has been assigned.
   */
  Set<Integer> getFolders();

  /**
   * @return true if this wave should be in the inbox.
   */
  boolean isInbox();

  /**
   * Gets the WantedEvaluationSet for a given wavelet.
   */
  WantedEvaluationSet getWantedEvaluationSet(Wavelet wavelet);

  /**
   * Tests whether the wave is mute.
   *
   * @return true if the wave is mute.
   */
  boolean isMute();

  /**
   * Tests whether the wave is archived.
   *
   * @return true if the wave is archived.
   */
  boolean isArchived();

  /**
   * Tests whether the wave is being followed.
   *
   * @return true if the wave is being followed, false if un-followed.
   */
  boolean isFollowed();

  /**
   * Tests whether the wave is trashed.
   *
   * @return true if the wave is in trash, false if anything else.
   */
  boolean isTrashed();

  /**
   * @return the version and hash of the wavelet the last time
   *   this wavelet was opened (and sighted by the user).
   */
  HashedVersion getSeenVersion(WaveletId id);

  /**
   * @return true if there is some wavelet which has been seen, false otherwise.
   */
  boolean hasBeenSeen();

  /**
   * @return true if there is a pending notification, false otherwise.
   */
  boolean hasPendingNotification();

  /**
   * Reads the value of the given key from the state of the given gadget.
   *
   * @param gadgetId ID of the gadget.
   * @param key State key.
   * @return Value for the given key or null if gadget or key is missing.
   */
  String getGadgetStateValue(String gadgetId, String key);

  /**
   * Gets the gadget state for the given gadget.
   *
   * @param gadgetId ID of the gadget to get the state of.
   * @return Gadget state as StateMap.
   */
  ReadableStringMap<String> getGadgetState(String gadgetId);
}
