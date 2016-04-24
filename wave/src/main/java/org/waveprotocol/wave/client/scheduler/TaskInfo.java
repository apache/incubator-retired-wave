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

import org.waveprotocol.box.stat.RequestScope;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;

import java.util.Map;

/**
 * Some information about a scheduled task.
 * Gives each a unique id, and also stores the priority.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
@SuppressWarnings("rawtypes")
final class TaskInfo {
  private static int nextId;
  final String id = Integer.toString(++nextId);
  final Priority priority;
  final double startTime;
  final double interval;
  final Schedulable job;
  final Map<Class, RequestScope.Value> scopeValues;
  private double nextExecuteTime;

  public TaskInfo(Priority p, Schedulable job) {
    this(p, 0, 0, job);
  }

  public TaskInfo(Priority p, double startTime, double interval, Schedulable job) {
    priority = p;
    this.startTime = startTime;
    this.interval = interval;
    this.nextExecuteTime = startTime;
    this.job = job;
    this.scopeValues = Timing.cloneScopeValues();
  }

  @Override
  public String toString() {
    return "TaskInfo { id: " + id + "; priority: " + priority + "; startTime: " +
      startTime + "; interval: " + interval + "; nextExecuteTime: " + nextExecuteTime +
      " }";
  }

  public double getNextExecuteTime() {
    return nextExecuteTime;
  }

  /**
   * Updates this task's next execution time, and returns true if it
   *   should be delayed again, false otherwise.
   *
   * @param now the current time
   * @return true if this task should be delayed.
   */
  public boolean calculateNextExecuteTime(double now) {
    // NOTE: >, not >=
    // A delayed process with an interval of zero is just a regular process
    // with a delayed start time, so there is no need to delay again. Then
    // it will no longer be special, just a regular process in the regular queue.
    if (interval > 0) {
      nextExecuteTime = Math.floor((now - startTime) / interval + 1) * interval + startTime;
      return true;
    }
    return false;
  }

  /**
   * Jitter the next execution in an attempt to create a unique next execution time for this task.
   * @return a new next execution time that is very close to the original execution time.
   */
  public double jitterNextExecuteTime() {
    // Find a unique time whose floor is still equal to the
    // given time. The chances that this strategy will result in
    // time growing by an entire millisecond is infinitesimal, and even
    // if it does, it just means that the job will be one millisecond late,
    // which is still much smaller than the granularity of setTimeout.
    nextExecuteTime += Math.random() * 0.1;
    return nextExecuteTime;
  }
}
