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


/**
 * Convenience type alias for a range represented by two Points.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class PointRange<N> {
  private final Point<N> first;
  private final Point<N> second;
  private final boolean isCollapsed;

  /**
   * Constructs a collapsed range.
   *
   * @param collapsedAt The point at which the collapsed range is located.
   */
  public PointRange(Point<N> collapsedAt) {
    assert collapsedAt != null;
    first = collapsedAt;
    second = collapsedAt;
    isCollapsed = true;
  }

  /** Constructor */
  public PointRange(Point<N> first, Point<N> second) {
    assert first != null : "First must not be null.";
    assert second != null : "Second must not be null.";
    this.first = first;
    this.second = second;
    isCollapsed = first.equals(second);
  }

  /**
   * @return True if the range is collapsed
   */
  public boolean isCollapsed() {
    return isCollapsed;
  }

  /**
   * @return first
   */
  public Point<N> getFirst() {
    return first;
  }

  /**
   * @return second
   */
  public Point<N> getSecond() {
    return second;
  }

  // Eclipse generated methods below

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return result;
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PointRange) {
      final PointRange other = (PointRange) obj;
      return first.equals(other.first) && second.equals(other.second);
    }
    return false;
  }

  @Override
  public String toString() {
    return isCollapsed() ? "Collapsed:[" + first + "]" : "Range:[" + first + ", " + second + "]";
  }
}
