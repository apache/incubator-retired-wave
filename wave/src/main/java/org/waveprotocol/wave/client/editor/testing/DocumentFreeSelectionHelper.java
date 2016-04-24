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

package org.waveprotocol.wave.client.editor.testing;

import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * A simple selection helper implementation with no knowledge of a content tree
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class DocumentFreeSelectionHelper implements SelectionHelper {
  /** Track the simple range directly. */
  FocusedRange selection = null;

  public DocumentFreeSelectionHelper(int start, int end) {
    selection = new FocusedRange(start, end);
  }

  public DocumentFreeSelectionHelper(FocusedRange range) {
    selection = range;
  }

  @Override
  public void clearSelection() {
    selection = null;
  }

  @Override
  public FocusedRange getSelectionRange() {
    return selection;
  }

  @Override
  public Range getOrderedSelectionRange() {
    return selection != null ? selection.asRange() : null;
  }

  @Override
  public void setSelectionRange(FocusedRange selection) {
    this.selection = selection;
  }

  @Override
  public Point<ContentNode> getFirstValidSelectionPoint() {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public Point<ContentNode> getLastValidSelectionPoint() {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public FocusedContentRange getSelectionPoints() {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public ContentRange getOrderedSelectionPoints() {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public boolean isValidSelectionPoint(Point<ContentNode> cp) {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public void setCaret(Point<ContentNode> caret) {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public void setSelectionPoints(Point<ContentNode> start, Point<ContentNode> end) {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }

  @Override
  public void setCaret(int caret) {
    throw new UnsupportedOperationException("DocumentFree SelectionHelper has no document content");
  }
}
