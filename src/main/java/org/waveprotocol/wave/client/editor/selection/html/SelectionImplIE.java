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

package org.waveprotocol.wave.client.editor.selection.html;

import static org.waveprotocol.wave.client.editor.selection.html.JsSelectionIE.Type.None;
import static org.waveprotocol.wave.client.editor.selection.html.JsTextRangeIE.CompareMode.EndToEnd;
import static org.waveprotocol.wave.client.editor.selection.html.JsTextRangeIE.CompareMode.StartToStart;
import static org.waveprotocol.wave.client.editor.selection.html.JsTextRangeIE.MoveUnit.character;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.ui.RootPanel;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.selection.html.JsTextRangeIE.CompareMode;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * IE-specific selection implementation
 *
 *
 */
class SelectionImplIE extends SelectionImpl {
  /**
   * We use a dummy inline element to help us set carets
   */
  private final static Element setter = Document.get().createElement("b");
  static {
    setter.setInnerHTML("x");
  }

  /**
   * The hint is the last seen point.
   */
  private Point<Node> hint;

  private String savedSelection;

  /**
   * Clears selection
   */
  @Override
  void clear() {
    JsSelectionIE.get().empty();
  }

  /**
   * {@inheritDoc}
   *
   * Note(user): IE's selection type reports 'Text' for non-collapsed selections,
   * but 'None' for carets as well as for entirely missing selection.
   */
  @Override
  FocusedPointRange<Node> get() {
    PointRange<Node> sel = getOrdered();

    // TODO(danilatos): Proper difference between focus and anchor
    return sel == null ? null : new FocusedPointRange<Node>(sel.getFirst(), sel.getSecond());
  }

  @Override
  boolean isOrdered() {
    // TODO(danilatos): Proper difference between focus and anchor
    return true;
  }

  @Override
  PointRange<Node> getOrdered() {
    // NOTE(user): try/catch here as JsTextRangeIE.duplicate throws an exception if the
    // selection is non-text, i.e. an image. Its much safer to wrap these IE native methods.
    // TODO(user): Decide whether returning null is the correct behaviour when exception is
    // thrown. If so, remove the logger.error().
    try {
      // Get selection + corresponding text range
      JsSelectionIE selection = JsSelectionIE.get();
      JsTextRangeIE start = selection.createRange();

      // Best test we have found for empty selection
      if (checkNoSelection(selection, start)) {
        return null;
      }

      // create two collapsed ranges, for each end of the selection
      JsTextRangeIE end = start.duplicate();
      start.collapse(true);
      end.collapse(false);

      // Translate to HtmlPoints
      Point<Node> startPoint = pointAtCollapsedRange(start);

      return JsTextRangeIE.equivalent(start, end) ? new PointRange<Node>(startPoint)
          : new PointRange<Node>(startPoint, pointAtCollapsedRange(end));
    } catch (JavaScriptException e) {
      logger.error().log("Cannot get selection", e);
      return null;
    }
  }

  private boolean pointMatchesRange(Point<Node> point, JsTextRangeIE target) {
    try {
    JsTextRangeIE tr = collapsedRangeAtPoint(point);
    return tr.compareEndPoints(StartToStart, target) == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private Point<Node> pointAtCollapsedRange(JsTextRangeIE target) {
    if (hint != null && isValid(hint) && pointMatchesRange(hint, target)) {
      return hint;
    }
    hint = pointAtCollapsedRangeInner(target);
    return hint;
  }

  private boolean isValid(Point<Node> p) {
    if (p.isInTextNode()) {
      return true;
    } else {
      Node nodeAfter = p.getNodeAfter();
      return nodeAfter == null || p.getContainer().equals(p.getNodeAfter().getParentElement());
    }
  }

  /**
   * @param target collapsed text range
   * @return HtmlPoint matching the collapsed text range
   */
  private Point<Node> pointAtCollapsedRangeInner(JsTextRangeIE target) {
    // The point is (mostly) either directly in target's parent,
    // or in a text node child of parent. (Occasionally, the point
    // sits right after the parent, see below.)
    Element parent = target.parentElement();

    // XXX(zdwang): For some reason IE 7 likes to focus on the input box of the
    // contacts pop up during the on key press event when you press shift+enter
    // on a blip. This causes attempt.moveToElementText(el) to thrown an
    // exception.
    // TODO(user): check with zdwang if this is still needed...
    if (parent.getTagName().equals("INPUT")) {
      return Point.inElement(parent, parent.getFirstChild());
    }

    // This catches an corner case where the target is actually
    // *outside* its parent node. This happens, e.g., in this case:
    // <p><thumbnail/>|</p>. Best look out for other such cases!
    while (parent.getAttribute("contentEditable").equalsIgnoreCase("false")) {
      parent = parent.getParentElement();
    }
    return binarySearchForRange(target, parent);
    // return searchForRangeUsingPaste(target, parent);
    // return linearSearchForRange(target, parent);
  }

  /**
   * Search for node by pasting that element at a textRange and locating that
   * element directly using getElementById. This is a huge shortcut when there
   * are many nodes in parent. However, use with caution as it can fragment text
   * nodes.
   *
   * NOTE(user): The text node fragmentation is a real issue, it causes repairs
   * to happen. The constant splitting and repairing can also have performance
   * issues that needs to be investigated. We should repair the damage here,
   * when its clear how to fix the problem.
   *
   * @param target
   * @param parent
   * @return Point
   */
  @SuppressWarnings("unused") // NOTE(user): Use later for nodes with many siblings.
  private Point<Node> searchForRangeUsingPaste(JsTextRangeIE target, Element parent) {
    Element elem = null;
    try {
      target.pasteHTML("<b id='__paste_target__'>X</b>");
      elem = Document.get().getElementById("__paste_target__");
      Node nextSibling = elem.getNextSibling();
      if (DomHelper.isTextNode(nextSibling)) {
        return Point.inText(nextSibling, 0);
      } else {
        return Point.inElement(parent, nextSibling);
      }
    } finally {
      if (elem != null) {
        elem.removeFromParent();
      }
    }
  }

  @SuppressWarnings("unused") // NOTE(user): may be used later
  private Point<Node> linearSearchForRange(JsTextRangeIE target, Element parent) {
    try {
      // We'll iterate through the parent's children while moving
      // a new collapsed range, attempt, through the points before
      // each child.
      Node child = parent.getFirstChild();
      // Start attempt at beginning of parent
      JsTextRangeIE attempt = JsTextRangeIE.create().moveToElementText(parent).collapse(true);
      while (child != null) {
        // Treat text node children separately
        if (DomHelper.isTextNode(child)) {
          // Move attempt to end of the text node
          int len = child.<Text> cast().getLength();
          attempt.move(character, len);
          // Test if attempt is now at or past target
          if (attempt.compareEndPoints(StartToStart, target) >= 0) {
            // Target is in this text node. Compute the offset by creating a new
            // text range from target to attempt and measuring the length of the
            // text in that range
            JsTextRangeIE dup =
                attempt.duplicate().setEndPoint(StartToStart, target)
                    .setEndPoint(EndToEnd, attempt);
            return Point.inText(child, len - dup.getText().length());
          }
        } else {
          // Child is an element. Move attempt before child, and test
          // if attempt is at or past range
          attempt.moveToElementText(child.<Element> cast()).collapse(true);
          if (attempt.compareEndPoints(StartToStart, target) >= 0) {
            // Return the point before child
            return Point.inElement(parent, child);
          } else {
            // Move attempt past child
            // We use our inline, non-empty marker element to do this.
            // We also leave it in the dom for max reliability until it's needed
            // later, or gets taken out in the finally clause at the end of this method
            child.getParentNode().insertAfter(setter, child);
            // skip pass the setter.
            child = child.getNextSibling();

            attempt.moveToElementText(setter).collapse(false);
          }
        }
        // Move to next child
        child = child.getNextSibling();
      }

      // We didn't find target before or in children; return point at end of
      // parent
      return Point.<Node> end(parent);
      // TODO(user): look out for other corner cases
      // TODO(user, danilatos): implement danilatos' optimisation of first
      // checking inside the last text node that held a point.
      // TODO(user): consider binary rather than linear search for target
      // TODO(user): does this handle the end of a <p>ab[</p><p>]cd</p> type
      // selection?
      // TODO(danilatos): When someone selects the "newline" at the edge
      // of a <p>, e.g. <p>abc[</p><p>]def</p> (where [ ] is the sel)
      // this reports the selection still as <p>abc[]</p><p>def</p>
      // It appears to be a detectable scenario when the first line
      // is not empty, but it isn't when both lines are empty. However,
      // I did notice a difference before when I was experimenting with
      // getBookmark(), so perhaps we could resort to tricks with checking
      // bookmarks around paragraph boundaries...
      // TODO(user, danilatos): consider making attempt and other ranges
      // used here static singletons
    } finally {
      setter.removeFromParent();
    }
  }

  private Point<Node> binarySearchForRange(JsTextRangeIE target, Element parent) {
    try {
      JsTextRangeIE attempt = JsTextRangeIE.create();
      int low = 0;
      int high = parent.getChildCount() - 1;
      while (low <= high) {
        int mid = (low + high) >>> 1;
        Node node = parent.getChild(mid);
        node.getParentNode().insertBefore(setter, node);
        attempt.moveToElementText(setter).collapse(false);
        int cmp = attempt.compareEndPoints(CompareMode.StartToEnd, target);

        if (cmp == 0) {
          if (DomHelper.isTextNode(node)) {
            return Point.inText(node, 0);
          } else {
            return Point.inElement(parent, node);
          }
        } else if (cmp > 0) {
          high = mid - 1;
        } else {
          if (DomHelper.isTextNode(node)) {
            JsTextRangeIE dup = attempt.duplicate();
            dup.setEndPoint(EndToEnd, target);
            if (dup.getText().length() <= node.<Text> cast().getLength()) {
              return Point.inText(node, dup.getText().length());
            }
          } else {
            attempt.moveToElementText(node.<Element> cast()).collapse(false);
            if (attempt.compareEndPoints(StartToStart, target) >= 0) {
              return Point.inElement(parent, node);
            }

          }
          low = mid + 1;
        }
        setter.removeFromParent();
      }
      return Point.<Node> end(parent);
    } finally {
      setter.removeFromParent();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void set(Point<Node> startPoint, Point<Node> endPoint) {
    JsTextRangeIE start = collapsedRangeAtPoint(startPoint);
    JsTextRangeIE end = collapsedRangeAtPoint(endPoint);

    // TODO(danilatos): Should be possible to do this more efficiently,
    //   by separately moving the end point and start point of the range,
    //   instead of creating 2 collapsed ranges.

    JsTextRangeIE.create()
    .setEndPoint(StartToStart, start)
    .setEndPoint(EndToEnd, end)
    .select();
  }

  /** {@inheritDoc} */
  @Override
  void set(Point<Node> point) {
    collapsedRangeAtPoint(point).select();
  }

  /**
   * @param range
   * @return input range  collapsed before setter element
   */
  private JsTextRangeIE collapseBeforeSetter(JsTextRangeIE range) {
    return range
    .moveToElementText(setter)
    .collapse(true);
  }

  /**
   * @param range
   * @param node
   * @return input range collapsed before node
   *
   * Note(user): tempting here to simply do range.moveToElementText(node).collapse(true)
   * when node is an element rather than using setter. This does *not* work in some
   * cases, though, for example when element is the outer div of an image thumbnail,
   * probably because of the contentEditable and unselectable attributes there.
   */
  private JsTextRangeIE collapseBeforeNode(JsTextRangeIE range, Node node) {
    try {
      node.getParentNode().insertBefore(setter, node);
      return collapseBeforeSetter(range);
    } finally {
      setter.removeFromParent();
    }
  }

  /**
   * @param range
   * @param element
   * @return input range collapsed at end of element
   */
  private JsTextRangeIE collapseAtEnd(JsTextRangeIE range, Element element) {
    try {
      element.appendChild(setter);
      return collapseBeforeSetter(range);
    } finally {
      setter.removeFromParent();
    }
  }

  /**
   * @param range
   * @param element
   * @return input range collapsed after element
   */
  @SuppressWarnings("unused")  // TODO(zdwang): Dead code. Left here, as it may be useful later.
  private JsTextRangeIE collapseAfterElement(JsTextRangeIE range, Element element) {
    Node next = element.getNextSibling();
    return (next != null) ?
        collapseBeforeNode(range, next) :
          collapseAtEnd(range, element.getParentElement());
  }

  /**
   * Given a node and an offset, returns a collapsed text range at that point.
   * @param point
   * @return collapsed TextRange
   */
  private JsTextRangeIE collapsedRangeAtPoint(Point<Node> point) {
    assert point != null && point.getContainer() != null;
    JsTextRangeIE range = JsTextRangeIE.create();
    if (point.isInTextNode()) {
      JsTextRangeIE collapsed = collapseBeforeNode(range, point.getContainer());
      collapsed.move(character, point.getTextOffset());
      return collapsed;
    } else {
      Element element = point.getContainer().cast();
      Node child = point.getNodeAfter();
      return child != null ?
          collapseBeforeNode(range, child) :
            collapseAtEnd(range, element);
    }
  }

  @Override
  boolean selectionExists() {
    JsSelectionIE selection = JsSelectionIE.get();
    return !checkNoSelection(selection, selection.createRange());
  }

  /**
   * Strange logic that tells us if the selection exists
   *
   * @param selection
   * @param range {@code selection.getRange()} This is an explicit parameter
   *   to save an object creation, because sometimes we already have this value.
   * @return true if there is no selection
   */
  private boolean checkNoSelection(JsSelectionIE selection, JsTextRangeIE range) {
    // Best test we have found for empty selection
    return None.equals(selection.getType())
        && RootPanel.getBodyElement().equals(range.parentElement());
  }

  @Override
  void saveSelection() {
    savedSelection = JsSelectionIE.get().createRange().getBookmark();
  }

  @Override
  void restoreSelection() {
    JsTextRangeIE range = JsTextRangeIE.create();
    range.moveToBookmark(savedSelection);
    range.select();
  }
}
