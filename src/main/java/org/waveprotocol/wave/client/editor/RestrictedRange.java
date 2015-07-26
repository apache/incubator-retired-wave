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

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Class representing a range that satisfies two constraints:
 *   1) It lies between node boundaries (not in text nodes)
 *   2) The two logical Points it represents share the same container
 *
 * This restricted scenario arises quite frequently, and this class
 * statically enforces the constraints.
 *
 * One of the most important aspects that distinguishes it from just
 * a pair of points is that, for the start of the range, we store
 * the node BEFORE, not the usual after. Therefore, the "node before"
 * and "node after" are not in the range, they bound it. This is
 * particularly useful when we wish to define a range that will
 * remain valid despite any changes to the nodes it contains.
 *
 * Numerous constructors and helper methods are provided. Some,
 * as usual, expect a document implementation for the range's
 * node type.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <N> Node type
 */
public class RestrictedRange<N> {
  /** Point after the range. */
  private final Point.El<N> after;
  /** node before the range */
  private final N nodeBefore;

  private RestrictedRange(Point.El<N> after, N nodeBefore) {
    this.after = after;
    this.nodeBefore = nodeBefore;
  }

  /**
   * Construct a restricted range bounded by the given parent,
   * node before and node after.
   */
  public static <N> RestrictedRange<N> boundedBy(N parent, N before, N after) {
    return new RestrictedRange<N>(Point.inElement(parent, after), before);
  }

  /**
   * Construct a collapsed range at the given point. Due to the properties
   * of this object, if nodes get inserted at that point, the range will
   * expand but remain valid.
   */
  public static <N, E extends N, T extends N> RestrictedRange<N> collapsedAt(
      ReadableDocument<N, E, T> doc, Point.El<N> collapsedAt) {
    return new RestrictedRange<N>(collapsedAt, Point.nodeBefore(doc, collapsedAt));
  }

  /**
   * Construct a collapsed range encompassing the given nodes. If the node(s) are
   * removed, the range will be collapsed but remain valid, if the node(s) have
   * siblings added, the range will expand.
   */
  public static <N, E extends N, T extends N> RestrictedRange<N> around(
      ReadableDocument<N, E, T> doc, N firstBoundedNode, N lastBoundedNode) {
    return new RestrictedRange<N>(
        Point.after(doc, lastBoundedNode),
        doc.getPreviousSibling(firstBoundedNode));
  }

  /**
   * Construct a range bounded by the node before, and the point after.
   */
  public static <N> RestrictedRange<N> between(N nodeBefore, Point.El<N> after) {
    return new RestrictedRange<N>(after, nodeBefore);
  }

  /**
   * Construct a range bounded by the two given element points. The document
   * parameter is required to find the node before.
   */
  public static <N, E extends N, T extends N> RestrictedRange<N> between(
      ReadableDocument<N, E, T> doc, Point.El<N> before, Point.El<N> after) {
    assert before.getContainer().equals(after.getContainer());
    N nodeBefore = before.getNodeAfter() == null
        ? doc.getLastChild(before.getContainer())
        : doc.getPreviousSibling(before.getNodeAfter());
    return new RestrictedRange<N>(after, nodeBefore);
  }

  /**
   * Constructs a restricted range from two arbitrary points. If the points
   * are in text nodes, they are rounded outwards to node boundaries.
   */
  public static <N, E extends N, T extends N> RestrictedRange<N> roundedBetween(
      ReadableDocument<N, E, T> doc, Point<N> before, Point<N> after) {
    Point.El<N> beforeSafe, afterSafe;
    beforeSafe = before.isInTextNode()
        ? Point.before(doc, before.getContainer())
        : before.asElementPoint();
    afterSafe = after.isInTextNode()
        ? Point.after(doc, after.getContainer())
        : after.asElementPoint();
    return between(doc, beforeSafe, afterSafe);
  }

  /**
   * @return The first node at the start of the range. (If the range is collapsed, this would
   *   basically be nodeAfter).
   */
  public <E extends N, T extends N> N getStartNode(ReadableDocument<N, E, T> doc) {
    N firstNode = nodeBefore == null
        ? doc.getFirstChild(after.getContainer())
        : doc.getNextSibling(nodeBefore);
    return firstNode;
  }

  /**
   * @return true if the given node is inside the range.
   * NOTE(danilatos): Only the top level is searched (no descending into child nodes)
   */
  public <E extends N, T extends N> boolean contains(ReadableDocument<N, E, T> doc, N node) {
    N fromIncl = getStartNode(doc);
    N toExcl = getNodeAfter();
    return isBetween(doc, node, fromIncl, toExcl);
  }

  /**
   * @return A point representing the start of the range
   */
  public <E extends N, T extends N> Point.El<N> getPointBefore(ReadableDocument<N, E, T> doc){
    return Point.inElement(after.getContainer(), getStartNode(doc));
  }

  /**
   * @return A point representing the end of the range
   */
  public Point.El<N> getPointAfter() {
    return after;
  }

  /** @return the "node before" */
  public N getNodeBefore() {
    return nodeBefore;
  }

  /** @return the "node after" */
  public N getNodeAfter() {
    return after.getNodeAfter();
  }

  /** @return the element immediately encompassing the range */
  public N getContainer() {
    return after.getContainer();
  }

  /**
   * Checks if a node is between fromInc1 and toExec1.
   * @param <N> node type
   * @param <E> element type
   * @param <T> text type
   * @param doc Needed to walk the nodes
   * @param node The node to check
   * @param fromIncl Is the node parameter at or after this node?
   * @param toExcl Is the node parameter before and not including this node?
   * @return true if the node parameter is in the specified range
   */
  private static <N, E extends N, T extends N> boolean isBetween(
      ReadableDocument<N, E, T> doc, N node, N fromIncl, N toExcl) {
    for (N n = fromIncl; n != null && n != toExcl; n = doc.getNextSibling(n)) {
      if (n == node) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return after.getContainer() + " ->( " + nodeBefore + " - " + after.getNodeAfter() + " )";
  }
}
