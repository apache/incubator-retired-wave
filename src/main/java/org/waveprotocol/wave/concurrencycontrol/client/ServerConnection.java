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

package org.waveprotocol.wave.concurrencycontrol.client;

import org.waveprotocol.wave.model.operation.wave.WaveletDelta;

/**
 * Receives deltas to send to the server.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface ServerConnection {
  /**
   * Sends the given delta on the wire. All ops in the delta must have the same author.
   * {@link #isOpen()} must be true before sending.
   *
   * @param delta delta to send (must not be null)
   */
  public void send(WaveletDelta delta);

  /**
   * Checks whether the connection is ready to accept deltas. Needed because the
   * connection can be closed but we haven't reconnected to the server for
   * recovery.
   *
   * @return whether the connection to the server open
   */
  public boolean isOpen();

  /**
   * @return Debug string that details profile information regarding data being sent.
   */
  public String debugGetProfilingInfo();
}
