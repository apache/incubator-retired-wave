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

/**
 * A simple timer that is associated with a specific task that may be
 * scheduled/cancelled. Because it is associated with a specific task, it may be
 * efficiently implemented to avoid unecessary object creation and other
 * overheads when scheduling.
 *
 * Rather than having the interface mandate mutability through a setter for the
 * runnable task, a factory interface is instead provided for most uses.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface SimpleTimer {
  /**
   * Factory to create a simple timer.
   */
  public interface Factory {
    /** Factory method */
    SimpleTimer create(Runnable runnable);
  }

  /**
   * @return The current time in millis, as a double
   */
  double getTime();

  /**
   * Run the task at the next available opportunity. Implicitly cancels any
   * other scheduling.
   */
  void schedule();

  /**
   * Run the task at the next available opportunity, but no earlier than the
   * given time. Implicitly cancels any other scheduling.
   *
   * @param when
   */
  void schedule(double when);

  /**
   * Cancel any scheduled running of the task.
   */
  void cancel();
}
