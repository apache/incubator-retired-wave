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

import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.NumberMap;
import org.waveprotocol.wave.model.util.NumberPriorityQueue;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Data structure used by BrowserBackedScheduler to store information needed to
 * keep track of delayed jobs.
 *
 * Creates only a single object per delayed job. This could be further optimised
 * at the cost of some code cleanliness.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DelayedJobRegistry {

  /**
   * Array of unique times for scheduled jobs
   */
  private final NumberPriorityQueue jobTimes = CollectionUtils.createPriorityQueue();

  /**
   * Map of times to job ids
   */
  private final NumberMap<String> delayedJobIds = CollectionUtils.createNumberMap();

  /**
   * Map of job ids to info
   */
  private final StringMap<TaskInfo> delayedJobs = CollectionUtils.<TaskInfo>createStringMap();

  /**
   * Constructor
   */
  public DelayedJobRegistry() {
  }

  /**
   * Schedule a delayed, optionally repeating job
   *
   * @param info
   */
  public void addDelayedJob(TaskInfo info) {
    delayedJobs.put(info.id, info);
    addDelayedTime(info);
  }

  /**
   * Remove a job from the data structure
   * @param id
   */
  public void removeDelayedJob(String id) {
    if (delayedJobs.containsKey(id)) {
      delayedJobIds.remove(delayedJobs.get(id).getNextExecuteTime());
      delayedJobs.remove(id);
    }
  }

  /**
   * Will get the next due delayed job with respect to the provided "now" time.
   * Note that if many jobs are scheduled for that moment, they will have slightly
   * larger values (usually in the order of less than 0.1), but this is accounted
   * for and they will be returned.
   *
   * @param now
   */
  public Schedulable getDueDelayedJob(double now) {
    while (jobTimes.size() > 0 && jobTimes.peek() <= now + 0.99) {
      double time = this.jobTimes.poll();

      if (!delayedJobIds.containsKey(time)) {
        // job was probably removed
        continue;
      }
      String id = delayedJobIds.get(time);

      TaskInfo info = delayedJobs.get(id);
      Schedulable job = info.job;

      this.removeDelayedJob(id);
      return job;
    }

    // No due job
    return null;
  }

  /**
   * @param id
   * @return True if the job with the given id is scheduled as a delayed job
   */
  public boolean has(String id) {
    return delayedJobs.containsKey(id);
  }

  /**
   * @return The next time that a delayed job is due to run, or -1 for no jobs
   */
  public double getNextDueDelayedJobTime() {
    return jobTimes.size() > 0 ? jobTimes.peek() : -1;
  }

  /**
   * Adds a mapping from a time to a job to run at that time.
   *
   * @param info
   */
  private void addDelayedTime(TaskInfo info) {
    // Find a unique time whose floor is still equal to the
    // given time. The chances that this strategy will result in
    // time growing by an entire millisecond is infinitesimal, and even
    // if it does, it just means that the job will be one millisecond late,
    // which is still much smaller than the granularity of setTimeout.
    while (delayedJobIds.containsKey(info.getNextExecuteTime())) {
      info.jitterNextExecuteTime();
    }
    jobTimes.offer(info.getNextExecuteTime());
    delayedJobIds.put(info.getNextExecuteTime(), info.id);
  }

  /**
   * Used for testing
   */
  boolean debugIsClear() {
    // Not checking jobTimes being empty because it is lazily cleaned up.
    return delayedJobIds.isEmpty() && delayedJobs.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "DJR[jobTimes:" + jobTimes
        + ",delayedJobIds:" + delayedJobIds
        + ",delayedJobs:" + delayedJobs + "]";
  }
}
