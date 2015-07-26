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

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.Builder;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.Cancellable;
import org.waveprotocol.wave.model.util.FuzzingBackOffScheduler.CollectiveScheduler;
import org.waveprotocol.wave.model.util.Scheduler.Command;

/**
 * Tests for FuzzingBackOffScheduler
 * @author zdwang@google.com (David Wang)
 */

public class FuzzingBackOffSchedulerTest extends TestCase {
  private class FakeCancellable implements Cancellable {
    boolean cancelled;

    @Override
    public void cancel() {
      cancelled = true;
    }

  }

  private class FakeNativeScheduler implements CollectiveScheduler {
    Command command;
    FakeCancellable cancellable;
    int millisec;

    @Override
    public Cancellable schedule(Command command, int millisec, int window) {
      this.millisec = millisec;
      this.command = command;
      this.cancellable = new FakeCancellable();
      return this.cancellable;
    }
  }

  private Command fakeComamnd;
  private FakeNativeScheduler fakeNativeScheduler;
  private Scheduler scheduler;

  @Override
  protected void setUp() {
    fakeComamnd = new Command() {
      @Override
      public void execute() {
      }
    };
    fakeNativeScheduler = new FakeNativeScheduler();
    scheduler = new Builder(fakeNativeScheduler)
        .setInitialBackOffMs(10)
        .setMaxBackOffMs(100)
        .setRandomisationFactor(0.5)
        .build();
  }

  public void testSchedule() {
    assertNull(fakeNativeScheduler.command);
    scheduler.schedule(fakeComamnd);
    assertNotNull(fakeNativeScheduler.command);
  }

  public void testScheduleMultiple() {
    // Schedule for the first time.
    scheduler.schedule(fakeComamnd);
    int lastMillisec = fakeNativeScheduler.millisec;
    FakeCancellable lastCancellable = fakeNativeScheduler.cancellable;
    assertNotNull(fakeNativeScheduler.command);
    assertFalse(fakeNativeScheduler.cancellable.cancelled);
    assertTrue(lastMillisec != 0);

    // Schedule again and check previous cancelled
    for (int i = 0; i < 20; i++) {
      scheduler.schedule(fakeComamnd);
      assertNotNull(fakeNativeScheduler.command);
      assertFalse(fakeNativeScheduler.cancellable.cancelled);
      assertNotSame(lastCancellable, fakeNativeScheduler.cancellable);
      assertTrue(lastCancellable.cancelled);
      lastMillisec = fakeNativeScheduler.millisec;
      lastCancellable = fakeNativeScheduler.cancellable;
    }

    // Check that the time have increased over time
    assertTrue(lastMillisec >= 50);
  }

  public void testScheduleMaxTimes() {
    int maxAttempts = 5;
    scheduler = new Builder(fakeNativeScheduler)
        .setInitialBackOffMs(10)
        .setMaxBackOffMs(100)
        .setRandomisationFactor(0.5)
        .setMaxAttempts(maxAttempts)
        .build();

    for (int i = 0; i < maxAttempts; i++) {
      assertTrue(scheduler.schedule(fakeComamnd));
    }
    assertFalse(scheduler.schedule(fakeComamnd));
  }

  public void testReset() {
    // schedule something.
    scheduler.schedule(fakeComamnd);
    FakeCancellable lastTask = fakeNativeScheduler.cancellable;
    assertNotNull(fakeNativeScheduler.cancellable);

    // reset it and check the previous is cancelled.
    scheduler.reset();
    assertTrue(lastTask.cancelled);

    scheduler.schedule(fakeComamnd);
    assertNotNull(fakeNativeScheduler.command);
    assertNotSame(lastTask, fakeNativeScheduler.command);
  }
}
