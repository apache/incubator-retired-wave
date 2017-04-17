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

import junit.framework.TestCase;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;

import static org.mockito.Mockito.*;

/**
 * Test case for IdempotentScheduler.
 *
 */

public class IdempotentSchedulerTest extends TestCase {

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
    is.schedule();

    verify(timer, times(1)).isScheduled(is);
    verify(timer, times(1)).scheduleRepeating(is, delay, delay);
  }

  public void testCancelsItselfAsTheTask() {
    when(timer.isScheduled(is)).thenReturn(true);
    is.cancel();
    verify(timer, times(1)).cancel(is);
  }
  public void testSchedulerExecutesTask() {
    is.execute();
    verify(task, times(1)).execute();
  }

  public void testMultipleScheduleCallsScheduleExactlyOnce() {
    when(timer.isScheduled(is)).thenReturn(true, false);

    is.schedule();
    is.schedule();

    verify(timer, times(2)).isScheduled(is);
    verify(timer, times(1)).scheduleRepeating(is, delay, delay);
  }

  public void testCancelWillNotCancelIfNotScheduled() {
    when(timer.isScheduled(is)).thenReturn(false);
    is.cancel();
    verify(timer, never()).cancel(is);
  }

  public void testWillRescheduleAfterTaskCompletion() {
    when(timer.isScheduled(is)).thenReturn(false);
    when(task.execute()).thenReturn(false);

    is.schedule();
    is.execute();  // Simulates call by timer.
    is.schedule();

    verify(timer, times(2)).scheduleRepeating(is, delay, delay);
  }

  public void testWillRescheduleAfterCancel() {
    when(timer.isScheduled(is)).thenReturn(false, true, false);

    is.schedule();
    is.cancel();
    is.schedule();

    verify(timer, times(1)).cancel(is);
    verify(timer, times(2)).scheduleRepeating(is, delay, delay);
  }
}
