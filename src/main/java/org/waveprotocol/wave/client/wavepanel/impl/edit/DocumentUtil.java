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


package org.waveprotocol.wave.client.wavepanel.impl.edit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.HtmlSelectionHelperImpl;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.selection.content.PassiveSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.Tx;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.document.util.TextLocator;

/**
 * ContentDocument utilities.
 */
public final class DocumentUtil {
  // Private constructor for utility class.
  private DocumentUtil() {
  }

  /** Creates a selection helper for a document. */
  private static SelectionHelper createSelectionHelper(ContentDocument doc) {
    Element e = doc.getFullContentView().getDocumentElement().getImplNodelet();
    HtmlSelectionHelper htmlSelection = new HtmlSelectionHelperImpl(e);
    return new PassiveSelectionHelper(
        htmlSelection, doc.getNodeManager(), doc.getRenderedView(), doc.getLocationMapper());
  }

  /**
   * Checks whether an editor has a selected range.
   *
   * @return false if the editor has no selection, or the selection is
   *         collapsed.
   */
  public static boolean hasRangeSelected(ContentDocument doc) {
    FocusedRange range = createSelectionHelper(doc).getSelectionRange();
    return range == null ? false : !range.isCollapsed();
  }

  /**
   * Finds a location in a document, near the focus of the browser selection,
   * suitable for inserting a doodad. The scanning code attempts not to break
   * words, and will move from the focus location towards the end of the
   * document until a suitable location is found, potentially jumping outside
   * the nearest line container.
   *
   * @param content document in which to locate the selection
   * @return the point offset, or -1 for no point (e.g., when there is no
   *         selection).
   */
  public static int getLocationNearSelection(ContentDocument content) {
    // Note: there is a small chance that the selection location reported here
    // is with respect to the current document state, out of which there may be
    // operations that have not yet been extracted by the typing extractor. The
    // problem is that we can't always safely force the typing extractor to
    // extract out those operations. Therefore, we assume that most of the time,
    // doodads are inserted via selection on a non-editing document.
    ContentRange selectionPoints = createSelectionHelper(content).getOrderedSelectionPoints();
    CMutableDocument document = content.getMutableDoc();
    ContentView view = content.getPersistentView();
    return DocumentUtil.getLocationNearSelection(document, view, selectionPoints);
  }

  @VisibleForTesting
  static <N, E extends N, T extends N> int getLocationNearSelection(
      MutableDocument<N, E, T> document, ReadableDocumentView<N, E, T> view,
      PointRange<N> selectionPoints) {
    // Note: there are some cases when content is not selected when this method
    // is called, i.e. when the selection is inside a transparent node. Until
    // then, be defensive and keep this check.
    if (selectionPoints == null) {
      return -1;
    }

    Point<N> second = selectionPoints.getSecond();
    // Special case: the second endpoint being at beginning of line
    if (!selectionPoints.isCollapsed()
        && LineContainers.isAtLineStart(document, DocHelper.getFilteredPoint(view, second))) {
      // Move to end of previous line if such exists
      E line = LineContainers.getRelatedLineElement(document, second);
      if (!LineContainers.isFirstLine(document, line)) {
        second = Point.before(document, line);
      }
    }

    // Find suitable insertion point near the end of the selection
    Point<N> point = DocHelper.getFilteredPoint(view, second);
    if (point.isInTextNode()) {
      Tx<N> textPoint = point.asTextPoint();

      // If there is inline whitespace on the left, then move the point until no
      // more inline whitespace. Then, move the point to the right while it
      // points to non-inline whitespace.
      textPoint = TextLocator.findCharacterBoundary(
          document, textPoint, TextLocator.NON_WHITESPACE_MATCHER, false);
      textPoint = TextLocator.findCharacterBoundary(
          document, textPoint, TextLocator.WHITESPACE_MATCHER, true);
      point = textPoint;
    }
    point = LineContainers.jumpOutToContainer(document, point);
    return point == null ? -1 : document.getLocation(point);
  }
}
