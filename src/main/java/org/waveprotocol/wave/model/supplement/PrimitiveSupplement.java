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
 * Exposes an abstract datatype for storing the state required to track
 * per-user state.  Specifically, that state is
 * <ul>
 *   <li>a last-read version for each blip (optional);</li>
 *   <li>a last-read version for each participant set (optional);</li>
 *   <li>a minimum last-read version for each wavelet (optional);</li>
 *   <li>a set of folder allocations; and</li>
 *   <li>a minimum archived version for each wavelet (optional).</li>
 *   <li>whether there is a pending notification for this wave.</li>
 *   <li>a mute flag.</li>
 * </ul>
 * There are no constraints between the types of versions.
 *
 * The intended interpretation of the last-read versions is that:
 * <ul>
 *   <li>a blip is read if and only if its last-modified version is less than
 *       or equal to either its last-read version or the wavelet-override
 *       version in this supplement; otherwise, it is unread</li>
 *   <li>the participants set of a wavelet is read if and only if its
 *       last-modified version is less than or equal to either the last-read
 *       participants version or the wavelet-override version in this
 *       supplement; otherwise, it is unread</li>
 * </ul>
 *
 * The primitive supplement exposes a set of wavelet archive versions and a mute flag.
 * These are then used by the above layer as an Inbox State.
 * If the mute flag is set to true the InboxState is considered {@link InboxState#MUTE},
 * regardless of the wavelet archive versions.
 * If each conversation wavelet has an archive version equal or greater than its current version,
 * the inbox state is interpreted as  {@link InboxState#ARCHIVE}.
 * If at least one conversation wavelet has a version greater than the archived version,
 * or at least one conversation has no archived version, and the inbox state is
 * not mute, then the inbox state is considered to be {@link InboxState#INBOX}.
 *
 */
public interface PrimitiveSupplement {
  /** Indicates the absence of a last-read version. */
  public static final int NO_VERSION = -1;

  //
  // Thread state concerns.
  //

  /**
   * @return the state (collapsed etc.) of a thread.
   *
   * @param waveletId wavelet in which the thread exists
   * @param threadId id of the thread to check
   */
  ThreadState getThreadState(WaveletId waveletId, String threadId);

  /**
   * Sets a thread's state for this user (including collapsed state).
   *
   * @param waveletId wavelet in which the thread exists
   * @param threadId  id of the thread to alter
   * @param newState  the new state of the thread to set, null if the state
   *                  is to be cleared.
   */
  void setThreadState(WaveletId waveletId, String threadId, ThreadState newState);

  /**
   * Gets the collection of wavelet ids for which some thread-state exists.
   *
   * @return wavelet ids that have some kind of associated thread state.
   */
  Iterable<WaveletId> getWaveletsWithThreadState();

  /**
   * Gets the collection of thread ids within a wavelet for which some
   * thread-state exists.
   *
   * @return thread ids that have a thread state.
   */
  Iterable<String> getStatefulThreads(WaveletId waveletId);

  //
  // Read/unread concerns.
  //

  /**
   * @return the overriding (minimum) last-read version for a wavelet, or
   *         {@link #NO_VERSION} if none exists.
   */
  int getLastReadWaveletVersion(WaveletId waveletId);

  /**
   * @return the last-read version of a blip, or {@link #NO_VERSION} if none
   *         exists.
   */
  int getLastReadBlipVersion(WaveletId waveletId, String blipId);

  /**
   * @return the last-read version of a participant set, or {@link #NO_VERSION}
   *         if none exists.
   */
  int getLastReadParticipantsVersion(WaveletId waveletId);

  /**
   * @return the last-read version of a Tags document, or {@link #NO_VERSION}
   *         if none exists.
   */
  int getLastReadTagsVersion(WaveletId waveletId);

  /**
   * Sets the overriding (minimum) last-read version for all parts of a
   * wavelet.
   *
   * @param waveletId  id of the wavelet whose read state is to be set
   * @param version    override last-read version to set
   */
  void setLastReadWaveletVersion(WaveletId waveletId, int version);

  /**
   * Sets the last-read version for a blip.
   *
   * @param waveletId  id of the blip's wavelet
   * @param blipId     id of the blip that has been read
   * @param version    wavelet version at which the blip is read
   */
  void setLastReadBlipVersion(WaveletId waveletId, String blipId, int version);

  /**
   * Sets the last-read version for a wavelet's participant set.
   *
   * @param waveletId  id of the participant set's wavelet
   * @param version    wavelet version at which the participant set is read
   */
  void setLastReadParticipantsVersion(WaveletId waveletId, int version);

  /**
   * Sets the last-read version for a wavelet's tags document.
   *
   * @param waveletId   id of the tags document's wavelet.
   * @param version     wavelet version at which the Tags document is read.
   */
  void setLastReadTagsVersion(WaveletId waveletId, int version);

  /**
   * Clears all tracked read-state.
   */
  void clearReadState();

  /**
   * Clears the read state of the given blip.
   *
   * @param waveletId
   * @param blipId
   */
  void clearBlipReadState(WaveletId waveletId, String blipId);

  /**
   * Gets the collection of wavelet ids for which some read-state exists.
   *
   * @return wavelet ids that have some kind of associated read state.
   */
  Iterable<WaveletId> getReadWavelets();

  /**
   * Gets the collection of blip ids within a wavelet for which some
   * read-state exists.
   *
   * @return blip ids that have a last-read version
   */
  Iterable<String> getReadBlips(WaveletId waveletId);

  //
  // Folder concerns.
  //

  /**
   * Gets the folders to which this wave has been allocated.
   *
   * @return set of folders.
   */
  Iterable<Integer> getFolders();

  /**
   * Tests if this wave is in a folder.
   *
   * @return true if this wave is allocated to folder {@code id}.
   */
  boolean isInFolder(int id);

  /**
   * Adds a folder allocation for this wave.
   *
   * @param id  folder id
   */
  void addFolder(int id);

  /**
   * Adds a folder allocation for this wave.
   *
   * @param id  folder id
   */
  void removeFolder(int id);

  /**
   * Removes all allocations.
   */
  void removeAllFolders();

  //
  // Inboxing.
  //

  /**
   * Sets the followed state to true.
   */
  void follow();

  /**
   * Sets the followed state to false.
   */
  void unfollow();

  /**
   * Clears the followed state.
   */
  void clearFollow();

  /**
   * @return the last archived version for a wavelet, or {@link #NO_VERSION} if none exists.
   */
  int getArchiveWaveletVersion(WaveletId waveletId);

  /**
   * Sets the archive version for a wavelet on this wave.
   *
   * @param waveletId a conversation wavelet.
   * @param waveletVersion a version for that conversation.
   */
  void archiveAtVersion(WaveletId waveletId, int waveletVersion);

  /**
   * Clears the archive versions for all wavelets on this wave.
   */
  void clearArchiveState();

  /**
   * Gets the collection of wavelet ids for which some archive version exists.
   *
   * @return wavelet ids that have been archived at some version.
   */
  Iterable<WaveletId> getArchiveWavelets();

  /**
   * Gets the following state (may be null).
   *
   * @return true if the wave is being followed, false if the wave is being
   *         unfollowed, null if unspecified.
   */
  Boolean getFollowed();

  //
  // Seen.
  //

  /**
   * Sets a "proof of having seen" a version of a wavelet on this wave.
   *
   * @param waveletId a conversation wavelet.
   * @param version a version and hash of the wavelet at version.
   */
  void setSeenVersion(WaveletId waveletId, HashedVersion version);

  /**
   * Clears a seen version for a wavelet.
   *
   * @param waveletId id of the wavelet seen version to clear
   */
  void clearSeenVersion(WaveletId waveletId);

  /**
   * TODO(user): rename to getSeenVersion (balance with setter)
   * @param waveletId a conversational wavelet
   * @return a composite representing a seen version/signature hash couple.
   */
  HashedVersion getSeenVersion(WaveletId waveletId);

  /**
   * Returns an unmodifiable set of seen wavelet ids.
   */
  Set<WaveletId> getSeenWavelets();

  //
  // Abuse.
  //

  /**
   * Gets the WantedEvaluations for this wave.
   *
   * @return All the WantedEvaluations that have ever been added to this wave.
   */
  Set<WantedEvaluation> getWantedEvaluations();

  /**
   * Adds a WantedEvaluation.
   */
  void addWantedEvaluation(WantedEvaluation evaluation);

  //
  // Notifications.
  //

  /**
   * @return the pending-notification state of the wave.
   */
  boolean getPendingNotification();

  /**
   * Gets the set of wavelet ids for which some notified-state exists.
   *
   * @return unmodifiable set of wavelet ids that have some kind of associated
   * notified state.
   */
  Set<WaveletId> getNotifiedWavelets();

  /**
   * @return the last-notified version for a wavelet, or {@link #NO_VERSION} if none
   *         exists.
   */
  int getNotifiedVersion(WaveletId waveletId);

  /**
   * Sets the overriding (minimum) notified version for all parts of a
   * wavelet.
   *
   * @param waveletId  id of the wavelet whose notified state is to be set
   * @param version    override notified version to set
   */
  void setNotifiedVersion(WaveletId waveletId, int version);

  /**
   * Clears the obsolete pending-notification boolean of the wave.
   */
  void clearPendingNotification();

  //
  // Gadgets.
  //

  /**
   * Retrieves the gadget state as a String-to-String map.
   *
   * @param gadgetId ID of the gadget that owns the state.
   * @return the gadget state.
   */
  public ReadableStringMap<String> getGadgetState(String gadgetId);

  /**
   * Stores the gadget state key-value pair.
   *
   * @param gadgetId ID of the gadget that owns the state.
   * @param key The key.
   * @param value A new value for the key. If null, the key is removed.
   */
  public void setGadgetState(String gadgetId, String key, String value);
}
