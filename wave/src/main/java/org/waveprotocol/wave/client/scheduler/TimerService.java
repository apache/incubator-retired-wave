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
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;


/**
 * An interface for setting up timers that makes testing easier. Production
 * implementation uses {@link org.waveprotocol.wave.client.scheduler.Scheduler}
 * package. This interface obeys the same contract as does the Scheduler
 * interface.
 *
 */
public interface TimerService {
  /**
   * Schedule a task to run once.
   *
   * @param task Task to schedule.
   */
  void schedule(Task task);

  /**
   * Schedule a process to run continuously over a number of separate time
   * slices until completion.
   *
   * @param process Process to schedule
   */
  void schedule(IncrementalTask process);

  /**
   * Same as {@link #schedule(Task)}, but begin after a minimumTime from now
   *
   * @param task Task to schedule
   * @param minimumTime Must be at least zero
   */
  void scheduleDelayed(Task task, int minimumTime);

  /**
   * Same as {@link #schedule(IncrementalTask)}, but begin after a minimumTime
   * from now
   *
   * @param process Process to schedule
   * @param minimumTime Must be at least zero
   */
  void scheduleDelayed(IncrementalTask process, int minimumTime);

  /**
   * Schedule a process to run each unit of work once per given time interval,
   * starting at least from the given minimum time from now, until completion
   *
   * @param process Process to schedule
   * @param minimumTime Must be at least zero
   * @param interval Must be at least zero
   */
  void scheduleRepeating(IncrementalTask process, int minimumTime, int interval);

  /**
   * Prevent the given job from running. Calling this on a job that is not
   * scheduled is a no-op.
   *
   * @param job
   */
  void cancel(Schedulable job);

  /**
   * @return whether the job was previously scheduled.
   */
  boolean isScheduled(Schedulable job);

  /**
   * @return The number of millisecond elapsed since this service started.
   */
  int elapsedMillis();

  /**
   * @return The current clock time in milliseconds since the UNIX epoch.
   * Double is preferred to long due to inefficient long implementation in GWT.
   */
  double currentTimeMillis();
}
