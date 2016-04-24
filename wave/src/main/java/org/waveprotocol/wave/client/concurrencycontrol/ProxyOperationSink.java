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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Queue;

/**
 * A sink that proxies for another sink. This sink is usable immediately after
 * construction. Operations that are consumed before the target sink is set are
 * simply queued. Setting the target sink flushes any operations that have been
 * queued. The target can be set only once, and after it is set, this sink
 * routes all operations to it.
 *
 */
public final class ProxyOperationSink<O extends Operation<?>> implements SilentOperationSink<O> {

  private Queue<O> queue;
  private SilentOperationSink<O> target;

  private ProxyOperationSink() {
  }

  /**
   * Creates a proxy sink.
   */
  public static <O extends Operation<?>> ProxyOperationSink<O> create() {
    return new ProxyOperationSink<O>();
  }

  /**
   * Sets this proxy's target. Any operations previously consumed by this proxy
   * will be forwarded to the target synchronously within this method. Future
   * operations consumed by this proxy will be sent directly to the target. This
   * target can be set at most once.
   *
   * @param target target to consume future operations
   */
  public void setTarget(SilentOperationSink<O> target) {
    Preconditions.checkState(this.target == null);
    this.target = target;

    if (queue != null) {
      while (!queue.isEmpty()) {
        target.consume(queue.poll());
      }
      queue = null;
    }
  }

  @Override
  public void consume(O op) {
    if (target != null) {
      target.consume(op);
    } else {
      if (queue == null) {
        queue = CollectionUtils.createQueue();
      }
      queue.add(op);
    }
  }
}
