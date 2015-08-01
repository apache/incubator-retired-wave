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
import com.google.gwt.user.client.IncrementalCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;

/**
 * Executes sub-tasks of an {@link IncrementalCommand} in time-slices.
 * In each time slice, the repeatable-command is executed until either the
 * slice expires, or the command reports that it has finished.  If the command
 * has not finished, it is resumed in a subsequent time slice.
 *
 * @deprecated Use Scheduler or TimerService instead
 */
@Deprecated
public class TimeSlicedCommandRepeater implements Scheduler.IncrementalTask {
  /** Default size of a time slice in milliseconds. */
  private static final int DEFAULT_TIME_SLICE_MS = 100;

  /** Default minimum separation between time slices in milliseconds. */
  // In practice on most/all browsers, a setTimeout delay or a setInterval interval has an
  // effective lower bound of about 10ms.  Certainly nowhere near the granularity of 0-2 ms.
  // This constant is therefore defaulted at this lower practical limit.  Setting it any lower
  // would give false impressions that the repeater would run its task roughly
  // 1 - (SEPARATION / SLICE) % of the time.
  private static final int SLICE_SEPARATION_MS = 10;

  /** Command run by this repeater. */
  private final IncrementalCommand command;

  /** Number of milliseconds in a time slice. */
  private final int sliceSize;

  /** Service that performs scheduling. */
  private final TimerService timer;

  /**
   * Time at which current slice must end, or 0 if not in slice.
   *
   * TODO(user): use Duration / doubles.
   * HOWEVER:  It appears that GWT's use of doubles as time holders is not
   *   compatible with double/int conversion (maybe not even double
   *   instantiation).  (see Duration.uncheckedConversion()).
   *   This makes the benefit of using doubles questionable, given the
   *   minefield for bugs this introduces, especially since Duration does not
   *   support creating Durations that represent non-now times (i.e., the end
   *   of the time slice).  We will probably have to patch Duration, or just
   *   write a better version of it.
   */
  private long timeSliceEnd;

  /** true if task is currently being run by repeating timer. */
  private boolean isScheduled;

  /**
   * Creates a TimeSlicedCommandRepeater.
   *
   * @param timer      scheduler
   * @param sliceSize  slice size in milliseconds
   * @param command    command to repeat
   */
  TimeSlicedCommandRepeater(TimerService timer, int sliceSize, IncrementalCommand command) {
    this.timer = timer;
    this.sliceSize = sliceSize;
    this.command = command;
  }

  /**
   * Creates a TimeSlicedCommandRepeater.
   *
   * @param command  command to repeat
   */
  public TimeSlicedCommandRepeater(TimerService timer, IncrementalCommand command) {
    this(timer, DEFAULT_TIME_SLICE_MS, command);
  }

  /**
   * Creates a TimeSlicedCommandRepeater.
   *
   * @param command  command to repeat
   */
  public TimeSlicedCommandRepeater(IncrementalCommand command, Priority p) {
    this(new SchedulerTimerService(SchedulerInstance.get(), p), command);
  }

  /**
   * @return true if and only if the repeating timer is scheduled
   */
  private boolean isScheduled() {
    return isScheduled;
  }

  /**
   * @return true if and only if a slice is currently
   */
  private boolean isRunningSlice() {
    return timeSliceEnd != 0L;
  }

  /**
   * Starts this command repeater in a deferred command.
   * If this repeater is already scheduled, or is currently executing a slice,
   * this method has no effect.
   */
  public void start() {
    if (!isScheduled() && !isRunningSlice()) {
      timer.scheduleRepeating(this, 0, SLICE_SEPARATION_MS);
      isScheduled = true;
    }
  }

  /**
   * Stops this repeater.  Also prevents the incremental-command from running
   * again (unless re-started of course).
   */
  public void stop() {
    if (isScheduled) {
      timer.cancel(this);
      isScheduled = false;
    }
  }

  /**
   * @return true if and only if there is time remaining in the current slice.
   */
  private boolean hasTimeLeft() {
    return Duration.currentTimeMillis() < timeSliceEnd;
  }

  /**
   * Runs a single slice.  The repeatable-command is executed until either the
   * end of the slice period, or the command reports that it has completed.
   */
  public boolean execute() {
    timeSliceEnd = System.currentTimeMillis() + sliceSize;
    try {
      boolean hasWorkToDo;
      do {
        hasWorkToDo = command.execute();
      } while (hasWorkToDo && hasTimeLeft() && isScheduled());

      if (!hasWorkToDo) {
        // terminate
        stop();
        return false;
      }
    } finally {
      timeSliceEnd = 0;
    }
    return true;  // Run until canceled
  }
}
