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


/**
 * Monitors the conversation for unread blips.
 */
public interface BlipReadStateMonitor {

  /**
   * Listener interface for changes to read/unread blip counts.
   */
  interface Listener {
    /**
     * Called when the read/unread count changes.
     */
    void onReadStateChanged();
  }

  /**
   * @return The current read blip count.
   */
  int getReadCount();

  /**
   * @return The current unread blip count.
   */
  int getUnreadCount();

  /**
   * Adds a listener to change events.
   */
  void addListener(Listener listener);

  /**
   * Removes a listener to change events.
   */
  void removeListener(Listener listener);
}
