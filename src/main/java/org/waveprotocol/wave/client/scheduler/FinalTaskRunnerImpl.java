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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;

/**
 * Convenience scheduleFinally wrapper.
 *
 * Behaves according to the following semantics:
 * <li> Tasks scheduled will be run at once.
 * <li> Tasks scheduled during task execution will be grouped into the current
 *      run.
 * <li> Tasks scheduled more than once, that have not yet executed, will only
 *      be executed once.
 * <li> The order in which tasks will actually execute is not defined.
 *
 * Custome behaviour may be defined at the start and end of a group run by
 * overriding {@code being} and {@code end}
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FinalTaskRunnerImpl implements FinalTaskRunner {

  private final IdentitySet<Scheduler.Task> tasks = CollectionUtils.createIdentitySet();

  private boolean isExecuting = false;

  private final ScheduledCommand command = new ScheduledCommand() {
    @Override
    public void execute() {
      begin();
      isExecuting = true;
      try {
        while (!tasks.isEmpty()) {
          Scheduler.Task task = tasks.someElement();
          tasks.remove(task);
          task.execute();
        }
      } finally {
        isExecuting = false;
        end();
      }
    }
  };

  /**
   * Override to provide functionality at the start of a task group
   */
  protected void begin() {

  }

  /**
   * Override to provide functionality at the end of a task group
   */
  protected void end() {

  }

  @Override
  public void scheduleFinally(Scheduler.Task task) {
    if (tasks.isEmpty() && !isExecuting) {
      com.google.gwt.core.client.Scheduler.get().scheduleFinally(command);
    }
    tasks.add(task);
  }
}
