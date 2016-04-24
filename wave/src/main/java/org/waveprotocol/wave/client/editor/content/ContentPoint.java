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

/**
 * Content points
 *
 * NOTE(danilatos): This is a MUTABLE object
 * TODO(danilatos): Deprecate this in favour of Point<ContentNode>
 *
 */
public final class ContentPoint {

  /**
   * The node
   */
  private ContentNode container;

  private ContentNode nodeAfter;

  /**
   * The offset
   */
  private int offset;

  /** Copy constructor */
  public ContentPoint(ContentPoint other) {
    container = other.container;
    nodeAfter = other.nodeAfter;
    offset = other.offset;
  }

  /**
   * Element-Node point
   * @see Point#inElement(Object, Object)
   */
  public ContentPoint(ContentElement container, ContentNode nodeAfter) {
    this.container = container;
    this.nodeAfter = nodeAfter;
    this.offset = -1;
  }

  /**
   * Text-Offset point
   * @see Point#inText(Object, int)
   */
  public ContentPoint(ContentTextNode textNode, int offset) {
    this.container = textNode;
    this.nodeAfter = null;
    this.offset = offset;
  }

  /**
   * @return The content point as a {@link Point}
   */
  public Point<ContentNode> asPoint() {
    return isInTextNode()
      ? Point.inText(container, offset)
      : Point.inElement(container, nodeAfter);
  }

  /**
   * Construct from a Point
   */
  public static ContentPoint fromPoint(Point<ContentNode> point) {
    return point.isInTextNode()
      ? new ContentPoint((ContentTextNode)point.getContainer(), point.getTextOffset())
      : new ContentPoint((ContentElement)point.getContainer(), point.getNodeAfter());
  }

  /**
   * @return The container node
   */
  public ContentNode getContainer() {
    return container;
  }

  /**
   * @return The node after the point. Only valid for element-node points
   */
  public ContentNode getNodeAfter() {
    assert !isInTextNode();
    return nodeAfter;
  }

  /**
   * @return The text offset. Only valid for text-offset points
   */
  public int getTextOffset() {
    assert isInTextNode();
    return offset;
  }

  /**
   * @return true if this is a text-offset point.
   */
  public boolean isInTextNode() {
    return offset >= 0;
  }

  /**
   * Mutator
   * @see #ContentPoint(ContentElement, ContentNode)
   */
  public ContentPoint set(ContentElement container, ContentNode nodeAfter) {
    this.container = container;
    this.nodeAfter = nodeAfter;
    this.offset = -1;
    return this;
  }

  /**
   * Mutator
   * @see #ContentPoint(ContentTextNode, int)
   */
  public ContentPoint set(ContentTextNode container, int offset) {
    this.container = container;
    this.offset = offset;
    this.nodeAfter = null;
    return this;
  }

  /**
   * Set the text offset. Only valid for text-offset points
   * @param offset
   */
  public void setTextOffset(int offset) {
    if (!isInTextNode()) {
      throw new RuntimeException("Can't set text offset of a point not in a text node");
    }
    this.offset = offset;
  }

  /**
   * Sets point at beginning of input node
   *
   * @param node
   * @return this for convenience
   */
  public ContentPoint setToBeginning(ContentNode node) {
    if (node instanceof ContentElement) {
      set((ContentElement)node, node.getFirstChild());
    } else {
      set((ContentTextNode)node, 0);
    }
    return this;
  }

  /**
   * Sets point at end of input node
   *
   * @param node
   * @return this for convenience
   */
  public ContentPoint setToEnd(ContentNode node) {
    if (node instanceof ContentElement) {
      set((ContentElement)node, null);
    } else {
      ContentTextNode textNode = (ContentTextNode) node;
      set(textNode, textNode.getLength());
    }
    return this;
  }

  /**
   * Sets point outside + before input node
   *
   * NOTE(user): potentially costly to call findChildIndex!
   *
   * @param node
   * @return this for convenience
   */
  public ContentPoint setToBefore(ContentNode node) {
    set(node.getParentElement(), node);
    return this;
  }

  /**
   * Sets point outside + after input node
   *
   * NOTE(user): potentially costly to call findChildIndex!
   *
   * @param node
   * @return this for convenience
   */
  public ContentPoint setToAfter(ContentNode node) {
    set(node.getParentElement(), node.getNextSibling());
    return this;
  }

  /**
   * @return the node before the current point in the given view, or null
   *   if isInTextNode
   */
  public ContentNode getNodeBefore(ContentView view) {
    return isInTextNode() ? null : (
        nodeAfter == null ? view.getLastChild(container) : view.getPreviousSibling(nodeAfter));
  }

  /**
   * @param node
   * @return true if point is directly in node
   */
  public boolean isIn(ContentNode node) {
    return container.equals(node);
  }

  /**
   * @return true if point is a beginning of its node
   */
  public boolean isAtBeginning() {
    return isInTextNode() ? offset == 0 : nodeAfter == container.getFirstChild();
  }

  /**
   * @return true if point is an end of its node
   */
  public boolean isAtEnd() {
    return isInTextNode()
        ? offset == getTextNodeLength()
        : nodeAfter == null;
  }

  private int getTextNodeLength() {
    return ((ContentTextNode)container).getLength();
  }

  /**
   * Moves the point outside its current element if the
   * offset places the point at the beginning or end of that
   * element. Biased towards a left move.
   *
   * @return True iff point was moved
   */
  public boolean maybeMoveOut() {
    int dir = 0;
    if (isInTextNode()) {
      if (offset == 0) {
        dir = -1;
      } else if (offset >= getTextNodeLength()) {
        dir = 1;
      }
    } else {
      if (nodeAfter == container.getFirstChild()) {
        dir = -1;
      } else if (nodeAfter == null) {
        dir = 1;
      }
    }

    if (dir != 0) {
      nodeAfter = container;
      container = nodeAfter.getParentElement();
      if (dir == 1) {
        nodeAfter = nodeAfter.getNextSibling();
      }
      offset = -1;
      return true;
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * Equality if node and offset are the same
   */
  @Override
  public final boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof ContentPoint) {
      ContentPoint point = (ContentPoint) o;
      return container.equals(point.getContainer()) && offset == point.getTextOffset()
        && (nodeAfter == point.getNodeAfter() || nodeAfter.equals(point.getNodeAfter()));
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return container.hashCode() + 37 * offset + 1009 * nodeAfter.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    // TODO(user): can we find way to use ContentNode.getPrettyHtml to print
    // the point more nicely?
    return "(" + container.toString() + ":" + (isInTextNode() ? offset : nodeAfter) + ")";
  }

}
