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
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Optimised data structure used by BrowserBackedScheduler to store currently running jobs.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class JobRegistry {

  /**
   * Jobs at each priority.
   *
   * Array of priority ordinals to ordered maps (which JSO maps inherently are)
   * Each map is an ordered map of id to job.
   */
  private final IntMap<Queue<Schedulable>> priorities = CollectionUtils.createIntMap();

  /** Controller that collects job counts. */
  private final Controller jobCounter;

  /** Used as part of a pair in a return value, to avoid creating an object */
  private Schedulable returnJob;

  /** Number of jobs in the registry, to provide fast {@link #isEmpty()}. */
  private int jobCount;

  /**
   * Creates a job registry.
   *
   * @param jobCounter  counter of jobs
   */
  public JobRegistry(Controller jobCounter) {
    this.jobCounter = jobCounter;

    for (Priority p : Priority.values()) {
      // TODO(zdwang): Use a fast LinkedList that doens't mak this class untestable.
      priorities.put(p.ordinal(), new LinkedList<Schedulable>());
    }
  }

  /**
   * Add a job at the given priority
   *
   * @param priority
   * @param job
   */
  public void add(Priority priority, Schedulable job) {
    assert job != null : "tried to add null job";
    Queue<Schedulable> queue = priorities.get(priority.ordinal());
    if (queue.remove(job)) {
      // Do nothing else
      queue.add(job);
      return;
    } else {
      queue.add(job);
      jobCount++;
      jobCounter.jobAdded(priority, job);
    }
  }

  /**
   * @param priority
   * @return the number of jobs at the given priority.
   */
  public int numJobsAtPriority(Priority priority) {
    return priorities.get(priority.ordinal()).size();
  }

  /**
   * Remove the first job for the given priority.
   * The job and its id can be retrieved by the various getRemoved* methods
   * (This is to avoid creating a pair object as a return value).
   *
   * @param priority
   */
  public void removeFirst(Priority priority) {
    Queue<Schedulable> queue = priorities.get(priority.ordinal());
    if (queue.isEmpty()) {
      returnJob = null;
    } else {
      returnJob = queue.poll();
      jobCount--;
      jobCounter.jobRemoved(priority, returnJob);
    }
  }

  /**
   * Obliterate the job with the given priority and id.
   * Does NOT store information to be retrieved like {@link #removeFirst(Priority)} does.
   *
   * @param priority
   * @param id
   */
  public void remove(Priority priority, Schedulable job) {
    if (priorities.get(priority.ordinal()).remove(job)) {
      jobCount--;
      jobCounter.jobRemoved(priority, job);
    }
  }

  /**
   * @return Job removed by {@link #removeFirst(Priority)}
   */
  public Schedulable getRemovedJob() {
    return returnJob;
  }

  /**
   * @return Job removed by {@link #removeFirst(Priority)}, Cast to IncrementalTask.
   */
  public IncrementalTask getRemovedJobAsProcess() {
    return (IncrementalTask) returnJob;
  }

  /**
   * @return Job removed by {@link #removeFirst(Priority)}, Cast to Task.
   */
  public Task getRemovedJobAsTask() {
    return (Task) returnJob;
  }

  /**
   * Used for testing
   */
  boolean debugIsClear() {
    for (Priority p : Priority.values()) {
      if (!priorities.get(p.ordinal()).isEmpty()) {
        assert jobCount > 0 : "Count 0 when: " + toString();

        return false;
      }
    }

    assert jobCount == 0 : "Count non-zero when: " + toString();
    return true;
  }

  /**
   * Tests if this registry has any jobs in it.
   *
   * @return true if this registry has no jobs, false otherwise.
   */
  boolean isEmpty() {
    return jobCount == 0;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    // NOTE(user): this causes "too much recursion" errors in Firefox?
    // return priorities.toSource();
    // ... so we do explicit building instead :(
    StringBuilder result = new StringBuilder();
    for (Priority p : Priority.values()) {
      result.append(" { priority: " + p + "; ");
      result.append(" jobs: " + priorities.get(p.ordinal()) + "; } ");
    }
    return result.toString();
  }

  /**
   * @return short description
   */
  public String debugShortDescription() {
    // NOTE(user): this causes "too much recursion" errors in Firefox?
    // return priorities.toSource();
    // ... so we do explicit building instead :(
    StringBuilder result = new StringBuilder();
    for (Priority p : Priority.values()) {
      result.append(" { priority: " + p + "; ");
      result.append(" jobs count: " + priorities.get(p.ordinal()).size() + "; } ");
    }
    return result.toString();
  }
}
