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


package org.waveprotocol.wave.client.scheduler;

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Queue;

/**
 * Constructs a queue of items to be processed one by one.
 *
 */
public abstract class QueueProcessor<T> implements IncrementalTask {
  private final Queue<T> queue;
  private final TimerService timer;

  /**
   * Creates a serial queue processor which serializes processing of the queued
   * items.
   *
   * @param timerService
   */
  public QueueProcessor(TimerService timerService) {
    this.timer = timerService;
    this.queue = CollectionUtils.createQueue();
  }

  /**
   * Processes an item.
   */
  protected abstract void process(T item);

  /**
   * Adds an item to be processed.
   */
  public final void add(T item) {
    if (queue.isEmpty()) {
      timer.schedule(this);
    }
    queue.add(item);
  }

  /**
   * Processes an item.
   */
  @Override
  public final boolean execute() {
    process(queue.poll());
    return !queue.isEmpty();
  }

  /**
   * Stops this processor if it is scheduled.
   * <p>
   * It is undefined whether unprocessed items will be processed if this
   * processor becomes scheduled again, or if those items will be discarded.
   */
  public final void cancel() {
    timer.cancel(this);
  }
}
