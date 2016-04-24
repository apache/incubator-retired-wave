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

import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presenter for a single priority level.
 *
 */
public class KnobPresenter implements KnobView.Listener {
  /** View */
  private final KnobView view;

  /** True if this widget is to be shown as enabled. */
  private boolean enabled = true;

  /** Currently running jobs */
  private final Set<Schedulable> jobs = new HashSet<Schedulable>();

  /** Jobs previously run, since last clear */
  private final Set<Schedulable> previousJobs = new HashSet<Schedulable>();

  /** Names of jobs not permitted to run */
  private final StringSet suppressedJobNames = CollectionUtils.createStringSet();

  /** Whether job name details are being displayed */
  private boolean detailsShown;

  /**
   * Creates a priority-level controller.
   *
   * @param view  display object
   */
  KnobPresenter(KnobView view) {
    this.view = view;

    // Set the view to reflect the current state.
    view.init(this);
    if (enabled) {
      view.enable();
    } else {
      view.disable();
    }
    view.showCount(jobs.size());

    detailsShown = false;
    view.hideJobs();
  }

  /**
   * Enables this level.
   */
  void enable() {
    if (!enabled) {
      enabled = true;
      view.enable();
    }
  }

  /**
   * Disables this level.
   */
  void disable() {
    if (enabled) {
      enabled = false;
      view.disable();
    }
  }

  /**
   * @return true if this level is enabled.
   */
  boolean isEnabled() {
    return enabled;
  }

  boolean isSuppressed(Schedulable job) {
    return !suppressedJobNames.isEmpty() && suppressedJobNames.contains(jobIdentifier(job));
  }

  void addJob(Schedulable job) {
    jobs.add(job);
    updateView();
  }

  void removeJob(Schedulable job) {
    jobs.remove(job);
    previousJobs.add(job);
    updateView();
  }

  private void updateView() {
    view.showCount(jobs.size());
    updateDetails();
  }

  private void updateDetails() {
    if (detailsShown) {
      showJobs();
    }
  }

  @Override
  public void onClicked() {
    // Toggle
    if (enabled) {
      disable();
    } else {
      enable();
    }
  }

  @Override
  public void onDetailsClicked() {
    detailsShown = !detailsShown;
    if (detailsShown) {
      showJobs();
    } else {
      view.hideJobs();
    }
  }

  @Override
  public void onClearClicked() {
    previousJobs.clear();
    updateView();
  }

  @Override
  public void onToggleTask(String name) {
    if (suppressedJobNames.contains(name)) {
      suppressedJobNames.remove(name);
    } else {
      suppressedJobNames.add(name);
    }
    updateDetails();
  }

  /**
   * This method is expensive, especially the rendering, so ensure not to call
   * it unless the jobs are actually being displayed.
   */
  private void showJobs() {
    List<String> list = new ArrayList<String>();
    List<String> previousList = new ArrayList<String>();
    for (Schedulable job : jobs) {
      list.add(jobIdentifier(job));
    }
    for (Schedulable job : previousJobs) {
      previousList.add(jobIdentifier(job));
    }
    view.showJobs(list, previousList, suppressedJobNames);
  }

  private String jobIdentifier(Schedulable job) {
    return job.getClass().getName();
  }
}
