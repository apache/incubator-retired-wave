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

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Miscellaneous document helper functions
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
//
//  DO NOT JUST PUT ANY ARBITRARY MISCELLANEOUS STUFF IN HERE
//
//  (Please think of the big picture - there is too much overlap
//   of partially useful utility methods)
//
//  If in doubt, send CL to dan
//
//  ALL NEW METHODS MUST BE 100% THOROUGHLY UNIT TESTED
//
public class DocHelper {

  /**
   * Expectations for top-level element existence, used by
   * {@link #getOrCreateFirstTopLevelElement(MutableDocument, String, Expectation)}
   * . Since the code that uses this does its own interpretation, it is a
   * requirement that the semantic intersection of any two values is empty.
   */
  private enum Expectation {
    NONE,
    ABSENT,
    PRESENT
  }

  /**
   * "Call" this method so the compiler can help us find code that will break
   * when we make the root an implicit object, and location zero refers to its
   * first child.
   *
   * A lot of test cases will need 1 subtracted from their use of hard coded
   * integer location values
   */
  public static void noteCodeThatWillBreakWithMultipleRoots() {
  }

  private static class NodeOffset<N> {
    /**
     * If is an element, then means "node after", and offset is meaningless
     * Otherwise, the NodeOffset is the same as an inTextNode point
     */
    N node;
    int offset;
  }

  /**
   * Action that can be applied to a node.
   *
   * @param <N> Node
   */
  public interface NodeAction<N> {
    void apply(N node);
  }

  private DocHelper() { }

  /**
   * Checks whether a location has some text immediately to its left.
   *
   * @return true if text data precedes the given location
   */
  public static <N, E extends N, T extends N> boolean textPrecedes(
      ReadableDocument<N, E, T> doc, LocationMapper<N> mapper, int location) {
    Point<N> point = mapper.locate(location);
    if (point.isInTextNode()) {
      return point.getTextOffset() > 0
          || doc.asText(doc.getPreviousSibling(point.getContainer())) != null;
    } else {
      return doc.asText(Point.nodeBefore(doc, point.asElementPoint())) != null;
    }
  }

  /**
   * Checks whether a location has some text immediately to its right.
   *
   * @return true if text data follows the given location
   */
  public static <N, E extends N, T extends N> boolean textFollows(
      LocationMapper<N> mapper, int location) {
    // Locating points always biases to the right, so this case is easy
    return mapper.locate(location).asTextPoint() != null;
  }

  /**
   * Returns the first element in the doc with the given tag name. The root
   * element will never match.
   *
   * @param doc document to look in
   * @param tagName tag name to find
   * @return the first element in the doc with tagName, or null if none exist
   */
  public static <N, E extends N> E getElementWithTagName(
      ReadableDocument<N, E, ?> doc, String tagName) {
    return getElementWithTagName(doc, tagName, doc.getDocumentElement());
  }

  /**
   * Returns the first element in a subtree with the given tag name. The subtree
   * root will never match.
   *
   * @param doc document to look in
   * @param tagName tag name to find
   * @param subtreeRoot of the subtree to search (exclusive)
   * @return the first element in the subtree with tagName, or null if none
   *         exist
   */
  public static <N, E extends N> E getElementWithTagName(ReadableDocument<N, E, ?> doc,
      String tagName, E subtreeRoot) {
    N node = DocHelper.getNextNodeDepthFirst(doc, subtreeRoot, subtreeRoot, true);
    while (node != null) {
      E element = doc.asElement(node);
      if (element != null) {
        if (doc.getTagName(element).equals(tagName)) {
          return element;
        }
      }
      node = DocHelper.getNextNodeDepthFirst(doc, node, subtreeRoot, true);
    }
    return null;
  }

  /**
   * Returns the last element in the doc with the given tag name. The subtree root
   * element will never match.
   *
   * @param doc document to look in
   * @param tagName tag name to find
   * @return the last element in the doc with tagName, or null if none exist
   */
  public static <N, E extends N> E getLastElementWithTagName(
      ReadableDocument<N, E, ?> doc, String tagName) {
    return getLastElementWithTagName(doc, tagName, doc.getDocumentElement());
  }

  /**
   * Returns the last element in a subtree with the given tag name. The subtree
   * root will never match.
   *
   * @param doc document to look in
   * @param tagName tag name to find
   * @return the last element in the subtree with tagName, or null if none exist
   */
  public static <N, E extends N> E getLastElementWithTagName(ReadableDocument<N, E, ?> doc,
      String tagName, E subtreeRoot) {
    N node = DocHelper.getPrevNodeDepthFirst(doc, subtreeRoot, subtreeRoot, true);
    while (node != null) {
      E element = doc.asElement(node);
      if (element != null) {
        if (doc.getTagName(element).equals(tagName)) {
          return element;
        }
      }
      node = DocHelper.getPrevNodeDepthFirst(doc, node, subtreeRoot, true);
    }
    return null;
  }

  /**
   * Get the text within the given element.
   */
  public static <N, E extends N, T extends N> String getText(ReadableWDocument<N, E, T> doc,
      E element) {
    return getText(doc, doc, element);
  }

  /**
   * Get the text within the given element.
   */
  public static <N, E extends N, T extends N> String getText(ReadableDocument<N, E, T> doc,
      LocationMapper<N> mapper, E element) {
    int start = mapper.getLocation(Point.start(doc, element));
    int end = mapper.getLocation(Point.<N>end(element));
    return DocHelper.getText(doc, mapper, start, end);
  }

  /**
   * Shortcut to get the text for an element with a specific tag name.
   * @see DocHelper#getElementWithTagName(ReadableDocument, String)
   * @see DocHelper#getText(ReadableDocument, LocationMapper, Object)
   */
  public static <N> String getTextForElement(
      ReadableWDocument<N, ?, ?> doc, String tagName) {
    return getTextForElement(doc, doc, tagName);
  }

  /**
   * Shortcut to get the text for an element with a specific tag name.
   * @see DocHelper#getElementWithTagName(ReadableDocument, String)
   * @see DocHelper#getText(ReadableDocument, LocationMapper, Object)
   */
  public static <N, E extends N, T extends N> String getTextForElement(
        ReadableDocument<N, E, T> doc, LocationMapper<N> mapper, String tagName) {
    E element = getElementWithTagName(doc, tagName);
    if (element != null) {
      return getText(doc, mapper, element);
    }
    return null;
  }

  /**
   * Variant that accepts an indexed document instead
   * @see #getText(ReadableDocument, LocationMapper, int, int)
   */
  public static <N> String getText(ReadableWDocument<N, ?, ?> doc, int start, int end) {
    return getText(doc, doc, start, end);
  }

  /**
   * Gets text between two locations, using a mapper to convert to points.
   * @see #getText(ReadableDocument, Point, Point)
   */
  public static <N, E extends N, T extends N> String getText(
      ReadableDocument<N, E, T> doc, LocationMapper<N> mapper,
      int start, int end) {
    Preconditions.checkPositionIndexes(start, end, mapper.size());
    Point<N> startPoint = mapper.locate(start);
    Point<N> endPoint = mapper.locate(end);
    return getText(doc, startPoint, endPoint);
  }

  /** Get the text between a given range */
  public static <N, E extends N, T extends N> String getText(
      ReadableDocument<N, E, T> doc, Point<N> startPoint, Point<N> endPoint) {
    NodeOffset<N> output = new NodeOffset<N>();

    getNodeAfterOutwards(doc, startPoint, output);
    N startNode = output.node;
    int startOffset = output.offset;

    getNodeAfterOutwards(doc, endPoint, output);
    N endNode = output.node;
    int endOffset = output.offset;

    if (startNode == null) {
      return "";
    }

    T text = doc.asText(startNode);
    if (doc.isSameNode(startNode, endNode)) {
      return text == null ? "" : doc.getData(text).substring(startOffset, endOffset);
    }

    StringBuilder str = new StringBuilder();
    if (text != null) {
      str.append(doc.getData(text).substring(startOffset));
    }

    N node = getNextNodeDepthFirst(doc, startNode, null, true);
    while (node != endNode) {
      text = doc.asText(node);
      if (text != null) {
        str.append(doc.getData(text));
      }
      node = getNextNodeDepthFirst(doc, node, null, true);
    }

    text = doc.asText(node);
    if (text != null) {
      str.append(doc.getData(text).substring(0, endOffset));
    }

    return str.toString();
  }

  /**
   * Step out of end tags, so we get something that is either in a text node,
   * or the node after our point in a pre-order traversal
   */
  private static <N, E extends N, T extends N> void getNodeAfterOutwards(
      ReadableDocument<N, E, T> doc, Point<N> point, NodeOffset<N> output) {
    N node;
    int startOffset;
    if (point.isInTextNode()) {
      node = point.getContainer();
      startOffset = point.getTextOffset();
    } else {
      node = point.getNodeAfter();
      if (node == null) {
        N parent = point.getContainer();
        while (parent != null) {
          node = doc.getNextSibling(parent);
          if (node != null) {
            break;
          }
          parent = doc.getParentElement(parent);
        }
      }
      startOffset = 0;
    }

    output.node = node;
    output.offset = startOffset;
  }

  /**
   * Get the next node in a depth first traversal.
   *
   * TODO(danilatos): Move this somewhere common (and use for better filtered
   * traversals).
   *
   * @param doc The view to use
   * @param start The node to start from
   * @param stopAt If we reach this node, return null. If already in the node,
   *        will only stop while exiting having traversed all its children. If
   *        we start outside it, it will not be entered.
   * @param enter Enter the start node if it is an element (false to skip its
   *        children - only applies to the start node)
   */
  public static <N> N getNextNodeDepthFirst(
      ReadableDocument<N, ?, ?> doc, N start, N stopAt, boolean enter) {
    return getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, true);
  }

  /**
   * Same as {@link #getNextNodeDepthFirst(ReadableDocument, Object, Object, boolean)},
   * but goes in the other direction
   */
  public static <N> N getPrevNodeDepthFirst(
      ReadableDocument<N, ?, ?> doc, N start, N stopAt, boolean enter) {
    return getNextOrPrevNodeDepthFirst(doc, start, stopAt, enter, false);
  }

  /**
   * Same as {@link #getNextNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * and {@link #getPrevNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * except direction is parametrised.
   * @param rightwards If true, then go rightwards, otherwise leftwards.
   */
  public static <N, E extends N, T extends N> N getNextOrPrevNodeDepthFirst(
      ReadableDocument<N, E, T> doc, N start, N stopAt, boolean enter, boolean rightwards) {
    // Default stopping place is the very top
    if (stopAt == null) {
      stopAt = doc.getDocumentElement();
    }

    // Maybe enter into an element
    N next;
    if (enter) {
      E element = doc.asElement(start);
      if (element != null) {
        next = rightwards ? doc.getFirstChild(element) : doc.getLastChild(element);
        if (next != null) {
          return next;
        }
      }
    }

    // Go upwards from exiting an element
    while (start != null && !doc.isSameNode(start, stopAt)) {
      next = rightwards ? doc.getNextSibling(start) : doc.getPreviousSibling(start);
      if (doc.isSameNode(next, stopAt)) {
        return null;
      }
      if (next != null) {
        return next;
      }
      start = doc.getParentElement(start);
    }

    return null;
  }

  /**
   * Same as {@link #getFilteredPoint(ReadableDocumentView, Point)}, but
   * returns an integer location
   */
  public static  <N, E extends N, T extends N> int getFilteredLocation(
      LocationMapper<N> locationMapper, ReadableDocumentView<N, E, T> filteredView,
      Point<N> point) {
    return locationMapper.getLocation(getFilteredPoint(filteredView, point));
  }

  /**
   * Gets the location of a given point in the DOM.
   *
   * @param filteredView
   * @param point
   * @return the location of the given point.
   */
  public static <N, E extends N, T extends N> Point<N> getFilteredPoint(
      ReadableDocumentView<N, E, T> filteredView, Point<N> point) {
    filteredView.onBeforeFilter(point);

    if (point.isInTextNode()) {
      N visible;

      visible = filteredView.getVisibleNode(point.getContainer());
      if (visible == point.getContainer()) {
        return point;
      } else {
        N next = getNextNodeDepthFirst(filteredView, point.getContainer(), visible, false);
        if (next == null) {
          return Point.inElement(visible, null);
        } else {
          return Point.before(filteredView, next);
        }
      }
    } else if (point.getNodeAfter() == null) {
      return getLocationOfNodeEnd(filteredView, point.getContainer());
    } else {
      return getLocationOfBeforeNode(filteredView, point.getNodeAfter());
    }
  }

  /**
   * Get location of the end of the inside of the given node
   */
  private static <N, E extends N, T extends N> Point<N> getLocationOfNodeEnd(
      ReadableDocumentView<N, E, T> doc, N node) {

    assert node != null : "Node is null";

    N parent = doc.getVisibleNode(node);
    assert parent != null : "Parent is null";

    if (parent == node) {
      return Point.end(node);
    }
    N next = DocHelper.getNextNodeDepthFirst(doc, node, parent, false);
    if (next == null) {
      return Point.end(parent);
    } else {
      return Point.before(doc, next);
    }
  }

  /**
   * Get location of the outside of the start of the given node
   */
  private static <N, E extends N, T extends N> Point<N> getLocationOfBeforeNode(
      ReadableDocumentView<N, E, T> doc, N node) {
    assert node != doc.getDocumentElement() : "Cannot get location outside of root element";

    N parent = doc.getVisibleNode(node);
    if (parent == node) {
      return Point.before(doc, node);
    }
    assert parent != null;

    N next = DocHelper.getNextNodeDepthFirst(doc, node, parent, true);
    if (next == null) {
      return Point.end(parent);
    } else {
      return Point.before(doc, next);
    }
  }

  public static <N, T extends N> int getItemSize(ReadableWDocument<N, ?, T> doc, N node) {
    // Short circuit if it's a text node, implementation is simpler
    T textNode = doc.asText(node);
    if (textNode != null) {
      return doc.getLength(textNode);
    }

    // Otherwise, calculate two locations and subtract
    N parent = doc.getParentElement(node);
    if (parent == null) {
      // Requesting size of the document root.
      // TODO(danilatos/anorth) This would change if we have multiple roots.
      noteCodeThatWillBreakWithMultipleRoots();
      return doc.size();
    }
    N next = doc.getNextSibling(node);
    int locationAfter = next != null ? doc.getLocation(next)
        : doc.getLocation(Point.end(parent));
    return locationAfter - doc.getLocation(node);
  }

  /**
   * Normalizes a point so that it is biased towards text nodes, and node ends
   * rather than node start.
   *
   * @param <N>
   * @param <E>
   * @param <T>
   * @param point
   * @param doc
   */
  public static <N, E extends N, T extends N> Point<N> normalizePoint(Point<N> point,
      ReadableDocument<N, E, T> doc) {
    N previous = null;
    if (!point.isInTextNode()) {
      previous = Point.nodeBefore(doc, point.asElementPoint());
      T nodeAfterAsText = doc.asText(point.getNodeAfter());
      if (nodeAfterAsText != null) {
        point = Point.<N>inText(nodeAfterAsText, 0);
      }
    } else if (point.getTextOffset() == 0) {
      previous = doc.getPreviousSibling(point.getContainer());
    }

    T previousAsText = doc.asText(previous);
    if (previous != null && previousAsText != null) {
      point = Point.inText(previous, doc.getLength(previousAsText));
    }

    return point;
  }


  /**
   * Left-aligns a position in a document, given a view over that document of places to align to.
   * Achieved by traversing the point backwards through the full document until a position in the
   * view is found, then returning a point at that position.
   *
   * @param current The point in the fullDoc to align
   * @param fullDoc Complete document
   * @param important view over the complete document
   * @return The aligned point in the full document (may use nodes not in the view)
   */
  public static <N, E extends N, T extends N> Point<N> leftAlign(Point<N> current,
      ReadableDocument<N, E, T> fullDoc, ReadableDocumentView<N, E, T> important) {
    if (current == null || current.isInTextNode()) {
      return current; // assume text nodes are already aligned
    }

    N parent = current.getContainer();
    N at = current.getNodeAfter();

    // calculate the node before the point
    N lastBefore = null;
    if (at == null) {
      lastBefore = fullDoc.getLastChild(parent);
    } else {
      lastBefore = fullDoc.getPreviousSibling(at);
    }

    // nothing before the at node, so move up one level
    N visibleParent = important.getVisibleNode(parent);
    if (lastBefore == null) {
      if (parent == visibleParent) {
        return Point.textOrElementStart(fullDoc, parent);
      }
      lastBefore = parent;
    }

    // and move backwards (starting from right-most child) until we have an important node
    N nodeLast = important.getVisibleNodeLast(lastBefore);
    N lcaVis = nodeLast == null ? lastBefore : nearestCommonAncestor(fullDoc, nodeLast, lastBefore);

    // special case when last visible is a parent - so use visibleParent iff it is a child of lcaVis
    if (isAncestor(fullDoc, lcaVis, visibleParent, false)) {
      return Point.textOrElementStart(fullDoc, visibleParent);
    } else {
      lastBefore = nodeLast;
    }

    // get the child after the node before the new point, then correct the parent in full document.
    at = lastBefore == null ? null : important.getNextSibling(lastBefore);
    if (at != null) {
      parent = fullDoc.getParentElement(at);
    } else if (lastBefore != null) {
      parent = fullDoc.getParentElement(lastBefore);
    }
    return at == null ? Point.end(parent) : Point.before(fullDoc, at);
  }

  /**
   * Gets the first child element of an element, if there is one.
   *
   * @param doc      document accessor
   * @param element  parent element
   * @return the first child element of {@code element} if there is one,
   *         otherwise {@code null}.
   */
  public static <N, E extends N> E getFirstChildElement(ReadableDocument<N, E, ?> doc, E element) {
    return getNextElementInclusive(doc, doc.getFirstChild(element), true);
  }

  /**
   * Gets the last child element of an element, if there is one.
   *
   * @param doc      document accessor
   * @param element  parent element
   * @return the last child element of {@code element} if there is one,
   *         otherwise {@code null}.
   */
  public static <N, E extends N> E getLastChildElement(ReadableDocument<N, E, ?> doc, E element) {
    return getNextElementInclusive(doc, doc.getLastChild(element), false);
  }

  /**
   * Gets the next sibling of an element that is also an element itself.
   *
   * @param doc      document accessor
   * @param element  an element
   * @return the next element sibling of {@code element} if there is one,
   *         otherwise {@code null}.
   */
  public static <N, E extends N> E getNextSiblingElement(ReadableDocument<N, E, ?> doc, E element) {
    return getNextElementInclusive(doc, doc.getNextSibling(element), true);
  }

  /**
   * @param doc document accessor.
   * @param element a document element.
   * @return The previous element sibling of {@code element} if there is one,
   *         otherwise {@code null}.
   */
  public static <N, E extends N> E getPreviousSiblingElement(
      ReadableDocument<N, E, ?> doc, E element) {
    Preconditions.checkNotNull(element, "Previous element for null element is undefined");
    Preconditions.checkNotNull(doc, "Previous element for null document is undefined");
    return getNextElementInclusive(doc, doc.getPreviousSibling(element), false);
  }

  /**
   * Returns a node as an element if it is one; otherwise, finds the next
   * sibling of that node that is an element.
   *
   * @param doc document accessor
   * @param node reference node
   * @return the next element in the inclusive sibling chain from {@code node}.
   */
  public static <N, E extends N> E getNextElementInclusive(ReadableDocument<N, E, ?> doc, N node,
      boolean forward) {
    E asElement = doc.asElement(node);

    while (node != null && asElement == null) {
      node = forward ? doc.getNextSibling(node) : doc.getPreviousSibling(node);
      asElement = doc.asElement(node);
    }
    return asElement;
  }

  /**
   * Apply action to a node and its descendants.
   *
   * @param doc         view for traversing
   * @param node        reference node
   * @param nodeAction  action to apply to node and its descendants
   */
  public static <N, E extends N, T extends N> void traverse(ReadableDocument<N, E, T> doc, N node,
      NodeAction<N> nodeAction) {
    for (; node != null; node = doc.getNextSibling(node)) {
      nodeAction.apply(node);
      traverse(doc, doc.getFirstChild(node), nodeAction);
    }
  }

  /**
   * Ensures the given point is at a node boundary, possibly splitting a text
   * node in order to do so, in which case a new point is returned.
   *
   * @param point
   * @return a point at the same place as the input point, guaranteed to be at
   *         a node boundary.
   */
  public static <N, T extends N> Point.El<N> ensureNodeBoundary(Point<N> point,
      ReadableDocument<N, ?, T> doc, TextNodeOrganiser<T> textNodeOrganiser) {

    Point.Tx<N> textPoint = point.asTextPoint();
    if (textPoint != null) {
      T textNode = doc.asText(textPoint.getContainer());
      N maybeSecond = textNodeOrganiser.splitText(textNode,
          textPoint.getTextOffset());
      if (maybeSecond != null) {
        return Point.inElement(doc.getParentElement(maybeSecond), maybeSecond);
      } else {
        return Point.inElement(doc.getParentElement(textNode), doc.getNextSibling(textNode));
      }
    } else {
      return point.asElementPoint();
    }
  }


  /**
   * Ensures the given point precedes a node, possibly splitting a text
   * node in order to do so, and possibly traversing until a node is found.
   *
   * @param point
   * @return a node at the same place as the input point, guaranteed to be at
   *         a node boundary. If there is no node, the next available node.
   */
  public static <N, T extends N> N ensureNodeBoundaryReturnNextNode(Point<N> point,
      ReadableDocument<N, ?, T> doc, TextNodeOrganiser<T> textNodeOrganiser) {

    Point.Tx<N> textStartPoint = point.asTextPoint();
    if (textStartPoint != null) {
      T textNode = doc.asText(textStartPoint.getContainer());
      N maybeSecond = textNodeOrganiser.splitText(textNode,
          textStartPoint.getTextOffset());
      if (maybeSecond != null) {
        return maybeSecond;
      } else {
        return getNextNodeDepthFirst(doc, textNode, null, false);
      }
    } else if (point.getNodeAfter() != null) {
      return point.getNodeAfter();
    } else {
      return getNextNodeDepthFirst(doc, point.getContainer(), null, false);
    }
  }

  /**
   * Generalisation of {@link WritableLocalDocument#transparentSlice(Object)},
   * allowing a slice at a point, returning a point.
   *
   * Avoids slicing where possible, including where the splitAt point would map
   * to a location in the persistent view corresponding to a point that is also
   * valid in the full view.
   */
  public static <N, E extends N, T extends N> Point<N> transparentSlice(Point<N> splitAt,
      DocumentContext<N, E, T> cxt) {

    // Convert to a point in the persistent view
    // TODO(danilatos) More efficiently? This is simple but brutish.
    int location = getFilteredLocation(cxt.locationMapper(), cxt.persistentView(), splitAt);
    Point<N> pPoint = cxt.locationMapper().locate(location);

    if (pPoint.isInTextNode()) {
      T text = cxt.document().asText(pPoint.getContainer());
      E pParent = cxt.document().getParentElement(text);
      if (pParent == cxt.annotatableContent().getParentElement(text)) {
        return pPoint;
      } else {
        pPoint = ensureNodeBoundary(pPoint, cxt.document(), cxt.textNodeOrganiser());
      }
    }

    if (pPoint.getNodeAfter() != null) {
      N nodeAfter = pPoint.getNodeAfter();
      if (cxt.annotatableContent().getParentElement(nodeAfter) != pPoint.getContainer()) {
        return Point.inElement(pPoint.getContainer(),
            cxt.annotatableContent().transparentSlice(nodeAfter));
      } else {
        return pPoint;
      }
    } else {
      return pPoint;
    }
  }

  /**
   * Counts how many children a particular element in a document has.
   *
   * @param doc The doc that the element is in.
   * @param elem An element.
   * @return Number of children the specified element has.
   */
  public static <N, E extends N, T extends N> int countChildren(
      ReadableDocument<Node, Element, Text> doc, Element elem) {
    int children = 0;
    Node currentChild = doc.getFirstChild(elem);

    while (currentChild != null) {
      children++;
      currentChild = doc.getNextSibling(currentChild);
    }

    return children;
  }

  /**
   * Does a linear search from the startNode for an element with the given id
   *
   * @param doc
   * @param subtreeRoot the element to start looking from. Only startNode or it's
   *        child elements will be found.
   * @param id id attribute's value
   * @return first matching element, or null if none found
   */
  public static <N, E extends N, T extends N> E findElementById(
      ReadableDocument<N, E, T> doc, E subtreeRoot, String id) {
    return findElementByAttr(doc, subtreeRoot, "id", id);
  }

  /**
   * Does a linear search for an element with the given id
   * @param doc
   * @param id id attribute's value
   * @return first matching element, or null if none found
   */
  public static <N, E extends N, T extends N> E findElementById(
      ReadableDocument<N, E, T> doc, String id) {
    return findElementByAttr(doc, "id", id);
  }

  /**
   * Iterates through startNode and its child elements and returns the first
   * with the matching name value pair amongst its attributes.
   */
  public static <N, E extends N, T extends N> E findElementByAttr(
      ReadableDocument<N, E, T> doc, E subtreeRoot, String name, String value) {

    Preconditions.checkNotNull(name, "name must not be null");
    Preconditions.checkNotNull(value, "value must not be null");

    for (E el : DocIterate.deepElements(doc, subtreeRoot, subtreeRoot)) {
      if (value.equals(doc.getAttribute(el, name))) {
        return el;
      }
    }

    return null;
  }

  /**
   * Iterates through elements in the document and returns the first with the
   * matching name value pair amongst its attributes.
   */
  public static <N, E extends N, T extends N> E findElementByAttr(
      ReadableDocument<N, E, T> doc, String name, String value) {
    return findElementByAttr(doc, doc.getDocumentElement(), name, value);
  }

  /**
   * Does a linear search for an element with the given id and
   * returns its location
   *
   * @param doc
   * @param id id attribute's value
   * @return first matching element's location, or -1 if none found
   */
  public static <N, E extends N, T extends N> int findLocationById(
      ReadableWDocument<N, E, T> doc, String id) {
    return findLocationByAttr(doc, "id", id);
  }

  /**
   * Returns the location of the first matching element
   *
   * @see #findElementByAttr(ReadableDocument, String, String)
   *
   * @return the location of the first matching element or -1 if none found
   */
  public static <N, E extends N, T extends N> int findLocationByAttr(
      ReadableWDocument<N, E, T> doc, String name, String value) {

    E el = findElementByAttr(doc, name, value);
    return el != null ? doc.getLocation(el) : -1;
  }

  /**
   * A predicate that matches the document's root element
   */
  public static final DocPredicate ROOT_PREDICATE = new DocPredicate() {
    @Override
    public <N, E extends N, T extends N> boolean apply(ReadableDocument<N, E, T> doc, N node) {
      return node == doc.getDocumentElement();
    }
  };

  /**
   * @return true if the node is an element with the given tag name
   */
  public static <N, E extends N> boolean isMatchingElement(
      final ReadableDocument<N, E, ?> doc, N node, String tagName) {
    E el = doc.asElement(node);
    return el != null && doc.getTagName(el).equals(tagName);
  }

  /**
   * Maneuvers the given point upwards such that its containing element matches
   * the given predicate. Where this requires an element point, the nodeAfter
   * will be forced rightwards as necessary. If the location is in a text node
   * whose parent matches the predicate, the location already satisfies.
   *
   * Will return the same point by identity where possible.
   *
   * @return the point within an element matching the predicate, or null if
   *    there were none.
   */
  @SuppressWarnings("unchecked") // safe
  public static <N, E extends N, T extends N> Point<N> jumpOut(
      ReadableDocument<N, E, T> doc, Point<N> location, DocPredicate predicate) {
    E el;
    N nodeAfter;
    if (location.isInTextNode()) {
      el = doc.getParentElement(location.getContainer());
      nodeAfter = doc.getNextSibling(location.getContainer());
      if (predicate.apply(doc, el)) {
        return location;
      }
    } else {
      assert doc.asElement(location.getContainer()) != null;
      el = (E) location.getContainer();
      nodeAfter = location.getNodeAfter();
    }
    while (el != null && !predicate.apply(doc, el)) {
      nodeAfter = doc.getNextSibling(el);
      el = doc.getParentElement(el);
    }
    if (el == null) {
      return null;
    }
    // nodeAfter is of type N, el is of type (E extends N), so inElement(el, nodeAfter)
    // should return a Point<N>. But Sun's java compiler doesn't figure that out,
    // so we need to hint: Point.<N>inElement(...)
    return el == location.getContainer() ? location : Point.<N>inElement(el, nodeAfter);
  }

  /**
   * Gets the first top-level element in a document.
   *
   * This is a transition method. It has a different contact for old ops vs new
   * ops. After moving to new ops, this method should be deleted and calls to it
   * replaced with the direct version.
   *
   * In old ops, this returns:
   * <code>
   *   doc.getDocumentElement();
   * </code>
   * and so is never null.
   *
   * In new ops, this returns:
   * <code>
   *   DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
   * </code>
   * and may be null.
   *
   * @param doc document
   * @return first top-level element in a document.  May be null.
   */
  private static <N, E extends N> E getOrCreateFirstTopLevelElement(MutableDocument<N, E, ?> doc,
      String tag, Expectation expectation) {

    N firstNode = doc.locate(0).getNodeAfter();
    if (expectation == Expectation.PRESENT && firstNode == null) {
      throw new IllegalArgumentException("Document has no top-level element");
    } else if (expectation == Expectation.ABSENT && firstNode != null) {
      throw new IllegalArgumentException("Document already has top-level node: " + firstNode);
    }

    if (firstNode == null) {
      return doc.createChildElement(doc.getDocumentElement(), tag, Attributes.EMPTY_MAP);
    } else {
      E firstElement = doc.asElement(firstNode);
      if (firstElement == null) {
        throw new IllegalArgumentException("First node is not an element: " + firstNode);
      }

      // Check that this element matches what is expected.
      String actualTag = doc.getTagName(firstElement);
      if (!tag.equals(actualTag)) {
        throw new RuntimeException("Document already has non-matching top-level element: "
            + firstElement);
      } else {
        return firstElement;
      }
    }
  }

  /**
   * Gets the first top-level element, creating it if it does not exist. If
   * there is an existing top-level element, but it does not match the expected
   * tag, this method fails.
   *
   * In order to avoid race conditions from multiple clients creating multiple
   * top-level elements, please consider using
   * {@link #expectAndGetFirstTopLevelElement(MutableDocument, String)} or
   * {@link #createFirstTopLevelElement(MutableDocument, String)} instead.
   *
   * @param doc document
   * @param tag tag name for the top-level element
   * @return first top-level element, created if necessary. Never null.
   */
  public static <E> E getOrCreateFirstTopLevelElement(MutableDocument<? super E, E, ?> doc,
      String tag) {
    return getOrCreateFirstTopLevelElement(doc, tag, Expectation.NONE);
  }

  /**
   * Gets the first top-level element if it is present.
   *
   * @param doc document
   * @param tag tag name for the top-level element
   * @throws RuntimeException if there is no such element, or it does not match
   *         the specific tag.
   * @return the first top-level element. Never null.
   */
  public static <E> E expectAndGetFirstTopLevelElement(MutableDocument<? super E, E, ?> doc,
      String tag) {
    return getOrCreateFirstTopLevelElement(doc, tag, Expectation.PRESENT);
  }

  /**
   * Creates the first top-level element. If a top-level element already exists,
   * this method fails.
   *
   * @param doc document
   * @param tag tag name for the top-level element
   * @throws RuntimeException if a top-level element already exists.
   * @return the newly created top-level element. Never null.
   */
  public static <E> E createFirstTopLevelElement(MutableDocument<? super E, E, ?> doc, String tag) {
    return getOrCreateFirstTopLevelElement(doc, tag, Expectation.ABSENT);
  }

  /**
   * Find the nearest common ancestor of two nodes
   *
   * @return The nearest common ancestor of node1 and node2
   */
  public static <N, E extends N, T extends N> N nearestCommonAncestor(
      ReadableDocument<N, E, T> doc, N node1, N node2) {
    IdentityMap<N, N> ancestors = CollectionUtils.createIdentityMap();

    if (node1 == node2) {
      return node1;
    }

    N commonAncestor = null;
    while (node1 != null || node2 != null) {
      if (node1 != null) {
        if (ancestors.has(node1)) {
          commonAncestor = node1;
          break;
        }
        ancestors.put(node1, node1);
        node1 = doc.getParentElement(node1);
      }
      if (node2 != null) {
        if (ancestors.has(node2)) {
          commonAncestor = node2;
          break;
        }
        ancestors.put(node2, node2);
        node2 = doc.getParentElement(node2);
      }
    }

    if (commonAncestor == null) {
      throw new IllegalArgumentException("nearestCommonAncestor: " +
          "Given nodes are not in the same document");
    }

    return commonAncestor;
  }

  /**
   * Checks whether a given node is an ancestory of another (either inclusive or exclusive).
   * @param doc Document for tree traversal
   * @param ancestor A (non-null) node to check to check if the next param is a descendant of
   * @param child The node whose ancestory is being checked
   * @param canEqual The result if the two nodes are equal
   */
  public static <N, E extends N, T extends N>
      boolean isAncestor(ReadableDocument<N, E, T> doc, N ancestor, N child, boolean canEqual) {
    Preconditions.checkNotNull(ancestor, "Shouldn't check ancestry of a null node");

    // keep going up the tree until we break out the parent (complexity = depth of child)
    while (child != null) {
      if (ancestor == child) {
        return canEqual;
      }
      canEqual = true; // now equality represents absolute descendancy
      child = doc.getParentElement(child);
    }
    return false; // no match
  }
}
