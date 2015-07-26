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

import com.google.gwt.user.client.Command;

import java.util.ArrayList;

/**
 * This is execute deferred commands in time slices so that the UI is not blocked up.
 *
 * There is something similar in the package private class
 * com.google.gwt.user.client.CommandExecutor, but it's too much trouble to mess around with
 * that gwt internal class to make it public, configurable etc.
 *
 * @author zdwang@google.com (David Wang)
 * @deprecated Use Scheduler or TimerService instead
 */
@Deprecated
public class TimeSlicedCommandQueue implements CommandQueue {
  private static final int DEFAULT_TIME_SLICE_MS = 10;

  // TODO(zdwang): Check if the LinkedList work in GWT.
  private final ArrayList<Command> queue = new ArrayList<Command>();

  /**
   * The time when the time slice started.
   */
  public long timeSliceStart;

  /**
   * The maximum time allowed to execute the commands in a single turn.
   */
  public int timeSliceMs = DEFAULT_TIME_SLICE_MS;

  /**
   * Are we executing the queue.
   */
  private boolean isExecutingQueue;

  private final TimerService timeService;

  private boolean isScheduled;


  /**
   * The timer that processes the queue.
   */
  private final Scheduler.IncrementalTask task = new Scheduler.IncrementalTask() {
    public boolean execute() {
      timeSliceStart = System.currentTimeMillis();
      executeQueue();
      if (queue.size() == 0 && isScheduled) {
        cancel();
      }
      timeSliceStart = 0;
      return true;  // Run until canceled
    }
  };

  /**
   * Force commands to run asynchronously.
   */
  private final boolean forceAsync;

  /**
   * Create a TimeSlicedCommandQueue using the given time service
   * @param service
   */
  public TimeSlicedCommandQueue(TimerService service, boolean forceAsync) {
    timeService = service;
    this.forceAsync = forceAsync;
  }

  /**
   * Create a time sliced command queue using the default time service,
   * and allowing commands to run in the same event loop.
   */
  public TimeSlicedCommandQueue() {
    this(new SchedulerTimerService(SchedulerInstance.get()), false);
  }

  /**
   * Create a times sliced command using the default time service.
   * @param forceAsync
   */
  public TimeSlicedCommandQueue(boolean forceAsync) {
    this(new SchedulerTimerService(SchedulerInstance.get()), forceAsync);
  }

  /**
   * Multiple calls to this method schedules only once.
   */
  private void scheduleRepeating(int periodMillis) {
    if (!isScheduled) {
      timeService.scheduleRepeating(task, 0, periodMillis);
      isScheduled = true;
    }
  }

  /**
   * Cancel the schedule runnable
   */
  private void cancel() {
    if (isScheduled) {
      timeService.cancel(task);
      isScheduled = false;
    }
  }

  private void executeQueue() {
    if (isExecutingQueue) {
      return;
    }

    isExecutingQueue = true;
    while (queue.size() > 0) {
      queue.remove(0).execute();
      long end = System.currentTimeMillis();
      if ((end - timeSliceStart) > timeSliceMs) {
        break;
      }
    }
    isExecutingQueue = false;
  }

  /**
   * Start execution immediately in the current thread. Calling this multiple
   * times in the same thread is the same as calling it once in that thread.
   * Any new commands added via addCommand() afterwards will be ran immediately
   * if we still have time left on the time slice.
   */
  public void start() {
    if (timeSliceStart == 0) {
      timeSliceStart = System.currentTimeMillis();
      executeQueue();
    }
  }

  /**
   * Try to execute immediately if we have time left. If not, create a deferred executor.
   */
  private void defer() {
    // Still has some time left in the current time slice
    if (!forceAsync && (System.currentTimeMillis() - timeSliceStart) < timeSliceMs) {
      executeQueue();
    }

    scheduleRepeating(1);
  }

  /**
   * Add a command for the queue to execute at a free UI time slice.
   * @param c
   */
  public void addCommand(Command c) {
    queue.add(c);
    defer();
  }

  /**
   * Stop the execution.
   */
  public void stop() {
    cancel();
    queue.clear();
  }

  /**
   * @param ms Time slice in millisecond to execute the commands in the queue
   *     before giving the thread back to UI.
   */
  public void setTimeSlice(int ms) {
    timeSliceMs = ms;
  }

  /** @return The size of the queue. */
  public int queueSize() {
    return queue.size();
  }
}
