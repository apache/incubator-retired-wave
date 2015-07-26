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

import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.util.IdentitySet;

/**
 * Monitors the conversation for the read/unread state of blips per thread.
 *
 * For the sake of efficiency, listener events will only arrive for threads
 * which have been explicitly queried, either through {@link #monitor} (valid
 * regardless of readiness) or implicitly through {@link #getReadCount} or
 * {@link #getUnreadCount} (only valid when ready). This is so that
 * implementations don't need to maintain unnecessary state for situations such
 * as non-inline threads, paged-out threads, and the root thread.
 */
public interface ThreadReadStateMonitor {

  /**
   * Listener interface for changes to read/unread blip counts.
   */
  interface Listener {
    /**
     * Called when the read/unread count changes for a set of threads.
     * Also fires when the monitor becomes ready, with the initial monitored
     * threads.
     *
     * With a conversation structured as follows:
     *
     *  | A (10, 20)
     *  | a
     *  | a
     *  \_ B (4, 16)
     *   | b
     *   | b
     *  \_ C (6, 4)
     *   | c
     *   | c
     *
     * If a blip becomes read in C then the event will be
     *   onReadStateChanged({A, C}) where
     *     - getReadCount(A) = 17 (10 + 6 + 1)
     *     - getUnreadCount(A) = 23 (20 + 4 - 1)
     *     - getReadCount(C) = 7 (6 + 1)
     *     - getUnreadCount(C) = 3 (4 - 1)
     * If a read blip is added to B then the event will be
     *   onReadStateChanged({A, B}) where
     *     - getReadCount(A) = 15 (10 + 4 + 1)
     *     - getUnreadCount(A) = 36 (20 + 16)
     *     - getReadCount(B) = 5 (4 + 1)
     *     - getUnreadCount(B) = 16
     * If an unread blip is added to A then the event will be
     *   onReadStateChanged({A}) where
     *     - getReadCount(A) = 10
     *     - getUnreadCount(A) = 21
     *
     * And so on.
     *
     * @param threads the set of threads which have been affected by a change
     */
    void onReadStateChanged(IdentitySet<ConversationThread> threads);
  }

  /**
   * Starts monitoring a thread, a valid operation before the monitor is ready.
   * Listener events will now include this thread when its read state changes.
   *
   * Note that querying a thread through {@link #getReadCount} or
   * {@link #getUnreadCount} implicitly monitors a thread, if the monitor is
   * ready.
   *
   * @param thread the thread to monitor
   */
  void monitor(ConversationThread thread);

  /**
   * Stops monitoring a thread, a valid operation before the monitor is ready.
   * Listener events will no longer include this thread.
   *
   * @param thread the thread to stop monitoring
   */
  void ignore(ConversationThread thread);

  /**
   * Gets the read count of a thread, and implicitly starts monitoring it.
   * Only valid when {@link #isReady} is true (if not, may throw an exception).
   *
   * @return the current read blip count for a thread
   */
  int getReadCount(ConversationThread thread);

  /**
   * Gets the unread count of a thread, and implicitly starts monitoring it.
   * Only valid when {@link #isReady} is true (if not, may throw an exception).
   *
   * @return the current unread blip count for a thread
   */
  int getUnreadCount(ConversationThread thread);
  
  int getTotalCount(ConversationThread thread);

  /**
   * Gets whether the monitor is ready to be queried for unread counts.  If not,
   * {@link #getReadCount} and {@link #getUnreadCount} should throw an
   * exception, while all other methods are valid.
   *
   * @return whether the monitor is ready
   */
  boolean isReady();

  /**
   * Adds a listener to change events of monitored threads.
   */
  void addListener(Listener listener);

  /**
   * Removes a listener to change events of monitored threads.
   */
  void removeListener(Listener listener);
}
