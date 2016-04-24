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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.impl.NodeManager;

import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class SelectionUtil {

  /**
   * Attempt to place the caret before the element, or in as near a valid
   * selection location as possible.
   */
  // TODO(danilatos): Should it be the selection helper's job to do this
  // filtering to ensure a valid selection?
  public static void placeCaretBeforeElement(SelectionHelper selection, ContentElement element) {
    selection.setCaret(
        DocHelper.getFilteredPoint(element.getRenderedContentView(),
            Point.<ContentNode>inElement(element.getParentElement(), element)));
  }

  /**
   * Attempt to place the caret after the element, or in as near a valid
   * selection location as possible.
   */
  // TODO(danilatos): Should it be the selection helper's job to do this
  // filtering to ensure a valid selection?
  public static void placeCaretAfterElement(SelectionHelper selection, ContentElement element) {
    selection.setCaret(
        DocHelper.getFilteredPoint(element.getRenderedContentView(),
            Point.<ContentNode>inElement(element.getParentElement(), element.getNextSibling())));
  }

  /**
   * Takes an html selection and returns it, or null if it's not related to editor content.
   *
   * @param htmlSelection Selection range to filter.
   * @return htmlSelection or null if there's no related content.
   */
  public static FocusedPointRange<Node> filterNonContentSelection(
      FocusedPointRange<Node> htmlSelection) {
    if (htmlSelection == null) {
      return null; // quick exit
    }

    // get just the focus point, finding the element it is inside.
    Point<Node> htmlFocus = htmlSelection.getFocus();
    Element el;
    if (htmlFocus.isInTextNode()) {
      el = htmlFocus.getContainer().getParentElement();
    } else {
      el = htmlFocus.getContainer().cast();
    }

    // Assume given range is always in the editor, the htmlHelper should guarantee that.
    while (!NodeManager.hasBackReference(el)) {
      if (NodeManager.getTransparency(el) == Skip.DEEP
          || el.getPropertyBoolean(ContentElement.COMPLEX_IMPLEMENTATION_MARKER)) {

        // Exception: when we explicitly want the selection still to be reported
        if (!NodeManager.mayContainSelectionEvenWhenDeep(el)) {
          htmlSelection = null;
          break;
        }
      }
      el = el.getParentElement();
    }

    return htmlSelection;
  }

  private SelectionUtil() {}
}
