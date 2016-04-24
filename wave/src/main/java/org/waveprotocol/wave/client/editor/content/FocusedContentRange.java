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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Convenience type alias
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FocusedContentRange extends FocusedPointRange<ContentNode>{

  private ContentRange orderedRange;
  private boolean isOrdered;

  /**
   * @param anchor
   * @param focus
   */
  public FocusedContentRange(Point<ContentNode> anchor, Point<ContentNode> focus) {
    super(anchor, focus);
  }

  /**
   * @param collapsedAt
   */
  public FocusedContentRange(Point<ContentNode> collapsedAt) {
    super(collapsedAt);
  }

  /**
   * Returns this range as an ordered range.
   *
   * Ordered means that the focus comes after the anchor (or they are the same)
   *
   * @param isOrdered redundant variable - it is a function of the range's
   *   focus and anchor. However, the check is expensive, and so instead we
   *   rely on the caller to know if the range is ordered or not, and ensure
   *   the correctness of the isOrdered parameter.
   * @return an ordered range
   */
  public ContentRange asOrderedRange(boolean isOrdered) {
    if (orderedRange == null) {
      orderedRange = isOrdered
          ? new ContentRange(getAnchor(), getFocus())
          : new ContentRange(getFocus(), getAnchor());
      this.isOrdered = isOrdered;
    } else {
      Preconditions.checkState(isOrdered == this.isOrdered,
          "Different isOrdered input from last time");
    }

    return orderedRange;
  }
}
