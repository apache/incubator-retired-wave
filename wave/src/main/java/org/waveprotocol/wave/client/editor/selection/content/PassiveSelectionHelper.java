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

package org.waveprotocol.wave.client.editor.selection.content;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.FilteredView;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.RangeTracker;

/**
 * A selection helper that tries to do some selection correction, but will
 * never create operations or change the content structure, so is not guaranteed
 * to be able to return a valid selection under all circumstances.
 *
 * @see SelectionHelper
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class PassiveSelectionHelper implements SelectionHelper {
  /** Filters to rendered notes where the cursor can be placed. */
  static class ValidSelectionContainerView extends
      FilteredView<ContentNode, ContentElement, ContentTextNode>  {

    public ValidSelectionContainerView(ContentView rawView){
      super(rawView);
    }

    @Override
    protected Skip getSkipLevel(ContentNode node) {
      if (node.isRendered()) {
        // Black list text:
        if (asText(node) != null) {
          return Skip.DEEP;
        }

        // Black list - for now at least, we don't want to prevent the cursor from going in
        // any arbitrary elements, just the ones that are definitely known to be invalid.
        return isKnownInvalidTopContainerForCursor(node) ? Skip.SHALLOW : Skip.NONE;
      } else {
        return Skip.DEEP;
      }
    }

  }

  static LoggerBundle logger = EditorStaticDeps.logger;

  final HtmlSelectionHelper htmlHelper;

  final LocationMapper<ContentNode> mapper;

  final NodeManager nodeManager;

  final ContentView renderedContentView;

  boolean needsCorrection;

  private RangeTracker savedSelection;

  /**
   * @param htmlHelper Low level helper for the html layer of selection getting
   */
  public PassiveSelectionHelper(HtmlSelectionHelper htmlHelper, NodeManager nodeManager,
      ContentView renderedContentView, LocationMapper<ContentNode> locationMapper) {

    this.htmlHelper = htmlHelper;
    this.mapper = locationMapper;
    this.nodeManager = nodeManager;
    this.renderedContentView = renderedContentView;
  }

  /** {@inheritDoc} */
  public void clearSelection() {
    NativeSelectionUtil.clear();
  }

  /** {@inheritDoc} */
  public FocusedContentRange getSelectionPoints() {
    FocusedPointRange<Node> range = htmlHelper.getHtmlSelection();
    try {
      needsCorrection = false;

      range = SelectionUtil.filterNonContentSelection(range);

      if (range == null) {
        return null;
      }
      Point<ContentNode>
        anchor = nodeletPointToFixedContentPoint(range.getAnchor()),
        focus = range.isCollapsed()
              ? anchor : nodeletPointToFixedContentPoint(range.getFocus());

      if (anchor == null || focus == null) {
        return null;
      }

      // Uncomment for verbose debugging
      // if (Debug.isOn(LogSeverity.DEBUG)) {
      //   logger.logXml("SELECTION: " + start + " - " + end);
      // }

      FocusedContentRange ret = range.isCollapsed()
          ? new FocusedContentRange(anchor) : new FocusedContentRange(anchor, focus);

      if (needsCorrection && ret != null) {
        setSelectionPoints(ret.getAnchor(), ret.getFocus());
      }

      return ret;
    } finally {
      needsCorrection = false;
    }
  }

  @Override
  public ContentRange getOrderedSelectionPoints() {
    FocusedContentRange selection = getSelectionPoints();
    return selection == null ? null : selection.asOrderedRange(NativeSelectionUtil.isOrdered());
  }

  /** {@inheritDoc} */
  public FocusedRange getSelectionRange() {
    FocusedContentRange contentRange = getSelectionPoints();
    if (contentRange == null) {
      return null;
    }
    return new FocusedRange(
        mapper.getLocation(contentRange.getAnchor()),
        mapper.getLocation(contentRange.getFocus())
    );
  }

  @Override
  public Range getOrderedSelectionRange() {
    FocusedRange selection = getSelectionRange();
    return selection != null ? selection.asRange() : null;
  }

  /** {@inheritDoc} */
  public void setSelectionRange(FocusedRange selection) {
    if (selection != null) {
      Point<ContentNode> anchor = mapper.locate(selection.getAnchor());
      Point<ContentNode> focus = selection.isCollapsed() ? anchor
          : mapper.locate(selection.getFocus());
      setSelectionPoints(anchor, focus);
    }
  }

  @Override
  public void setCaret(int caret) {
    Point<ContentNode> collapsed = mapper.locate(caret);
    setCaret(collapsed);
  }

  /**
   * First check if the content point is attached, then- If it is a content text
   * point, check that the offset is <= length. If it is a Content element
   * point, assert that nodeAfter is a child of the container.
   *
   * NOTE(user): This is not a catch-all check, but should catch most cases,
   * add more checks here as needed.
   *
   * @param cp
   * @return true if the point is valid
   */
  public boolean isValidSelectionPoint(Point<ContentNode> cp) {
    if (!cp.getContainer().isContentAttached()) {
      return false;
    }

    if (cp.getContainer().isTextNode()) {
      ContentTextNode textNode = (ContentTextNode) cp.getContainer();
      return cp.getTextOffset() <= textNode.getLength();
    } else {
      ContentNode nodeAfter = cp.getNodeAfter();
      return nodeAfter == null || cp.getContainer() == nodeAfter.getParentElement();
    }
  }

  /** {@inheritDoc} */
  public void setSelectionPoints(Point<ContentNode> anchor, Point<ContentNode> focus) {
    boolean collapsed = (anchor == focus);
    anchor = findOrCreateValidSelectionPoint2(anchor);
    focus = collapsed ? anchor : findOrCreateValidSelectionPoint2(focus);
    Point<Node> nodeletAnchor = anchor != null
        ? nodeManager.wrapperPointToNodeletPoint(anchor) : null;

    if (nodeletAnchor != null) {
      Point<Node> nodeletFocus = anchor == focus ? nodeletAnchor :
          nodeManager.wrapperPointToNodeletPoint(focus);

      if (nodeletFocus != null) {
        FocusedPointRange<Node> range = new FocusedPointRange<Node>(nodeletAnchor, nodeletFocus);
        // Ignore if there is no matching html location
        if (range != null) {
          NativeSelectionUtil.set(range);
        }
      }
    }

    // TODO(user): investigate the cause of the loop.
    if (savedSelection != null) {
      FocusedRange range = new FocusedRange(mapper.getLocation(anchor), mapper.getLocation(focus));
      savedSelection.trackRange(range);
    }
  }

  /**
   * Saves the current selection in the range tracker.
   *
   * NOTE(danilatos): This could be optimised to use the inputs to the various
   * selection setting methods, but they are four different versions so the code
   * would be somewhat uglier. Do it if speed becomes a problem.
   */
  private void saveSelection() {
    if (savedSelection != null) {
      FocusedRange range = getSelectionRange();
      if (range != null) {
        savedSelection.trackRange(range);
      }
    }
  }

  /** {@inheritDoc} */
  public void setCaret(Point<ContentNode> caret) {
    if (caret == null) {
      throw new IllegalArgumentException("setCaret: caret may not be null");
    }

    caret = findOrCreateValidSelectionPoint2(caret);
    Point<Node> nodeletCaret = // check if we have a place:
      (caret == null ? null : nodeManager.wrapperPointToNodeletPoint(caret));

    // Ignore if there is no matching html location
    if (nodeletCaret != null) {
      NativeSelectionUtil.setCaret(nodeletCaret);
    }

    saveSelection();
  }

  /**
   * Finds a Point given a nodelet/offset pair. Internally traps inconsistency
   * exceptions and does its best to give a useful answer anyway. Takes note of
   * any inconsistency exceptions and schedules a check of the vicinity later
   * on, to possibly repair if there is still a problem. Might also call flush
   * and try again, only if it is the aggressive implementation.
   *
   * Also determines if this is a valid place for a selection, and if not,
   * adjusts accordingly, possibly even changing the document. This means it may
   * have side effects. Use other methods that don't have side effects if you
   * don't want them.
   *
   * Rationale for having it deal with inconsistencies here: It is used often at
   * the start of a typing sequence, but if the user is hammering the keyboard,
   * sometimes dom nodes seem to appear before we deal with the key events. Also
   * happens normally with IME.
   *
   * @param point
   * @return ContentNode
   */
  private Point<ContentNode> nodeletPointToFixedContentPoint(Point<Node> point) {
    Point<ContentNode> ret;

    try {
      try {
        ret = nodeManager.nodeletPointToWrapperPoint(point);

      } catch (RuntimeException e) {
        // Safe to catch - the guarded code should be stateless
        logger.error().log("CAUGHT RUNTIME EXCEPTION in nodeletPointToFixedContentPoint " + e);
        assert false : "" + e;
        // maybe call flush and try again, if we are in aggressive mode
        ret = nodeletPointToWrapperPointAttempt2(point);
      }

      // TODO(danilatos): There are cases where HtmlInserted and HtmlMissing are
      // thrown and caught here under ABNORMAL circumstances, as opposed to normal.
      // In those cases, we should probably be doing a repair as well.
    } catch (HtmlInserted e) {
      // This might not be accurate, but usually will be. I can't think of anything
      // too nasty that could happen if this isn't accurate, the worst is a no-op
      // when we would have normally done some typing extraction or whatnot.
      // These comments and todos below also apply to the other catch statement.
      // TODO(danilatos): Investigate this further
      // It's possible we won't get the content point as a text point
      // in some cases, when we would like to. Improve this. However the typing extractor
      // is designed to cope with this. I'm not sure how well that scenario is tested though.
      // Figure out a way to unit test this stuff. Usually it only ever
      // comes up when someone is smashing the keyboard...
      ret = e.getContentPoint();
      needsCorrection = true;
    } catch (HtmlMissing e) {
      ret = Point.before(renderedContentView, e.getBrokenNode());
      needsCorrection = true;
    }
    // It's highly unlikely that ret.getContainer() could ever be null, but it technically,
    // at least for now, is a valid point (refers to either before or after the root element.
    // We can later on choose to assert that it is never null in Point's constructor,
    // if we decide such points are always invalid.
    if (ret == null || ret.getContainer() == null) {
      return null;
    }

    if (ret.isInTextNode()) {
      int textNodeLength = ret.getContainer().asText().getLength();
      if (ret.getTextOffset() > textNodeLength) {
        //
        String consistency;
        if (htmlHelper instanceof EditorImpl) {
          consistency = (((EditorImpl) htmlHelper).isConsistent() ? "YES" : "NO");
        } else {
          // This means htmlHelper isn't an editor, so we don't have access to that info.
          // Find another way if this comes up.
          consistency = "(no editor available)";
        }

        logger.error().log("Text offset too big for text node, " +
            "editor consistency: '" + consistency + "'");

        ret = Point.inText(ret.getContainer(), textNodeLength);
      }
    } else if (isKnownInvalidTopContainerForCursor(ret.getContainer())) {
      // We need to correct the selection, it should never be
      // at the top level.
      ret = findOrCreateValidSelectionPoint(ret.asElementPoint());
      needsCorrection = true;
    }
    assert ret == null || ret.getContainer() != null;
    return ret;
  }

  /**
   * Override this to do something more advanced if we came across a text node
   * that isn't represented in the content. E.g. call flush and try again.
   *
   * @throws HtmlMissing
   * @throws HtmlInserted
   */
  protected Point<ContentNode> nodeletPointToWrapperPointAttempt2(Point<Node> point)
      throws HtmlInserted, HtmlMissing {
    return null;
  }

  /**
   * Takes a point where the selection should not be and finds a nearby location
   * that is valid.
   *
   * It should not return a point that is in a different "region" from the given
   * input, where selecting across regions does not work in some browsers. For
   * example, if the invalid point is outside a p, inside the top level
   * document, it should not simply go inside an adjacent image thumbnail
   * caption. (very unlikely anyway). More likely might be, it should not go
   * inside an adjacent table cell, IF they end up being implemented as separate
   * editable regions.
   *
   * IMPORTANT: This method may also change the dom, if there is no valid place
   * to put a cursor! (This is kind of like doing a repair)
   *
   * Current implementation assumptions: The reason the input is invalid is
   * because its container node is the top node in a multi-line region. For
   * example, the document element. A td might be another example, if it is
   * implemented as multiline requiring p elements inside it. An image caption
   * is not, because it is not multiline.
   *
   * The implementation does not assume that the input is invalid because it is
   * in an area that is not editable, for example between an image and its
   * caption. This is not yet known to occur in practice, so is ignored for now.
   *
   * @param point a possibly invalid selection point
   * @return a valid selection point, or null if one is neither found nor created
   */
  @VisibleForTesting
  Point<ContentNode> findOrCreateValidSelectionPoint(Point.El<ContentNode> point) {
    // TODO(patcoleman): refactor this into cleaner code - possible separating find and create.
    ValidSelectionContainerView validContainerView =
      new ValidSelectionContainerView(renderedContentView);
    ContentElement container = (ContentElement) point.getContainer();
    assert renderedContentView.getVisibleNode(container) == container : "Container: " + container;

    // Valid position for cursor, so stop where we are:
    if (!isKnownInvalidTopContainerForCursor(container)) {
      return point;
    }

    ContentNode nodeAfter = point.getNodeAfter();
    ContentNode newContainer;

    if (nodeAfter == null) {
      // place the cursor in the right-most valid child of the current container
      newContainer = validContainerView.getLastChild(container);
    } else {
      // we want to place the cursor at the end of the previous node
      newContainer = validContainerView.getVisibleNodePrevious(nodeAfter);
      if (newContainer != null) {
        // Special-case: if nodeAfter is already valid, find the previous valid point:
        if (newContainer.equals(nodeAfter)) {
          // Check to see if we've found ourselves before a visible, valid point:
          newContainer = validContainerView.getVisibleNodePrevious(nodeAfter.getParentElement());
          if (newContainer == nodeAfter.getParentElement()) {
            return Point.before(validContainerView, nodeAfter);
          }
        }
        // otherwise, use the end of this previous node.
        if (newContainer != null) {
          return Point.end(newContainer);
        }
      }

      // if placing just before didn't work, try at the start of the next valid container:
      newContainer = validContainerView.getVisibleNodeFirst(nodeAfter);
      if (newContainer != null) {
        return Point.inElement(newContainer, renderedContentView.getFirstChild(newContainer));
      }
    }

    // can't place before or after, so handle specially, maybe creating one if required:
    if (newContainer == null) {
      newContainer = maybePlaceMissingCursorContainer(point);
    }
    return newContainer != null ? Point.end(newContainer) : null;
  }

  Point<ContentNode> findOrCreateValidSelectionPoint2(Point<ContentNode> point) {
    Point.El<ContentNode> asElementPoint = point.asElementPoint();
    return asElementPoint == null ? point : findOrCreateValidSelectionPoint(asElementPoint);
  }

  /**
   * Will return true if the container is definitely known to be an invalid selection container.
   *
   * Returning false implies nothing.
   */
  private static boolean isKnownInvalidTopContainerForCursor(ContentNode node) {
    if (node == null) {
      return true;
    }
    // Document root and line containers are known to be invalid.
    return node.getParentElement() == null || LineRendering.isLineContainerElement(node);
  }

  /**
   * Override this to possibly insert a missing paragraph so the selection has
   * somewhere to go
   *
   * @return new paragraph, or null
   */
  protected ContentElement maybePlaceMissingCursorContainer(Point.El<ContentNode> at) {
    return null;
  }

  /** {@inheritDoc} */
  public Point<ContentNode> getFirstValidSelectionPoint() {
    ContentElement root = renderedContentView.getDocumentElement();
    // Must use filtered view, because of assertion in findOrCreateValidSelectionPoint
    ContentNode first = renderedContentView.getFirstChild(root);

    // assert there's no transparent wrapper, which would render the point invalid
    // for many uses
    assert first == null || first.getParentElement() == root;

    Point<ContentNode> point = findOrCreateValidSelectionPoint(Point.inElement(root, first));
    if (point == null) {
      throw new RuntimeException("Could not create a valid selection point!");
    }
    return point;
  }

  /** {@inheritDoc} */
  public Point<ContentNode> getLastValidSelectionPoint() {
    Point<ContentNode> point = findOrCreateValidSelectionPoint(
        Point.<ContentNode>end(renderedContentView.getDocumentElement()));
    if (point == null) {
      throw new RuntimeException("Could not create a valid selection point!");
    }
    return point;
  }

  public void setSelectionTracker(RangeTracker tracker) {
    savedSelection = tracker;
  }
}
