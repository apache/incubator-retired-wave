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


package org.waveprotocol.wave.client.scheduler.testing;

import org.waveprotocol.wave.client.scheduler.SimpleTimer;

/**
 * Timer that is ticked forward manually instead of by the passing of time
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FakeSimpleTimer implements SimpleTimer {

  private final Runnable runnable;

  private double currentTimeMillis = 0;
  private double scheduledTime = Double.MAX_VALUE;

  /**
   * @param runnable The runnable that is to be scheduled
   */
  public FakeSimpleTimer(Runnable runnable) {
    this.runnable = runnable;
  }

  @Override
  public void cancel() {
    scheduledTime = Double.MAX_VALUE;
  }

  @Override
  public double getTime() {
    return currentTimeMillis;
  }

  @Override
  public void schedule() {
    scheduledTime = currentTimeMillis;
  }

  @Override
  public void schedule(double when) {
    scheduledTime = when;
  }

  /**
   * Progress the clock, but do not trigger the runnable
   * @param millis
   */
  public void tick(int millis) {
    currentTimeMillis += millis;
  }

  /**
   * Run the runnable if it is due
   */
  public void trigger() {
    if (currentTimeMillis >= scheduledTime) {
      scheduledTime = Double.MAX_VALUE;
      runnable.run();
    }
  }

  /**
   * Equivalent to calling tick(millis) and then trigger()
   * @param millis
   */
  public void trigger(int millis) {
    tick(millis);
    trigger();
  }

  /**
   * @return The time when the runnable is scheduled to go
   */
  public double getScheduledTime() {
    return scheduledTime;
  }
}
