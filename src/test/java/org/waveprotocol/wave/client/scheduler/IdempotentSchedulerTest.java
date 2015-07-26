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

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

/**
 * Test case for IdempotentScheduler.
 *
 */

public class IdempotentSchedulerTest extends MockObjectTestCase {

  // Mocks.
  private IncrementalTask task;
  private TimerService timer;
  private int delay;

  // Test target.
  private IdempotentScheduler is;

  @Override
  protected void setUp() {
    task = mock(IncrementalTask.class);
    timer = mock(TimerService.class);
    delay = 50;

    is = IdempotentScheduler.builder().with(timer).with(delay).build(task);
  }

  public void testSchedulesItselfAsTheTask() {
    checking(new Expectations() {{
        oneOf(timer).isScheduled(with(is));
        will(returnValue(false));
        oneOf(timer).scheduleRepeating(is, delay, delay);
    }});

    is.schedule();
  }

  public void testCancelsItselfAsTheTask() {
    checking(new Expectations() {{
      oneOf(timer).isScheduled(with(is));
      will(returnValue(true));
      oneOf(timer).cancel(is);
    }});

    is.cancel();
  }
  public void testSchedulerExecutesTask() {
    checking(new Expectations() {{
      oneOf(task).execute();
    }});

    is.execute();
  }

  public void testMultipleScheduleCallsScheduleExactlyOnce() {
    checking(new Expectations() {{
      oneOf(timer).isScheduled(with(is));
      will(returnValue(false));
      oneOf(timer).scheduleRepeating(is, delay, delay);
      oneOf(timer).isScheduled(with(is));
      will(returnValue(true));
    }});

    is.schedule();
    is.schedule();
  }

  public void testCancelWillNotCancelIfNotScheduled() {
    checking(new Expectations() {{
      oneOf(timer).isScheduled(with(is));
      will(returnValue(false));
    }});

    is.cancel();
  }

  public void testWillRescheduleAfterTaskCompletion() {
    checking(new Expectations() {{
      oneOf(timer).isScheduled(with(is));
      will(returnValue(false));
      oneOf(timer).scheduleRepeating(is, delay, delay);
      oneOf(task).execute();
      will(returnValue(false));  // Indicates termination
      oneOf(timer).isScheduled(with(is));
      will(returnValue(false));
      oneOf(timer).scheduleRepeating(is, delay, delay);
    }});

    is.schedule();
    is.execute();  // Simulates call by timer.
    is.schedule();
  }

  public void testWillRescheduleAfterCancel() {
    checking(new Expectations() {{
      oneOf(timer).isScheduled(with(is)); will(returnValue(false));
      oneOf(timer).scheduleRepeating(is, delay, delay);
      oneOf(timer).isScheduled(with(is)); will(returnValue(true));
      oneOf(timer).cancel(is);
      oneOf(timer).isScheduled(with(is));
      will(returnValue(false));
      oneOf(timer).scheduleRepeating(is, delay, delay);
    }});

    is.schedule();
    is.cancel();
    is.schedule();
  }
}
