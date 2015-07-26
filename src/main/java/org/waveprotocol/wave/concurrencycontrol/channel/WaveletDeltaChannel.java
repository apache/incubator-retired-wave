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
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

public interface WaveletDeltaChannel {
  /**
   * Receiver of message from a delta channel.
   *
   * A receiver will first receive a connection message followed by zero or more
   * deltas, commits, acks, or nacks, and eventually a termination message. If
   * the channel is reset from either end this sequence may repeat.
   */
  public interface Receiver {
    /**
     * Called upon successful connection (re-)establishment with initial
     * connection state. If the channel connects to the latest wavelet version
     * then {@code wavelet} contains the wavelet state at connection and {@code
     * connectVersion} equals {@currentVersion}. If the channel reconnects to a
     * prior known version then {@code wavelet} wave is null and {@code
     * currentVersion} may be later than {@code connectVersion}.
     *
     * @param connectVersion the wavelet version at which the channel connection
     * @param currentVersion the most recent wavelet version at the time of
     *        connection.
     * @throws ChannelException if the channel fails
     */
    public void onConnection(HashedVersion connectVersion, HashedVersion currentVersion)
        throws ChannelException;

    /**
     * Called when a delta is received from the server.
     *
     * @param delta a delta (not sent on this channel)
     * @throws ChannelException if the channel fails
     */
    public void onDelta(TransformedWaveletDelta delta) throws ChannelException;

    /**
     * Called when the wave server has committed operations to persistent
     * storage.
     *
     * @param version the latest committed version
     * @throws ChannelException if the channel fails
     */
    public void onCommit(long version) throws ChannelException;

    /**
     * Called when the server acknowledges (accepts) operations sent on
     * this connection.
     *
     * @param opsApplied number of ops from delta applied by wave server
     * @param version server's wavelet version after last applied operation
     * @throws ChannelException if the channel fails
     */
    public void onAck(int opsApplied, HashedVersion version) throws ChannelException;

    /**
     * Called when the server rejects a delta operation sent on this
     * connection.
     * @param responseCode reason for nack
     * @param errorString optional explanation of error
     * @param version server's wavelet version at time of rejection
     *
     * @throws ChannelException if the channel fails
     */
    public void onNack(ResponseCode responseCode, String errorString, long version)
        throws ChannelException;
  }

  /**
   * Produces a single client WaveDeltaMessage. Will be called at most once
   * ("one-shot").
   */
  public interface Transmitter {
    /**
     * Wave delta channel message from wave client to wave server, consisting of
     * a delta and a requestCommit flag.
     *
     * TODO(anorth): remove ignored requestCommit parameter.
     */
    public final class ClientMessage {
      private final WaveletDelta delta;
      private final boolean commitRequest;
      public ClientMessage(WaveletDelta delta, boolean commitRequest) {
        this.delta = delta;
        this.commitRequest = commitRequest;
      }
      public WaveletDelta getDelta() { return delta; }
      public boolean hasCommitRequest() { return commitRequest; }
    }

    /**
     * Called when channel is ready to transmit.
     *
     * @return the delta to transmit, null means nothing to send (for instance,
     *         if the operation to send has been undone)
     */
    public ClientMessage takeMessage();
  }

  /**
   * Resets this delta channel and installs a new receiver. Any messages already
   * received from the server are dropped. The caller must ensure that the next
   * message (if any) received by this channel is a (re)connection message.
   *
   * @param receiver receiver for future messages from this channel (or null)
   */
  public void reset(Receiver receiver);

  /**
   * Signals the intent to send a delta to the server. When the channel is ready
   * to transmit the delta, it invokes the transmitter's
   * {@link Transmitter#takeMessage()} method to get the delta to transmit.
   *
   * The transmitter is "one-shot": if its takeMessage is invoked, the
   * transmitter is forgotten. This send method must be called (at least) once
   * for every outgoing message.
   *
   * The send method will replace the transmitter from the last call to send, if
   * that transmitter has not been invoked in the meanwhile.
   *
   * Clients must only call send once until an ack or nack is received. The
   * channel must be connected.
   *
   * @param t provides the delta to send when channel is ready to transmit it
   */
  public void send(Transmitter t);

  /**
   * @return Debug string that details profile information regarding data being sent.
   */
  public String debugGetProfilingInfo();
}
