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

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Convenience type alias for a range represented by two ContentNode Points.
 *
 * First and Second will always be ordered
 *
 * Focus and Anchor can be either way
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ContentRange extends PointRange<ContentNode> {

  /** Constructor */
  public ContentRange(Point<ContentNode> collapsedAt) {
    super(collapsedAt);
  }

  /** Constructor */
  public ContentRange(Point<ContentNode> first, Point<ContentNode> second) {
    super(first, second);
  }

// TODO: finish
//
//
//  private boolean areOrdered(Point<ContentNode> first, Point<ContentNode> second) {
//    ContentNode firstNode = first.getContainer();
//    ContentNode secondNode = second.getContainer();
//
//    ContentNode firstNodeAfter = first.getNodeAfter();
//    ContentNode secondNodeAfter = second.getNodeAfter();
//    if (firstNode == secondNode) {
//      if (first.isInTextNode()) {
//        return first.getTextOffset() <= second.getTextOffset();
//      } else {
//        if (firstNodeAfter == null) {
//          return secondNodeAfter == null;
//        } else {
//          return secondNodeAfter == null ? true : firstNodeAfter.compareTo(secondNodeAfter) < 0;
//        }
//      }
//    }
//
//    if (!first.isInTextNode() && first.getNodeAfter())
//
//    if (first.isInTextNode()) {
//      if (second.isInTextNode()) {
//        int cmp = firstNode.compareTo(secondNode);
//        if (cmp == 0) {
//          return first.getTextOffset() <= second.getTextOffset();
//        } else {
//          return cmp < 0;
//        }
//      } else if (second.getNodeAfter() == null) {
//        return !secondNode.isOrIsAncestorOf(firstNode) && firstNode.compareTo(secondNode) < 0;
//      } else {
//        return firstNode.compareTo(second.getNodeAfter()) < 0;
//      }
//    } else if (firstNodeAfter == null) {
//      if (second.isInTextNode()) {
//        return !areOrdered(second, first);
//      } else if (second.getNodeAfter() == null) {
//        return
//      }
//    }
//  }
}
