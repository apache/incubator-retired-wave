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

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ClientDocumentContext.EditingConcerns;
import org.waveprotocol.wave.client.editor.content.ExtendedClientDocumentContext.LowLevelEditingConcerns;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;

import org.waveprotocol.wave.model.document.indexed.SizedObject;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Class to maintain the native selection across dom mutations, with minimal use
 * of actually setting the selection to restore it (because this interferes with
 * IMEs)
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SelectionMaintainer {

  private EditingConcerns editingConcerns;

  private final SizedObject document;

  SelectionMaintainer(SizedObject document) {
    this.document = document;
  }

  public void attachEditor(EditingConcerns sadPackage) {
    this.editingConcerns = sadPackage;
  }

  public void detachEditor() {
    editingConcerns = null;
  }

  /**
   * Describes a change to a text nodelet
   */
  public enum TextNodeChangeType {
    /** insert or delete data */
    DATA,
    /** split the node */
    SPLIT,
    /** move the node */
    MOVE,
    /** delete the node */
    REMOVE,
    /** call setData() */
    REPLACE_DATA
  }

  // Saved selection information.
  // There is some redundancy in the information here, the text node and offset might
  // be updated, the point is only valid for element points.
  // TODO(danilatos): Clean this up, without creating new point objects on every change.
  // Use a mutable point class, when one exists.

  private Point<Node> savedSelectionAnchor = null;
  private Text savedSelectionAnchorTextNodelet = null;
  private int savedSelectionAnchorOffset;

  private Point<Node> savedSelectionFocus = null;
  private Text savedSelectionFocusTextNodelet = null;
  private int savedSelectionFocusOffset;

  private boolean needToRestoreSelection = false;

  private FocusedRange savedSelection;

  /**
   * Bad to actually save and restore selections in a nested fashion. Only do it
   * for the outer most calls to matching save and restore.
   */
  private int savedSelectionDepth = 0;

  boolean isNested() {
    return savedSelectionDepth > 0;
  }

  /**
   * For recovery purposes only. Delete later, if no 3rd eye reports from errors
   * logged at call sites.
   */
  void hackForceClearDepth() {
    savedSelectionDepth = 0;
  }

  void startDontSaveSelection() {
    savedSelectionDepth++;
  }

  void endDontSaveSelection() {
    savedSelectionDepth--;
  }


  /**
   * Enter a block where the selection will be preserved against the adverse
   * effects of dom mutation.
   *
   * Must be balanced with a call to either {@link #restoreSelection()} or
   * {@link #restoreSelection(DocOp)}
   *
   * Code that mutates the HTML dom must be guarded by calls to save and
   * restore.
   */
  public void saveSelection() {
    if (editingConcerns != null && savedSelectionDepth == 0) {
      needToRestoreSelection = false;
      SelectionHelper helper = editingConcerns.getSelectionHelper();

      // Sometimes the document is not in an editing context, in which
      // case there is no selection helper.
      if (helper != null) {
        FocusedPointRange<Node> htmlSelection = NativeSelectionUtil.get();
        if (htmlSelection != null) {
          savedSelectionAnchor = htmlSelection.getAnchor();
          if (savedSelectionAnchor.isInTextNode()) {
            savedSelectionAnchorTextNodelet = savedSelectionAnchor.getContainer().cast();
            savedSelectionAnchorOffset = savedSelectionAnchor.getTextOffset();
          }
          savedSelectionFocus = htmlSelection.getFocus();
          if (savedSelectionFocus.isInTextNode()) {
            savedSelectionFocusTextNodelet = savedSelectionFocus.getContainer().cast();
            savedSelectionFocusOffset = savedSelectionFocus.getTextOffset();
          }
          savedSelection = helper.getSelectionRange();
        }
      }
    }

    savedSelectionDepth++;
  }

  /**
   * Restores the selection to the same document location from when it was
   * saved, only if it is determined that the selection was inappropriately
   * altered by a DOM mutation.
   */
  public void restoreSelection() {
    restoreSelection(null);
  }

  /**
   * Same as {@link #restoreSelection()}, but if the selection is actually
   * required to be explicitly set, transform its saved location with the given
   * modifier.
   */
  public void restoreSelection(DocOp modifier) {
    savedSelectionDepth--;

    if (editingConcerns != null && savedSelectionDepth == 0) {
      try {
        if (savedSelection != null) {
          // selectionChangedInappropriately() only deals with selection boundaries. If we had
          // a ranged selection, and we're in a browser where changing it internally matters,
          // then just always restore the selection. This is safe to do w.r.t. IMEs because it's
          // unlikely to have a ranged selection during an uncommitted IME state.
          needToRestoreSelection |= QuirksConstants.RANGED_SELECTION_AFFECTED_BY_INTERNAL_CHANGED &&
              !savedSelection.isCollapsed();

          if (needToRestoreSelection || selectionChangedInappropriately()) {
            if (modifier != null) {
              savedSelection = RangeHelper.applyModifier(savedSelection, modifier);
            }

            EditorStaticDeps.logger.trace().log("Restoring selection");
            if (document.size() >= 4) {
              editingConcerns.getSelectionHelper().setSelectionRange(savedSelection);
            }
          } else {
            EditorStaticDeps.logger.trace().log("Not restoring selection");
          }
        }
      } finally {
        savedSelection = null;
        savedSelectionAnchor = null;
        savedSelectionAnchorTextNodelet = null;
        savedSelectionAnchorOffset = 0;
        savedSelectionFocus = null;
        savedSelectionFocusTextNodelet = null;
        savedSelectionFocusOffset = 0;
      }
    }
  }

  /**
   * Checks if the selection has been removed, or is somewhere where we are not
   * expecting it to be.
   */
  private boolean selectionChangedInappropriately() {

    // The selection got lost. We need to restore it
    if (!NativeSelectionUtil.selectionExists()) {
      return true;
    }

    NativeSelectionUtil.cacheClear();

    FocusedPointRange<Node> htmlSelection = NativeSelectionUtil.get();

    Point<Node> newAnchor = htmlSelection.getAnchor();
    if (savedSelectionAnchor.isInTextNode()) {
      if (savedSelectionAnchorTextNodelet != newAnchor.getContainer() ||
          savedSelectionAnchorOffset != newAnchor.getTextOffset()) {
        return true;
      }
    } else {
      if (savedSelectionAnchor.getContainer() != newAnchor.getContainer()) {
        return true;
      }
    }

    Point<Node> newFocus = htmlSelection.getFocus();
    if (savedSelectionFocus.isInTextNode()) {
      if (savedSelectionFocusTextNodelet != newFocus.getContainer() ||
          savedSelectionFocusOffset != newFocus.getTextOffset()) {
        return true;
      }
    } else {
      if (savedSelectionFocus.getContainer() != newFocus.getContainer()) {
        return true;
      }
    }

    return false;
  }

  /**
   * @see LowLevelEditingConcerns#textNodeletAffected(Text, int, int, TextNodeChangeType)
   */
  void textNodeletAffected(Text nodelet, int affectedAfterOffset, int insertionAmount,
      TextNodeChangeType changeType) {

    if (needToRestoreSelection == true) {
      return;
    }
    switch (changeType) {
    case DATA:
      if (!QuirksConstants.OK_SELECTION_ACROSS_TEXT_NODE_DATA_CHANGES &&
          matchesSelectionTextNodes(nodelet, affectedAfterOffset)) {
        needToRestoreSelection = true;
      } else {
        maybeUpdateNodeOffsets(nodelet, affectedAfterOffset, nodelet, insertionAmount);
      }
      return;
    case SPLIT:
      if (matchesSelectionTextNodes(nodelet, affectedAfterOffset)) {
        if (!QuirksConstants.OK_SELECTION_ACROSS_TEXT_NODE_SPLITS) {
          needToRestoreSelection = true;
        } else {
          maybeUpdateNodeOffsets(nodelet, affectedAfterOffset,
              nodelet.getNextSibling().<Text>cast(), -affectedAfterOffset);
        }
      }
      return;
    case REMOVE:
      if (!QuirksConstants.OK_SELECTION_ACROSS_NODE_REMOVALS &&
          matchesSelectionTextNodes(nodelet)) {
        needToRestoreSelection = true;
      }
      return;
    case MOVE:
    case REPLACE_DATA:
      if (matchesSelectionTextNodes(nodelet)) {
        needToRestoreSelection = true;
      }
      return;
    }
  }

  private void maybeUpdateNodeOffsets(Text nodelet, int affectedAfterOffset,
      Text newNodelet, int offsetDifference) {
    if (nodelet == savedSelectionAnchorTextNodelet &&
        savedSelectionAnchorOffset > affectedAfterOffset) {
      savedSelectionAnchorOffset += offsetDifference;
      savedSelectionAnchorTextNodelet = newNodelet;
    }
    if (nodelet == savedSelectionFocusTextNodelet &&
        savedSelectionFocusOffset > affectedAfterOffset) {
      savedSelectionFocusOffset += offsetDifference;
      savedSelectionFocusTextNodelet = newNodelet;
    }
  }

  private boolean matchesSelectionTextNodes(Text nodelet) {
    return nodelet == savedSelectionAnchorTextNodelet || nodelet == savedSelectionFocusTextNodelet;
  }

  private boolean matchesSelectionTextNodes(Text nodelet, int affectedAfterOffset) {
    if (savedSelection == null) {
      return false;
    }
    if (savedSelection.isOrdered()) {
      if (nodelet == savedSelectionAnchorTextNodelet) {
        return savedSelectionAnchorOffset > affectedAfterOffset;
      } else if (nodelet == savedSelectionFocusTextNodelet) {
        return true;
      }
    } else {
      // The inverse of the above
      if (nodelet == savedSelectionFocusTextNodelet) {
        return savedSelectionFocusOffset > affectedAfterOffset;
      } else if (nodelet == savedSelectionAnchorTextNodelet) {
        return true;
      }
    }
    return false;
  }
}
