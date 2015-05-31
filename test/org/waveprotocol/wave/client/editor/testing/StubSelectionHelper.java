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
 * Empty implementation. Suitable for subclassing.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class StubSelectionHelper implements SelectionHelper {

  public static final StubSelectionHelper INSTANCE = new StubSelectionHelper();

  public void clearSelection() {

  }

  public FocusedContentRange getSelectionPoints() {
    return null;
  }

  @Override
  public ContentRange getOrderedSelectionPoints() {
    return null;
  }

  public FocusedRange getSelectionRange() {
    return null;
  }

  @Override
  public Range getOrderedSelectionRange() {
    return null;
  }

  public void setCaret(Point<ContentNode> caret) {

  }

  @Override
  public void setCaret(int caret) {

  }

  public void setSelectionPoints(Point<ContentNode> start,
      Point<ContentNode> end) {

  }

  public void setSelectionRange(FocusedRange selection) {

  }

  public Point<ContentNode> getFirstValidSelectionPoint() {
    return null;
  }

  public Point<ContentNode> getLastValidSelectionPoint() {
    return null;
  }

  public boolean isValidSelectionPoint(Point<ContentNode> cp) {
    return false;
  }
}
