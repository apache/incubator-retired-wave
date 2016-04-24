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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.extract.SelectionMatcher.LazyPoint.Type;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Helper class to note html point corresponding to given content point.
 *
 * The class is constructed with a pair of content points.  Then, it provides methods
 * that takes a ContentPoint and corresponding html point. If it the given content point
 * matches the initial content points, then it'll note the html points as a match.
 *
 */
public final class SelectionMatcher {
  /**
   * The selection in the content that needs to be matched.
   */
  private final Point<ContentNode> contentStart;
  private final Point<ContentNode> contentEnd;

  /**
   * The root container for new selection.
   */
  private Node htmlRootContainer;

  /**
   * The new matching selection.
   */
  private LazyPoint htmlStart = null;
  private LazyPoint htmlEnd = null;

  /**
   * Lazy point so we can provide a reference to a point, before the DOM is
   * fully constructed, i.e.
   * <p><br/>|</p>
   *
   * The Point here has parent <p> with nodeAfter null. However, if we express
   * it as point with nodeBefore = <br/> the point is valid even if nodes are
   * inserted after the <br/>
   */
  interface LazyPoint {
    enum Type {
      BEFORE_NODE,
      AFTER_NODE,
      AT_START,
    }

    /**
     * @return the corresponding Point.
     */
    Point<Node> getPoint();
  }

  /**
   * A variant of LazyPoint where we are given the explicit point.
   */
  private final class EagerPoint implements LazyPoint {
    private final Point<Node> explicit;
    EagerPoint(Point<Node> p) {
      assert p != null;
      assert htmlRootContainer.isOrHasChild(p.getContainer()) : "container not attached";
      this.explicit = p;
    }

    @Override
    public Point<Node> getPoint() {
      return explicit;
    }
  }

  /**
   * @param n
   * @return a LazyPoint before the give node.
   */
  LazyPoint beforeNode(Node n) {
    return new LazyPointImpl(n, Type.BEFORE_NODE);
  }

  /**
   * @param n
   * @return a LazyPoint after the give node.
   */
  LazyPoint afterNode(Node n) {
    return new LazyPointImpl(n, Type.AFTER_NODE);
  }

  /**
   * @param n
   * @return a LazyPoint at the start of the given node.
   */
  LazyPoint atStart(Node n) {
    return new LazyPointImpl(n, Type.AT_START);
  }

  private final class LazyPointImpl implements LazyPoint {
    private final Node ref;
    private final Type pointType;

    LazyPointImpl(Node ref, Type pointType) {
      assert ref != null;
      assert htmlRootContainer.isOrHasChild(ref) : "Reference node not attached";
      this.ref = ref;
      this.pointType = pointType;
    }

    /**
     * Evalulates the LazyPoint to a Point.
     */
    @Override
    public Point<Node> getPoint() {
      assert ref.getParentElement() != null : "Reference node must be attached when getting point";

      switch (pointType) {
        case AFTER_NODE:
          return Point.inElement(ref.getParentElement(), ref.getNextSibling());
        case BEFORE_NODE:
          return Point.inElement(ref.getParentElement(), ref);
        case AT_START:
          return Point.inElement(ref, ref.getFirstChild());
        default:
          throw new RuntimeException("invalid case");
      }
    }
  }

  /**
   * Initializes a SelectionMatcher with a contentSelection.
   *
   * @param contentStart
   * @param contentEnd
   */
  SelectionMatcher(Point<ContentNode> contentStart,
      Point<ContentNode> contentEnd) {
    this.contentStart = contentStart;
    this.contentEnd = contentEnd;
  }

  /**
   * Note the points if matched.
   *
   * If the initial content start or content end match the boundary of source,
   * then note the html point on the corresponding boundary of clone.
   *
   * If source is a text node, or is empty, note the html point inside the
   * corresponding position in clone.
   *
   * @param source
   * @param clone
   */
  public void maybeNoteHtml(ContentNode source, Node clone) {
    Preconditions.checkArgument(source != null, "Source must be non-null");
    Preconditions.checkArgument(source.getParentElement() != null, "Source must have parent.");
    assert htmlRootContainer.isOrHasChild(clone) : "Reference node must be attached";
    if (htmlStart == null) {
      maybeSetStartAtBoundary(source, clone);
      maybeSetStartInText(source, clone);
      maybeSetStartInEmptyElement(source, clone);
    }

    if (htmlEnd == null) {
      maybeSetEndAtBoundary(source, clone);
      maybeSetEndInText(source, clone);
      maybeSetEndInEmptyElement(source, clone);
    }
  }

  /**
   * Ensures the selection contains this node, if the original selection lies inside its subtree.
   *
   * @param node
   * @param dstParent
   * @param onSubtree
   */
  public void noteSelectionInNode(ContentNode node, Element dstParent, boolean onSubtree) {
    if (contentStart.getContainer() == node ||
        contentStart.equals(Point.inElement(node.getParentElement(), node))) {
      htmlStart = dstParent.hasChildNodes() ?
          afterNode(dstParent.getLastChild()) :
            atStart(dstParent);
    }

    if (contentEnd.getContainer() == node ||
        contentEnd.equals(Point.inElement(node.getParentElement(), node))) {
      htmlEnd = dstParent.hasChildNodes() ?
          afterNode(dstParent.getLastChild()) :
            atStart(dstParent);
    }
  }

  /**
   * Sets the root html container. The new selection must lie inside the subtree
   * of this element. This is primarily used for checking that the selection is
   * valid.
   *
   * @param n
   */
  public void setHtmlRootContainer(Node n) {
    htmlRootContainer = n;
  }

  /**
   * Gets the noted start point.
   */
  public Point<Node> getHtmlStart() {
    return htmlStart != null ? htmlStart.getPoint() : null;
  }

  /**
   * Gets the noted end point
   */
  public Point<Node> getHtmlEnd() {
    return htmlEnd != null ? htmlEnd.getPoint() : null;
  }

  private void maybeSetStartAtBoundary(ContentNode c, Node node) {
    if (contentStart.equals(Point.inElement(c.getParentElement(), c))) {
      htmlStart = beforeNode(node);
    } else if (contentStart.equals((Point.inElement(c.getParentElement(), c.getNextSibling())))) {
      htmlStart = afterNode(node);
    }
  }

  private void maybeSetEndAtBoundary(ContentNode c, Node node) {
    if (contentEnd.equals(Point.inElement(c.getParentElement(), c))) {
      htmlEnd = beforeNode(node);
    } else if (contentEnd.equals((Point.inElement(c.getParentElement(), c.getNextSibling())))) {
      htmlEnd = afterNode(node);
    }
  }


  private void maybeSetStartInEmptyElement(ContentNode source, Node clone) {
    LazyPoint p = getCorespondingPointInEmptyElement(contentStart, source, clone);
    if (p != null) {
      htmlStart = p;
    }
  }

  private void maybeSetEndInEmptyElement(ContentNode source, Node clone) {
    LazyPoint p = getCorespondingPointInEmptyElement(contentEnd, source, clone);
    if (p != null) {
      htmlEnd = p;
    }
  }

  private LazyPoint getCorespondingPointInEmptyElement(Point<ContentNode> selection,
      ContentNode source, Node clone) {
    if (source instanceof ContentElement && source.getFirstChild() == null
        && selection.equals(Point.inElement(source, null))) {
      if (clone instanceof Element) {
        return new EagerPoint(Point.inElement(clone, null));
      } else {
        return new EagerPoint(Point.inElement(clone.getParentElement(), clone));
      }
    } else {
      return null;
    }
  }

  private void maybeSetEndInText(ContentNode source, Node clone) {
    LazyPoint matchedTextSelection = matchTextSelection(contentEnd, source, clone);
    if (matchedTextSelection != null) {
      htmlEnd = matchedTextSelection;
    }
  }

  private void maybeSetStartInText(ContentNode source, Node clone) {
    LazyPoint matchedTextSelection = matchTextSelection(contentStart, source, clone);
    if (matchedTextSelection != null) {
      htmlStart = matchedTextSelection;
    }
  }

  private LazyPoint matchTextSelection(
      Point<ContentNode> selection, ContentNode source, Node clone) {
    if (selection.isInTextNode() && selection.getContainer() == source) {
      assert clone instanceof Text;
      return new EagerPoint(Point.<Node>inText(clone, selection.getTextOffset()));
    } else {
      return null;
    }
  }
}
