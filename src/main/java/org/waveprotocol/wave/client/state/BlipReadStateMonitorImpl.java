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


package org.waveprotocol.wave.client.state;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.model.conversation.BlipIterators;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collection;
import java.util.List;

/**
 * Eagerly monitors the read/unread state of all blips in all conversations in a
 * wave, broadcasting events when the number of read and/or unread blips
 * changes.
 */
public final class BlipReadStateMonitorImpl extends ObservableSupplementedWave.ListenerImpl
    implements BlipReadStateMonitor, ObservableConversation.Listener,
    ObservableConversationView.Listener {

  /**
   * Logging for state changes. Added June 2010; note that many log statements
   * output the current read/unread state, which is actually an O(n) operation
   * in the number of blips since the JS implementation IdentityMap counts all
   * entries (albeit very cheaply).
   */
  private static final DomLogger LOG = new DomLogger("blip-read-state");

  private final IdentitySet<ConversationBlip> readBlips = CollectionUtils.createIdentitySet();
  private final IdentitySet<ConversationBlip> unreadBlips = CollectionUtils.createIdentitySet();
  // IdentitySet has no O(1) size() method, so sizes must be maintained
  // manually.
  private int read;
  private int unread;
  private final CopyOnWriteSet<BlipReadStateMonitor.Listener> listeners = CopyOnWriteSet.create();
  private final ObservableSupplementedWave supplementedWave;
  private final ObservableConversationView conversationView;
  private final WaveId waveId;

  /**
   * @return a new BlipReadStateMonitor
   */
  public static BlipReadStateMonitorImpl create(WaveId waveId,
      ObservableSupplementedWave supplementedWave, ObservableConversationView conversationView) {
    BlipReadStateMonitorImpl monitor = new BlipReadStateMonitorImpl(waveId,
        supplementedWave, conversationView);
    monitor.init();
    return monitor;
  }

  private BlipReadStateMonitorImpl(WaveId waveId, ObservableSupplementedWave supplementedWave,
      ObservableConversationView conversationView) {
    Preconditions.checkNotNull(waveId, "waveId cannot be null");
    Preconditions.checkNotNull(supplementedWave, "supplementedWave cannot be null");
    Preconditions.checkNotNull(conversationView, "conversationView cannot be null");
    this.waveId = waveId;
    this.supplementedWave = supplementedWave;
    this.conversationView = conversationView;
  }

  private void init() {
    // Count the existing blips.  This will also set haveCountedBlips to true
    countBlips();

    // Listen to existing conversations.
    for (ObservableConversation conversation : conversationView.getConversations()) {
      conversation.addListener(this);
    }

    // Listen for new conversations and supplement events.
    supplementedWave.addListener(this);
    conversationView.addListener(this);
  }

  //
  // Debugging (for DebugMenu).
  //

  public Collection<String> debugGetReadBlips() {
    final List<String> result = CollectionUtils.newArrayList();
    readBlips.each(new IdentitySet.Proc<ConversationBlip>() {
      @Override
      public void apply(ConversationBlip blip) {
        result.add(blip.getId());
      }
    });
    return result;
  }

  public Collection<String> debugGetUnreadBlips() {
    final List<String> result = CollectionUtils.newArrayList();
    unreadBlips.each(new IdentitySet.Proc<ConversationBlip>() {
      @Override
      public void apply(ConversationBlip blip) {
        result.add(blip.getId());
      }
    });
    return result;
  }

  //
  // BlipReadStateMonitor
  //

  @Override
  public int getReadCount() {
    assert read == readBlips.countEntries();
    return read;
  }

  @Override
  public int getUnreadCount() {
    assert unread == unreadBlips.countEntries();
    return unread;
  }

  @Override
  public void addListener(BlipReadStateMonitor.Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(BlipReadStateMonitor.Listener listener) {
    listeners.remove(listener);
  }

  //
  // ObservableConversation.Listener
  //

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
    logChange("added", blip);
    handleBlipAdded(blip);
  }

  private void handleBlipAdded(ObservableConversationBlip blip) {
    // Add this blip.
    updateOrInsertReadUnread(blip);

    // Add all replies.
    for (ObservableConversationThread replyThread : blip.getReplyThreads()) {
      handleThreadAdded(replyThread);
    }
  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
    logChange("deleted", blip);
    handleBlipRemoved(blip);
  }

  private void handleBlipRemoved(ObservableConversationBlip blip) {
    // Remove this blip.
    removeReadUnread(blip);

    // Remove all inline replies (non-inline replies will just be reanchored).
    for (ObservableConversationThread replyThread : blip.getReplyThreads()) {
      handleThreadRemoved(replyThread);
    }
  }

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
    handleThreadAdded(thread);
  }

  private void handleThreadAdded(ObservableConversationThread thread) {
    // Add all direct blips.  Descendant blips will be added recursively.
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipAdded(blip);
    }
  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
    handleThreadRemoved(thread);
  }

  private void handleThreadRemoved(ObservableConversationThread thread) {
    // Remove all direct blips.  Descendant blips will be removed recursively.
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipRemoved(blip);
    }
  }

  //
  // ObservableConversationView.Listener
  //

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    conversation.addListener(this);
    handleThreadAdded(conversation.getRootThread());
  }

  @Override
  public void onConversationRemoved(ObservableConversation conversation) {
    conversation.removeListener(this);
    handleThreadRemoved(conversation.getRootThread());
  }

  //
  // ObservableSupplementedWave.Listener
  //

  @Override
  public void onMaybeBlipReadChanged(ObservableConversationBlip blip) {
    // We only care about blips that we already know about.
    if (readBlips.contains(blip) || unreadBlips.contains(blip)) {
      if (updateOrInsertReadUnread(blip)) {
        logChange("read changed", blip);
      }
    }
  }

  @Override
  public void onMaybeWaveletReadChanged() {
    countBlips();
    notifyListeners();
  }

  //
  // Helpers.
  //

  /**
   * Populates {@link #readBlips} and {@link #unreadBlips} by counting all blips.
   */
  private void countBlips() {
    readBlips.clear();
    read = 0;
    unreadBlips.clear();
    unread = 0;

    for (Conversation conversation : conversationView.getConversations()) {
      for (ConversationBlip blip : BlipIterators.breadthFirst(conversation)) {
        if (supplementedWave.isUnread(blip)) {
          unreadBlips.add(blip);
          unread++;
        } else {
          readBlips.add(blip);
          read++;
        }
      }
    }
  }

  /**
   * Inserts the blip into the correct read/unread set and removes from the
   * other, and notifies listeners as needed.
   */
  private boolean updateOrInsertReadUnread(ConversationBlip blip) {
    boolean changed = false;
    if (isUnread(blip)) {
      if (readBlips.contains(blip)) {
        readBlips.remove(blip);
        read--;
        changed = true;
      }
      if (!unreadBlips.contains(blip)) {
        unreadBlips.add(blip);
        unread++;
        changed = true;
      }
    } else {
      if (unreadBlips.contains(blip)) {
        unreadBlips.remove(blip);
        unread--;
        changed = true;
      }
      if (!readBlips.contains(blip)) {
        readBlips.add(blip);
        read++;
        changed = true;
      }
    }
    if (changed) {
      notifyListeners();
    }
    return changed;
  }

  /**
   * Removes the blip from all possible locations in the read and unread set
   * and notifies listeners as needed.
   */
  private void removeReadUnread(ConversationBlip blip) {
    boolean changed = false;
    if (readBlips.contains(blip)) {
      readBlips.remove(blip);
      read--;
      changed = true;
    }
    if (unreadBlips.contains(blip)) {
      unreadBlips.remove(blip);
      unread--;
      changed = true;
    }
    if (changed) {
      notifyListeners();
    }
  }

  /**
   * Determines whether the given blip is unread.
   */
  private boolean isUnread(ConversationBlip blip) {
    return supplementedWave.isUnread(blip);
  }

  /**
   * Notifies listeners of a change.
   */
  private void notifyListeners() {
    LOG.trace().log(waveId, ": notifying read/unread change ", read, "/", unread);
    for (Listener listener : listeners) {
      listener.onReadStateChanged();
    }
  }

  /**
   * Log some action with the blip information and read/unread state.
   */
  private void logChange(String action, ConversationBlip blip) {
    LOG.trace().log(blip, ": ", action, " now ", getReadCount(), "/", getUnreadCount());
  }

  @Override public void onBlipContributorAdded(ObservableConversationBlip blip,
      ParticipantId contributor) {}
  @Override public void onBlipContributorRemoved(ObservableConversationBlip blip,
      ParticipantId contributor) {}
  @Override public void onBlipSumbitted(ObservableConversationBlip blip) {}
  @Override public void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
      long newTimestamp) {}
  @Override public void onInlineThreadAdded(ObservableConversationThread thread, int location) {}
  @Override public void onParticipantAdded(ParticipantId participant) {}
  @Override public void onParticipantRemoved(ParticipantId participant) {}
}
