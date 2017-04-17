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

import junit.framework.TestCase;
import org.mockito.InOrder;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;

import static org.mockito.Mockito.*;

/**
 * The FakeTimerService is a complicated enough fake that it deserves its own
 * tests. (Normally I wouldn't test fakes.)
 *
 */
public class FakeTimerServiceTest extends TestCase {
  private Task oneoff;
  private IncrementalTask repeating;
  private FakeTimerService timer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    oneoff = mock(Task.class, "oneoff");
    repeating = mock(IncrementalTask.class, "repeating");
    when(repeating.execute()).thenReturn(true);
    timer = new FakeTimerService();
  }

  public void testTicks() {
    timer.scheduleDelayed(oneoff, 500);
    timer.scheduleRepeating(repeating, 0, 1000);

    timer.tick(1000);
    timer.tick(500);
    timer.tick(499);
    verify(oneoff, times(1)).execute();
    verify(repeating, times(2)).execute();

    timer.tick(1);
    timer.tick(1);
    timer.tick(1);
    verify(repeating, times(3)).execute();

    timer.tick(999);
    verify(repeating, times(4)).execute();

    timer.tick(3000);
    verify(repeating, times(7)).execute();
  }

  public void testCancel() {
    timer.scheduleDelayed(oneoff, 500);
    timer.cancel(oneoff);
    timer.scheduleRepeating(repeating, 0, 1000);
    timer.cancel(repeating);

    timer.tick(10 * 1000);

    verify(oneoff, never()).execute();
    verify(repeating, never()).execute();
  }

  public void testScheduleWillExecuteImmediatelyOnAnyTick() {
    timer.schedule(oneoff);

    timer.tick(0);
    verify(oneoff, times(1)).execute();
  }

    /** Tests that scheduling a task with negative start time throws an exception. */
  public void testNegativeStartTime() {
    try {
      timer.scheduleDelayed(oneoff, -1);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
    try {
      timer.scheduleRepeating(repeating, -1, 10);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  /** Tests that scheduling a task with negative interval throws an exception. */
  public void testNegativeInterval() {
    try {
      timer.scheduleRepeating(repeating, 500, -1);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  /**
   * Tests that a repeated task with interval 0 is repeatedly executed
   * at the same time until it returns false.
   */
  public void testInterval0() {
    timer.scheduleRepeating(repeating, 500, 0);
    timer.tick(499);

    verify(repeating, never()).execute();
    when(repeating.execute()).thenReturn(true, true, true, true, false);

    timer.tick(1);
    verify(repeating, times(5)).execute();
  }

  /**
   * Tests that when there are several tasks with interval 0 running at the same time,
   * all of them are executed once before the first one is executed again.
   */
  public void testTasksRepeatedlyRunningAtSameTimeAreScheduledFairly() {
    int taskCount = 10;
    final IncrementalTask[] tasks = new IncrementalTask[taskCount];
    for (int i = 0; i < taskCount; i++) {
      tasks[i] = mock(IncrementalTask.class, "repeating_" + i);
      if (i==0) {
        when(tasks[i].execute()).thenReturn(true, false);
      } else {
        when(tasks[i].execute()).thenReturn(false);
      }
      timer.scheduleRepeating(tasks[i], 1, 0);
    }

    InOrder inOrder = inOrder((Object[]) tasks);

    timer.tick(1);

    for (int i = 0; i < taskCount; i++) {
      inOrder.verify(tasks[i]).execute();
    }
    inOrder.verify(tasks[0]).execute();

    verify(tasks[0], times(2)).execute();
  }

  public void testReschedulingCancelsFirst() {
    timer.scheduleDelayed(oneoff, 500);

    timer.tick(499);
    verify(oneoff, never()).execute();

    timer.scheduleDelayed(oneoff, 1000);

    timer.tick(2);
    verify(oneoff, never()).execute();

    timer.tick(1000);
    verify(oneoff, times(1)).execute();
  }

  /**
   * Tests that if multiple tasks are scheduled for the same time, they are all
   * executed.
   */
  public void testMultipleTasksAtSameTime() {
    final int time = 100;
    timer.scheduleDelayed(oneoff, time);
    final Task anotherTask = mock(Task.class, "another_task");
    timer.scheduleDelayed(anotherTask, time);

    timer.tick(time);
    verify(oneoff, times(1)).execute();
    verify(anotherTask, times(1)).execute();
  }

  /** Tests that multiple processes can be run at the same time. */
  public void testMultipleProcessesAtSameTime() {
    final int time = 100;
    final IncrementalTask anotherTask = mock(IncrementalTask.class, "anotherTask");
    timer.scheduleRepeating(repeating, 0, time);
    timer.scheduleRepeating(anotherTask, time, time);

    timer.tick(0);
    verify(repeating, times(1)).execute();

    timer.tick(time);
    verify(repeating, times(2)).execute();
    verify(anotherTask, times(1)).execute();
  }

  /**
   * Tests that when the timer advances across more than one execution time
   * point of the same repeated task, the task is run several times.
   */
  public void testRepeatedTaskMakesUpForMissedExecutions() {
    timer.scheduleRepeating(repeating, 5, 10);

    timer.tick(4);
    verify(repeating, never()).execute();
    timer.tick(99);
    verify(repeating, times(10)).execute();
  }

  /**
   * Tests that when {@link IncrementalTask#execute()} returns false, the task
   * is no longer rescheduled for execution.
   */
  public void testIncrementalTaskIsCanceledIfItReturnsFalse() {
    timer.scheduleRepeating(repeating, 0, 1);
    when(repeating.execute()).thenReturn(true, true, true, true, false);

    timer.tick(4);
    verify(repeating, times(5)).execute();
    timer.tick(100);
    verify(repeating, times(5)).execute();
  }

  /**
   * Tests that a non-repeating task is scheduled only once, even though
   * internally in the {@link FakeTimerService} it is modeled as an
   * {@link IncrementalTask} that repeats every 1 msec but then opts out of the
   * rescheduling by returning false from {@link IncrementalTask#execute()}.
   */
  public void testNormalTaskIsRunOnlyOnce() {
    timer.schedule(oneoff);
    timer.tick(1000);
    verify(oneoff, times(1)).execute();
  }
}
