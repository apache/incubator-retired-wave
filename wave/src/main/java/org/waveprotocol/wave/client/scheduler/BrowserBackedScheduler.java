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

import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.IdentityMap.ProcV;

/**
 * Implementation of a scheduler using two optimised javascript object
 * data structures for the registry of jobs.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class BrowserBackedScheduler implements Scheduler {
  private final SimpleTimer timer;

  private final Runnable runner = new Runnable() {
    public void run() {
      nextSliceRunTime = Double.MAX_VALUE;
      workSlice(timeSliceMillis);
      double next = getNextRunTime();
      if (next == 0) {
        maybeScheduleSlice();
      } else if (next > 0) {
        maybeScheduleSlice(next);
      }
    }
  };

  /** Controller for enabling/disabling priority levels, showing job counts, etc. */
  private final Controller controller;

  /**
   * Map of scheduled tasks to info about them
   */
  // TODO(danilatos): Swap out this implementation with a JSO based map when not in
  // hosted mode, where hashCode() for reference-based equality is a perfect hash
  private final IdentityMap<Schedulable, TaskInfo> taskInfos = CollectionUtils.createIdentityMap();

  /**
   * Simple registry of jobs that are scheduled to run at the next available
   * opportunity.
   * This class maintains the invariant that if a job exists in {@code jobs},
   * then that job also exists in {@code taskInfos}.
   */
  private final JobRegistry jobs;

  /**
   * More complicated registry of delayed & repeating jobs. When something is
   * due to be run, it is taken from here and added to the {@link #jobs}
   * variable for actual execution.
   * This class maintains the invariant that if a job exists in
   * {@code delayedjobs}, then that job also exists in {@code taskInfos}.
   */
  private final DelayedJobRegistry delayedJobs = new DelayedJobRegistry();

  /**
   * How long each work slice should go for
   */
  private int timeSliceMillis = 100;

  /**
   * When the next work slice is scheduled to run.
   * 0 means a slice is already scheduled to run as soon as possible.
   * >0 means a slice is scheduled to run at some epoch time, which is hopefully in the future.
   */
  private double nextSliceRunTime = Double.MAX_VALUE;

  /**
   * The list of listeners that are interested in tasks that takes too long.
   */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.createListSet();

  public BrowserBackedScheduler(SimpleTimer.Factory timerFactory) {
    this(timerFactory, Controller.NOOP);
  }

  /**
   * @param timerFactory
   */
  public BrowserBackedScheduler(SimpleTimer.Factory timerFactory, Controller controller) {
    this.timer = timerFactory.create(runner);
    this.controller = controller;
    this.jobs = new JobRegistry(controller);
  }

  @Override
  public void schedule(Priority priority, Task task) {
    Preconditions.checkArgument(priority != Priority.INTERNAL_SUPPRESS, "Don't use internal level");
    scheduleJob(priority, task);
  }

  @Override
  public void schedule(Priority priority, IncrementalTask process) {
    Preconditions.checkArgument(priority != Priority.INTERNAL_SUPPRESS, "Don't use internal level");
    scheduleJob(priority, process);
  }

  /**
   * Type independent worker for equivalent overloaded methods
   */
  private void scheduleJob(Priority priority, Schedulable job) {
    if (controller.isSuppressed(priority, job) && priority != Priority.INTERNAL_SUPPRESS) {
      scheduleJob(Priority.INTERNAL_SUPPRESS, job);
      return;
    }

    TaskInfo info = taskInfos.get(job);

    // Cancel job if already scheduled
    if (info != null) {

      // Optimisation: nothing's changed, just return.
      if (priority == info.priority) {
        return;
      }

      cancel(job);
    }

    info = createTask(priority, job);
    jobs.add(priority, job);

    maybeScheduleSlice();
  }

  private TaskInfo createTask(Priority priority, Schedulable job) {
    TaskInfo info = new TaskInfo(priority, job);
    taskInfos.put(job, info);
    return info;
  }

  private TaskInfo createDelayedTask(Priority priority, Schedulable job,
      double startTime, double interval) {
    TaskInfo info = new TaskInfo(priority, startTime, interval, job);
    taskInfos.put(job, info);
    return info;
  }

  @Override
  public void scheduleDelayed(Priority priority, Task task, int minimumTime) {
    Preconditions.checkArgument(minimumTime >= 0, "Minimum time must be at least zero");
    Preconditions.checkArgument(priority != Priority.INTERNAL_SUPPRESS, "Don't use internal level");

    if (minimumTime == 0) {
      schedule(priority, task);
    }
    scheduleDelayedJob(priority, task, minimumTime, -1);
  }

  @Override
  public void scheduleDelayed(Priority priority, IncrementalTask process, int minimumTime) {
    scheduleRepeating(priority, process, minimumTime, 0);
  }

  @Override
  public void scheduleRepeating(Priority priority, IncrementalTask process,
      int minimumTime, int interval) {
    Preconditions.checkArgument(minimumTime >= 0, "Minimum time must be at least zero");
    Preconditions.checkArgument(interval >= 0, "Interval must be at least zero");
    Preconditions.checkArgument(priority != Priority.INTERNAL_SUPPRESS, "Don't use internal level");

    if (interval == 0 && minimumTime == 0) {
      schedule(priority, process);
    }
    scheduleDelayedJob(priority, process, minimumTime, interval);
  }

  /**
   * Worker for the delayed & repeating methods
   * @param interval if -1, then not repeating
   */
  private void scheduleDelayedJob(Priority priority, Schedulable job,
      int minimumTime, int interval) {
    if (controller.isSuppressed(priority, job) && priority != Priority.INTERNAL_SUPPRESS) {
      scheduleDelayedJob(Priority.INTERNAL_SUPPRESS, job, minimumTime, interval);
      return;
    }

    TaskInfo info = taskInfos.get(job);
    if (info != null) {
      cancel(job);
    }

    double now = timer.getTime();
    double startTime = now + minimumTime;

    info = createDelayedTask(priority, job, startTime, interval);
    delayedJobs.addDelayedJob(info);

    maybeScheduleSlice(startTime);
  }

  @Override
  public void cancel(Schedulable command) {
    TaskInfo info = taskInfos.removeAndReturn(command);
    if (info != null) {
      jobs.remove(info.priority, command);
      delayedJobs.removeDelayedJob(info.id);

      if (taskInfos.isEmpty()) {
        unscheduleSlice();
      }
    }
  }

  @Override
  public boolean isScheduled(Schedulable job) {
    return taskInfos.get(job) != null;
  }

  private boolean hasJob(Schedulable command) {
    return taskInfos.has(command);
  }

  @Override
  public void noteUserActivity() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("noteUserActivity");
  }

  /**
   * Set the size of a work time slice before work is deferred again
   * @param millis
   */
  public void setTimeSlice(int millis) {
    timeSliceMillis = millis;
  }

  private boolean hasTasks() {
    return !taskInfos.isEmpty();
  }

  /**
   * Do a unit of work from the given priority
   *
   * @param priority
   * @return true if there are more work units left in the scheduler
   */
  // TODO(danilatos): Unit test this method (maybe change to package private)
  private boolean workUnit(Priority priority, int maxMillis) {
    jobs.removeFirst(priority);
    Schedulable job = jobs.getRemovedJob();

    if (job == null) {
      return false;
    }

    double start = timer.getTime();

    TaskInfo taskInfo = taskInfos.get(job);
    Timer profilingTimer = null;
    if (taskInfo != null && Timing.isEnabled()) {
      Timing.enterScope(taskInfo.scopeValues);
      profilingTimer = Timing.start("schedule " + job.getClass().getSimpleName());
    }
    try {
      if (job instanceof IncrementalTask) {
        boolean isFinished = !jobs.getRemovedJobAsProcess().execute();

        if (isFinished) {
          // Remove all trace
          cancel(job);
        } else {
          TaskInfo task = taskInfos.get(job);
          // If the job has more work to do, we add it back into the job queue, unless it has has
          // already been cancelled during execution (which would imply !hasJob)
          if (task != null && hasJob(job)) {
            // if it is a repeating job, add it to a delay before we contiune
            if (task.calculateNextExecuteTime(start)) {
              delayedJobs.addDelayedJob(task);
            } else if (!delayedJobs.has(task.id)) {
              jobs.add(priority, job);
            }
          }
        }
      } else {
        Task task = jobs.getRemovedJobAsTask();
        // Remove all trace.
        cancel(job);
        task.execute();
      }
    } finally {
      if (profilingTimer != null) {
        Timing.stop(profilingTimer);
        Timing.exitScope();
      }
    }

    int timeSpent = (int) ( timer.getTime() - start);

    // This will only be useful when debugging in deobfuscated mode.
    triggerOnJobExecuted(job, timeSpent);

    return hasTasks();
  }

  /**
   * Try to execute all the jobs at the given priority. At least 1 job at the given
   * priority will be executed.
   * @param priority
   * @param maxMillis the max number of millisec we are allowed to execute one task before
   *    reporting it for a task that's too slow.
   * @param endTime if we exceeded this time, then we should stop and return false.
   * @return true if there are more time left that we can use to execute other jobs.
   */
  private boolean workAll(Priority priority, int maxMillis, double endTime) {
    if (controller.isRunnable(priority) && jobs.numJobsAtPriority(priority) != 0) {
      boolean moreWork;
      boolean moreTime;
      do {
        moreWork = workUnit(priority, maxMillis);
        // TODO(user):
        //   Add the following:
        //
        // double duration = finish - start;
        // if (duration > MAX_DURATION_MS && Debug.errorClient().shouldLog()) {
        //   Debug.errorClient().log("HIGH priority task took a whopping: "
        //       + ((int) duration) + "ms to run");
        // }
        //
        // after dependencies are cleaned up such that Debug does not depend on Scheduler, so that
        // Scheduler can depend on Debug without a cycle.
        moreTime = timer.getTime() < endTime;
      } while (moreWork && moreTime);

      return moreTime;
    }
    return true;
  }

  /**
   * Work for the specified period or until there is no more work to do,
   * whichever comes first
   *
   * @param maxMillis
   */
  // TODO(danilatos): Unit test this method (maybe change to package private)
  private void workSlice(int maxMillis) {
    double now = timer.getTime();

    if (controller.isRunnable(Priority.CRITICAL)) {
      // Always do all critical tasks in one go
      while (workUnit(Priority.CRITICAL, maxMillis)) {}
    }

    Schedulable delayedJob;
    while ((delayedJob = delayedJobs.getDueDelayedJob(now)) != null) {
      TaskInfo info = taskInfos.get(delayedJob);
      jobs.add(info.priority, delayedJob);
    }

    //
    // Run HIGH priority tasks to the exclusion of MEDIUM and LOW priority tasks.
    // Also, always run at least one unit of HIGH priority, regardless of how long the previous
    // CRITICAL tasks took.
    //
    double end = now + maxMillis;
    for (Priority p : Priority.values()) {
      if (!workAll(p, maxMillis, end)) {
        return;
      }
    }
  }

  /**
   * @return Next time that a work slice should be due (not necessarily currently scheduled)
   *   -1 means nothing to run, 0 means run as soon as possible, and >0 means don't run until
   *   that many ms have elapsed.
   */
  private double getNextRunTime() {
    // If there are normal jobs waitin, run as soon as possible.
    // Otherwise, run when the next delayed job wants to run.
    if (!jobs.isEmpty()) {
      return 0;
    } else {
      return delayedJobs.getNextDueDelayedJobTime();
    }
  }

  /**
   * Ensure a work slice is scheduled to run at the next available opportunity
   */
  private void maybeScheduleSlice() {
    if (nextSliceRunTime > 0) {
      timer.schedule();
      nextSliceRunTime = 0;
    }
  }

  /**
   * Ensure a work slice is scheduled to run no later than the given time
   * @param when System time in millis when a slice should run
   */
  private void maybeScheduleSlice(double when) {
    if (nextSliceRunTime > when) {
      timer.schedule(when);
      nextSliceRunTime = when;
    }
  }

  /**
   * Don't run the next slice
   */
  private void unscheduleSlice() {
    nextSliceRunTime = Double.MAX_VALUE;
    timer.cancel();
  }

  /** Used for testing */
  boolean debugIsClear() {
    return taskInfos.isEmpty() && jobs.debugIsClear() && delayedJobs.debugIsClear();
  }

  @Override
  public String debugShortDescription() {
    return "Scheduler[num ids:" + taskInfos.countEntries() + ", jobs:" +
        jobs.debugShortDescription() + ", delayed: " + delayedJobs.toString() + "]";
  }

  @Override
  public String toString() {
    return "Scheduler[ids:" + tasks() + ", jobs:" + jobs.toString()
        + ", delayed: " + delayedJobs.toString() + "]";
  }

  private String tasks() {
    final StringBuilder b = new StringBuilder();
    taskInfos.each(new ProcV<Schedulable, TaskInfo>() {
      @Override
      public void apply(Schedulable key, TaskInfo item) {
        b.append("{ task: " + item);
        b.append("; ");
        b.append("job: " + key + " } ");
      }
    });
    return b.toString();
  }

  /**
   * Gets a UI component for controlling this scheduler's priority levels.
   *
   * @return the knobs control, or {@code null} if there is no knobs panel.
   */
  public Widget getController() {
    return controller.asWidget();
  }

  private void triggerOnJobExecuted(Schedulable job, int timeSpent) {
    for (Listener l : listeners) {
      l.onJobExecuted(job, timeSpent);
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
