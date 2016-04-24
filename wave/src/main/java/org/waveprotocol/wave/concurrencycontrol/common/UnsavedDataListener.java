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

package org.waveprotocol.wave.concurrencycontrol.common;

/**
 * Listener for information about whether data has been acknowledged and
 * committed by servers.
 *
 * A typical connection state:
 * <pre>
 * D = client delta
 *
 *  acknowledged
 *    | |   |
 *    v v   v
 * ---D-D---D---D------------D-D---D--D----------------> Time
 *   ^          ^            ^
 *   |          |            |
 * commited   in flight    queue start
 * </pre>
 *
 * @author jochen@google.com (Jochen Bekmann)
 * @author zdwang@google.com (David Wang)
 */
public interface UnsavedDataListener {
  /**
   * Provides information about acknowledged and committed data.
   */
  public interface UnsavedDataInfo {
    /**
     * Size of data sent but not yet acknowledged.
     */
    int inFlightSize();

    /**
     * An estimate of the size of the total unacknowledged data: in flight data
     * plus data accumulating waiting to be sent.
     */
    int estimateUnacknowledgedSize();

    /**
     * An estimate of the size of the total uncommitted data: unacknowledged
     * data plus acknowledged but not committed data.
     */
    int estimateUncommittedSize();

    /**
     * Latest acknowledgement version.
     */
    long laskAckVersion();

    /**
     * Latest commit version.
     */
    long lastCommitVersion();

    /**
     * Calling this could be expensive due to stringification of data.
     * Refrain from calling this constantly.
     *
     * @return A debug string about the unsaved data.
     */
    String getInfo();
  }

  /**
   * Called by the source of updates. Will not be called after onClose() was
   * called.
   *
   * @param unsavedDataInfo information about data that's unsaved. This
   *        object is only valid during this method call. Invoking methods on
   *        this object after this call may give you incomplete data.
   */
  void onUpdate(UnsavedDataInfo unsavedDataInfo);

  /**
   * Called when the resource has been closed. This removes the updater's entry
   * in the listener.
   *
   * @param everythingCommitted true if all data on the server has been
   *        committed.
   */
  void onClose(boolean everythingCommitted);
}
