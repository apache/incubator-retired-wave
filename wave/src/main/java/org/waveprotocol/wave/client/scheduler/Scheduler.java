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

/**
 * Unified interface for scheduling asynchronous work.
 *
 * This interface provides a relatively compact set of methods and
 * sub-interfaces for dealing with the varying task scheduling concerns of the
 * single-threaded web client. The central idea is that scheduling the same job
 * more than once (where "same" is defined by object identity) overrides the
 * previous scheduling of that same job.
 *
 * Contract:
 * <ul>
 * <li>The exact order of execution of jobs with equal priorities and delays is
 * undefined</li>
 * <li>The semantics of scheduling a job with any of the schedule variant
 * methods below are identical to doing that but also calling cancel on it
 * first. For example, this means that the delay timer is reset, and the
 * priority is changed to the most recent value.</li>
 * <li>Scheduling a job is the same as scheduling a delayed job with a delay of
 * zero</li>
 * <li>Scheduling a repeating process with a positive delay but zero interval
 * is the same as scheduling a delayed process</li>
 * <li>Scheduling an incremental task is the same as scheduling a repeating
 * incremental task with a delay and interval both of zero</li>
 * <li>If the interval of a repeating job is too small and enough happens
 * before it is next up for being run so that it "deserves" to be run more than
 * once, the additional runs are dropped. They are not queued up or "owed" to it
 * in any way.</li>
 * </ul>
 *
 * NOTE(danilatos): There is currently no mechanism for jobs to be notified of
 * cancellation. The reason is that code written to depend on this sort of
 * functionality can suffer from difficult to debug race conditions. Ideally
 * code should be structured differently, so that it does not need this
 * facility. If it is really needed, it can also be implemented on top of this
 * scheduler fairly simply. We might revisit this decision if it later turns out
 * to be a much needed feature, in which case there are some easy changes to be
 * made here to allow it.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Scheduler {

  /**
   * This is mainly used to listen to tasks that takes too long to run.
   *
   * @author zdwang@google.com (David Wang)
   */
  public interface Listener {
    /**
     * The given job is taking too long.
     */
    void onJobExecuted(Schedulable job, int timeSpent);
  }

  /**
   * Tag interface for things the scheduler accepts.
   */
  public interface Schedulable {
  }

  /**
   * A unit of work to be run once.
   *
   * NOTE(danilatos): Same as GWT's Command
   *
   * The reason for the separate interface is to clarify its coupling to the
   * Scheduler interface, and the subtly different semantics involving
   * scheduling an already scheduled task. An additional reason is to provide
   * the common super interface for type-tagging reasons.
   */
  public interface Task extends Schedulable {

    /**
     * Do the entirety of a small amount of work
     */
    void execute();
  }

  /**
   * A large task to be run incrementally as several units
   *
   * NOTE(danilatos): Same as GWT's IncrementalCommand. Often referred to as
   * "Process" for brevity.
   *
   * The reason for the separate interface is the same as for Task
   */
  public interface IncrementalTask extends Schedulable {

    /**
     * Do a small unit of a large amount of work
     *
     * @return true if there is additional work, false if the task has completed
     */
    boolean execute();
  }

  /**
   * Priority levels at which tasks execute. These are not distinct merely by
   * their ordering, but each also has some distinct semantics that are
   * described below.
   */
  public enum Priority {
    /**
     * Jobs that need to not be delayed any more than the single first timeout.
     * This means that critical tasks do not have the usual time-slice
     * semantics, and so there is not much point in scheduling an incremental
     * task at this priority.
     *
     * Almost nothing should be critical, because this risks slow script alerts.
     * Only tasks that depend on having exactly timeout delay for correctness
     * should be scheduled at this priority, and they should be small.
     */
    CRITICAL,

    /**
     * High priority jobs whose correctness doesn't depend on eschewing the
     * possibility of extra delays.
     */
    HIGH,

    /**
     * Between HIGH and LOW.
     */
    MEDIUM,

    /**
     * Jobs to run at the lowest priority. Anything that should not hold up high
     * priority jobs. These jobs may also be additionally throttled when there
     * is user activity.
     */
    LOW,

    /**
     * Do not use.
     *
     * NOTE(danilatos): This is a quick hack to allow suppression of individual
     * tasks for debugging purposes. It is not perfect and when someone has the
     * time they can do this "properly" without using this enum.
     *
     * Individual tasks may be disabled using the schedule knobs, and what this
     * actually does is cause them to always be scheduled at this super low
     * priority, which is also disabled by default when the knobs are used. That
     * is all - enabling this priority will flush the jobs, and it should then
     * be immediately disabled again to avoid strange behaviour.
     */
    INTERNAL_SUPPRESS;
  }

  /**
   * Tell the scheduler that the user is active. This may cause it to throttle
   * low priority jobs for a short period. In IE, having excessive background
   * work can cause undesirable effects such as slow rendering and dropped keys
   * when typing. It might be that this method is not needed by non-IE
   * implementations, but should still be called.
   */
  public void noteUserActivity();

  /**
   * Schedule a task to run once at the given priority.
   *
   * @param task
   */
  void schedule(Priority priority, Task task);

  /**
   * Schedule a process to run continuously at the given priority over a number
   * over separate time slices until completion.
   *
   * @param priority
   * @param process
   */
  void schedule(Priority priority, IncrementalTask process);

  /**
   * Same as {@link #schedule(Priority, Task)}, but begin after a minimumTime
   * from now
   *
   * @param priority
   * @param task
   * @param minimumTime
   *          Must be at least zero
   */
  void scheduleDelayed(Priority priority, Task task, int minimumTime);

  /**
   * Same as {@link #schedule(Priority, IncrementalTask)}, but begin after a
   * minimumTime from now
   *
   * @param priority
   * @param process
   * @param minimumTime
   *          Must be at least zero
   */
  void scheduleDelayed(Priority priority, IncrementalTask process,
      int minimumTime);

  /**
   * Schedule a process to run each unit of work once per given time interval,
   * starting at least from the given minimum time from now, until completion
   *
   * @param priority
   * @param process
   * @param minimumTime
   *          Must be at least zero
   * @param interval
   *          Must be at least zero
   */
  void scheduleRepeating(Priority priority, IncrementalTask process,
      int minimumTime, int interval);

  /**
   * Prevent the given job from running. Calling this on a job that is not
   * scheduled is a no-op.
   *
   * @param job
   */
  void cancel(Schedulable job);

  /**
   * @param job
   * @return true if the given job is already scheduled
   */
  boolean isScheduled(Schedulable job);

  /**
   * Adds a listener that may be interested in tasks that takes too long.
   * @param listener must not be null.
   */
  void addListener(Listener listener);


  /**
   * Removes a listener.
   * @param listener no effect if it's not a known listener.
   */
  void removeListener(Listener listener);

  /**
   * @return a short description.
   */
  public String debugShortDescription();
}
