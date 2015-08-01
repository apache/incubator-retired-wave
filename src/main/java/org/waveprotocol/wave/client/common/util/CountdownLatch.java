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

package org.waveprotocol.wave.client.common.util;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.Command;

/**
 * Simple synchronization tool that waits for some fixed number of calls to
 * {@link #tick()}, and invokes a command on the last one. Any further calls to
 * {@link #tick()} produce an error.
 * <p>
 * The intended use case for this class is a gate that waits for some set of
 * asynchronous tasks to complete.
 *
 */
public final class CountdownLatch {
  /** Command to execute when the all ticks have occurred. */
  private final Command whenZero;

  /** Number of expected ticks. */
  private int count;

  private CountdownLatch(int count, Command whenZero) {
    this.whenZero = whenZero;
    this.count = count;
  }

  /**
   * Creates a countdown latch.
   *
   * @param count number of ticks
   * @param whenZero command to execute after {@code count} ticks
   * @return a new latch.
   */
  public static CountdownLatch create(int count, Command whenZero) {
    return new CountdownLatch(count, whenZero);
  }

  /**
   * Ticks this counter.
   *
   * @throws IllegalStateException if this counter has already been ticked the
   *         expected number of times.
   */
  public void tick() {
    Preconditions.checkState(count > 0);
    count--;
    if (count == 0) {
      whenZero.execute();
    }
  }
}
