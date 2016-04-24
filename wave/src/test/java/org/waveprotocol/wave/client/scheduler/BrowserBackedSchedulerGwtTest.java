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

import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Listener;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.testing.FakeSimpleTimer;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Test case for BrowserBackedScheduler
 *
 * Does not just test the interface, also checks that its use of the timer is
 * limited, and that the data structures are clean when no tasks are remaining,
 * etc.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class BrowserBackedSchedulerGwtTest extends GWTTestCase {

  /** Usable priorities (excludes {@link Priority#INTERNAL_SUPPRESS}) */
  private static final Collection<Priority> priorities =
      EnumSet.complementOf(EnumSet.of(Priority.INTERNAL_SUPPRESS));


  private abstract class HasProgress {
    HasProgress prev;

    public abstract boolean hasProgressed();

    void checkOrder() {
      if (prev != null) {
        assertTrue("Tasks executed out of order", prev.hasProgressed());
      }
    }
  }

  private class FakeTask extends HasProgress implements Scheduler.Task {
    private final int durationMillis;
    private boolean wasExecuted = false;

    public FakeTask(int durationMillis) {
      this.durationMillis = durationMillis;
    }

    public void execute() {
      assertTrue("Task executed more than once", !wasExecuted);
      wasExecuted = true;
      providedTimer.tick(durationMillis);
    }

    @Override
    public boolean hasProgressed() {
      return wasExecuted;
    }

    @Override
    public String toString() {
      return "FakeTask " + durationMillis + " ms";
    }
  }

  private class FakeProcess extends HasProgress implements
      Scheduler.IncrementalTask {
    private final int[] jobDurations;
    int current = 0;

    public FakeProcess(int... jobDurations) {
      this.jobDurations = jobDurations;
    }

    public boolean execute() {
      assertTrue("Completed process was executed again", !hasFinished());

      providedTimer.tick(jobDurations[current]);
      current++;
      return !hasFinished();
    }

    @Override
    public boolean hasProgressed() {
      return current > 0;
    }

    public boolean hasFinished() {
      return current >= jobDurations.length;
    }
  }

  private class IntervalProcess implements Scheduler.IncrementalTask {
    int expectedNextTime = 0;
    final int interval;

    public IntervalProcess(int firstTime, int interval) {
      expectedNextTime = firstTime;
      this.interval = interval;
    }

    public boolean execute() {
      assertEquals(expectedNextTime, providedTimer.getTime(), 0.01);
      expectedNextTime += interval;
      return true;
    }
  }

  private final SimpleTimer.Factory provider = new SimpleTimer.Factory() {
    public SimpleTimer create(Runnable runnable) {
      assert providedTimer == null
        : "Not expecting to provide more than one timer at a time";
      return providedTimer = new FakeSimpleTimer(runnable);
    }
  };

  private FakeSimpleTimer providedTimer;
  private BrowserBackedScheduler scheduler;
  private List<FakeTask> tasks;
  private ArrayList<Pair<Schedulable, Integer>> executedJob;

  public void testIsScheduledReportsScheduledState() {
    FakeTask task1 = new FakeTask(1);
    FakeTask task2 = new FakeTask(1);
    FakeTask task3 = new FakeTask(1);
    scheduler.schedule(Priority.LOW, task1);
    scheduler.scheduleDelayed(Priority.LOW, task3, 20);

    assertTrue(scheduler.isScheduled(task1));
    assertFalse(scheduler.isScheduled(task2));
    assertTrue(scheduler.isScheduled(task3));

    providedTimer.trigger(10);

    assertFalse(scheduler.isScheduled(task1));
    assertFalse(scheduler.isScheduled(task2));
    assertTrue(scheduler.isScheduled(task3));

    providedTimer.trigger(20);

    assertFalse(scheduler.isScheduled(task1));
    assertFalse(scheduler.isScheduled(task2));
    assertFalse(scheduler.isScheduled(task3));
  }

  public void testIsScheduledReportsFalseForRunningTask() {
    scheduler.schedule(Priority.LOW, new Scheduler.Task() {
      public void execute() {
        assertFalse(scheduler.isScheduled(this));
      }
    });
    scheduler.scheduleDelayed(Priority.LOW, new Scheduler.Task() {
      public void execute() {
        assertFalse(scheduler.isScheduled(this));
      }
    }, 10);
    providedTimer.trigger(20);
  }

  public void testAllCriticalTasksRun() {
    scheduleTasks(4, Priority.CRITICAL, 5);
    providedTimer.trigger();
    assertTasksRun(true, 0, 3);
    assertEmpty(true);
  }

  public void testNonCriticalTasksChunked() {
    scheduleTasks(5, Priority.HIGH, 4);
    providedTimer.trigger();
    assertTasksRun(true, 0, 2);
    assertTasksRun(false, 3, 4);
    providedTimer.trigger();
    assertTasksRun(true, 0, 4);
    assertEmpty(true);
  }

  public void testTasksPrioritised() {
    scheduleTasks(5, Priority.LOW, 4);
    scheduleTasks(5, Priority.HIGH, 4);
    providedTimer.trigger();
    assertTasksRun(false, 0, 4);
    assertTasksRun(true, 5, 7);
    assertTasksRun(false, 8, 9);
    providedTimer.trigger();
    assertTasksRun(true, 5, 9);
    assertTasksRun(true, 0, 0);
    assertTasksRun(false, 1, 4);
    providedTimer.trigger();
    assertTasksRun(true, 1, 3);
    assertTasksRun(false, 4, 4);
    providedTimer.trigger();
    assertTasksRun(true, 0, 9);
    assertEmpty(true);
  }

  public void testTasksScheduledOneAtATime() {
    for (Priority p : priorities) {
      FakeTask task = new FakeTask(10);
      scheduler.schedule(p, task);
      scheduler.schedule(p, task); // Should be a no-op
      providedTimer.trigger();
      assertTrue("Task was not run at all", task.wasExecuted);
    }
    assertEmpty(true);
  }

  public void testRescheduledTasksIncreasePriority() {
    FakeTask task = new FakeTask(1);
    FakeTask longLow = new FakeTask(20);
    FakeTask shortLow = new FakeTask(1);
    scheduler.schedule(Priority.LOW, longLow);
    scheduler.schedule(Priority.LOW, shortLow);
    scheduler.schedule(Priority.LOW, task);
    scheduler.schedule(Priority.HIGH, task); // Should upgrade priority
    providedTimer.trigger();
    assertTrue("Task might not have been upgraded", task.wasExecuted);
    assertTrue("Low priority task out of order", !shortLow.wasExecuted);
    assertEmpty(false);
    trigger(3);
    assertEmpty(true);
  }

  public void testRescheduledTasksDecreasePriority() {
    FakeTask task = new FakeTask(1);
    FakeTask longLow = new FakeTask(20);
    FakeTask shortLow = new FakeTask(1);
    scheduler.schedule(Priority.LOW, longLow);
    scheduler.schedule(Priority.LOW, shortLow);
    scheduler.schedule(Priority.HIGH, task);
    scheduler.schedule(Priority.LOW, task); // Should replace priority
    providedTimer.trigger();
    assertFalse("Task might not have been downgraded", task.wasExecuted);
    assertTrue("Low priority task out of order", !shortLow.wasExecuted);
    assertEmpty(false);
    trigger(3);
    assertEmpty(true);
  }

  public void testProcessesInterleave() {
    final FakeProcess[] lastProc = new FakeProcess[1];

    class InterleavingProc extends FakeProcess {
      public InterleavingProc(int... jobTimes) {
        super(jobTimes);
      }

      @Override
      public boolean execute() {
        assertTrue("Did not interleave", lastProc[0] != this);
        lastProc[0] = this;
        return super.execute();
      }
    }

    FakeProcess proc1 = new InterleavingProc(5, 5, 5);
    FakeProcess proc2 = new InterleavingProc(5, 5, 5);

    scheduler.schedule(Priority.LOW, proc1);
    scheduler.schedule(Priority.LOW, proc2);

    scheduler.setTimeSlice(20);

    providedTimer.trigger();
    assertTrue(proc1.hasProgressed() && proc2.hasProgressed());
    assertEmpty(false);
    providedTimer.trigger();
    assertEmpty(true);
    assertTrue(proc1.hasFinished() && proc2.hasFinished());
  }

  public void testCancelling() {
    FakeProcess process1 = new FakeProcess(10, 10, 10, 10, 10, 10);
    FakeProcess process2 = new FakeProcess(10, 10, 10, 10, 10, 10);
    FakeTask task1 = new FakeTask(1);
    FakeTask task2 = new FakeTask(1);
    scheduler.schedule(Priority.HIGH, process1);
    scheduler.schedule(Priority.LOW, task1);
    scheduler.schedule(Priority.LOW, task2);
    scheduler.schedule(Priority.LOW, process2);

    providedTimer.trigger();
    assertTrue(process1.hasProgressed());
    assertFalse(process1.hasFinished() || process2.hasProgressed());
    assertFalse(task1.wasExecuted || task2.wasExecuted);
    assertEmpty(false);

    scheduler.cancel(task1);
    scheduler.cancel(task2);
    scheduler.cancel(process1);
    scheduler.cancel(process2);
    assertEmpty(true);

    scheduler.schedule(Priority.LOW, task2);

    providedTimer.trigger();
    assertTrue(task2.wasExecuted);
    assertFalse(process1.hasFinished() || process2.hasProgressed());
    assertFalse(task1.wasExecuted);
    assertEmpty(true);
  }

  public void testCancellingAnIncrementalTaskDuringExecutionIgnoresContinueValue() {
    IncrementalTask mock = new IncrementalTask() {
      boolean cancelled;
      @Override
      public boolean execute() {
        if (cancelled) {
          fail("Cancelled task was not removed from scheduler");
        }
        scheduler.cancel(this);
        cancelled = true;
        return true;
      }
    };

    scheduler.scheduleRepeating(Priority.LOW, mock, 0, 50);
    providedTimer.trigger();
    for (int i = 0; i < 50; i++) {
      providedTimer.trigger(1);
    }
    assertEmpty(true);
  }

  public void testRepeatingJob() {
    Scheduler.IncrementalTask p = new IntervalProcess(0, 50);
    scheduler.scheduleRepeating(Priority.LOW, p, 0, 50);

    providedTimer.trigger();
    for (int i = 0; i < 200; i++) {
      providedTimer.trigger(1);
    }

    assertEmpty(false);

    scheduler.cancel(p);

    assertEmpty(true);
  }

  public void testCoincidingRepeatingJobs() {
    for (int i = 0; i < 10; i++) {
      Scheduler.IncrementalTask p = new IntervalProcess(0, 50);
      scheduler.scheduleRepeating(Priority.LOW, p, 0, 50);
    }

    providedTimer.trigger();
    while (providedTimer.getTime() < 200) {
      providedTimer.trigger(1);
    }

    assertEmpty(false);
  }

  public void testDelayedProcessBehavesNormallyOnceStarted() {
    FakeProcess p = new FakeProcess(5, 5, 5, 5);
    scheduler.scheduleDelayed(Priority.LOW, p, 10);
    commonTestDelayedProcessBehavesNormallyOnceStarted(p);
  }

  public void testDelayedZeroIntervalProcessBehavesNormallyOnceStarted() {
    FakeProcess p = new FakeProcess(5, 5, 5, 5);
    scheduler.scheduleRepeating(Priority.LOW, p, 10, 0);
    commonTestDelayedProcessBehavesNormallyOnceStarted(p);
  }

  private void commonTestDelayedProcessBehavesNormallyOnceStarted(FakeProcess p) {
    providedTimer.trigger(9);
    assertFalse(p.hasProgressed());
    providedTimer.trigger(1);
    assertTrue(p.hasProgressed() && !p.hasFinished());
    providedTimer.trigger();
    assertTrue(p.hasFinished());
    assertEmpty(true);
  }

  /**
   * Test that missed out runs of a repeating job are dropped
   */
  public void testRepeatingJobNotOwedExtraUnitsWhenStarved() {
    FakeProcess p = new FakeProcess(1, 1, 1, 1);
    scheduler.scheduleRepeating(Priority.LOW, p, 0, 5);
    for (int i = 0; i < 4; i++) {
      assertEmpty(false);
      providedTimer.trigger(100);
      assertTrue(p.current == i + 1);
    }
    assertTrue(p.hasFinished());
    assertEmpty(true);
  }

  public void testRescheduleDelayedJobResetsTimer() {
    FakeTask t = new FakeTask(5);
    scheduler.scheduleDelayed(Priority.LOW, t, 50);

    providedTimer.trigger(40); // @40, scheduled @50
    assertEquals(50, providedTimer.getScheduledTime(), 0.001);
    scheduler.scheduleDelayed(Priority.LOW, t, 50);

    providedTimer.trigger(40); // @80, scheduled @90
    assertFalse(t.wasExecuted);
    assertEquals(90, providedTimer.getScheduledTime(), 0.001);

    providedTimer.trigger(50); // Past @90
    assertTrue(t.wasExecuted);
    assertEmpty(true);
  }

  public void testRepeatingJobs() {

    final int[] intervals = new int[] { 30, 40, 70 };

    for (int i : intervals) {
      Scheduler.IncrementalTask p = new IntervalProcess(0, i);
      scheduler.scheduleRepeating(Priority.LOW, p, 0, i);
    }

    assertEmpty(false);

    for (int i = 0; i < 500; i++) {
      providedTimer.trigger();
      providedTimer.tick(1);
    }
  }

  public void testDelayedJobs() {

    FakeTask task1 = new FakeTask(1);
    FakeTask task2 = new FakeTask(1);
    FakeTask task3 = new FakeTask(1);
    FakeProcess process1 = new FakeProcess(5, 5, 5, 5, 5, 5);
    FakeProcess process2 = new FakeProcess(5, 5, 5, 5, 5, 5);

    // Schedule a bunch of delayed jobs and check the use of
    // the timer is optimised
    scheduler.scheduleDelayed(Priority.HIGH, task1, 50);
    assertEquals(50, providedTimer.getScheduledTime(), 0.1);

    scheduler.scheduleDelayed(Priority.LOW, task2, 60);
    assertEquals(50, providedTimer.getScheduledTime(), 0.1);

    scheduler.scheduleDelayed(Priority.LOW, task3, 40);
    assertEquals(40, providedTimer.getScheduledTime(), 0.1);

    scheduler.scheduleRepeating(Priority.LOW, process1, 50, 20);
    assertEquals(40, providedTimer.getScheduledTime(), 0.1);

    scheduler.scheduleRepeating(Priority.HIGH, process2, 30, 20);
    assertEquals(30, providedTimer.getScheduledTime(), 0.1);

    task3.prev = process2;
    task1.prev = task3;
    process1.prev = task1;
    task2.prev = process1;

    while (providedTimer.getTime() < 100) {
      providedTimer.trigger(2);
    }

    assertTrue(task1.wasExecuted && task2.wasExecuted && task3.wasExecuted);
    assertTrue(process1.hasProgressed() && process2.hasProgressed());
    assertFalse(process1.hasFinished() && process2.hasFinished());
    assertEmpty(false);

    while (providedTimer.getTime() < 300) {
      providedTimer.trigger(2);
    }

    assertEmpty(true);
  }

  public void testTaskTakingTooLong() {
    // Make slow task 500ms
    scheduleTask(Priority.LOW, 500);

    providedTimer.trigger();
    assertTasksRun(true, 0, 0);
    assertEmpty(true);

    // Check we have gotten call back for task too slow
    assertEquals(1, executedJob.size());
    assertEquals("FakeTask 500 ms", executedJob.get(0).first.toString());
    assertEquals(new Integer(500), executedJob.get(0).second);
  }


  // HELPERS

  @Override
  protected void gwtSetUp() throws Exception {
    providedTimer = null;
    scheduler = new BrowserBackedScheduler(provider, Controller.NOOP);
    scheduler.setTimeSlice(10);
    tasks = new ArrayList<FakeTask>();
    executedJob = new ArrayList<Pair<Schedulable, Integer>>();
    scheduler.addListener(new Listener() {
      @Override
      public void onJobExecuted(Schedulable task, int timeSpent) {
        if (timeSpent > 100) {
          executedJob.add(new Pair<Schedulable, Integer>(task, timeSpent));
        }
      }
    });
  }

  protected int scheduleTask(Priority priority, int duration) {
    FakeTask task = new FakeTask(duration);
    int index = tasks.size();
    scheduler.schedule(priority, task);
    tasks.add(task);
    return index;
  }

  protected int scheduleTasks(int qty, Priority priority, int duration) {
    int last = -1;
    for (int i = 0; i < qty; i++) {
      last = scheduleTask(priority, duration);
    }
    return last;
  }

  protected void assertTasksRun(boolean wereRun, int startIndexInclusive,
      int endIndexInclusive) {
    for (int i = startIndexInclusive; i <= endIndexInclusive; i++) {
      boolean actual = tasks.get(i).wasExecuted;
      assertEquals("Expected task " + i + " was" + (actual ? "" : " not")
          + " run", wereRun, actual);
    }
  }

  protected void assertEmpty(boolean empty) {
    assertTrue("Should " + (empty ? "" : "not ") + "be empty: "
        + scheduler.toString(), scheduler.debugIsClear() ^ !empty);
    double time = providedTimer.getScheduledTime();
    assertTrue("Incorrect leftover timer scheduling: " + time,
        empty ? time == Double.MAX_VALUE : time < Double.MAX_VALUE);
  }

  protected void trigger(int numTimes) {
    for (int i = 0; i < numTimes; i++) {
      providedTimer.trigger();
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.scheduler.tests";
  }
}
