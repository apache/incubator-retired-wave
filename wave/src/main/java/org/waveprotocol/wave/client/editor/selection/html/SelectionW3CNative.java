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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.common.util.DomHelper.ElementEditability;

/**
 * Wrapper for W3C selection.
 *
 * http://www.whatwg.org/specs/web-apps/current-work/multipage/editing.html#documentSelection
 *
 */
public class SelectionW3CNative extends JavaScriptObject {
  protected SelectionW3CNative() {
  }

  /**
   * Gets the current selection.
   *
   * NOTE(patcoleman): **IMPORTANT**
   * It is possible the selection nodes will not be within the actual document (e.g. in shadow dom)
   * This should only be used when it is known that the selection is in the page's real DOM,
   * or when the selection is used to set or clear selection, rather than reading it.
   */
  public static native SelectionW3CNative getSelectionUnsafe() /*-{
    return $wnd.getSelection();
  }-*/;

  /**
   * Gets the current selection, trying to place it in correspond actual DOM when the selection
   * itself is reported as not being in the actual document.
   * @return The selection in the page's DOM document, or null if it cannot be calculated.
   */
  public static SelectionW3CNative getSelectionGuarded() {
    SelectionW3CNative selection = getSelectionUnsafe();
    // NOTE(patcoleman) -
    // It is possible for the selection to be in a node in the shadow, which
    //   causes errors whenever you try to read attributes.
    if (selection != null && DomHelper.isUnreadable(selection.anchorNode())) {
      if (UserAgent.isFirefox()) {
        // In firefox, the focus can be practically anywhere, so we give up:
        return null;
      } else if (UserAgent.isWebkit()) {
        // In webkit, the focus should be on the element the shadow dom comes from:
        selection.setCaret(NativeSelectionUtil.getActiveElement(), 0);
        return selection;
      }
      return null; // not sure what anything else does, so be safe and assume we're beyond repair.
    }
    return selection;
  }

  /**
   * Gets the selection range at specified index.
   * @param index
   */
  public final native JsRange getRangeAt(int index) /*-{
    return this.getRangeAt(index);
  }-*/;

  public final native Node anchorNode() /*-{
    return this.anchorNode;
  }-*/;

  public final native Node focusNode() /*-{
    return this.focusNode;
  }-*/;

  public final native int anchorOffset() /*-{
    return this.anchorOffset;
  }-*/;

  public final native int focusOffset() /*-{
    return this.focusOffset;
  }-*/;

  /**
   * Clears the selection.
   */
  public final native void removeAllRanges() /*-{
    this.removeAllRanges();
  }-*/;

  /**
   * Returns the number of ranges selected.
   */
  public final native int rangeCount() /*-{
    return this.rangeCount;
  }-*/;

  /**
   * Add a new range to the selection object.
   * @param jsRange
   */
  public final native void addRange(JsRange jsRange) /*-{
    this.addRange(jsRange);
  }-*/;

  public final void setAnchorAndFocus(Node anchorNode, int anchorOffset,
      Node focusNode, int focusOffset) {
    if (QuirksConstants.HAS_BASE_AND_EXTENT) {
      // NOTE(danilatos): While extend() would also work for webkit,
      // we have to use setBaseAndExtent because it appears to reuse
      // the existing range, rather than needing to replace it, and
      // doing otherwise stuffs up the IME composition state
      setBaseAndExtent(anchorNode, anchorOffset, focusNode, focusOffset);
    } else {

      // We assume that anchor node is in the same focusable area.
      // If not, death will ensue.
      setFocusElement(focusNode);

      // TODO(danilatos): Investigate just using extend() twice - i.e.
      // extend to the anchor -> collapse to end -> extend to the focus.
      JsRange range = JsRange.create();
      range.setStart(anchorNode, anchorOffset);
      range.collapse(true);

      removeAllRanges();
      addRange(range);

      if (focusNode != anchorNode || focusOffset != anchorOffset) {
        try {
            extend(focusNode, focusOffset);
        } catch (JavaScriptException e) {
          NativeSelectionUtil.LOG.error().logPlainText(
              "error extending selection from " + anchorNode + ":" + anchorOffset +
              " to " + focusNode + ":" + focusOffset);
          removeAllRanges();
          range = JsRange.create();
          range.setStart(anchorNode, anchorOffset);
          range.collapse(true);
          range.setEnd(focusNode, focusOffset);
          addRange(range);
        }
      }
    }
  }

  public final void setCaret(Node node, int offset) {
    // See notes in setAnchorAndFocus(Node, int, Node, int)
    if (QuirksConstants.HAS_BASE_AND_EXTENT) {
      setBaseAndExtent(node, offset, node, offset);
    } else {
      // Required by firefox. Sigh.
      setFocusElement(node);

      // TODO(danilatos): Investigate just using extend() twice - i.e.
      // extend to the anchor -> collapse to end -> extend to the focus.
      JsRange range = JsRange.create();
      range.setStart(node, offset);
      range.collapse(true);

      removeAllRanges();
      addRange(range);
    }
  }

  /**
   * Supported by at least FF and Webkit
   *
   * Warning: The focus must not be the same as the current anchor, or firefox
   * will throw an exception. Also, the currently editable region must have
   * focus, or firefox will throw an exception.
   *
   * @param focusNode
   * @param focusOffset
   */
  private final native void extend(Node focusNode, int focusOffset) /*-{
    this.extend(focusNode, focusOffset);
  }-*/;

  /**
   * Sets the selection to a single range.
   * NOTE(patcoleman): ** Not all browsers, guard with QuirksConstants.HAS_BASE_AND_EXTENT **
   */
  private final native void setBaseAndExtent(Node anchorNode, int anchorOffset,
      Node focusNode, int focusOffset) /*-{
    this.setBaseAndExtent(anchorNode, anchorOffset, focusNode, focusOffset);
  }-*/;


  /**
   * Ensure that the rendered content view's DOM has focus
   *
   * NOTE(patcoleman): Fixes firefox bug that causes invalid selections while
   * mutating DOM that doesn't have focus - fixed by finding the next parent element directly
   * editable, and forcing this to have the focus.
   */
  private static void setFocusElement(Node node) {
    if (UserAgent.isFirefox()) {
      // adjust to parent if node is a text node
      Element toFocus = null;
      if (DomHelper.isTextNode(node)) {
        toFocus = node.getParentElement();
      } else {
        toFocus = node.<Element>cast();
      }

      // traverse up until we have a concretely editable element:
      while (toFocus != null &&
          DomHelper.getContentEditability(toFocus) != ElementEditability.EDITABLE) {
        toFocus = toFocus.getParentElement();
      }
      // then focus it:
      if (toFocus != null) {
        DomHelper.focus(toFocus);
      }
    }
  }
}
