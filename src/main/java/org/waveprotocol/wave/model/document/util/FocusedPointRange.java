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
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class FocusedPointRange<N> {
  private final Point<N> anchor;
  private final Point<N> focus;
  private final boolean isCollapsed;

  /**
   * Constructs a collapsed range.
   *
   * @param collapsedAt The point at which the collapsed range is located.
   */
  public FocusedPointRange(Point<N> collapsedAt) {
    assert collapsedAt != null;
    anchor = collapsedAt;
    focus = collapsedAt;
    isCollapsed = true;
  }

  /**
   * @param anchor
   * @param focus
   */
  public FocusedPointRange(Point<N> anchor, Point<N> focus) {
    this.anchor = Preconditions.checkNotNull(anchor, "anchor");
    this.focus = Preconditions.checkNotNull(focus, "focus");
    this.isCollapsed = anchor.equals(focus);
  }

  /**
   * @return True if the range is collapsed
   */
  public boolean isCollapsed() {
    return isCollapsed;
  }

  /**
   * @return the anchor
   */
  public Point<N> getAnchor() {
    return anchor;
  }

  /**
   * @return the focus
   */
  public Point<N> getFocus() {
    return focus;
  }

  @Override
  public String toString() {
    return "FocusedPointRange(" + getAnchor() + " -> "  + getFocus() + ")";
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + anchor.hashCode();
    result = prime * result + focus.hashCode();
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof FocusedPointRange)) return false;
    FocusedPointRange other = (FocusedPointRange) obj;
    if (!anchor.equals(other.anchor)) return false;
    if (!focus.equals(other.focus)) return false;
    return true;
  }


}
