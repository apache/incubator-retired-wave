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

import org.waveprotocol.wave.client.scheduler.Scheduler.Task;


/**
 * A simple replacement for DeferredCommand that uses our scheduler.
 *
 * All new code should uses the scheduler directly.
 *
 */
public class ScheduleCommand {
  private static final Scheduler scheduler = SchedulerInstance.get();

  /**
   * Add a command to be executed later.
   *
   * @param cmd
   */
  public static void addCommand(Task cmd) {
    scheduler.schedule(Scheduler.Priority.LOW, cmd);
  }

  /**
   * Add a command to be executed later.
   *
   * @param cmd
   */
  public static void addCommand(Scheduler.IncrementalTask cmd) {
    scheduler.schedule(Scheduler.Priority.LOW, cmd);
  }
}
