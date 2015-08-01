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

package org.waveprotocol.box.webclient.search;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Queue;

/**
 * An item pool that attempts to ensure that its free pool always has a minimum
 * number of items.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class ToppingUpPool<T> implements Pool<T>, IncrementalTask {

  /**
   * Item factory.
   */
  public interface Factory<T> {
    T create();
  }

  /** Timer for running the top-up task. */
  private final TimerService timer;

  /** Desired number of items to be ready in the free pool. */
  private final int minimumFreeCount;

  /** Store of freed items. */
  private final Queue<T> free = CollectionUtils.createQueue();

  /** Object factory. */
  private final Factory<? extends T> factory;

  /** Number of items created. */
  private int created;

  @VisibleForTesting
  ToppingUpPool(TimerService timer, Factory<? extends T> factory, int minimumFreeCount) {
    this.timer = timer;
    this.factory = factory;
    this.minimumFreeCount = minimumFreeCount;
    warmUpLater();
  }

  /**
   * Creates a pool.
   *
   * @param factory factory for creating items
   * @param minSize number of items to keep free in the pool
   */
  public static <T> ToppingUpPool<T> create(Factory<? extends T> factory, int minSize) {
    return new ToppingUpPool<T>(SchedulerInstance.getLowPriorityTimer(), factory, minSize);
  }

  /**
   * @return a fresh, new, item.
   */
  private T summon() {
    created++;
    return factory.create();
  }

  @Override
  public T get() {
    T item;
    if (free.isEmpty()) {
      item = summon();
    } else {
      item = free.poll();
      // If the free pool just became empty, schedule top-up.
      if (free.isEmpty()) {
        warmUpLater();
      }
    }
    return item;
  }

  @Override
  public void recycle(T itemUi) {
    free.add(itemUi);
  }

  /**
   * @return the number of items produced by this pool that are still in use
   *         (i.e., have not been recycled).
   */
  public int getWildCount() {
    return created - free.size();
  }

  //
  // Task that, when triggered, ensures the free pool has a minimum size.
  //

  /**
   * Warms up the item pool at some time in the future.
   */
  private void warmUpLater() {
    if (!timer.isScheduled(this)) {
      timer.schedule(this);
    }
  }

  @Override
  public boolean execute() {
    if (free.size() < minimumFreeCount) {
      free.add(summon());
    }
    // Keep running while still more items to create
    return free.size() < minimumFreeCount;
  }
}
