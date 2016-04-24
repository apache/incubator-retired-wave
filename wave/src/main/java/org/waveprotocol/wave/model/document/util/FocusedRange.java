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


import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Similar to a {@link Range}, except it is not canonicalised to have a start
 * &lt;= end. Instead, it has an "anchor" and a "focus", which do not have
 * ordering constraints
 *
 * Start and end are also provided for a canonicalised view.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class FocusedRange {

  private final int anchor;
  private final int focus;

  private Range range;

  /**
   * Construct a range
   *
   * @param anchor
   * @param focus
   */
  public FocusedRange(int anchor, int focus) {
    if (anchor < 0 || focus < 0) {
      Preconditions.illegalArgument("Bad focused range: (" + anchor + ", " + focus + ")");
    }
    this.anchor = anchor;
    this.focus = focus;
  }

  /**
   * Create from an ordered range
   *
   * @param range
   * @param ordered
   */
  public FocusedRange(Range range, boolean ordered) {
    if (ordered) {
      this.anchor = range.getStart();
      this.focus = range.getEnd();
    } else {
      this.anchor = range.getEnd();
      this.focus = range.getStart();
    }
  }

  /**
   * Construct a collapsed range
   *
   * @param collapsedAt
   */
  public FocusedRange(int collapsedAt) {
    this(collapsedAt, collapsedAt);
  }

  /**
   * @return anchor location, may or may not be before the focus
   */
  public int getAnchor() {
    return anchor;
  }

  /**
   * @return focus location, may or may not be before the anchor
   */
  public int getFocus() {
    return focus;
  }

  /**
   * @return true if the range is collapsed
   */
  public boolean isCollapsed() {
    return anchor == focus;
  }

  /**
   * @return true if the anchor is less than or equal to the focus
   */
  public boolean isOrdered() {
    return anchor <= focus;
  }

  /**
   * Get an guaranteed ordered range out of the current possibly unordered
   * range.
   *
   * The return value is cached
   */
  public Range asRange() {
    if (range == null) {
      range = anchor < focus ? new Range(anchor, focus) : new Range(focus, anchor);
    }
    return range;
  }

  @Override
  public int hashCode() {
    return anchor + 37 * focus;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof FocusedRange)) return false;
    final FocusedRange other = (FocusedRange) obj;
    if (focus != other.focus) return false;
    if (anchor != other.anchor) return false;
    return true;
  }

  @Override
  public String toString() {
    return "FocusedRange(" + getAnchor()
        + (isCollapsed() ? "" : "->" + getFocus())
        + ")";
  }

}
