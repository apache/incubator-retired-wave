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

package org.waveprotocol.wave.client.concurrencycontrol;

import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.concurrencycontrol.wave.FlushingOperationSink;
import org.waveprotocol.wave.concurrencycontrol.wave.OperationSucker;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;

/**
 * Binds a wave's wavelets with supplied operation channels.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class StaticChannelBinder {

  private final WaveletOperationalizer operationalizer;
  private final WaveDocuments<? extends CcDocument> docRegistry;

  /**
   * Creates a binder for a wave.
   *
   * @param operationalizer operationalizer of the wave
   * @param docRegistry document registry of the wave
   */
  public StaticChannelBinder(
      WaveletOperationalizer operationalizer, WaveDocuments<? extends CcDocument> docRegistry) {
    this.operationalizer = operationalizer;
    this.docRegistry = docRegistry;
  }

  /**
   * Connects a wavelet's operation sinks with an operation channel.
   *
   * @param id id of the wavelet to bind
   * @param channel channel to bind
   */
  public void bind(String id, OperationChannel channel) {
    Pair<SilentOperationSink<WaveletOperation>, ProxyOperationSink<WaveletOperation>> sinks =
        operationalizer.getSinks(id);

    // Bind the two ends together.
    OperationSucker.start(channel, asFlushing(id, sinks.first));
    sinks.second.setTarget(asOpSink(channel));
  }

  /**
   * Adapts a regular operation sink as a flushing sink.
   */
  private FlushingOperationSink<WaveletOperation> asFlushing(
      final String waveletId, final SilentOperationSink<WaveletOperation> target) {
    return new FlushingOperationSink<WaveletOperation>() {
      @Override
      public void consume(WaveletOperation op) {
        target.consume(op);
      }

      @Override
      public boolean flush(WaveletOperation op, Runnable c) {
        if (op instanceof WaveletBlipOperation) {
          CcDocument doc =
              docRegistry.getBlipDocument(waveletId, ((WaveletBlipOperation) op).getBlipId());
          if (doc != null) {
            return doc.flush(c);
          }
        }
        return true;
      }
    };
  }

  /**
   * Adapts an operation channel, making it look like an operation sink. The
   * only reason a channel is not already a sink is because it has a more
   * general acceptor that takes a varargs parameter.
   */
  private static SilentOperationSink<WaveletOperation> asOpSink(final OperationChannel target) {
    return new SilentOperationSink<WaveletOperation>() {
      @Override
      public void consume(WaveletOperation op) {
        try {
          target.send(op);
        } catch (ChannelException e) {
          throw new RuntimeException("Send failed, channel is broken", e);
        }
      }
    };
  }
}
