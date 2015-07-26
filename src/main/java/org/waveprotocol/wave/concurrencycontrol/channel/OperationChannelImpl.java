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

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.client.ConcurrencyControl;
import org.waveprotocol.wave.concurrencycontrol.client.ServerConnection;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;


/**
 * This operation channel implementation mediates between a WaveletDeltaChannel
 * and client ConcurrencyControl. It sends operations via concurrency control in
 * deltas through a wavelet delta channel, and accepts deltas from the the delta
 * channel as broadcast from the server. Ops are transformed and queued in the
 * ConcurrencyControl class.
 *
 * The delta channel is connected through a raw wavelet channel.
 *
 *   ConcurrencyControl
 *          ^
 *          |
 *          v
 *   OperationChannel  <=>  WaveletDeltaChannel <=> WaveletChannel
 *
 * The channel allows reconnection when after some failures by providing
 * reconnection versions and reconciling concurrency control state
 * with the server's state. Recovery can fail when more than one client modifies
 * the wave, and the wave server crashes losing history about the order of
 * deltas accepted.
 *
 * @see ConcurrencyControl
 * @see WaveletDeltaChannel
 *
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */
class OperationChannelImpl implements InternalOperationChannel {

  private static enum State {
    /** Initial state, or when we are recovering from disconnection. */
    NOT_CONNECTED,
    /** After onConnection(). */
    CONNECTED,
    /** After close(). */
    CLOSED
  }

  private final LoggerBundle logger;
  private final Accessibility accessibility;

  // "Final", but null after the channel is closed.
  private WaveletDeltaChannel deltaChannel;
  private Listener listener;
  private ConcurrencyControl cc;
  private State state = State.NOT_CONNECTED;

  /**
   * CC server connection which passes requests to the delta channel.
   */
  private final ServerConnection ccServerConnection = new ServerConnection() {
    @Override
    public boolean isOpen() {
      return state == State.CONNECTED;
    }

    @Override
    public void send(final WaveletDelta delta) {
      if (logger.trace().shouldLog()) {
        logger.trace().log("sending delta: ", delta);
      }
      deltaChannel.send(new WaveletDeltaChannel.Transmitter() {
          public ClientMessage takeMessage() {
            return new ClientMessage(delta, false);
          }
        });
    }

    @Override
    public String debugGetProfilingInfo() {
      return deltaChannel.debugGetProfilingInfo();
    }
  };

  /**
   * CC client listener which passes events on to the connection listener.
   */
  private final ConcurrencyControl.ConnectionListener ccListener =
      new ConcurrencyControl.ConnectionListener() {
        @Override
        public void onOperationReceived() {
          signalOperationReceived();
        }
      };

  /**
   * Constructs an operation channel for the named wave.
   *
   * Any sent operations before onConnection are queued to be sent upon
   * connection.
   *
   * @param opsLogger logger to use for operationChannel logging
   * @param deltaChannel delta channel server connection
   * @param cc concurrency control module
   */
  OperationChannelImpl(LoggerBundle opsLogger, WaveletDeltaChannel deltaChannel,
      ConcurrencyControl cc, Accessibility accessibility) {
    this.deltaChannel = deltaChannel;
    this.cc = cc;
    this.accessibility = accessibility;
    this.logger = opsLogger;
    this.state = State.NOT_CONNECTED;

    cc.initialise(ccServerConnection, ccListener);
  }

  // WaveletDeltaChannel.Receiver implementation //

  @Override
  public void onConnection(HashedVersion connectVersion, HashedVersion currentVersion)
      throws ChannelException {
    Preconditions.checkState(state == State.NOT_CONNECTED,
        "OperationChannel received onConnection in state " + state);
    Preconditions.checkState(cc != null, "Cannot connect a closed channel");
    state = State.CONNECTED;
    cc.onOpen(connectVersion, currentVersion);
  }

  @Override
  public void onDelta(TransformedWaveletDelta delta) throws ChannelException {
    try {
      if (logger.trace().shouldLog()) {
        logger.trace().log("Received delta: ", delta);
      }
      cc.onServerDelta(delta);
    } catch (TransformException e) {
      throw new ChannelException(ResponseCode.INVALID_OPERATION,
          "Operation channel failed on server delta: " + this + ", " + deltaChannel + ", " + cc,
          e, Recoverable.NOT_RECOVERABLE, null, null);
    } catch (OperationException e) {
      throw new ChannelException(
          e.isSchemaViolation() ? ResponseCode.SCHEMA_VIOLATION : ResponseCode.INVALID_OPERATION,
          "Operation channel failed on server delta: " + this + ", " + deltaChannel + ", " + cc,
          e, Recoverable.NOT_RECOVERABLE, null, null);
    }
  }

  @Override
  public void onAck(int opsApplied, HashedVersion signature) throws ChannelException {
    try {
      cc.onSuccess(opsApplied, signature);
    } catch (TransformException e) {
      throw new ChannelException(ResponseCode.INVALID_OPERATION,
          "Operation channel failed on ack: " + this + ", " + deltaChannel + ", " + cc,
          e, Recoverable.NOT_RECOVERABLE, null,
          null);
    }
  }

  @Override
  public void onNack(ResponseCode responseCode, String errorString, long version)
      throws ChannelException {
    throw new ChannelException(responseCode,
        "Operation channel failed on nack: code=" + responseCode + ", "
        + errorString + ", " + this + ", " + deltaChannel + ", " + cc,
        null, Recoverable.NOT_RECOVERABLE, null, null);

  }

  @Override
  public void onCommit(long version) {
    cc.onCommit(version);
  }

  // BaseOperationChannel implementation //

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public void send(WaveletOperation... operations) throws ChannelException {
    if (state == State.CLOSED) {
      // TODO(anorth): throw an exception here after fixing clients.
      logger.error().log("Cannot send to closed operation channel: " + this);
    } else if (accessibility.isWritable()) {
      try {
        cc.onClientOperations(operations);
      } catch (TransformException e) {
        throw new ChannelException(ResponseCode.INVALID_OPERATION,
            "Operation channel failed on send: " + this + ", " + deltaChannel + ", " + cc, e,
            Recoverable.NOT_RECOVERABLE, null, null);
      }
    } else {
      throw new ChannelException(ResponseCode.NOT_AUTHORIZED,
          "Attempt to write to inaccessible wavelet", null, Recoverable.NOT_RECOVERABLE, null, null);
    }
  }

  @Override
  public WaveletOperation receive() {
    if (state == State.CLOSED) {
      // TODO(anorth): throw an exception here after Wally doesn't do this.
      logger.error().log("Cannot receive from closed operation channel: " + this);
      return null;
    } else {
      return cc.receive();
    }
  }

  @Override
  public WaveletOperation peek() {
    if (state == State.CLOSED) {
      // TODO(anorth): throw an exception here after Wally doesn't do this.
      logger.error().log("Cannot peek at closed operation channel: " + this);
      return null;
    } else {
      return cc.peek();
    }
  }

  @Override
  public List<HashedVersion> getReconnectVersions() {
    if (state == State.CLOSED) {
      throw new IllegalStateException("Cannot query closed operation channel: " + this);
    }
    return cc.getReconnectionVersions();
  }

  /**
   * Resets this channel ready to be reconnected.
   */
  @Override
  public void reset() {
    state = State.NOT_CONNECTED;
  }

  /**
   * Closes this channel permanently.
   */
  @Override
  public void close() {
    Preconditions.checkState(state != State.CLOSED, "Cannot close already-closed channel");
    state = State.CLOSED;
    cc.close();

    // Disconnect everything.
    cc = null;
    deltaChannel = null;
    listener = null;
  }

  /**
   * Signals to the listener than an operation is ready.
   */
  private void signalOperationReceived () {
    if (listener != null) {
      listener.onOperationReceived();
    }
  }

  @Override
  public String toString() {
    return "Operation Channel State = [state: " + state + "]";
  }

  @Override
  public String getDebugString() {
    return cc + "==========\n" +
        OperationChannelImpl.this.toString() + "==========\n" +
        deltaChannel;
  }
}
