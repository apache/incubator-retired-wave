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

package org.waveprotocol.wave.client.widget.overflowpanel;

import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;

/**
 * Decorator which can be attached to an OverflowPanel, and makes sure that overflowing widgets
 * are added to a dropdown.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class OverflowPanelUpdater {
  /**
   * Required interface for anything this decorator is to control.
   */
  public interface OverflowPanel {
    /**
     * Called at the start/end of the calculation to reset state.
     * Note that other method calls should occur between these two, in particular isVisible
     * and hasOverflowed are only required to be valid between calls.
     */
    void onBeginOverflowLayout();
    void onEndOverflowLayout();

    /**
     * Shows the more button. On {@link OverflowPanelUpdater#updateState} this
     * will be called (if at all) before moving any items to the overflow
     * bucket.
     */
    void showMoreButton();

    /** Moves an indexed widget into the overflow bucket. */
    void moveToOverflowBucket(int index);

    /** Checks whether an indexed widget on the panel would be visible (if not overflowing). */
    boolean isVisible(int index);

    /** Checks whether an indexed widget on the panel has overflowed. */
    boolean hasOverflowed(int index);

    /** Gets the number of widgets in the panel. */
    int getWidgetCount();
  }

  /** Panel this decorates */
  private final OverflowPanel panel;

  /** Task for updating state of toolbar buttons */
  private final Task stateUpdater = new Task() {
    @Override
    public void execute() {
      updateState();
    }
  };

  /**
   * Create the decorator to wrap an overflow panel.
   * @param panel The overflow-aware panel this decorator wraps
   */
  public OverflowPanelUpdater(OverflowPanel panel) {
    this.panel = panel;
  }

  /**
   * Updates the overflow state - runs backwards through the contained widgets until a visible one
   * is found on the top row, then moves everything after it into the overflow bucket.
   */
  public void updateState() {
    panel.onBeginOverflowLayout();

    // run backwards through the items until we find one on the top line
    int numChildren = panel.getWidgetCount();
    int lastOnTop;
    boolean itemHasOverflowed = false;

    for (lastOnTop = numChildren - 1; lastOnTop >= 0; lastOnTop--) {
      if (!panel.isVisible(lastOnTop)) {
        continue; // skip invisible widgets
      }
      if (!panel.hasOverflowed(lastOnTop)) {
        break; // stop once we hit widgets that haven't overflowed.
      }
      itemHasOverflowed = true;
    }

    // Show the button if any buttons have overflowed; this will change the
    // layout, so continue calculating which buttons have overflowed.
    if (itemHasOverflowed) {
      panel.showMoreButton();
    }

    for (; lastOnTop >= 0; lastOnTop--) {
      if (!panel.isVisible(lastOnTop)) {
        continue;
      }
      if (!panel.hasOverflowed(lastOnTop)) {
        break;
      }
    }

    // Move all the overflowed buttons to the overflow panel.
    while (++lastOnTop < numChildren) {
      panel.moveToOverflowBucket(lastOnTop);
    }

    panel.onEndOverflowLayout();
  }

  /**
   * Schedules a deferred call to updateState, if such a call is not already scheduled.  This update
   * is deferred because it is dependent on layout, which depends on StyleInjector, which defers
   * things by default.
   */
  public void updateStateEventually() {
    if (!SchedulerInstance.get().isScheduled(stateUpdater)) {
      SchedulerInstance.get().schedule(Priority.LOW, stateUpdater);
    }
  }
}
