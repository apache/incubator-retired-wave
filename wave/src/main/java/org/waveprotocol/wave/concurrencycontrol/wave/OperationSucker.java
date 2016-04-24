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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

/**
 * Sucks operations out of an operation channel, and sends them to a sink for
 * application.
 *
 * @author zdwang@google.com (David Wang)
 */
public class OperationSucker implements OperationChannel.Listener {

  /** Channel from which this sucker sucks operations. */
  private final OperationChannel opChannel;

  /** The sink to which each operation is passed after it is sucked. */
  private final FlushingOperationSink<WaveletOperation> listener;

  /**
   * Flags if listener.flush(resumeCallback) returned false and
   * has not yet invoked resumeCallback.
   */
  private boolean receptionIsPaused = false;

  /** Whether this sucker has been shut down. */
  private boolean isShutdown = false;

  /** Callback for the flusher to use to resume the receive loop. */
  private final Runnable resumeCallback = new Runnable() {
    public void run() {
      if (!receptionIsPaused) {
        throw new IllegalStateException(
            "Invalid attempt to resume wave channel reception when it's not paused. " +
            "Resume callback called back synchronously or repeatedly?");
      }
      receptionIsPaused = false;
      receiveLoop();
    }
  };

  /**
   * Registers a listener on an operation channel that sucks out operations as
   * soon as they appear, sending them to a target sink.
   *
   * @param channel channel from which operations are to be pulled
   * @param sink sink to which operations are to be pushed
   */
  public static void start(OperationChannel channel, FlushingOperationSink<WaveletOperation> sink) {
    channel.setListener(new OperationSucker(channel, sink));
  }

  /**
   * This creates a concurrency control manager with an unconnected channel.
   * Call openWave() to connect to the wave server.
   *
   * @param channel channel from which to take ops
   * @param sink sink to which to send ops
   */
  public OperationSucker(OperationChannel channel, FlushingOperationSink<WaveletOperation> sink) {
    this.opChannel = channel;
    this.listener = sink;
  }

  @Override
  public void onOperationReceived() {
    if (!receptionIsPaused) {
      receiveLoop();
    }
  }

  /**
   * Shuts down this sucker. No more ops will be received.
   */
  public void shutdown() {
    this.isShutdown = true;
  }

  /**
   * Pass any available transformed server operations from the op channel to the listener,
   * while the listener is willing to receive operations.  If the receiver is unwilling,
   * we pause reception and await that the receiver resumes reception and reinvokes the
   * receive loop.
   *
   * Assumes reception is not paused on entry.
   */
  private void receiveLoop() {
    if (isShutdown) {
      return;
    }

    WaveletOperation next = opChannel.peek();
    while (next != null && !isShutdown) {
      // We flush the listener before every operation and specify what the next operation
      // will be, so the listener can choose to only flush the (editor of the) affected blip.
      // (Alternatively, we could tell the listener to flush everything before entering the
      // receive loop and then receive and pass on all available operations without further
      // flushes. That would be faster if, generally, there are more ops per batch than
      // blips in the wave. We have no evidence that that's the case.)
      receptionIsPaused = !listener.flush(next, resumeCallback);
      if (receptionIsPaused) {
        // The listener will call resume callback later, which clears receptionIsPaused
        // and runs the receive loop. We stop the receive loop until then.
        return;
      }

      // We take the next operation and pass it to the listener, if the
      // operation is still the same, that is, it has not been modified
      // by operation transformation against any client operations sent
      // to us from within the call to serverOperationListener.flush().
      // Otherwise we cycle around and call flush again, just to be sure.
      WaveletOperation newNext = opChannel.peek();
      if (next == newNext) {
        listener.consume(opChannel.receive());
        if (!isShutdown) {
          next = opChannel.peek();
        }
      } else {
        next = newNext;
      }
    }
  }
}
