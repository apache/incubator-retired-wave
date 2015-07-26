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

package org.waveprotocol.wave.client.editor.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Provides a more restricted and higher level interface for EditorEventHandler
 * to interact with the editor.
 *
 */
public interface EditorInteractor {
  /**
   * Give editor's listeners a chance to preview the event and
   * prevent it from handling it
   *
   * @returns true iff handled
   */
  boolean notifyListeners(SignalEvent event);

  /**
   * Returns whether the editor is in editing mode.
   */
  boolean isEditing();

  /**
   * @return true if the typing extractor is pending a flush
   */
  boolean isTyping();

  /**
   * @return true if browser-triggered mutation events are expected, e.g.
   * when the typing extractor has already been notified.
   * Contrast with {@link #shouldIgnoreMutations()}
   */
  boolean isExpectingMutationEvents();

  /**
   * Contrast with {@link #isExpectingMutationEvents()}
   * @return true if mutations should be ignored because they are being triggered programmatically
   */
  boolean shouldIgnoreMutations();

  /**
   * Notify the typing extractor that changes have occured at caret.
   * @param caret
   * @param useHtmlCaret temporary hack - the content caret cannot safely be ascertained,
   *   use the html caret directly instead and ignore the caret parameter.
   * @param isReplace indicates whether the delete is actually the first half of a replace.
   * @return false if the event should be allowed (typing extractor correctly notified)
   *   true if it should be cancelled (problem notifying typing extractor).
   */
  boolean notifyTypingExtractor(Point<ContentNode> caret, boolean useHtmlCaret, boolean isReplace);

  /**
   * Force editor to flush.
   */
  void forceFlush();

  /**
   * Delete range.
   * @param first
   * @param second
   * @param isReplace indicates whether the delete is actually the first half of a replace.
   */
  Point<ContentNode> deleteRange(Point<ContentNode> first, Point<ContentNode> second,
      boolean isReplace);

  /**
   * Insert text into the document
   * @param at
   * @param text
   * @param isReplace indicates whether the insert is actually the first half of a replace.
   */
  Point<ContentNode> insertText(Point<ContentNode> at, String text, boolean isReplace);

  /**
   * Find the ContentNode corresponding to a dom node.
   * @param target
   */
  ContentElement findElementWrapper(Element target);

  /**
   * Normalizes a point so that it is biased towards text nodes, and node ends
   * rather than node start.
   * @param caret
   */
  Point<ContentNode> normalizePoint(Point<ContentNode> caret);

  /**
   * Get selection points.
   */
  FocusedContentRange getSelectionPoints();

  /**
   * Check whether the selection is within a content area.
   */
  boolean hasContentSelection();

  /**
   * @return true if the focus is after the anchor (or the selection is collapsed)
   */
  boolean selectionIsOrdered();

  /**
   * Set caret.
   * @param caret
   */
  void setCaret(Point<ContentNode> caret);

  /**
   * The user typed at the end of a link, and the hack described in
   * {QuirksConstants#LIES_ABOUT_CARET_AT_LINK_END_BOUNDARY} occurred.
   *
   * @param textNode
   */
  void noteWebkitEndOfLinkHackOccurred(Text textNode);

  /**
   * @return current html selection
   */
  FocusedPointRange<Node> getHtmlSelection();

  /**
   * Reset the annotations on the editor's caret
   */
  void clearCaretAnnotations();

  /**
   * Word delete from the specified location to the end of the word.
   * @param caret
   */
  void deleteWordStartingAt(Point<ContentNode> caret);

  /**
   * Word delete from the start of the word to the specified location.
   * @param caret
   */
  void deleteWordEndingAt(Point<ContentNode> caret);

  /**
   * IME composition commenced at the given location
   * @param caret
   */
  void compositionStart(Point<ContentNode> caret);

  /**
   * Composition state updated
   */
  void compositionUpdate();

  /**
   * IME composition completed
   *
   * @return the new selection
   */
  FocusedContentRange compositionEnd();

  /**
   * Adds an undo checkpoint
   */
  void checkpoint(FocusedContentRange currentRange);

  /**
   * Rebias the selection, given an initial default direction to hint at how it should be.
   */
  void rebiasSelection(CursorDirection defaultDirection);
}
