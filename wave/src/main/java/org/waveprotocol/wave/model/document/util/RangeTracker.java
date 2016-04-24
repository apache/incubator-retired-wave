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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.MutableAnnotationSet.Local;

/**
 * Uses local annotations to keep track of a range.
 *
 */
public class RangeTracker {
  private static final Object trackValue = new Object();

  private final Local annotations;
  private final String startKey;
  private final String endKey;
  private boolean isTracking = false;

  /**
   * @param localAnnotationSet
   * @param debugPrefix debug string used to generate tracking key.
   */
  public RangeTracker(Local localAnnotationSet, String debugPrefix) {
    this.annotations = localAnnotationSet;
    this.startKey = Annotations.makeUniqueLocal(debugPrefix);
    this.endKey = Annotations.makeUniqueLocal(debugPrefix);
  }

  /**
   * @see #RangeTracker(Local, String)
   * @param localAnnotationSet
   */
  public RangeTracker(Local localAnnotationSet) {
    this(localAnnotationSet, "tracker");
  }

  /**
   * Marks the given range.
   * @param range
   */
  public void trackRange(Range range) {
    isTracking = true;
    int size = annotations.size();
    annotations.resetAnnotation(range.getStart(), size, startKey, trackValue);
    annotations.resetAnnotation(range.getEnd(), size, endKey, trackValue);
  }

  /**
   * Marks the given range.
   * @param range
   */
  public void trackRange(FocusedRange range) {
    isTracking = true;
    int size = annotations.size();
    annotations.resetAnnotation(range.getAnchor(), size, startKey, trackValue);
    annotations.resetAnnotation(range.getFocus(), size, endKey, trackValue);
  }

  /**
   * Clears the marked range.
   */
  public void clearRange() {
    isTracking = false;
    annotations.resetAnnotation(0, annotations.size(), startKey, null);
    annotations.resetAnnotation(0, annotations.size(), endKey, null);
  }

  /**
   * Gets the marked range, ensuring start <= end
   */
  // TODO(danilatos): Rename to getOrderedRange?
  public Range getRange() {
    FocusedRange r = getFocusedRange();
    return r != null ? r.asRange() : null;
  }

  /**
   * @return the marked range
   */
  public FocusedRange getFocusedRange() {
    if (!isTracking) {
      return null;
    }

    int size = annotations.size();
    int start = Annotations.firstAnnotationBoundary(annotations, 0, size, startKey, null);
    int end = Annotations.firstAnnotationBoundary(annotations, 0, size, endKey, null);

    return new FocusedRange(start, end);
  }
}
