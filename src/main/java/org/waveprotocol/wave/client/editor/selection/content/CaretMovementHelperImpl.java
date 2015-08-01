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

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.TextLocator;

/**
 * Implementation of CaretMovement that manually traverses the document.
 *
 * TODO(user): Add an IE specific implementation that uses native methods.
 *
 */
public final class CaretMovementHelperImpl implements CaretMovementHelper {
  // We use the persistent view here rather than renderedView because
  // the word boundary detection code works on contiguous text nodes. We want to
  // ignore rendered nodes such as formatting.
  private final ReadableDocument<ContentNode, ContentElement, ContentTextNode> persistentView;
  private final SelectionHelper selectionHelper;

  public CaretMovementHelperImpl(
      ReadableDocument<ContentNode, ContentElement, ContentTextNode> persistentView,
      SelectionHelper selectionHelper) {
    this.persistentView = persistentView;
    this.selectionHelper = selectionHelper;
  }

  @Override
  public Point<ContentNode> getWordBoundary(boolean forward) {
    FocusedPointRange<ContentNode> range = selectionHelper.getSelectionPoints();
    if (range == null) {
      return null;
    }
    return TextLocator.getWordBoundary(range.getFocus(), persistentView, forward);
  }
}
