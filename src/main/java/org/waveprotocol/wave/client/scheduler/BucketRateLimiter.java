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


import com.google.gwt.core.client.GWT;
import org.waveprotocol.wave.client.common.util.FastQueue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * An implementation of the standard token bucket rate limiter.
 * See http://en.wikipedia.org/wiki/Token_bucket for more detail.
 *
 */
public class BucketRateLimiter {
  /**
   * The maximum number of tokens stored in the bucket.
   */
  private final int maxStoredTokens;

  /**
   * The maximum number of outstanding requests allowed. If more than this is
   * added to the queue, the oldest one will get cancelled.
   */
  private final int maxOutstandingRequests;

  /**
   * The period at which tokens are added to the bucket.
   */
  private final int tokenPeriodMs;

  /**
   * The scheduling interval between the timer delay.
   */
  private final int schedulingInterval;

  /**
   * The timer service used to execute the command when a new token is
   * available.
   */
  private final TimerService service;

  // This implementation must be suitable for FIFO behaviour
  private final Queue<CancellableCommand> outstandingRequests =
      GWT.isClient() ? new FastQueue<CancellableCommand>() : new LinkedList<CancellableCommand>();

  /**
   * The time that last token was given out.
   */
  private int lastTokenTime;

  /**
   * Number of available tokens
   */
  private int numTokens;

  /**
   * true if task is currently being run by timer.
   */
  private boolean isScheduled;

  private final Scheduler.Task task = new Scheduler.Task() {
    /** Used for toString() so we can report was was executed and what took too long */
    private final List<CancellableCommand> lastTasks = new ArrayList<CancellableCommand>();

    public void execute() {
      lastTasks.clear();

      updateTokenCount();
      while (numTokens > 0 && outstandingRequests.size() > 0) {
        numTokens--;
        CancellableCommand command = outstandingRequests.poll();
        lastTasks.add(command);
        command.execute();
      }
      // reschedule the timer.
      if (outstandingRequests.size() > 0) {
        service.scheduleDelayed(this, schedulingInterval);
      } else {
        // limit the number of stored token
        numTokens = Math.min(numTokens, maxStoredTokens);
        isScheduled = false;
      }
    }

    @Override
    public String toString() {
      return "BucketRateLimiter. [Last tasks: " + lastTasks + "]";
    }
  };

  /**
   * Create a new BucketRateLimiter, with numTokens = 0.
   *
   * @param service TimerService to use for scheduling.
   * @param maxOutstandingRequests The maximum number of command that can exist
   *        in the limiter. if more than this number of outstanding requests has
   *        been added, the oldest ones will get cancelled until the number of
   *        scheduled command is belong this limit.
   * @param maxStoredTokens The maximum number of token that can be saved up.
   * @param tokenPeriodMs
   */
  public BucketRateLimiter(TimerService service, int maxOutstandingRequests, int maxStoredTokens,
      int tokenPeriodMs) {
    this(service, maxOutstandingRequests, maxStoredTokens, 0, tokenPeriodMs);
  }

  /**
   * Create a new BucketRateLimiter.
   *
   * @param service TimerService to use for scheduling.
   * @param maxOutstandingRequests The maximum number of command that can exist
   *        in the limiter. if more than this number of outstanding requests has
   *        been added, the oldest ones will get cancelled until the number of
   *        scheduled command is belong this limit.
   * @param maxStoredTokens The maximum number of token that can be saved up.
   * @param numTokens The number of tokens that the BucketRateLimiter starts with.
   * @param tokenPeriodMs
   */
  public BucketRateLimiter(TimerService service, int maxOutstandingRequests, int maxStoredTokens,
      int numTokens, int tokenPeriodMs) {
    this.maxStoredTokens = maxStoredTokens;
    this.tokenPeriodMs = tokenPeriodMs;
    // For simplicity, we assume that the schedulingInterval is the same as tokenPeriodMs.
    this.schedulingInterval = tokenPeriodMs;
    this.service = service;
    this.maxOutstandingRequests = maxOutstandingRequests;
    this.lastTokenTime = service.elapsedMillis();
    this.numTokens = numTokens;
  }

  /**
   * Schedule a rate limited command.
   */
  public void schedule(CancellableCommand command) {
    if (isScheduled) {
      // the bucket is still waiting for more tokens (hence scheduled)
      addCommandToQueue(command);
    } else {
      // work out the number of tokens should be available
      updateTokenCount();
      // clip it at the maximum stored amount.
      numTokens = Math.min(numTokens, maxStoredTokens);

      // if there is a token available, fire event immediately with the token.
      if (numTokens > 0) {
        numTokens--;
        // Queue must be empty (because non-empty queue implies isScheduled), so we can
        // run the command directly, rather than task.execute(), because there is no need
        // to reschedule afterwards.
        command.execute();
      } else {
        // Not enough tokens - schedule to run later.
        addCommandToQueue(command);
        schedule();
      }
    }
  }

  /**
   * Schedules this task against the timer service.
   */
  private void schedule() {
    service.scheduleDelayed(task, schedulingInterval);
    isScheduled = true;
  }

  /**
   * Add the cancelable command to the queue of commands to execute.
   * @param command
   */
  private void addCommandToQueue(CancellableCommand command) {
    outstandingRequests.add(command);
    while (outstandingRequests.size() > maxOutstandingRequests) {
      outstandingRequests.poll().onCancelled();
    }
  }

  /**
   * Update the number of tokens.
   */
  private void updateTokenCount() {
    // add new tokens based on elapsed time
    int now = service.elapsedMillis();
    int timeElapsed = now - lastTokenTime;
    if (timeElapsed >= tokenPeriodMs) {
      numTokens += timeElapsed / tokenPeriodMs;
      lastTokenTime = now - timeElapsed % tokenPeriodMs;
    }
  }

  /**
   * Clear the available tokens and the token timer.
   */
  public void clearTokens() {
    numTokens = 0;
    lastTokenTime = service.elapsedMillis();
    // TODO(oshlack/reuben): Change this to use scheduler.isScheduled(task) once it exists.
    if (isScheduled) {
      service.scheduleDelayed(task, schedulingInterval);
    }
  }

  /**
   * Cancel all scheduled commands.
   */
  public void cancelAll() {
    terminateScheduleTask();
    for (CancellableCommand request : outstandingRequests) {
      request.onCancelled();
    }
    outstandingRequests.clear();
  }

  /**
   * Cancel a scheduled command.
   *
   * @return true if the command is a part of the queue and it is cancelled.
   */
  public boolean cancel(CancellableCommand command) {
    if (outstandingRequests.remove(command)) {
      command.onCancelled();
      return true;
    }
    return false;
  }

  private void terminateScheduleTask() {
    if (isScheduled) {
      service.cancel(task);
      isScheduled = false;
    }
  }

  public void executeAll() {
    terminateScheduleTask();
    for (CancellableCommand request : outstandingRequests) {
      request.execute();
    }
    outstandingRequests.clear();
  }
}
