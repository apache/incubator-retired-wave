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

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.HashSet;
import java.util.Set;

/**
 * Canonical implementation of a {@link Supplement}.
 *
 */
public final class SupplementImpl implements Supplement {

  /** Semantic-free ADT. */
  private final PrimitiveSupplement primitive;

  /**
   * Creates a supplement.
   *
   * @param primitive underlying data-holding primitive
   */
  public SupplementImpl(PrimitiveSupplement primitive) {
    this.primitive = primitive;
  }

  @Override
  public ThreadState getThreadState(WaveletId waveletId, String threadId) {
    return primitive.getThreadState(waveletId, threadId);
  }

  @Override
  public void setThreadState(WaveletId waveletId, String threadId, ThreadState newState) {
    primitive.setThreadState(waveletId, threadId, newState);
  }

  /**
   * Tests if a component is unread, given its last-read and last-modified
   * versions.
   *
   * @param read      last-read version
   * @param modified  last-modified version
   * @return true if the last-read version is the special
   *         {@link PrimitiveSupplement#NO_VERSION} value, or if it is less than
   *         the last-modified value.
   */
  private boolean isUnread(int read, int modified) {
    return read == PrimitiveSupplement.NO_VERSION || read < modified;
  }

  /**
   * {@inheritDoc}
   *
   * A blip is unread if, and only if (a) the read-version for that blip either
   * does not exist or is less than the last-modified version; and (b) the
   * wavelet-override version either does not exist or is less than the blip's
   * last-modified version.
   */
  @Override
  public boolean isBlipUnread(WaveletId waveletId, String blipId, int version) {
    return isUnread(primitive.getLastReadBlipVersion(waveletId, blipId), version)
        && isUnread(primitive.getLastReadWaveletVersion(waveletId), version);
  }

  /**
   * {@inheritDoc}
   *
   * The participants collection is unread if, and only if (a) its read-version
   * either does not exist or is less than its last-modified version; and (b)
   * the wavelet-override version either does not exist or is less than the
   * participants' last-modified version.
   */
  @Override
  public boolean isParticipantsUnread(WaveletId waveletId, int version) {
    return isUnread(primitive.getLastReadParticipantsVersion(waveletId), version)
        && isUnread(primitive.getLastReadWaveletVersion(waveletId), version);
  }

  @Override
  public boolean haveParticipantsEverBeenRead(WaveletId waveletId) {
    return primitive.getLastReadParticipantsVersion(waveletId) != PrimitiveSupplement.NO_VERSION
        || primitive.getLastReadWaveletVersion(waveletId) != PrimitiveSupplement.NO_VERSION;
  }

  @Override
  public boolean isTagsUnread(WaveletId waveletId, int version) {
    return isUnread(primitive.getLastReadTagsVersion(waveletId), version)
        && isUnread(primitive.getLastReadWaveletVersion(waveletId), version);
  }

  /**
   * {@inheritDoc}
   *
   * If the blip is already considered as read, this method has no effect.
   */
  @Override
  public void markBlipAsRead(WaveletId waveletId, String blipId, int version) {
    if (isBlipUnread(waveletId, blipId, version)) {
      primitive.setLastReadBlipVersion(waveletId, blipId, version);
    }
  }

  /**
   * {@inheritDoc}
   *
   * If the participants collection is already considered as read, this method
   * has no effect.
   */
  @Override
  public void markParticipantsAsRead(WaveletId waveletId, int version) {
    if (isParticipantsUnread(waveletId, version)) {
      primitive.setLastReadParticipantsVersion(waveletId, version);
    }
  }

  /**
   * {@inheritDoc}
   *
   * If the tags document is already considered as read, this method
   * has no effect.
   */
  @Override
  public void markTagsAsRead(WaveletId waveletId, int version) {
    if (isTagsUnread(waveletId, version)) {
      primitive.setLastReadTagsVersion(waveletId, version);
    }
  }

  @Override
  public void markWaveletAsRead(WaveletId waveletId, int version) {
    primitive.setLastReadWaveletVersion(waveletId, version);
  }

  @Override
  public void markAsUnread() {
    primitive.clearReadState();
  }

  @Override
  public Set<Integer> getFolders() {
    return toSet(primitive.getFolders());
  }

  @Override
  public void moveToFolder(int id) {
    primitive.removeAllFolders();
    primitive.addFolder(id);
  }

  @Override
  public void removeAllFolders() {
    primitive.removeAllFolders();
  }

  @Override
  public boolean isArchived(WaveletId waveletId, int version) {
    return (primitive.getArchiveWaveletVersion(waveletId) >= version);
  }

  @Override
  public void follow() {
    primitive.follow();
  }

  @Override
  public void unfollow() {
    primitive.clearArchiveState();
    primitive.unfollow();
  }

  @Override
  public boolean isFollowed(boolean defaultFollowed) {
    Boolean explicitFollow = primitive.getFollowed();
    return explicitFollow != null ? explicitFollow : defaultFollowed;
  }

  @Override
  public void clearArchive() {
    primitive.clearArchiveState();
  }

  @Override
  public void archive(WaveletId waveletId, int version) {
    primitive.archiveAtVersion(waveletId, version);
  }

  @Override
  public void setSeenVersion(WaveletId waveletId, HashedVersion signature) {
    primitive.setSeenVersion(waveletId, signature);
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId waveletId) {
    return primitive.getSeenVersion(waveletId);
  }

  public int getNotifiedVersion(WaveletId waveletId) {
    return primitive.getNotifiedVersion(waveletId);
  }

  @Override
  public Set<WaveletId> getSeenWavelets() {
    return primitive.getSeenWavelets();
  }

  @Override
  public boolean hasSeenVersion() {
    return !primitive.getSeenWavelets().isEmpty();
  }

  private static <T> Set<T> toSet(Iterable<T> items) {
    Set<T> set = new HashSet<T>();
    for (T item : items) {
      set.add(item);
    }
    return set;
  }

  @Override
  public WantedEvaluationSet getWantedEvaluationSet(WaveletId waveletId) {
    Set<WantedEvaluation> relevantEvaluations = new HashSet<WantedEvaluation>();
    for (WantedEvaluation evaluation : primitive.getWantedEvaluations()) {
      // evaluation.getWaveletId() may be null - so must compare in this order
      if (waveletId.equals(evaluation.getWaveletId())) {
        relevantEvaluations.add(evaluation);
      }
    }
    return new SimpleWantedEvaluationSet(waveletId, relevantEvaluations);
  }

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    primitive.addWantedEvaluation(evaluation);
  }

  @Override
  public boolean hasPendingNotification() {
    Boolean pending = primitive.getPendingNotification();
    return (pending == null ? false : pending);
  }

  @Override
  public boolean hasPendingNotification(WaveletId waveletId) {
    if (!hasNotifiedVersion()) {
      // If we have not used the new approach of notified versions yet,
      // we need to check if there is a pending notification recorded
      // using the old pending notification flag.
      // TODO(user): migrate UDWs to replace the pending notification
      // flag with notified versions at the current version.
      return hasPendingNotification();
    }
    return getSeenVersion(waveletId).getVersion() < getNotifiedVersion(waveletId);
  }

  @Override
  public void markWaveletAsNotified(WaveletId waveletId, int version) {
    primitive.setNotifiedVersion(waveletId, version);
  }

  @Override
  public boolean hasNotifiedVersion() {
    return !primitive.getNotifiedWavelets().isEmpty();
  }

  @Override
  public void clearPendingNotification() {
    primitive.clearPendingNotification();
  }

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return primitive.getGadgetState(gadgetId);
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    primitive.setGadgetState(gadgetId, key, value);
  }
}
