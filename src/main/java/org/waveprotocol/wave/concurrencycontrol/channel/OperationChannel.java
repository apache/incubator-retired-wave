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

package org.waveprotocol.wave.concurrencycontrol.channel;

import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;


/**
 * A client socket for a wavelet. Sends/receives operations to/from the wave
 * server and transforms (outbound) client operations against (inbound) server
 * operations.
 *
 * An operation channel can operate without network connection to the server by
 * queuing outbound operations until a network connection is established at a
 * later time.
 *
 * If the network connection fails during operation, or the wave server restarts
 * or fails over, a channel will silently try to reconnect and recover (by
 * selectively retransforming and retransmitting relevant client operations) but
 * will notify if recovery fails.
 *
 */
public interface OperationChannel {

  /**
   * A listener for operations being received by a channel.
   */
  public interface Listener {
    /**
     * Invoked when a remote operation received to the channel.
     */
    void onOperationReceived();
  }

  /**
   * Sets the channel listener.
   *
   * @param listener
   */
  void setListener(Listener listener);

  /**
   * Sends client operations to the wave server.
   * If the channel is not connected, the operations are queued.
   *
   * @param operations  client operations to send
   * @throws ChannelException if the channel is corrupt or misused
   */
  void send(WaveletOperation ... operations) throws ChannelException;

  /**
   * Receive (transformed) server operation from the wave server, if any is available.
   *
   * @return the next server operation, if any is received from the server (and makes it
   *         through transformation), null otherwise
   */
  WaveletOperation receive();

  /**
   * Peek at the next (transformed) server operation from the wave server, if any is available.
   *
   * @return the next server operation, if any is received from the server (and makes it
   *         through transformation), null otherwise
   */
  WaveletOperation peek();

  /**
   * Gets the wavelet versions at which this channel could reconnect.
   */
  List<HashedVersion> getReconnectVersions();

  /**
   * @return A dump of the state of cc stack. This is really only used for debugging purposes.
   */
  String getDebugString();
}
