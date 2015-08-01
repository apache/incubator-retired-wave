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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gwt.core.client.Scheduler;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.Scheduler.Command;

/**
 * Test case for {@link IdempotentFinally}.
 *
 */

public class IdempotentFinallyTest extends TestCase {

  private static class TestCommand extends IdempotentFinally {
    private final Command command;

    public TestCommand(Scheduler scheduler, Command command) {
      super(scheduler);
      this.command = command;
    }

    @Override
    protected void doExecute() {
      command.execute();
    }
  }

  // Mocks
  private Command command;
  private Scheduler scheduler;

  // Target
  private IdempotentFinally target;

  @Override
  protected void setUp() {
    command = mock(Command.class);
    scheduler = mock(Scheduler.class);
    target = new TestCommand(scheduler, command);
  }

  //
  // Test calls to scheduler.
  //

  public void testScheduleSchedules() {
    target.schedule();
    verify(scheduler).scheduleFinally(target);
  }

  public void testScheduleIsIdempotent() {
    target.schedule();
    target.schedule();
    verify(scheduler).scheduleFinally(target);
    verifyNoMoreInteractions(scheduler);
  }

  public void testScheduleThenCancelSchedules() {
    target.schedule();
    target.cancel();
    verify(scheduler).scheduleFinally(target);
    verifyNoMoreInteractions(scheduler);
  }

  public void testScheduleAfterCancelDoesNotReschedule() {
    target.schedule();
    target.cancel();
    target.schedule();
    verify(scheduler).scheduleFinally(target);
    verifyNoMoreInteractions(scheduler);
  }

  public void testScheduleAfterPassDoesReschedule() {
    target.schedule();
    target.cancel();
    target.execute();
    target.schedule();
    verify(scheduler, times(2)).scheduleFinally(target);
    verifyNoMoreInteractions(scheduler);
  }

  //
  // Tests for expected callbacks from scheduler.
  //

  public void testScheduleRuns() {
    target.schedule();
    // Simulate scheduler callback
    target.execute();

    verify(command).execute();
  }

  public void testScheduleThenCancelDoesNotRun() {
    target.schedule();
    target.cancel();
    // Simulate scheduler callback
    target.execute();

    verify(command, never()).execute();
  }

  public void testScheduleThenCancelThenScheduleDoesRun() {
    target.schedule();
    target.cancel();
    target.schedule();
    // Simulate scheduler callback
    target.execute();

    verify(command, times(1)).execute();
  }

  public void testScheduleThenCancelTwiceDoesNotRun() {
    target.schedule();
    target.cancel();
    target.cancel();
    target.schedule();
    // Simulate scheduler callback
    target.execute();

    verify(command, times(1)).execute();
  }

  //
  // Resilience checks.
  //

  public void testExecuteWithoutScheduleFails() {
    try {
      target.execute();
      fail();
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  public void testExtraExecuteAfterScheduleFails() {
    target.schedule();
    target.execute();
    try {
      target.execute();
      fail();
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  public void testExtraExecuteAfterCancelFails() {
    target.schedule();
    target.cancel();
    target.execute();

    try {
      target.execute();
      fail();
    } catch (IllegalStateException e) {
      // Expected.
    }
  }
}
