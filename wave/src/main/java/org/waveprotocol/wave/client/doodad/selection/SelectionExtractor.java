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

package org.waveprotocol.wave.client.doodad.selection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Contains logic for reporting a selection as annotations.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SelectionExtractor implements EditorUpdateListener {

  private final TimerService clock;
  private final String address;
  private final String sessionId;

  public SelectionExtractor(TimerService clock, String address, String sessionId) {
    Preconditions.checkNotNull(address, "Address must not be null");
    Preconditions.checkNotNull(sessionId, "Session id must not be null");
    this.clock = clock;
    this.address = address;
    this.sessionId = sessionId;
  }

  /**
   * Starts writing the location of this session's browser selection into the
   * edited document.
   */
  public void start(EditorContext context) {
    writeSelection(context);
    context.addUpdateListener(this);
  }

  /**
   * Stops writing the location of this session's browser selection into the
   * edited document.
   */
  public void stop(EditorContext context) {
    context.removeUpdateListener(this);
    MutableAnnotationSet.Persistent document = context.getDocument();
    int size = document.size();
    String rangeKey = SelectionAnnotationHandler.rangeKey(sessionId);
    String endKey = SelectionAnnotationHandler.endKey(sessionId);
    String dataKey = SelectionAnnotationHandler.dataKey(sessionId);

    document.setAnnotation(0, size, dataKey, null);
    document.setAnnotation(0, size, rangeKey, null);
    document.setAnnotation(0, size, endKey, null);
  }

  @Override
  public void onUpdate(EditorUpdateEvent event) {
    if (event.selectionLocationChanged()) {
      EditorContext context = event.context();

      // Special case to exempt caret annotations from being undoable.
      context.getResponsibilityManager().startIndirectSequence();
      try {
        writeSelection(context);
      } finally {
        context.getResponsibilityManager().endIndirectSequence();
      }
    }
  }

  private void writeSelection(EditorContext context) {
    MutableAnnotationSet.Persistent document = context.getDocument();
    FocusedRange selection = context.getSelectionHelper().getSelectionRange();
    String compositionState = context.getImeCompositionState();
    double currentTimeMillis = clock.currentTimeMillis();

    writeSelection(document, selection, compositionState, currentTimeMillis);
  }

  @VisibleForTesting
  void writeSelection(MutableAnnotationSet.Persistent document, FocusedRange selection,
      String compositionState, double currentTimeMillis) {
    // TODO(danilatos): Use focus and not end
    Range range = selection == null ? null : selection.asRange();
    String rangeKey = SelectionAnnotationHandler.rangeKey(sessionId);
    String endKey = SelectionAnnotationHandler.endKey(sessionId);
    String dataKey = SelectionAnnotationHandler.dataKey(sessionId);
    String value = address;

    int size = document.size();

    // If we have a selection, then continually update regardless of old value,
    // to refresh the timestamp.
    if (range != null) {
      document.setAnnotation(0, size, dataKey, address + "," + currentTimeMillis + ","
          + (compositionState != null ? compositionState : ""));
    }

    // TODO(danilatos): This fiddliness is necessary to avoid gratuitous
    // re-rendering.
    // Later the code can just use the resetAnnotation method, which will be
    // MUCH
    // simpler, once we have proper fine-granularity notifications.
    int currentFocus = document.firstAnnotationChange(0, size, endKey, null);
    int currentEnd = document.lastAnnotationChange(0, size, rangeKey, null);
    if (currentEnd == -1) {
      currentEnd = currentFocus;
    }
    if (currentEnd != -1) {
      // if old selection is annotated
      int currentStart = document.firstAnnotationChange(0, size, rangeKey, null);
      if (currentStart == -1 || currentStart > currentEnd) {
        currentStart = currentEnd;
      }
      if (range != null) {
        // if new selection exists
        int newStart = range.getStart();
        int newEnd = range.getEnd();
        int newFocus = selection.getFocus();

        if (newFocus < currentFocus) {
          document.setAnnotation(newFocus, currentFocus, endKey, value);
        } else if (newFocus > currentFocus) {
          document.setAnnotation(currentFocus, newFocus, endKey, null);
        }

        if (currentStart >= newEnd || newStart >= currentEnd) {
          // If not overlapping
          document.setAnnotation(currentStart, currentEnd, rangeKey, null);
          document.setAnnotation(newStart, newEnd, rangeKey, value);
        } else {
          // If overlapping

          if (currentStart < newStart) {
            document.setAnnotation(currentStart, newStart, rangeKey, null);
          } else if (currentStart > newStart) {
            document.setAnnotation(newStart, currentStart, rangeKey, value);
          }
          if (currentEnd < newEnd) {
            document.setAnnotation(currentEnd, newEnd, rangeKey, value);
          } else if (currentEnd > newEnd) {
            document.setAnnotation(newEnd, currentEnd, rangeKey, null);
          }
        }
      } else {
        // no new selection, clear old one
        document.setAnnotation(currentFocus, size, endKey, null);
        document.setAnnotation(currentStart, currentEnd, rangeKey, null);
        document.setAnnotation(0, size, dataKey, null);
      }
    } else {
      // no old selection
      if (range != null) {
        // new selection exists
        document.setAnnotation(selection.getFocus(), size, endKey, value);
        document.setAnnotation(range.getStart(), range.getEnd(), rangeKey, value);
      }
    }
  }
}
