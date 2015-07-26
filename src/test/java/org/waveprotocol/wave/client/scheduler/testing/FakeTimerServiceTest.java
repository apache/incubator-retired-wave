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

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;

import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.Expectations;
import org.jmock.Sequence;

/**
 * The FakeTimerService is a complicated enough fake that it deserves its own
 * tests. (Normally I wouldn't test fakes.)
 *
 */

public class FakeTimerServiceTest extends MockObjectTestCase {
  private Task oneoff;
  private IncrementalTask repeating;
  private FakeTimerService timer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    oneoff = mock(Task.class, "oneoff");
    repeating = mock(IncrementalTask.class, "repeating");
    timer = new FakeTimerService();
  }

  public void testTicks() {
    timer.scheduleDelayed(oneoff, 500);
    timer.scheduleRepeating(repeating, 0, 1000);

    checking(new Expectations() {{
      one(oneoff).execute();
      exactly(2).of(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(1000);
    timer.tick(500);
    timer.tick(499);
    checking(new Expectations() {{
      one(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(1);
    timer.tick(1);
    timer.tick(1);
    checking(new Expectations() {{
      one(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(999);
    checking(new Expectations() {{
      exactly(3).of(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(3000);
  }

  public void testCancel() {
    timer.scheduleDelayed(oneoff, 500);
    timer.cancel(oneoff);
    timer.scheduleRepeating(repeating, 0, 1000);
    timer.cancel(repeating);

    checking(new Expectations() {{
      never(repeating).execute();
      never(oneoff).execute();
    }});

    timer.tick(10 * 1000);
  }

  public void testScheduleWillExecuteImmediatelyOnAnyTick() {
    timer.schedule(oneoff);

    checking(new Expectations() {{
      one(oneoff).execute();
    }});

    timer.tick(0);
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
    checking(new Expectations() {{
      exactly(5).of(repeating).execute();
      will(returnValue(true));
      one(repeating).execute();
      will(returnValue(false));
      never(repeating).execute();
    }});
    timer.tick(1);
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
      timer.scheduleRepeating(tasks[i], 1, 0);
    }
    final Sequence seq = sequence("callOrder");
    checking(new Expectations() {{
      for (IncrementalTask task : tasks) {
        one(task).execute();
        will(returnValue(true));
        inSequence(seq);
      }
      for (IncrementalTask task : tasks) {
        one(task).execute();
        will(returnValue(false));
        inSequence(seq);

        never(task).execute();
      }
    }});
    timer.tick(1);
  }

  public void testReschedulingCancelsFirst() {
    timer.scheduleDelayed(oneoff, 500);

    checking(new Expectations() {{
      never(oneoff).execute();
    }});

    timer.tick(499);
    timer.scheduleDelayed(oneoff, 1000);
    timer.tick(2);

    checking(new Expectations() {{
      one(oneoff).execute();
    }});

    timer.tick(1000);
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
    checking(new Expectations() {{
      one(oneoff).execute();
      one(anotherTask).execute();
    }});
    timer.tick(time);
  }

  /** Tests that multiple processes can be run at the same time. */
  public void testMultipleProcessesAtSameTime() {
    final int time = 100;
    final IncrementalTask anotherTask = mock(IncrementalTask.class, "anotherTask");
    timer.scheduleRepeating(repeating, 0, time);
    timer.scheduleRepeating(anotherTask, time, time);

    checking(new Expectations() {{
      one(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(0);

    checking(new Expectations() {{
      one(repeating).execute();
      will(returnValue(true));
      one(anotherTask).execute();
      will(returnValue(true));
    }});
    timer.tick(time);
  }

  /**
   * Tests that when the timer advances across more than one execution time
   * point of the same repeated task, the task is run several times.
   */
  public void testRepeatedTaskMakesUpForMissedExecutions() {
    timer.scheduleRepeating(repeating, 5, 10);
    timer.tick(4);
    checking(new Expectations() {{
      exactly(10).of(repeating).execute();
      will(returnValue(true));
    }});
    timer.tick(99);
  }

  /**
   * Tests that when {@link IncrementalTask#execute()} returns false, the task
   * is no longer rescheduled for execution.
   */
  public void testIncrementalTaskIsCanceledIfItReturnsFalse() {
    timer.scheduleRepeating(repeating, 0, 1);
    checking(new Expectations() {{
      exactly(10).of(repeating).execute();
      will(returnValue(true));
      one(repeating).execute();
      will(returnValue(false));
      never(repeating).execute();
    }});
    timer.tick(1000);
  }

  /**
   * Tests that a non-repeating task is scheduled only once, even though
   * internally in the {@link FakeTimerService} it is modeled as an
   * {@link IncrementalTask} that repeats every 1 msec but then opts out of the
   * rescheduling by returning false from {@link IncrementalTask#execute()}.
   */
  public void testNormalTaskIsRunOnlyOnce() {
    timer.schedule(oneoff);
    checking(new Expectations() {{
      one(oneoff).execute();
    }});
    timer.tick(1000);
  }
}
