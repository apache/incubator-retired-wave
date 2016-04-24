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

package org.waveprotocol.wave.client.editor;

import com.google.gwt.core.client.GWT;

import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * Editor update event implementation.
 *
 * Keeps track of listeners and scheduling.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class EditorUpdateEventImpl implements EditorUpdateEvent {

  /** Schedule interval for internal throttling */
  private static final int INITIAL_NOTIFY_SCHEDULE_DELAY_MS = 20;

  // This is used, among other things, for updating toolbar buttons, which
  // typically involve annotation queries, which are quite slow.
  // In non-compiled mode, a single button update is on the order of 10ms. In
  // compiled mode, it is more in the order of 1ms.
  // With ~40 buttons to update, there is significant lag in non-compiled
  // mode. This is addressed by rate-limiting the updates, with a very large
  // delay in compiled mode. In non-compiled mode, frequent lag of ~50ms is
  // noticeable, and so we still rate limit the updates, but with a more
  // generous speed.
  // Update at most twice per second in compiled mode, but much less
  // frequently in non-compiled mode.
  private static final int NOTIFY_SCHEDULE_DELAY_GAP_MS = GWT.isScript() ? 500 : 2000;

  /** Set of objects listening in to our editor */
  private final CopyOnWriteSet<EditorUpdateEvent.EditorUpdateListener> updateListeners =
    CopyOnWriteSet.create();

  /** Editor this update refers to. */
  private final EditorImpl editor;

  private boolean notifyAgain = false;

  /**
   * @see EditorUpdateEvent#selectionCoordsChanged()
   */
  private boolean notedSelectionCoordsChanged = false;

  /**
   * @see EditorUpdateEvent#selectionLocationChanged()
   */
  private boolean notedSelectionLocationChanged = false;

  /**
   * @see EditorUpdateEvent#contentChanged()
   */
  private boolean notedContentChanged = false;

  /**
   * @see EditorUpdateEvent#contentChangedDirectlyByUser()
   */
  private boolean notedUserDirectlyChangedContent = false;

  /** Used for debugging, e.g. tracking down bad handlers */
  private final StringSet suppressedEventNames = CollectionUtils.createStringSet();

  /** Task to notify listeners of an update in the editor */
  IncrementalTask notificationTask = new IncrementalTask() {
    int delays = 0;

    @Override
    public boolean execute() {
      if (!editor.isConsistent()) {
        // reschedule
        if (EditorStaticDeps.logger.trace().shouldLog()) {
          EditorStaticDeps.logger.trace().log("Notification deferred for consistency reasons");
        }
        delays++;
        if (delays == 20) {
          EditorStaticDeps.logger.error().log("More than 20 notification delays encountered - " +
              "possibly uncleared extraction state");
        }

        notifyAgain = true;
      } else {
        if (editor.hasDocument()) {
          if (EditorStaticDeps.logger.trace().shouldLog()) {
            EditorStaticDeps.logger.trace().log("EditorUpdateEvent: " +
                "selCoords:" + notedSelectionCoordsChanged + ", " +
                "selLoc:" + notedSelectionLocationChanged + ", " +
                "content:" + notedContentChanged + ", " +
                "userDirectlyChangedContent:" + notedUserDirectlyChangedContent);
          }

          // alert the listeners:
          for (EditorUpdateEvent.EditorUpdateListener l : updateListeners) {
            if (suppressedEventNames.contains(l.getClass().getName())) {
              continue;
            }

            l.onUpdate(EditorUpdateEventImpl.this);
          }
          notedSelectionCoordsChanged = false;
          notedSelectionLocationChanged = false;
          notedContentChanged = false;
          notedUserDirectlyChangedContent = false;
        }
        delays = 0;
        if (EditorStaticDeps.logger.trace().shouldLog()) {
          EditorStaticDeps.logger.trace().log("Notification sent");
        }
      }

      boolean ret = notifyAgain;
      notifyAgain = false;
      return ret;
    }

    @Override
    public String toString() {
      return "EditorUpdateEventImpl.notificationTask [update listeners: " + updateListeners + "]";
    }
  };

  EditorUpdateEventImpl(EditorImpl editor) {
    this.editor = editor;
  }

  @Override
  public boolean selectionCoordsChanged() {
    return notedSelectionCoordsChanged;
  }

  @Override
  public boolean selectionLocationChanged() {
    return notedSelectionLocationChanged;
  }

  @Override
  public boolean contentChanged() {
    return notedContentChanged;
  }

  @Override
  public boolean contentChangedDirectlyByUser() {
    return notedUserDirectlyChangedContent;
  }

  /**
   * Schedule the editor's update notification
   */
  void scheduleUpdateNotification(
      boolean selectionCoordsChanged,
      boolean selectionLocationChanged,
      boolean contentChanged,
      boolean userDirectlyChangedContent) {
    // Internal editor throttling
    // We want special behaviour, here where we do not reset the delay every
    // time this method is called by rescheduling - so we first check if the
    // notification task is scheduled.
    notedSelectionCoordsChanged |= selectionCoordsChanged;
    notedSelectionLocationChanged |= selectionLocationChanged;
    notedContentChanged |= contentChanged;
    notedUserDirectlyChangedContent |= userDirectlyChangedContent;

    Scheduler scheduler = SchedulerInstance.get();
    if (!scheduler.isScheduled(notificationTask)) {
      scheduler.scheduleRepeating(Priority.MEDIUM, notificationTask,
          INITIAL_NOTIFY_SCHEDULE_DELAY_MS, NOTIFY_SCHEDULE_DELAY_GAP_MS);
    } else {
      notifyAgain = true;
    }
  }

  void addUpdateListener(EditorUpdateEvent.EditorUpdateListener listener) {
    updateListeners.add(listener);
  }

  void removeUpdateListener(EditorUpdateEvent.EditorUpdateListener listener) {
    updateListeners.remove(listener);
  }

  void flushUpdates() {
    SchedulerInstance.get().cancel(notificationTask);
    notificationTask.execute();
  }

  void clear() {
    SchedulerInstance.get().cancel(notificationTask);
    updateListeners.clear();
  }

  @Override
  public EditorContext context() {
    return editor;
  }

  /**
   * Suppresses running of update events whose class name matches {@code name}.
   * For debugging only.
   *
   * @param name class name of object implementing
   *        {@link EditorUpdateEvent.EditorUpdateListener}
   * @param suppress trrue to suppress, false to allow
   */
  public void debugSuppressUpdateEvent(String name, boolean suppress) {
    if (suppress) {
      suppressedEventNames.add(name);
    } else {
      suppressedEventNames.remove(name);
    }
  }

  /**
   * Gets the class names of all registered event listeners. For debugging only.
   *
   * @return a new string set each time, containing the class names of all
   *         registered update event listeners. it is safe to modify this set.
   */
  public StringSet debugGetAllUpdateEventNames() {
    StringSet events = CollectionUtils.createStringSet();
    for (EditorUpdateEvent.EditorUpdateListener l : updateListeners) {
      events.add(l.getClass().getName());
    }
    return events;
  }

  /**
   * Gets the class names of suppressed event listeners. For debugging only.
   *
   * @return a live readable view of the currently suppressed events.
   */
  public ReadableStringSet debugGetSuppressedUpdateEventNames() {
    return suppressedEventNames;
  }
}
