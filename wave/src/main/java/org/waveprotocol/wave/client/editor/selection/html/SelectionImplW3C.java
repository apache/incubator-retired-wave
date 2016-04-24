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

import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Firefox/Webkit common implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SelectionImplW3C extends SelectionImpl {

  /**
   * Shorthand selection debug logger
   */
  @SuppressWarnings("hiding")
  static LoggerBundle logger = NativeSelectionUtil.LOG;

  // NOTE(danilatos): This doesn't handle multiple ranges. Not a big worry
  // for now anyway.
  private FocusedPointRange<Node> savedSelection;

  /**
   * Constructor
   */
  SelectionImplW3C() {
    logger.trace().log("Constructed SelectionImplW3C");
  }

  /**
   * Fast implementation to check if there is a selection or not.
   * @return true if there is a selection
   */
  @Override
  boolean selectionExists() {
    return nativeCheckSelectionExists();
  }

  private static native boolean nativeCheckSelectionExists() /*-{
    var selection = $wnd.getSelection();
    return selection.rangeCount > 0;
  }-*/;

  @Override
  FocusedPointRange<Node> get() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    if (selection == null) {
      // NOTE(danilatos): This seems to happen very rarely with FF.
      // I wonder what the reproducible cause is.
      return null;
    }
    Node anchorNode = selection.anchorNode();
    Node focusNode = selection.focusNode();
    return focusNode == null ? null : constructRange(
        anchorNode, selection.anchorOffset(),
        focusNode, selection.focusOffset());
  }

  @Override
  PointRange<Node> getOrdered() {
    JsRange jsRange = getSelectionRange();
    return toOrderedPointRange(jsRange);
  }

  @Override
  boolean isOrdered() {
    JsRange jsRange = getSelectionRange();
    if (jsRange == null) {
      return true;
    }
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    Node anchorNode = selection.anchorNode();
    int anchorOffset = selection.anchorOffset();
    boolean ret = anchorNode == jsRange.startContainer() && anchorOffset == jsRange.startOffset();

    // check that if the anchor doesn't match the start, then the focus does.
    assert ret || focusIsAtStart(selection, jsRange);

    return ret;
  }

  boolean focusIsAtStart(SelectionW3CNative selection, JsRange jsRange) {
    Node focusNode = selection.focusNode();
    int focusOffset = selection.focusOffset();
    return focusNode == jsRange.startContainer() && focusOffset == jsRange.startOffset();
  }

  /**
   * Gets the native JsRange for the current selection.
   */
  JsRange getSelectionRange() {
    SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
    return selection != null && selection.rangeCount() > 0 ? selection.getRangeAt(0) : null;
  }

  /**
   * Construct a point range from a js range
   */
  public static PointRange<Node> toOrderedPointRange(JsRange jsRange) {
    if (jsRange == null) {
      return null;
    }
    return new PointRange<Node>(
        DomHelper.nodeOffsetToNodeletPoint(jsRange.startContainer(), jsRange.startOffset()),
        DomHelper.nodeOffsetToNodeletPoint(jsRange.endContainer(), jsRange.endOffset()));
  }

  @Override
  void set(Point<Node> anchor, Point<Node> focus) {

    int anchorOffset;
    int focusOffset;
    JsRange range = JsRange.create();

    Node anchorNode = anchor.getContainer();
    Node focusNode = focus.getContainer();

    assert anchorNode != null : "Anchor node must not be null.";
    assert focusNode != null : "Focus node must not be null.";

    if (anchor.isInTextNode()) {
      anchorOffset = anchor.getTextOffset();
    } else if (anchor.getNodeAfter() == null) {
      anchorOffset = anchorNode.getChildCount();
    } else {
      anchorNode = anchor.getNodeAfter().getParentElement();
      range.setStartBefore(anchor.getNodeAfter());
      anchorOffset = range.startOffset();
    }

    if (focus.isInTextNode()) {
      focusOffset = focus.getTextOffset();
    } else {
      Node focusNodeAfter = focus.getNodeAfter();
      if (focusNodeAfter == null) {
        focusOffset = focusNode.getChildCount();
      } else {
        focusNode = focusNodeAfter.getParentElement();
        assert focusNode != null : "focus node must not be null";
        range.setStartBefore(focusNodeAfter);
        focusOffset = range.startOffset();
      }
    }

    SelectionW3CNative.getSelectionUnsafe().setAnchorAndFocus(
        anchorNode, anchorOffset, focusNode, focusOffset);
  }

  @Override
  void set(Point<Node> focus) {

    int anchorOffset;
    int focusOffset;
    JsRange range = JsRange.create();

    Node focusNode = focus.getContainer();

    if (focus.isInTextNode()) {
      focusOffset = focus.getTextOffset();
    } else if (focus.getNodeAfter() == null) {
      focusOffset = focusNode.getChildCount();
    } else {
      range.setStartBefore(focus.getNodeAfter());
      focusOffset = range.startOffset();
    }

    SelectionW3CNative.getSelectionUnsafe().setCaret(focusNode, focusOffset);
  }

  /**
   * Clears selection
   */
  @Override
  void clear() {
    SelectionW3CNative.getSelectionUnsafe().removeAllRanges();
  }

  /**
   * Factory method for JSNI use
   *
   * @param anchorNode
   * @param anchorOffset
   * @param focusNode
   * @param focusOffset
   * @return new Range object
   */
  private static FocusedPointRange<Node> constructRange(
      Node anchorNode, int anchorOffset,
      Node focusNode, int focusOffset) {
    return new FocusedPointRange<Node>(
        DomHelper.nodeOffsetToNodeletPoint(anchorNode, anchorOffset),
        DomHelper.nodeOffsetToNodeletPoint(focusNode, focusOffset));
  }

  @Override
  void saveSelection() {
    savedSelection = get();
  }

  @Override
  void restoreSelection() {
    if (savedSelection != null) {
      set(savedSelection.getAnchor(), savedSelection.getFocus());
    }
  }
}
