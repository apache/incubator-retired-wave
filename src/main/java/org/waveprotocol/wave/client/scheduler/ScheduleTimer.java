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

import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;

/**
 * A drop in replacement for the timer object that is backed by the scheduler.
 *
 */
public abstract class ScheduleTimer implements Scheduler.IncrementalTask {
  /** Backing back timerService allows for easier unit testing */
  private final TimerService timerService;

  private boolean isRepeating = false;
  // This flag is needed because we use an Incremental task in timer.
  // An incremental task will get cancel if it return false, even if it has just be
  // rescheduled again.  We need this flag so that we can return true to the scheduler
  // to stop the task from being cancelled.
  private boolean scheduledAgain = false;

  @Override
  public boolean execute() {
    scheduledAgain = false;
    run();
    return isRepeating || scheduledAgain;
  }

  public ScheduleTimer(TimerService timerService) {
    this.timerService = timerService;
  }

  public ScheduleTimer(Priority priority) {
    this(new SchedulerTimerService(SchedulerInstance.get(), priority));
  }

  public ScheduleTimer() {
    this(Priority.LOW);
  }

  /**
   * Cancels this timer.
   */
  public void cancel() {
    timerService.cancel(this);
    isRepeating = false;
  }

  /**
   * This method will be called when a timer fires. Override it to implement the
   * timer's logic.
   */
  public abstract void run();

  /**
   * Schedules a timer to elapse in the future.
   *
   * @param delayMillis how long to wait before the timer elapses, in
   *          milliseconds
   */
  public void schedule(int delayMillis) {
    if (delayMillis <= 0) {
      throw new IllegalArgumentException("must be positive");
    }
    cancel();
    isRepeating = false;
    scheduledAgain = true;
    timerService.scheduleDelayed(this, delayMillis);
  }

  /**
   * Schedules a timer that elapses repeatedly.
   *
   * @param periodMillis  minmium number of milliseconds to wait between the
   *                      starts of repeated executions
   */
  public void scheduleRepeating(int periodMillis) {
    scheduleRepeating(periodMillis, periodMillis);
  }

  /**
   * Schedules a timer that elapses repeatedly.
   *
   * @param delayMillis   minimum number of milliseconds
   * @param periodMillis  minmium number of milliseconds to wait between the
   *                      starts of repeated executions
   */
  public void scheduleRepeating(int delayMillis, int periodMillis) {
    if (periodMillis <= 0) {
      throw new IllegalArgumentException("must be positive");
    }
    cancel();
    isRepeating = true;
    scheduledAgain = false;
    timerService.scheduleRepeating(this, delayMillis, periodMillis);
  }

  /** @return true if this timer currently scheduled */
  public boolean isScheduled() {
    return timerService.isScheduled(this);
  }
}
