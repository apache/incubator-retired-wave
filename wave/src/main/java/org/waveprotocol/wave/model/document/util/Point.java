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

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Represents a point in a DOM. A point is the empty space between nodes or characters.
 * Immutable.
 *
 * A point can be described either by
 * 1. A parent node + child offset (node offset for element parent, char offset for text parent)
 * 2. A parent node + node after (the node after the point)
 *
 * "Final" - private constructors only.
 *
 * TODO(danilatos): Consider boxing up the doc in the point?
 * Disadvantages: can't have different views for the same point object, extra field required
 * Advantages: Simplify method signatures, safer equals method based on isSameNode
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <N> The Node type used in the DOM.
 */
public abstract class Point<N> {


  /**
   * Useful argument checking method for validating a point with respect
   * to a document
   *
   * @param doc
   * @param point
   * @param msgPrefix string to prepend to exception, if thrown
   */
  public static <N, E extends N, T extends N> void checkPoint(
      ReadableDocument<N, E, T> doc, Point<N> point, String msgPrefix) {
    if (point.isInTextNode()) {
      checkOffset(doc, doc.asText(point.getContainer()), point.getTextOffset(), msgPrefix);
    } else {
      checkRelationship(doc, doc.asElement(point.getContainer()), point.getNodeAfter(), msgPrefix);
    }
  }

  /**
   * Useful argument checking method for validating that an offset lies
   * within a text node
   *
   * @param doc
   * @param textNode
   * @param offset
   * @param msgPrefix string to prepend to exception, if thrown
   */
  public static <N, T extends N> void checkOffset(
      ReadableDocument<N, ?, T> doc, T textNode, int offset, String msgPrefix) {
    Preconditions.checkNotNull(textNode, "Container must not be null");
    if (offset < 0 || offset > doc.getLength(textNode)) {
      throw new IllegalArgumentException(
          msgPrefix + ": offset '" + offset + "' is not inside text node, " +
          "length " + doc.getLength(textNode) + ", text: '" + doc.getData(textNode) + "'");
    }
  }

  /**
   * Useful argument checking method for validating that a nodeAfter is
   * null or the child of the given parent
   *
   * @param doc
   * @param parent
   * @param nodeAfter
   * @param msgPrefix string to prepend to exception, if thrown
   */
  public static <N, E extends N> void checkRelationship(
      ReadableDocument<N, E, ?> doc, E parent, N nodeAfter, String msgPrefix) {
    Preconditions.checkNotNull(parent, "Container must not be null");
    if (nodeAfter != null && doc.getParentElement(nodeAfter) != parent) {
      throw new IllegalArgumentException(
          msgPrefix + ": nodeAfter must be null or a child of parent");
    }
  }

  /**
   * An Element-Node style point
   * Use this for type safety where possible
   */
  public final static class El<X> extends Point<X> {
    El(El<X> other) {
      super(other);
    }

    El(X container, X nodeAfter) {
      super(container, nodeAfter);
    }

    /** {@inheritDoc} */
    @Override
    public Point.El<X> asElementPoint() {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Point.Tx<X> asTextPoint() {
      return null;
    }
  }

  /**
   * A Text-Offset style point
   * Use this for type safety where possible
   */
  public final static class Tx<X> extends Point<X> {
    Tx(Tx<X> other) {
      super(other);
    }

    Tx(X container, int offset) {
      super(container, offset);
    }

    /** {@inheritDoc} */
    @Override
    public Point.El<X> asElementPoint() {
      return null;
    }

    /** {@inheritDoc} */
    @Override
    public Point.Tx<X> asTextPoint() {
      return this;
    }
  }

  private final N container;
  private final N nodeAfter;
  private final int offset;

  /**
   * Same as {@link #enclosingElement(ReadableDocument, Point)}, except
   * does it for a node. So if it is an element, return it, if it is
   * a text node, return its parent.
   */
  public static <N, E extends N, T extends N> E enclosingElement(
      ReadableDocument<N,E,T> doc, N node) {
    E el = doc.asElement(node);
    return el == null ? doc.getParentElement(node) : el;
  }

  /**
   * Get the nearest enclosing element (not text node). For an e-n point, that
   * is simply for the container. For a t-o point, that is the container's parent.
   * @param doc
   * @param point
   * @return The nearest enclosing Element
   */
  public static <N, E extends N, T extends N> E enclosingElement(
      ReadableDocument<N,E,T> doc, Point<N> point) {
    return enclosingElement(doc, point.getContainer());
  }

  /**
   * @param doc
   * @param point
   * @return Node before the given point
   */
  public static <N, E extends N, T extends N> E elementBefore(
      ReadableDocument<N,E,T> doc, Point<N> point) {
    if (point.isInTextNode()) {
      if (point.getTextOffset() > 0) {
        return null;
      } else {
        return doc.asElement(doc.getPreviousSibling(point.getContainer()));
      }
    } else {
      return doc.asElement(nodeBefore(doc, point.asElementPoint()));
    }
  }

  /**
   * @param doc
   * @param point
   * @return Node before the given point
   */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N> E elementAfter(
      ReadableDocument<N,E,T> doc, Point<N> point) {
    if (point.isInTextNode()) {
      if (point.getTextOffset() < doc.getLength((T) point.getContainer())) {
        return null;
      } else {
        return doc.asElement(doc.getNextSibling(point.getContainer()));
      }
    } else {
      return doc.asElement(point.getNodeAfter());
    }
  }

  /**
   * If the given point is equivalent to the end of the inside of an element,
   * return that element, otherwise null.
   */
  @SuppressWarnings("unchecked")
  public static <N, E extends N, T extends N> E elementEndingAt(
      ReadableDocument<N,E,T> doc, Point<N> point) {
    if (point.isInTextNode()) {
      if (point.getTextOffset() < doc.getLength((T) point.getContainer())) {
        return null;
      } else if (doc.getNextSibling(point.getContainer()) == null) {
        return doc.getParentElement(point.getContainer());
      } else {
        return null;
      }
    } else {
      return point.getNodeAfter() == null ? (E) point.getContainer() : null;
    }
  }


  /**
   * @param doc
   * @param point
   * @return Node before the given point
   */
  public static <N, E extends N, T extends N> N nodeBefore(
      ReadableDocument<N,E,T> doc, Point.El<N> point) {
    N node = point.getNodeAfter();
    return node == null ? doc.getLastChild(point.getContainer()) : doc.getPreviousSibling(node);
  }

  /**
   * Construct a point before the given node
   */
  public static <N, E extends N, T extends N> El<N> before(
      ReadableDocument<N,E,T> doc, N node) {
    return new El<N>(doc.getParentElement(node), node);
  }

  /**
   * Construct a point after the given node
   */
  public static <N, E extends N, T extends N> El<N> after(
      ReadableDocument<N,E,T> doc, N node) {
    return new El<N>(doc.getParentElement(node), doc.getNextSibling(node));
  }

  /**
   * Construct a point inside the start of the given element (just after the
   * start tag).
   */
  public static <N, E extends N> El<N> start(
      ReadableDocument<N,E,?> doc, E element) {
    return new El<N>(element, doc.getFirstChild(element));
  }

  /**
   * Construct a point at the start of either an element or text node.
   * @param doc Document containing the node
   * @param node The node to get the start of
   * @return The Point.El start if node is an element, otherwise the position at text offset 0.
   */
  public static <N, E extends N, T extends N> Point<N> textOrElementStart(
      ReadableDocument<N,E,T> doc, N node) {
    E elt = doc.asElement(node);
    return elt == null ? inText(node, 0) : start(doc, elt); // change based on type of node
  }

  /**
   * Construct a point at the end of either an element or text node.
   * @param doc Document containing the node
   * @param node The node to get the start of
   * @return The Point.El end if node is an element, otherwise the position at the last text offset.
   */
  public static <N, E extends N, T extends N> Point<N> textOrElementEnd(
      ReadableDocument<N,E,T> doc, N node) {
    E elt = doc.asElement(node);
    return elt == null ? inText(node, doc.getData(doc.asText(node)).length()) : end(node);
  }


  /**
   * Construct a point inside the end of the given element (just before the
   * end tag).
   */
  public static <N> El<N> end(N element) {
    return new El<N>(element, null);
  }

  /**
   * Construct an e-n point in element, just before nodeAfter (if nodeAfter is
   * null, then inside the end of element).
   *
   * @param element
   * @param nodeAfter
   */
  public static <N> El<N> inElement(N element, N nodeAfter) {
    return new El<N>(element, nodeAfter);
  }

  /**
   * Similar to {@link #inElement(Object, Object)}
   *
   * @param element Parent
   * @param nodeBefore The node BEFORE the point, or null for the START of parent
   * @return point
   */
  public static <N, E extends N, T extends N> El<N> inElementReverse(
      ReadableDocument<N,E,T> doc, E element, N nodeBefore) {
    return new El<N>(element, nodeBefore == null
        ? doc.getFirstChild(element)
        : doc.getNextSibling(nodeBefore));
  }

  /**
   * Construct a t-o point inside textNode
   * @param textNode
   * @param charOffset
   */
  public static <N> Tx<N> inText(N textNode, int charOffset) {
    return new Tx<N>(textNode, charOffset);
  }

  /**
   * Construct a copy of a point
   */
  public static <N> Point<N> dup(Point<N> other) {
    return other instanceof El ? new El<N>((El<N>) other) : new Tx<N>((Tx<N>) other);
  }

  Point(N container, N nodeAfter) {
    Preconditions.checkNotNull(container, "Container must not be null");
    this.container = container;
    this.nodeAfter = nodeAfter;
    this.offset = -1;
  }

  Point(N container, int offset) {
    Preconditions.checkNotNull(container, "Container must not be null");
    this.container = container;
    this.nodeAfter = null;
    this.offset = offset;
  }

  private Point(Point<N> other) {
    this.container = other.container;
    this.nodeAfter = other.nodeAfter;
    this.offset = other.offset;
    assert container != null;
  }

  /**
   * @return Containing element
   */
  public N getContainer() {
    return container;
  }

  /**
   * @return The node after. Only valid for e-n points.
   */
  // TODO(danilatos): Make this method only on Point.El
  public N getNodeAfter() {
    if (isInTextNode()) {
      throw new IllegalStateException(
        "getNodeAfter() can only be called on points within elements");
    }
    return nodeAfter;
  }

  /**
   * @return The text offset. Only valid for t-o points.
   */
  //TODO(danilatos): Make this method only on Point.Tx
  public int getTextOffset() {
    if (!isInTextNode()) {
      throw new IllegalStateException(
        "getOffset() can only be called on points within text nodes");
    }
    return offset;
  }

  /**
   * Maybe cast to t-o point.
   * @return As a text point if it is one, or null otherwise
   */
  public abstract Point.Tx<N> asTextPoint();

  /**
   * Maybe cast to e-n point
   * @return As an element point if it is one, or null otherwise
   */
  public abstract Point.El<N> asElementPoint();

  /**
   * @return true if is a t-o point
   */
  public boolean isInTextNode() {
    return offset >= 0;
  }

  /**
   * @return true if the point is the inside of the end of an element
   *         (conceptually before the closing tag). Note, of course, that a
   *         point at the end of a text node before a closing tag will not
   *         return true for this.
   */
  public boolean isInElementEnd() {
    return !isInTextNode() && getNodeAfter() == null;
  }

  /**
   * @return true if the point immediately precedes a node. (conceptually before
   *         the closing tag). Note, of course, that a point at the end of a
   *         text node before a node will not return true for this.
   */
  public boolean isBeforeNode() {
    return !isInTextNode() && getNodeAfter() != null;
  }

  /**
   * The "canonical" node is
   * - for text points, the text node
   * - nodeAfter, when it is not null
   * - the container, if nodeAfter is null
   *
   * It would be desirable to change Point to become a 3 way disjunctive type
   * as opposed to the current two options (corresponding to the 3 options
   * above). This is because if nodeAfter is not null, the point class implies
   * that container is the parent of nodeAfter, which might be true only in
   * some filtered views of a document.
   *
   * @return the canonical node of this point
   */
  public N getCanonicalNode() {
    return isInTextNode() || nodeAfter == null ? container : nodeAfter;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "{" + container + "~" + (isInTextNode()
        ? getTextOffset() + "" : getNodeAfter() + "") + "}";
  }

  /**
   * eclipse generated hashCode & equals
   * Caveat: Assumption is that .equals() is valid for nodes
   */
  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((container == null) ? 0 : container.hashCode());
    result = prime * result + ((nodeAfter == null) ? 0 : nodeAfter.hashCode());
    result = prime * result + offset;
    return result;
  }

  /**
   * eclipse generated hashCode & equals
   * Caveat: Assumption is that .equals() is valid for nodes
   */
  @SuppressWarnings("unchecked")
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Point<N> other = (Point<N>) obj;
    if (container == null) {
      if (other.container != null) {
        return false;
      }
    } else if (!container.equals(other.container)) {
      return false;
    }
    if (nodeAfter == null) {
      if (other.nodeAfter != null) {
        return false;
      }
    } else if (!nodeAfter.equals(other.nodeAfter)) {
      return false;
    }
    if (offset != other.offset) {
      return false;
    }
    return true;
  }
}
