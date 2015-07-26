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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.client.gadget.GadgetLog.log;

import org.waveprotocol.wave.client.scheduler.ScheduleTimer;

/**
 * Implements delayed submit logic to aggregate consecutive operations. Avoids
 * excessive submit requests when several changes happen within a short period
 * of time.
 *
 */
public class Submitter {

  /**
   * Defines the submit method for the submitter.
   */
  public static interface SubmitTask {
    /**
     * Submit method.
     */
    void doSubmit();
  }

  /** Submit delay. */
  private final int timeoutMs;

  /** Submit interface. */
  private final SubmitTask task;

  /** Waiting to submit flag. */
  private boolean submitScheduled;

  /**
   * Constructs blip submitter object that aggregates submits that happen within
   * timeoutMs time interval.
   *
   * @param timeoutMs time interval in milliseconds to aggregate the submit
   *        commands; if 0 the submit is immediate.
   * @param task the submit task interface.
   */
  public Submitter(int timeoutMs, SubmitTask task) {
    this.task = task;
    this.timeoutMs = (timeoutMs > 0) ? timeoutMs : 0;
    submitScheduled = false;
  }

  /**
   * Performs submit immediately, resets the scheduled flag, suppresses and logs
   * all exceptions.
   */
  public void submitImmediately() {
    try {
      task.doSubmit();
      submitScheduled = false;
    } catch (Exception e) {
      // Suppress and ignore all exceptions.
      log("Submit exception " + e.getMessage());
    }
  }

  /**
   * Triggers scheduled submit.
   */
  public void triggerScheduledSubmit() {
    submitIfScheduled();
  }

  /**
   * Schedules submit. If timeout is greater than 0 this method either schedules
   * a submit or ignores the request if submit is already scheduled.
   */
  public void submit() {
    if (timeoutMs == 0) {
      submitImmediately();
    } else {
      // No synchronization issues in GWT.
      if (!submitScheduled) {
        submitScheduled = true;
        new ScheduleTimer() {
          @Override
          public void run() {
            submitIfScheduled();
          }
        }.schedule(timeoutMs);
      }
    }
  }

  /**
   * Performs submit if submit is scheduled.
   */
  private void submitIfScheduled() {
    if (submitScheduled) {
      submitImmediately();
    }
  }
}
