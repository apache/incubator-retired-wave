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

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Closure of a schedule command that provides idempotent rescheduling.
 *
 * NOTE(user): All the use cases of rescheduling I've seen want idempotent
 * behaviour. Perhaps that should be the default semantics for the scheduler?
 *
 */
public final class IdempotentScheduler implements IncrementalTask {
  /**
   * A debug interface for measuring the efficiency with which the wrapped task
   * executes.
   */
  public interface EfficiencyRecorder {
    void startWhole();

    void endWhole();

    void startUnit();

    void endUnit();
  }

  /**
   * A no-op implementation of the recorder.
   */
  @SuppressWarnings("unused")
  private final static EfficiencyRecorder NO_RECORDING = new EfficiencyRecorder() {
    @Override
    public void startWhole() {
    }

    @Override
    public void endWhole() {
    }

    @Override
    public void startUnit() {
    }

    @Override
    public void endUnit() {
    }
  };

  private final EfficiencyRecorder recorder;
  private final TimerService scheduler;
  private final IncrementalTask task;
  private final int interval;

  public static class Builder {
    private EfficiencyRecorder recorder = NO_RECORDING;
    private TimerService timer;
    private int interval = 0;

    public Builder with(EfficiencyRecorder recorder) {
      this.recorder = recorder;
      return this;
    }

    public Builder with(TimerService timer) {
      this.timer = timer;
      return this;
    }

    public Builder with(int interval) {
      this.interval = interval;
      return this;
    }

    public IdempotentScheduler build(IncrementalTask task) {
      Preconditions.checkNotNull(task, "task must not be null");

      // NOTE(user): this must be lazily created to avoid test dependency on SchedulerInstance.
      if (timer == null) {
        timer = SchedulerInstance.getLowPriorityTimer();
      }
      return new IdempotentScheduler(timer, task, recorder, interval);
    }
  }

  private IdempotentScheduler(TimerService scheduler, IncrementalTask task,
      EfficiencyRecorder recorder, int interval) {
    this.scheduler = scheduler;
    this.task = task;
    this.recorder = recorder;
    this.interval = interval;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Cancels this task.
   */
  public void cancel() {
    if (scheduler.isScheduled(this)) {
      scheduler.cancel(this);
      recorder.endWhole();
    }
  }

  /**
   * Schedules this task, if not already scheduled.
   */
  public void schedule() {
    if (!scheduler.isScheduled(this)) {
      scheduler.scheduleRepeating(this, interval, interval);
      recorder.startWhole();
    }
  }

  @Override
  public boolean execute() {
    recorder.startUnit();
    boolean shouldContinue = task.execute();
    recorder.endUnit();
    if (!shouldContinue) {
      recorder.endWhole();
    }
    return shouldContinue;
  }

  @Override
  public String toString() {
    return "IdempotentScheduler - wrapping: " + task.toString();
  }
}
