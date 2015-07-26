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

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.impl.SchedulerImpl;

/**
 * A command that is idempotentally schedulable and cancellable.
 *
 * This class primarily exists to work around the absence of isScheduled() and
 * cancel() methods in GWT's scheduler interface.
 *
 */
public abstract class IdempotentFinally implements ScheduledCommand {
  private enum State {
    /** Not scheduled. */
    NONE,
    /** Scheduled, and will run doExecute() when invoked. */
    SCHEDULED_TO_RUN,
    /** Scheduled, but will do nothing when invoked. */
    SCHEDULED_TO_SKIP
  }

  private final Scheduler scheduler;
  private State state = State.NONE;

  @VisibleForTesting
  IdempotentFinally(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  protected IdempotentFinally() {
    this(SchedulerImpl.get());
  }

  /**
   * Schedules this command to run before the end of this event loop. If it is
   * already scheduled to run, does nothing. If it was previously cancelled, it
   * is uncancelled.
   */
  public final void schedule() {
    switch(state) {
      case NONE:
        scheduler.scheduleFinally(this);
        state = State.SCHEDULED_TO_RUN;
        break;
      case SCHEDULED_TO_SKIP:
        // Already scheduled.
        state = State.SCHEDULED_TO_RUN;
        break;
      case SCHEDULED_TO_RUN:
        // Do nothing.
        break;
    }
    assert state == State.SCHEDULED_TO_RUN;
  }

  /**
   * Cancels this command from running, if it is currently scheduled.
   */
  public final void cancel() {
    switch(state) {
      case NONE:
        // Do nothing.
        break;
      case SCHEDULED_TO_SKIP:
        // Do nothing.
        break;
      case SCHEDULED_TO_RUN:
        // Already scheduled.
        state = State.SCHEDULED_TO_SKIP;
        break;
    }
    assert state != State.SCHEDULED_TO_RUN;
  }

  @Override
  public final void execute() {
    switch (state) {
      case NONE:
        throw new IllegalStateException( "IdempotentFinally run when not scheduled");
      case SCHEDULED_TO_SKIP:
        state = State.NONE;
        // Skip doExecute();
        break;
      case SCHEDULED_TO_RUN:
        state = State.NONE;
        doExecute();
        break;
    }
  }

  /**
   * Runs this command. Subclasses must override this to provide their command
   * logic.
   */
  protected abstract void doExecute();
}
