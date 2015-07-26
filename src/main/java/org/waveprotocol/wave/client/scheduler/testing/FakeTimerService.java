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

import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * TimerService to be used during testing, can call scheduled services without
 * sleeping.
 *
 */
public class FakeTimerService implements TimerService {
  private int currentTime = 0;
  private int instanceCount = 0;

  /**
   * Maps absolute start time (currentTime + minimumTime) to a Task that should
   * run at or after that time.
   */
  private final PriorityQueue<TimedTask> tasks = new PriorityQueue<TimedTask>();

  private double startTime = System.currentTimeMillis();

  /** A {@link IncrementalTask} along with the time when it should run. */
  private class TimedTask implements Comparable<TimedTask> {

    private final IncrementalTask task;
    private final int time;
    private final int interval;
    private final int instanceNumber; // compareTo() tiebreaker for fair scheduling for interval==0

    /**
     * A task that is to be run at the specified time, and then repeatedly each
     * time the specified interval has passed.
     *
     * @param time earliest time when the task should run
     * @param task The task to run
     * @param interval delay before the next time the task is to be run. Must be
     *        {@literal >= 0}. If {@link IncrementalTask#execute()} returns
     *        false, the task is not rerun again; Clients of this class must make
     *        sure that this eventually happens for tasks with an interval 0, as
     *        otherwise {@link FakeTimerService#tick(int)} will repeat the task
     *        indefinitely and never return.
     * @throws IllegalArgumentException if time or interval < 0
     */
    TimedTask(int time, IncrementalTask task, int interval) {
      this.task = task;
      this.time = time;
      this.interval = interval;
      if (task == null) {
        throw new NullPointerException("null task");
      }
      if (time < 0) {
        throw new IllegalArgumentException("Expected time >= 0, got " + time);
      }
      if (interval < 0) {
        throw new IllegalArgumentException("Expected interval >= 0, got " + interval);
      }
      this.instanceNumber = instanceCount++;
    }

    /** A task that is to be run only once, at or after the specified time. */
    TimedTask(int time, Task task) {
      // Note: Even though we specify an interval of 1 here, the task is never
      // re-run because {@code TaskAsIncrementalTask.execute()} returns false.
      this(time, new TaskAsIncrementalTask(task), 1);
    }

    /** The task to run at or after time {@link #getTime()}. */
    public IncrementalTask getTask() {
      return task;
    }

    /** The earliest time at which {@link #getTask()} should run. */
    public int getTime() {
      return time;
    }

    /**
     * A TimedTask representing the next time the underlying task should be
     * excuted (if {@link IncrementalTask#execute()} returns true).
     */
    TimedTask nextExecution() {
      return new TimedTask(time + interval, task, interval);
    }

    /**
     * {@inheritDoc}
     *
     * This order is inconsistent with equals.
     */
    @Override
    public int compareTo(TimedTask that) {
      int result = this.time - that.time;
      if (result == 0) {
        // This way, when a task with delay 0 is rescheduled, it will be scheduled past all other
        // pending tasks at the same time due to its higher instance number. Thus, when there are
        // several tasks at the same time with delay 0, they will be scheduled fairly.
        result = this.instanceNumber - that.instanceNumber;
      }
      return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TimedTask)) {
        return false;
      } else {
        TimedTask that = (TimedTask) obj;
        return this.time == that.time && this.task.equals(that.task);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return time + 31 * task.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "TimedTask[time=" + time + ", task=" + task + "]";
    }
  }

  /**
   * Adapter that views a {@link Task} as an {@link IncrementalTask} which
   * returns false from {@link #execute} and therefore runs only once.
   */
  private static final class TaskAsIncrementalTask implements IncrementalTask {
    private final Task task;

    /** Views the specified task as an IncrementalTask. */
    public TaskAsIncrementalTask(Task task) {
      this.task = task;
      if (task == null) {
        throw new NullPointerException("null task");
      }
    }

    /** The underlying task. */
    final Task getTask() {
      return task;
    }

    /** {@link Task#execute() Executes} the wrapped Task and returns false. */
    @Override
    public boolean execute() {
      task.execute();
      return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TaskAsIncrementalTask)) {
        return false;
      } else {
        TaskAsIncrementalTask that = (TaskAsIncrementalTask) obj;
        return this.task.equals(that.task);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return task.hashCode() * 31 + 17;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "TaskAsIncrementalTask[" + task + "]";
    }
  }

  @Override
  public void schedule(Task task) {
    cancel(task);
    scheduleDelayed(task, 0);
  }

  @Override
  public void schedule(IncrementalTask process) {
    scheduleRepeating(process, 0, 0);
  }

  @Override
  public void scheduleDelayed(Task task, int minimumTime) {
    cancel(task);
    tasks.add(new TimedTask(currentTime + minimumTime, task));
  }

  @Override
  public void scheduleDelayed(IncrementalTask process, int minimumTime) {
    cancel(process);
    scheduleRepeating(process, minimumTime, 1);
  }

  @Override
  public void scheduleRepeating(IncrementalTask process, int minimumTime, int interval) {
    cancel(process);
    tasks.add(new TimedTask(currentTime + minimumTime, process, interval));
  }

  private boolean findOrCancel(Schedulable job, boolean remove) {
    IncrementalTask incrementalTask;
    if (job instanceof IncrementalTask) {
      incrementalTask = (IncrementalTask) job;
    } else if (job instanceof Task) {
      incrementalTask = new TaskAsIncrementalTask((Task) job);
    } else {
      throw new IllegalArgumentException("cancel: Unknown schedulable type: " + job.getClass());
    }
    Iterator<TimedTask> iterator = tasks.iterator();
    boolean found = false;
    while (iterator.hasNext()) {
      if (iterator.next().getTask().equals(incrementalTask)) {
        if (remove) {
          iterator.remove();
          found = true;
        } else {
         return true;
        }
      }
    }
    return found;
  }

  @Override
  public void cancel(Schedulable job) {
    findOrCancel(job, true);
  }

  @Override
  public boolean isScheduled(Schedulable job) {
    return findOrCancel(job, false);
  }

  @Override
  public int elapsedMillis() {
    return currentTime;
  }

  @Override
  public double currentTimeMillis() {
    return startTime + currentTime;
  }

  /**
   * Sets startTime of this TimerService to given time.
   *
   * @param startTime New value for startTime to take.
   */
  public void setStartTime(double startTime) {
    this.startTime = startTime;
  }

  /**
   * Advances specified millis, running all tasks in between.
   *
   * @param millisToAdvance Number of milliseconds to advance the timer.
   */
  public void tick(int millisToAdvance) {
    currentTime += millisToAdvance;
    while (!tasks.isEmpty() && tasks.peek().getTime() <= currentTime) {
      TimedTask timedTask = tasks.poll();
      IncrementalTask task = timedTask.getTask();
      boolean doReschedule = task.execute();
      if (doReschedule) {
        // Note: If the next execution is at or before currentTime, the task
        // will be re-executed before this tick() call returns.
        tasks.add(timedTask.nextExecution());
      }
    }
  }

  public int countTasksScheduled() {
    return tasks.size();
  }
}
