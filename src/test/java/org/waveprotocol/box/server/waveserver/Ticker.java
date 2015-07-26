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

package org.waveprotocol.box.server.waveserver;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.util.Set;

/**
 * Provides global, simple, deterministic single-threaded events based on global ticks.
 */
public class Ticker {
  /** An easy number of ticks easy to divide, add, and multiply. */
  public static final int EASY_TICKS = (2 * 2 * 2 * 2) * (3 * 3 * 3);

  private int globalTicks = 0;
  private final Multimap<Integer, Runnable> tickers = ArrayListMultimap.create();

  /**
   * Add an event to run after the global tick counter has run to the given number of ticks.  The
   * event will be run immediately if the ticks were in the past.
   */
  public void runAt(int ticks, Runnable doneEvent) {
    int finishedAt = ticks - globalTicks;
    if (finishedAt <= 0) {
      doneEvent.run();
    } else {
      tickers.put(finishedAt, doneEvent);
    }
  }

  /**
   * Advance the global tick counter and run finished events.
   */
  public void tick(int ticks) {
    globalTicks += ticks;
    Set<Integer> tickerView = ImmutableSet.copyOf(tickers.keySet());

    for (int i : tickerView) {
      if (i <= globalTicks) {
        for (Runnable doneEvent : tickers.get(i)) {
          doneEvent.run();
        }
        tickers.removeAll(i);
      }
    }
  }
}
