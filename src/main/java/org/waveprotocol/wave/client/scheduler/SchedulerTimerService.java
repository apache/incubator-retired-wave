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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;

/**
 * Implements a TimerService using the Wave client's Scheduler package.
 *
 */
public class SchedulerTimerService implements TimerService {
  /**
   * Scheduler to use for scheduling tasks.
   */
  private final Scheduler scheduler;

  /**
   * Priority jobs will be run at.
   */
  private final Priority priority;

  /**
   * The start time of when SchedulerTimerService is created.
   */
  private final double start = currentTimeMillis();

  /**
   * Creates a new TimerService that runs all tasks at LOW priority by default.
   *
   * @param scheduler Scheduler to use.
   */
  public SchedulerTimerService(Scheduler scheduler) {
    this(scheduler, Priority.LOW);
  }

  /**
   * Creates a new TimerService that runs tasks at the given priority.
   *
   * @param scheduler Scheduler to use.
   * @param priority Priority to run scheduled jobs at.
   */
  public SchedulerTimerService(Scheduler scheduler, Priority priority) {
    this.scheduler = scheduler;
    this.priority = priority;
  }

  @Override
  public void schedule(Task task) {
    scheduler.schedule(priority, task);
  }

  @Override
  public void schedule(IncrementalTask process) {
    scheduler.schedule(priority, process);
  }

  @Override
  public void scheduleDelayed(Task task, int minimumTime) {
    scheduler.scheduleDelayed(priority, task, minimumTime);
  }

  @Override
  public void scheduleDelayed(IncrementalTask process, int minimumTime) {
    scheduler.scheduleDelayed(priority, process, minimumTime);
  }

  @Override
  public void scheduleRepeating(IncrementalTask process, int minimumTime, int interval) {
    scheduler.scheduleRepeating(priority, process, minimumTime, interval);
  }

  @Override
  public void cancel(Schedulable job) {
    scheduler.cancel(job);
  }

  @Override
  public boolean isScheduled(Schedulable job) {
    return scheduler.isScheduled(job);
  }

  @Override
  public int elapsedMillis() {
    return (int) (currentTimeMillis() - start);
  }

  @Override
  public double currentTimeMillis() {
    // Replace this with just Duration.currentTimeMillis() when it is itself
    // implemented with a GWT.isClient() check.
    return GWT.isClient()
        ? Duration.currentTimeMillis()
        : System.currentTimeMillis();
  }
}
