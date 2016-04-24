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
 * Base implementation of anything that decorates a supplemented wave.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public abstract class SupplementedWaveWrapper<T extends SupplementedWave>
    implements SupplementedWave {

  /** Decorated supplement . */
  protected final T delegate;

  protected SupplementedWaveWrapper(T delegate) {
    this.delegate = delegate;
  }

  //
  // Read/unread.
  //

  @Override
  public void markAsRead(ConversationBlip blip) {
    delegate.markAsRead(blip);
  }

  @Override
  public void markAsUnread() {
    delegate.markAsUnread();
  }

  @Override
  public void markAsRead() {
    delegate.markAsRead();
  }

  @Override
  public void markParticipantAsRead(Wavelet wavelet) {
    delegate.markParticipantAsRead(wavelet);
  }

  @Override
  public void markTagsAsRead(Wavelet wavelet) {
    delegate.markTagsAsRead(wavelet);
  }

  @Override
  public boolean isUnread(ConversationBlip blip) {
    return delegate.isUnread(blip);
  }

  @Override
  public boolean isParticipantsUnread(Wavelet wavelet) {
    return delegate.isParticipantsUnread(wavelet);
  }

  @Override
  public boolean isTagsUnread(Wavelet wavelet) {
    return delegate.isTagsUnread(wavelet);
  }

  @Override
  public boolean isTrashed() {
    return delegate.isTrashed();
  }

  //
  // Inbox and folders.
  //

  @Override
  public boolean isArchived() {
    return delegate.isArchived();
  }

  @Override
  public boolean isFollowed() {
    return delegate.isFollowed();
  }

  @Override
  public boolean isInbox() {
    return delegate.isInbox();
  }

  @Override
  public boolean isMute() {
    return delegate.isMute();
  }

  @Override
  public void inbox() {
    delegate.inbox();
  }

  @Override
  public void archive() {
    delegate.archive();
  }

  @Override
  public void follow() {
    delegate.follow();
  }

  @Override
  public void unfollow() {
    delegate.unfollow();
  }

  @Override
  public void mute() {
    delegate.mute();
  }

  @Override
  public Set<Integer> getFolders() {
    return delegate.getFolders();
  }

  @Override
  public void moveToFolder(int folderId) {
    delegate.moveToFolder(folderId);
  }

  //
  // Seen.
  //

  @Override
  public void see() {
    delegate.see();
  }

  @Override
  public void see(Wavelet wavelet) {
    delegate.see(wavelet);
  }

  @Override
  public HashedVersion getSeenVersion(WaveletId id) {
    return delegate.getSeenVersion(id);
  }

  @Override
  public boolean hasBeenSeen() {
    return delegate.hasBeenSeen();
  }

  //
  // Abuse.
  //

  @Override
  public void addWantedEvaluation(WantedEvaluation evaluation) {
    delegate.addWantedEvaluation(evaluation);
  }

  @Override
  public WantedEvaluationSet getWantedEvaluationSet(Wavelet wavelet) {
    return delegate.getWantedEvaluationSet(wavelet);
  }

  //
  // Collapse state.
  //

  @Override
  public ThreadState getThreadState(ConversationThread thread) {
    return delegate.getThreadState(thread);
  }


  @Override
  public void setThreadState(ConversationThread thread, ThreadState state) {
    delegate.setThreadState(thread, state);
  }

  //
  // Notifications.
  //

  @Override
  public boolean hasPendingNotification() {
    return delegate.hasPendingNotification();
  }

  @Override
  public void markAsNotified() {
    delegate.markAsNotified();
  }

  @Override
  public boolean haveParticipantsEverBeenRead(Wavelet wavelet) {
    return delegate.haveParticipantsEverBeenRead(wavelet);
  }

  //
  // Gadgets.
  //

  @Override
  public ReadableStringMap<String> getGadgetState(String gadgetId) {
    return delegate.getGadgetState(gadgetId);
  }

  @Override
  public String getGadgetStateValue(String gadgetId, String key) {
    return delegate.getGadgetStateValue(gadgetId, key);
  }

  @Override
  public void setGadgetState(String gadgetId, String key, String value) {
    delegate.setGadgetState(gadgetId, key, value);
  }
}
