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

import org.waveprotocol.wave.model.util.FuzzingBackOffGenerator.BackOffParameters;

/**
 * Schedules a task with fuzzing fibonacci backoff schedule. The
 * Actual scheduling is delegated to a native scheduler.
 * This class does not interpret the delay values, but simply passes
 * them through to the native scheduler.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FuzzingBackOffScheduler implements Scheduler {
  /** The scheduled task that can be cancelled */
  public interface Cancellable {
    /**
     * Cancel this task.
     */
    void cancel();
  }

  /**
   * Allows injection of actually defer executing a task. This because the
   * client and server has different scheduling implementations. A collective
   * scheduler tries to executed the task at the given targetTimeMs, but may
   * execute the task at minAllowedMs if it is more efficient to do so.
   */
  public interface CollectiveScheduler {

    /**
     * Schedules a task to be executed between minAllowedMs and targetTimeMs
     * milliseconds in the future.
     *
     * @param task the task to execute
     * @param minAllowedMs the minimum amount of time to wait.
     * @param targetTimeMs the target delay to wait
     */
    Cancellable schedule(Command task, int minAllowedMs, int targetTimeMs);
  }

  private final FuzzingBackOffGenerator generator;
  private final CollectiveScheduler scheduler;
  private final int maxAttempts;

  private Cancellable scheduledTask;
  private int attempts = 0;

  /**
   * @param initialBackOffMs Initial value to back off. This class does not interpret the meaning of
   *    this value.
   * @param maxBackOffMs Max value to back off
   * @param randomisationFactor between 0 and 1 to control the range of randomness.
   * @param scheduler assumed not null.
   * @param maxAttempts maximum number of times to schedule a task
   */
  private FuzzingBackOffScheduler(int initialBackOffMs, int maxBackOffMs,
      double randomisationFactor, CollectiveScheduler scheduler, int maxAttempts) {
    this.generator = new FuzzingBackOffGenerator(initialBackOffMs, maxBackOffMs,
        randomisationFactor);
    this.scheduler = scheduler;
    this.maxAttempts = maxAttempts;
  }

  @Override
  public void reset() {
    generator.reset();
    if (scheduledTask != null) {
      scheduledTask.cancel();
    }
    scheduledTask = null;
  }

  @Override
  public boolean schedule(Command task) {
    if (scheduledTask != null) {
      scheduledTask.cancel();
    }
    if (attempts >= maxAttempts) {
      return false;
    }
    BackOffParameters parameters = generator.next();
    scheduledTask = scheduler.schedule(task, parameters.minimumDelay, parameters.targetDelay);
    attempts++;
    return true;
  }

  /**
   * Builder for FuzzingBackOffSchedulers.
   */
  public static class Builder {
    private int initialBackOffMs = 10;
    private int maxBackOffMs = 5000;
    private double randomisationFactor = 0.5;
    private final CollectiveScheduler scheduler;
    private int maxAttempts = Integer.MAX_VALUE;

    /**
     * Constructor.
     *
     * @param scheduler the underlying scheduler to use to actually execute tasks.
     */
    public Builder(CollectiveScheduler scheduler) {
      this.scheduler = scheduler;
    }

    /**
     * Build the scheduler.
     *
     * @return the newly created scheduler
     */
    public Scheduler build() {
      return new FuzzingBackOffScheduler(initialBackOffMs, maxBackOffMs, randomisationFactor,
          scheduler, maxAttempts);
    }

    public Builder setInitialBackOffMs(int initialBackOffMs) {
      this.initialBackOffMs = initialBackOffMs;
      return this;
    }

    public Builder setMaxBackOffMs(int maxBackOffMs) {
      this.maxBackOffMs = maxBackOffMs;
      return this;
    }

    public Builder setRandomisationFactor(double randomisationFactor) {
      this.randomisationFactor = randomisationFactor;
      return this;
    }

    public Builder setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
    }
  }
}
