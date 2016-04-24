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

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * Connects a data object with operation sinks that make it live.
 *
 * @param <T> type to which operations apply
 * @param <O> operation type
 */
public final class LiveTarget<T, O extends Operation<? super T>> {
  /** Target data object. */
  private final T target;

  /** Sink that executes consumed operations. */
  private final SilentOperationSink<O> executor;

  /** Output sink for outgoing operations. */
  private final ProxyOperationSink<O> output;

  /**
   * Creates a live-target triple.
   */
  private LiveTarget(T target, SilentOperationSink<O> executor, ProxyOperationSink<O> output) {
    this.target = target;
    this.executor = executor;
    this.output = output;
  }

  /**
   * Creates a live-target.
   */
  public static <T, O extends Operation<? super T>> LiveTarget<T, O> create(final T data) {
    ProxyOperationSink<O> output = ProxyOperationSink.create();
    SilentOperationSink<O> executor = new SilentOperationSink<O>() {
      @Override
      public void consume(O operation) {
        try {
          operation.apply(data);
        } catch (OperationException e) {
          // Fail this object permanently
          throw new OperationRuntimeException("Error applying op", e);
        }
      }
    };
    return new LiveTarget<T, O>(data, executor, output);
  }

  /** @return the target object being controlled by operations. */
  public T getTarget() {
    return target;
  }

  /** @return the sink that executes operations on the target. */
  public SilentOperationSink<O> getExecutorSink() {
    return executor;
  }

  /** @return the sink to which operations from the target are sent. */
  public ProxyOperationSink<O> getOutputSink() {
    return output;
  }
}
