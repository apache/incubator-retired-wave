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

import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;

/**
 * Something that can control the behaviour of the scheduler, and display
 * controls in the UI.
 *
 * The only control mechanism is the ability to enable or disable priority
 * levels, exposed by {@link #isRunnable(Priority)}.
 *
 * A controller is also notified when jobs are added and removed
 *
 */
public interface Controller {

  /**
   * Tells this controller a job was added.
   *
   * @param priority  priority level
   * @param job       the job
   */
  void jobAdded(Priority priority, Schedulable job);

  /**
   * Tells this controller a job was removed.
   *
   * @param priority  priority level
   * @param job       the job
   */
  void jobRemoved(Priority priority, Schedulable job);

  /**
   * Queries whether a priority level should be run or not.
   *
   * @param priority  priority level
   * @return true if tasks in {@code priority} should be run; false if they
   *         should not be run.
   */
  boolean isRunnable(Priority priority);

  /**
   * Queries whether an individual job is to be suppressed when run at a specific priority.
   * This is independent of {@link #isRunnable(Priority)}
   *
   * @param priority
   * @param job
   * @return true if the job is to be suppressed
   */
  boolean isSuppressed(Priority priority, Schedulable job);

  /**
   * Gets the view of this controller, if it has one.
   *
   * @return the controller's view, or {@code null} if there is no view.
   */
  Widget asWidget();

  /**
   * Controller implementation that does nothing.  GWT optimizations should make
   * the cost of this implementation zero.
   */
  public static final Controller NOOP = new Controller() {
    @Override
    public Widget asWidget() {
      return null;
    }

    @Override
    public void jobAdded(Priority priority, Schedulable job) {
      // Do nothing
    }

    @Override
    public void jobRemoved(Priority priority, Schedulable job) {
      // Do nothing
    }

    @Override
    public boolean isRunnable(Priority priority) {
      return true;
    }

    @Override
    public boolean isSuppressed(Priority priority, Schedulable job) {
      return false;
    }
  };
}
