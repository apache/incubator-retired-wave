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

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWave;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Monitors the threads in a conversation for unread blips and efficiently
 * maintains a count of them.
 *
 * Counts are lazily initialised such that the thread state will not be counted
 * nor tracked unless the thread count has been explicitly queried.  This allows
 * for nice optimisations such as not counting root threads (if this monitor is
 * only used for inline reply counts), paging of threads, etc.
 */
public final class ThreadReadStateMonitorImpl extends ObservableSupplementedWave.ListenerImpl
    implements ThreadReadStateMonitor, ObservableConversation.Listener,
    ObservableConversationView.Listener {

  /**
   * Maintains the read/unread state of all blips in order to translate the
   * "maybe" events from the model/supplement into "definite" events.
   * Events must be pushed via the "handle" methods.
   */
  private class ReadStateBucket {
    private final IdentitySet<ConversationBlip> readBlips = CollectionUtils.createIdentitySet();
    private final IdentitySet<ConversationBlip> unreadBlips = CollectionUtils.createIdentitySet();

    /**
     * Notifies the bucket that a blip has been added.
     *
     * @return whether the bucket state changed as a result, i.e. if the blip
     *         was not already present in the bucket
     */
    public boolean handleBlipAdded(ConversationBlip blip) {
      if (!contains(blip)) {
        if (getSupplement().isUnread(blip)) {
          unreadBlips.add(blip);
        } else {
          readBlips.add(blip);
        }
        return true;
      } else {
        return false;
      }
    }

    /**
     * Notifies the bucket that a blip has been removed.
     *
     * @return whether the bucket state changed as a result, i.e. if the blip
     *         was not present in the bucket
     */
    public boolean handleBlipRemoved(ConversationBlip blip) {
      if (contains(blip)) {
        unreadBlips.remove(blip);
        readBlips.remove(blip);
        return true;
      } else {
        return false;
      }
    }

    /**
     * @return whether the bucket has seen a given blip
     */
    public boolean contains(ConversationBlip blip) {
      assert !(readBlips.contains(blip) && unreadBlips.contains(blip));
      return readBlips.contains(blip) || unreadBlips.contains(blip);
    }

    /**
     * Notifies the bucket that a blip read state may have changed.
     *
     * @return whether the read state of the blip definitely changed
     */
    public boolean handleMaybeBlipReadStateChanged(ConversationBlip blip) {
      if (getSupplement().isUnread(blip)) {
        if (readBlips.contains(blip)) {
          readBlips.remove(blip);
          unreadBlips.add(blip);
          return true;
        }
      } else {
        if (unreadBlips.contains(blip)) {
          unreadBlips.remove(blip);
          readBlips.add(blip);
          return true;
        }
      }
      return false;
    }

    /**
     * Notifies the bucket that a wavelet's read state may have changed.
     *
     * @return whether the read state of any blips changed
     */
    public boolean handleWaveletReadStateChanged() {
      // Calculate all blips which changed read state.
      final IdentitySet<ConversationBlip> changedBlips = CollectionUtils.createIdentitySet();

      readBlips.each(new IdentitySet.Proc<ConversationBlip>() {
        @Override
        public void apply(ConversationBlip blip) {
          if (getSupplement().isUnread(blip)) {
            changedBlips.add(blip);
          }
        }
      });

      unreadBlips.each(new IdentitySet.Proc<ConversationBlip>() {
        @Override
        public void apply(ConversationBlip blip) {
          if (!getSupplement().isUnread(blip)) {
            changedBlips.add(blip);
          }
        }
      });

      // Trigger events on changed blips.
      changedBlips.each(new IdentitySet.Proc<ConversationBlip>() {
        @Override
        public void apply(ConversationBlip blip) {
          boolean changed = handleMaybeBlipReadStateChanged(blip);
          assert changed;
        }
      });

      return !changedBlips.isEmpty();
    }
  }

  /**
   * Contains the state of all transitive child blips of a single thread.
   *
   * Events must be pushed via the "handle" methods. If a ThreadReadState exists
   * it *must* have all relevant events to it; the
   * {@link ThreadReadState#isMonitored} field is for efficiency where a
   * ThreadReadState might exist for a thread even though that thread is not
   * officially monitored; if so, callers must still push events but may query
   * {@link ThreadReadState#isMonitored()} to determine whether listeners should
   * receive read state changes for this thread.
   */
  private class ThreadReadState {
    private final ConversationThread thread;
    private int readCount = 0;
    private int unreadCount = 0;
    boolean isMonitored = false;
    boolean isDirty = true;

    public ThreadReadState(ConversationThread thread) {
      this.thread = thread;
    }

    /**
     * @return the current read count
     */
    public int getReadCount() {
      if (isDirty) {
        countBlips();
      }
      return readCount;
    }

    /**
     * @return the current unread count
     */
    public int getUnreadCount() {
      if (isDirty) {
        countBlips();
      }
      return unreadCount;
    }

    /**
     * Sets whether the thread should be included in events.
     */
    public void setIsMonitored(boolean isMonitored) {
      this.isMonitored = isMonitored;
    }

    /**
     * @return whether the thread should be included in events
     */
    public boolean isMonitored() {
      return isMonitored;
    }

    /**
     * Notifies the thread monitor that a blip has been added.
     */
    public void handleBlipAdded(ConversationBlip blip) {
      if (getSupplement().isUnread(blip)) {
        unreadCount++;
      } else {
        readCount++;
      }
    }

    /**
     * Notifies the thread monitor that a blip has been removed.
     */
    public void handleBlipRemoved(ConversationBlip blip) {
      if (getSupplement().isUnread(blip)) {
        unreadCount--;
      } else {
        readCount--;
      }
    }

    /**
     * Notifies the thread monitor that a blip's read state has
     * definitely changed.
     */
    public void handleBlipReadStateChanged(ConversationBlip blip) {
      if (getSupplement().isUnread(blip)) {
        unreadCount++;
        readCount--;
      } else {
        readCount++;
        unreadCount--;
      }
    }

    /**
     * Flags the read state as "dirty" such that it will be recomputed when next
     * queried.
     */
    public void flagAsDirty() {
      isDirty = true;
    }

    /**
     * Forces the thread monitor to count blips.
     */
    private void countBlips() {
      readCount = 0;
      unreadCount = 0;
      countBlipsInner(thread);
      isDirty = false;
    }

    private void countBlipsInner(ConversationThread thread) {
      for (ConversationBlip blip : thread.getBlips()) {
        if (knownBlips.contains(blip)) {
          if (getSupplement().isUnread(blip)) {
            unreadCount++;
          } else {
            readCount++;
          }
        }
        // Add blips from reply threads.
        for (ConversationThread replyThread : blip.getReplyThreads()) {
          // Always create monitors (for efficiency in initialisation); but
          // don't necessarily include them in events unless (via monitor/
          // getReadState/getUnreadState) they are explicitly included.
          ThreadReadState replyState = getOrCreateThreadState(replyThread);
          readCount += replyState.getReadCount();
          unreadCount += replyState.getUnreadCount();
        }
      }
    }
  }

  /** All blips known to the monitor. */
  private final ReadStateBucket knownBlips = new ReadStateBucket();

  /** Read/unread state of each thread. */
  private final IdentityMap<ConversationThread, ThreadReadState> threadStates =
      CollectionUtils.createIdentityMap();

  /** Event listeners. */
  private final CopyOnWriteSet<ThreadReadStateMonitor.Listener> listeners =
      CopyOnWriteSet.create();

  /**
   * A latch used to distribute events consistently; see comment near
   * {@link #countUpEvent} and {@link #countDownEvent} where the latch is
   * manipulated.
   */
  private int eventLatch = 0;

  /**
   * Threads which have events on them during a sequence of state changes.
   */
  private final IdentitySet<ConversationThread> eventThreads = CollectionUtils.createIdentitySet();

  private final ObservableSupplementedWave supplementedWave;
  private final ObservableConversationView conversationView;

  /** Whether this monitor is ready, set to true in {@link #init()}. */
  private boolean isReady = false;

  /**
   * Threads which have been {@link #monitor}ed before being ready. Set to null
   * in {@link #init()}.
   */
  private IdentitySet<ConversationThread> threadsToNotifyWhenReady =
      CollectionUtils.createIdentitySet();

  /**
   * Creates and initializes a ThreadReadStateMonitorImpl.
   *
   * @param conversationView the wave to monitor thread read state of
   */
  public static ThreadReadStateMonitorImpl create(ObservableSupplementedWave supplementedWave,
      ObservableConversationView conversationView) {
    ThreadReadStateMonitorImpl monitor = new ThreadReadStateMonitorImpl(
        supplementedWave, conversationView);
    monitor.init();
    return monitor;
  }

  private ThreadReadStateMonitorImpl(ObservableSupplementedWave supplementedWave,
      ObservableConversationView conversationView) {
    Preconditions.checkNotNull(supplementedWave, "supplementedWave cannot be null");
    Preconditions.checkNotNull(conversationView, "conversationView cannot be null");
    this.supplementedWave = supplementedWave;
    this.conversationView = conversationView;
  }

  private void init() {
    // Add existing conversations.  The monitor isn't ready through this, so no
    // events should be generated.
    for (ObservableConversation conversation : conversationView.getConversations()) {
      onConversationAdded(conversation);
    }

    // Listen for new converations.
    conversationView.addListener(this);

    // Listen to supplement read/unread events.
    supplementedWave.addListener(this);

    isReady = true;

    if (!threadsToNotifyWhenReady.isEmpty()) {
      // Initialise any queried threads.
      threadsToNotifyWhenReady.each(new IdentitySet.Proc<ConversationThread>() {
        @Override
        public void apply(ConversationThread thread) {
          monitor(thread);
        }
      });

      // Notify listeners of the queried threads.
      notifyListeners(threadsToNotifyWhenReady);
      threadsToNotifyWhenReady.clear();
      threadsToNotifyWhenReady = null;
    }
  }

  //
  // ThreadReadStateMonitor
  //

  @Override
  public void monitor(ConversationThread thread) {
    if (isReady) {
      getOrCreateMonitoredThreadState(thread);
    } else {
      threadsToNotifyWhenReady.add(thread);
    }
  }

  @Override
  public void ignore(ConversationThread thread) {
    if (isReady) {
      threadStates.remove(thread);
    } else {
      threadsToNotifyWhenReady.remove(thread);
    }
  }

  @Override
  public int getReadCount(ConversationThread thread) {
    Preconditions.checkState(isReady, "ThreadReadStateMonitor queried before ready");
    return getOrCreateMonitoredThreadState(thread).getReadCount();
  }

  @Override
  public int getUnreadCount(ConversationThread thread) {
    Preconditions.checkState(isReady, "ThreadReadStateMonitor queried before ready");
    return getOrCreateMonitoredThreadState(thread).getUnreadCount();
  }

  @Override
  public int getTotalCount(ConversationThread thread) {
    return getReadCount(thread) + getUnreadCount(thread);
  }

  @Override
  public boolean isReady() {
    return isReady;
  }

  @Override
  public void addListener(ThreadReadStateMonitor.Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ThreadReadStateMonitor.Listener listener) {
    listeners.remove(listener);
  }

  //
  // ObservableConversation.Listener
  //

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
    handleThreadAdded(thread);
  }

  /**
   * Recursively adds a thread to be monitored.
   */
  private void handleThreadAdded(ObservableConversationThread thread) {
    // Note: thread state monitor added lazily.
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipAdded(blip);
      // Recursion will continue for reply threads of the blip in handleBlipAdded.
    }
  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
    handleThreadRemoved(thread);
    assert !threadStates.has(thread);
  }

  /**
   * Recursively removes a thread, the inverse of {@link #handleThreadAdded}.
   */
  private void handleThreadRemoved(ObservableConversationThread thread) {
    // Note: threadStates might not necessarily contain this thread due to laziness.
    threadStates.remove(thread);
    for (ObservableConversationBlip blip : thread.getBlips()) {
      handleBlipRemoved(blip);
    }
  }

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
    handleBlipAdded(blip);
  }

  private void handleBlipAdded(ObservableConversationBlip blip) {
    // Check that this isn't a repeated event.
    if (!knownBlips.handleBlipAdded(blip)) {
      return;
    }

    countUpEvent();

    // Handle the event for all parent threads.
    ConversationThread thread = blip.getThread();
    while (thread != null) {
      ThreadReadState state = threadStates.get(thread);
      if (state != null) {
        state.handleBlipAdded(blip);
        registerEventIfMonitored(state);
      }
      thread = getParent(thread);
    }

    // Add any reply threads.
    for (ObservableConversationThread replyThread : blip.getReplyThreads()) {
      handleThreadAdded(replyThread);
    }

    countDownEvent();
  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
    handleBlipRemoved(blip);
  }

  private void handleBlipRemoved(ObservableConversationBlip blip) {
    // Check that this isn't a repeated event.
    if (!knownBlips.handleBlipRemoved(blip)) {
      return;
    }

    countUpEvent();

    // Handle the event for all parent threads.
    ConversationThread thread = blip.getThread();
    while (thread != null) {
      ThreadReadState state = threadStates.get(thread);
      if (state != null) {
        state.handleBlipRemoved(blip);
        registerEventIfMonitored(state);
      }
      thread = getParent(thread);
    }

    // Remove any inline child threads of this blip (non-inline replies will
    // just be reanchored).
    for (ObservableConversationThread replyThread : blip.getReplyThreads()) {
        handleThreadRemoved(replyThread);
    }

    countDownEvent();
  }

  //
  // ObservableConversationView.Listener
  //

  @Override
  public void onConversationAdded(ObservableConversation conversation) {
    handleThreadAdded(conversation.getRootThread());
    conversation.addListener(this);
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
    // Check that a blip really did change state.
    if (!knownBlips.handleMaybeBlipReadStateChanged(blip)) {
      return;
    }

    countUpEvent();

    ObservableConversationThread thread = blip.getThread();
    while (thread != null) {
      ThreadReadState state = threadStates.get(thread);
      if (state != null) {
        state.handleBlipReadStateChanged(blip);
        registerEventIfMonitored(state);
      }
      thread = getParent(thread);
    }

    countDownEvent();
  }

  @Override
  public void onMaybeWaveletReadChanged() {
    countUpEvent();

    // Bucket should be notified first.
    knownBlips.handleWaveletReadStateChanged();

    // Set all thread counts as "dirty" so that when next queried they will be
    // recalculated.
    threadStates.each(new IdentityMap.ProcV<ConversationThread, ThreadReadState>() {
      @Override
      public void apply(ConversationThread thread, ThreadReadState state) {
        state.flagAsDirty();
        registerEventIfMonitored(state);
      }
    });

    countDownEvent();
  }

  //
  // Event distribution helpers.
  //
  // The complicated nature of thread and blip adding/removing warrants some
  // specialised event distribution code.  E.g. if a blip is removed then
  //   - all parent threads must be notified of the deletion, and also
  //   - all inline child threads must be deleted, which in turn means
  //   - all parent threads must be notified, and all the while
  //   - listeners must be notified in a consistent state.
  // To solve this circular dependency without firing too many events, we set
  // up a "latch" for the events.  Methods should "count up" on the latch before
  // registering events, then "count down" when done.  If the latch reaches 0,
  // distribute events on the registered threads then clear them.
  //

  /**
   * Called by methods to register interest in an event.
   */
  private void countUpEvent() {
    eventLatch++;
    assert eventLatch > 0;
  }

  /**
   * Called by methods when their registered interest is completed.
   */
  private void countDownEvent() {
    assert eventLatch > 0;
    eventLatch--;
    if (eventLatch == 0 && !eventThreads.isEmpty()) {
      notifyListeners(eventThreads);
      eventThreads.clear();
    }
  }

  /**
   * Registers an event on a thread such that some time in the future a call
   * to {@link #countDownEvent} will cause listeners to be notified of changes on it.
   * An event is only registered if the thread state is actually monitored.
   */
  private void registerEventIfMonitored(ThreadReadState state) {
    assert eventLatch > 0;
    if (state.isMonitored()) {
      eventThreads.add(state.thread);
    }
  }

  /**
   * Notifies listeners of a change to a collection of threads.  Should only be
   * called from {@link #countDownEvent} apart from on initialisation.
   */
  private void notifyListeners(IdentitySet<ConversationThread> threads) {
    for (Listener listener : listeners) {
      listener.onReadStateChanged(threads);
    }
  }

  //
  // Helpers.
  //

  /**
   * @return the supplement, which must be ready
   */
  private SupplementedWave getSupplement() {
    ObservableSupplementedWave supplement = supplementedWave;
    assert supplement != null;
//    assert supplement.isReady();
    return supplement;
  }

  /**
   * Gets the ThreadReadState for a thread, creating one if necessary.
   */
  private ThreadReadState getOrCreateThreadState(ConversationThread thread) {
    assert thread != null;
    assert isReady;
    ThreadReadState state = threadStates.get(thread);
    if (state == null) {
      state = new ThreadReadState(thread);
      threadStates.put(thread, state);
    }
    return state;
  }

  /**
   * Gets the ThreadReadState for a thread, creating one if necessary, and
   * including it in events regardless of whether it was created or not.
   */
  private ThreadReadState getOrCreateMonitoredThreadState(ConversationThread thread) {
    ThreadReadState state = getOrCreateThreadState(thread);
    state.setIsMonitored(true);
    return state;
  }

  /**
   * @return the parent thread of a thread, or null if the thread is top-level
   */
  private ConversationThread getParent(ConversationThread thread) {
    return (thread.getParentBlip() != null) ? thread.getParentBlip().getThread() : null;
  }

  /**
   * @return the parent thread of a thread, or null if the thread is top-level
   */
  private ObservableConversationThread getParent(ObservableConversationThread thread) {
    return thread.getParentBlip() != null ? thread.getParentBlip().getThread() : null;
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
