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


package org.waveprotocol.wave.client.scheduler.knobs;

import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collection;

/**
 * View interface for a singl priority-level UI control.
 *
 */
public interface KnobView {

  /**
   * Receiver of UI events.
   */
  interface Listener {
    void onClicked();
    void onDetailsClicked();
    void onClearClicked();
    void onToggleTask(String name);
  }

  /**
   * Creator of views.
   */
  interface Factory {
    /**
     * Creates a UI component for a single priority level.
     *
     * @param priority  level being controlled
     * @return a new view for level {@code priority}.
     */
    KnobView create(Priority priority);
  }

  /**
   * Starts using this view.
   *
   * @param listener  event receiver
   */
  void init(Listener listener);

  /**
   * Stops using this view.
   */
  void reset();

  /**
   * Shows this level as being enabled.
   */
  void enable();

  /**
   * Shows this level as being disabled.
   */
  void disable();

  /**
   * Shows the number of jobs in this priority level.
   *
   * @param count  job count
   */
  void showCount(int count);

  /**
   * Shows/updates the names of jobs running / having run.
   *
   * @param currentJobs
   * @param oldJobs
   * @param suppressedJobs current and old jobs that won't run
   */
  void showJobs(Collection<String> currentJobs, Collection<String> oldJobs,
      ReadableStringSet suppressedJobs);

  /**
   * Hides the job name details.
   */
  void hideJobs();
}
