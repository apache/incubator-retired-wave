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


package org.waveprotocol.wave.client;

import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.Cancellable;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.CollectiveScheduler;
import org.waveprotocol.wave.model.util.Scheduler.Command;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * A fuzzy scheduler that attempts to executed as many of it's schedule task as
 * possible. This scheduler is being given a list of tasks which should be
 * executed between [minTime, targetTime], the aim of this task is to process
 * all of them at the same time.
 *
 * Conceptually problem this class solves is: Given a list of ranges R = {[min, max]},
 * find a set of points P such that
 *     for_all([min, max] in R, exist(p in P, min <= p && p <= max)) and |P| is minimum.
 * Or informally, we want the minimum number of points such that all ranges in R are covered.
 *
 * However, since we only need to schedule 1 task at a time, we only need the
 * first point in P.
 *
 * The algorithm to solve this problem is straight forward, if we sort all the
 * ranges by their max value, then the smallest max value is guaranteed to be in
 * a optimal set of points P.
 *
 */
public final class OptimalGroupingScheduler implements CollectiveScheduler {
  /**
   * A task that should be executed in at a time minTime <= t <= maxTime
   *
   */
  private class FuzzyTask implements Cancellable, Comparable<FuzzyTask> {
    /**
     * The command to be executed
     */
    private Command command;

    /**
     * The minimum time we are willing to execute this task
     */
    final int minTime;

    /**
     * The maximum time we are willing to execute this task
     */
    final int maxTime;

    /**
     * Each task is given a unique entry number, this is used as the final tie breaker
     * when comparing tasks.
     */
    final int entryNum;

    FuzzyTask(Command command, int minTime, int maxTime) {
      this.command = command;
      this.minTime = minTime;
      this.maxTime = maxTime;
      entryNum = numFuzzyTaskCreated++;
    }

    @Override
    public void cancel() {
      if (command != null) {
        command = null;

        allTasksOrderedByMinTime.remove(this);
        scheduleTaskRunner();
      }
    }

    @Override
    public int compareTo(FuzzyTask o) {
      // order by min time, tight break by min time
      if (minTime != o.minTime) {
        return minTime - o.minTime;
      }
      if (maxTime != o.maxTime) {
        return maxTime - o.maxTime;
      }
      // all else failed, use entry number to tie break
      return entryNum - o.entryNum;
    }

    /**
     * Execute the current task.
     *
     * @return true if the task has been executed, false otherwise.
     */
    public boolean execute() {
      assert command != null;
      command.execute();
      command = null;
      return true;
    }

    /**
     * @return is this task needed to be executed
     */
    public boolean needToBeExecuted() {
      return command != null;
    }

    @Override
    public String toString() {
      return Integer.toString(entryNum);
    }
  }

  /**
   * A comparator that order FuzzyTask by their max time.
   */
  private static Comparator<FuzzyTask> MAX_TIME_COMPARATOR = new Comparator<FuzzyTask>() {
    @Override
    public int compare(FuzzyTask o1, FuzzyTask o2) {
      // order by max time, tie break by min time
      if (o1.maxTime != o2.maxTime) {
        return o1.maxTime - o2.maxTime;
      }
      if (o1.minTime != o2.minTime) {
        return o1.minTime - o2.minTime;
      }
      // all else failed, use entry number to tie break
      return o1.entryNum - o2.entryNum;
    }
  };

  /**
   * The number of fuzzy task created.
   */
  private static int numFuzzyTaskCreated = 0;

  /**
   * A priority queue store all the tasks order max time. This queue is a
   * superset of allTasksOrderedByMinTime. This list contains some
   * cancelled/executed tasks because we want to avoid random access remove
   * because it is an O(N) operation to remove anything that not at the head.
   * Instead, we keep the task in the queue until it reaches the head, then we
   * remove them.
   */
  private final PriorityQueue<FuzzyTask> listByMaxTimes =
      new PriorityQueue<FuzzyTask>(11, MAX_TIME_COMPARATOR);

  /**
   * A priority queue of all the task ordered by start time.
   */
  private final PriorityQueue<FuzzyTask> allTasksOrderedByMinTime =
      new PriorityQueue<FuzzyTask>();

  private final TimerService timerService;
  private final Task commandRunner = new Task() {
    @Override
    public void execute() {
      if (allTasksOrderedByMinTime.isEmpty()) {
        return;
      }

      isExecuting = true;
      // set next scheduled time to 0 to disable scheduling
      nextScheduledTime = 0;
      int now = timerService.elapsedMillis();

      while (!allTasksOrderedByMinTime.isEmpty() &&
             allTasksOrderedByMinTime.peek().minTime <= now) {
        FuzzyTask task = allTasksOrderedByMinTime.poll();
        task.execute();
      }
      isExecuting = false;
      scheduleTaskRunner();
    }
  };

  /**
   * A flag to indicate if we are in the middle of executing scheduled task.
   */
  private boolean isExecuting = false;

  /**
   * The time we are scheduled to run again.
   */
  private int nextScheduledTime = Integer.MAX_VALUE;

  public OptimalGroupingScheduler(TimerService timerService) {
    this.timerService = timerService;
  }

  @Override
  public Cancellable schedule(final Command command, int minAllowedTime, int targetTimeMs) {
    targetTimeMs = Math.max(1, targetTimeMs);
    minAllowedTime = Math.min(targetTimeMs, minAllowedTime);
    int now = timerService.elapsedMillis();
    int minTime = minAllowedTime + now;
    int maxTime = targetTimeMs + now;

    FuzzyTask task = new FuzzyTask(command, minTime, maxTime);
    listByMaxTimes.offer(task);
    allTasksOrderedByMinTime.offer(task);
    scheduleTaskRunner();

    return task;
  }

  private void scheduleTaskRunner() {
    cleanUpOrderByMaxTime();
    if (!isExecuting && !listByMaxTimes.isEmpty() &&
        nextScheduledTime != listByMaxTimes.peek().maxTime) {
      nextScheduledTime = listByMaxTimes.peek().maxTime;
      int delayTime = Math.max(1,
          Math.round(nextScheduledTime - timerService.elapsedMillis()));
      timerService.scheduleDelayed(commandRunner, delayTime);
    }
  }

  private void cleanUpOrderByMaxTime() {
    // remove all executed/cancelled entry from the head of the queue
    while (!listByMaxTimes.isEmpty() &&
        !listByMaxTimes.peek().needToBeExecuted()) {
      listByMaxTimes.poll();
    }
    assert allTasksOrderedByMinTime.size() <= listByMaxTimes.size();
  }
}
