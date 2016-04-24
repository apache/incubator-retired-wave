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

package org.waveprotocol.wave.testing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * A executor which defers execution until requested.
 *
 * This class is designed to be used as an in-thread fake of an executor, and is
 * not thread safe.
 *
 * @author anorth@google.com (Alex North)
 */
public final class DeferredExecutor implements Executor {
  private final Semaphore numTasks = new Semaphore(0);
  private List<Runnable> thingsToExecute = Lists.newArrayList();
  private boolean shutdownCalled = false;

  @Override
  public void execute(Runnable command) throws RejectedExecutionException {
    if (shutdownCalled) {
      throw new RejectedExecutionException();
    }
    thingsToExecute.add(command);
    numTasks.release();
  }

  public List<Runnable> shutdown() {
    shutdownCalled = true;
    runQueuedCommands();
    List<Runnable> unexecuted = Lists.newArrayList(thingsToExecute);
    thingsToExecute.clear();
    return unexecuted;
  }

  public boolean isShutdown() {
    return shutdownCalled;
  }

  public void checkShutdown() {
    Preconditions.checkState(isShutdown(), "Not shut down");
    Preconditions.checkState(thingsToExecute.size() == 0, "Still has things to execute");
  }

  /**
   * Runs all commands which have been scheduled and removes them from the
   * queue. Commands are executed in the calling thread.
   *
   * Note that commands scheduled as a result of executed commands are not
   * immediately run.
   *
   * @see #runAllCommands
   */
  public void runQueuedCommands() {
    List<Runnable> thingsToExecuteNow = thingsToExecute;
    thingsToExecute = new ArrayList<Runnable>();
    for (Runnable r : thingsToExecuteNow) {
      r.run();
    }
  }

  /**
   * Runs commands until no commands remain to run.
   */
  public void runAllCommands() {
    while (!thingsToExecute.isEmpty()) {
      runQueuedCommands();
    }
  }

  /**
   * Waits for howMany tasks to be submitted.
   */
  public void waitForTasks(int howMany) {
    numTasks.acquireUninterruptibly(howMany);
  }
}

