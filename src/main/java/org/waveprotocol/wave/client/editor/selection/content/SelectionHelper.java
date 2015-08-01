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

import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Provides a content-level view for getting and setting the selection
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface SelectionHelper {

  /**
   * @return The current selection as an unordered content range
   */
  FocusedContentRange getSelectionPoints();

  /**
   * @return The current selection as an ordered range of selection points
   */
  ContentRange getOrderedSelectionPoints();

  /**
   * @return The current selection
   */
  FocusedRange getSelectionRange();

  /**
   * @return The current selection as an ordered range
   */
  Range getOrderedSelectionRange();

  /**
   * Set the selection
   * @param anchor
   * @param focus
   */
  void setSelectionPoints(Point<ContentNode> anchor, Point<ContentNode> focus);

  /**
   * @param selection The selection to set
   */
  void setSelectionRange(FocusedRange selection);

  /**
   * @param caret The collapsed selection to set
   */
  void setCaret(int caret);

  /**
   * TODO(danilatos): Inconsistent with setSelection. Clean this up.
   * @param caret The collapsed selection to set
   */
  void setCaret(Point<ContentNode> caret);

  /**
   * Clears the selection
   */
  void clearSelection();

  /**
   * @return The first valid point where a cursor may go
   */
  Point<ContentNode> getFirstValidSelectionPoint();

  /**
   * @return The last valid point where a cursor may go
   */
  Point<ContentNode> getLastValidSelectionPoint();

  /**
   * Checks if a point is a valid selection point.
   * @param cp
   */
  boolean isValidSelectionPoint(Point<ContentNode> cp);

  /**
   * Stub selection helper that does nothing. Useful when the document is not
   * attached to an editor.
   */
  public static final SelectionHelper NOP_IMPL = new SelectionHelper() {

    @Override
    public void clearSelection() {
    }

    @Override
    public Point<ContentNode> getFirstValidSelectionPoint() {
      return null;
    }

    @Override
    public Point<ContentNode> getLastValidSelectionPoint() {
      return null;
    }

    @Override
    public ContentRange getOrderedSelectionPoints() {
      return null;
    }

    @Override
    public Range getOrderedSelectionRange() {
      return null;
    }

    @Override
    public FocusedContentRange getSelectionPoints() {
      return null;
    }

    @Override
    public FocusedRange getSelectionRange() {
      return null;
    }

    @Override
    public boolean isValidSelectionPoint(Point<ContentNode> cp) {
      return false;
    }

    @Override
    public void setCaret(int caret) {
    }

    @Override
    public void setCaret(Point<ContentNode> caret) {
    }

    @Override
    public void setSelectionPoints(Point<ContentNode> anchor, Point<ContentNode> focus) {
    }

    @Override
    public void setSelectionRange(FocusedRange selection) {
    }
  };
}
