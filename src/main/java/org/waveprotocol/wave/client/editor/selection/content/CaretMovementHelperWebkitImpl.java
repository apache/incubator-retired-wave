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

import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.html.JsRange;
import org.waveprotocol.wave.client.editor.selection.html.SelectionW3CNative;
import org.waveprotocol.wave.client.editor.selection.html.SelectionWebkit;
import org.waveprotocol.wave.client.editor.selection.html.SelectionWebkit.Direction;
import org.waveprotocol.wave.client.editor.selection.html.SelectionWebkit.MoveUnit;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Caret movement using Webkit's extensions to Selection.
 *
 */
public class CaretMovementHelperWebkitImpl implements CaretMovementHelper {
  private final NodeManager nodeManager;

  public CaretMovementHelperWebkitImpl(NodeManager nodeManager) {
    this.nodeManager = nodeManager;
  }

  public Point<ContentNode> getWordBoundary(boolean forward) {
    Point<ContentNode> boundary = null;
    SelectionWebkit s = SelectionWebkit.getSelection();
    if (forward) {
      s.move(Direction.FORWARD, MoveUnit.WORD);
    } else {
      s.move(Direction.BACKWARD, MoveUnit.WORD);
    }
    try {
      SelectionW3CNative selection = SelectionW3CNative.getSelectionGuarded();
      JsRange range = s.getRangeAt(0);
      boundary = nodeManager.nodeOffsetToWrapperPoint(
          selection.focusNode(), selection.focusOffset());
    } catch (HtmlMissing e) {
      EditorStaticDeps.logger.fatal().log("html missing not handled", e);
    } catch (HtmlInserted e) {
      EditorStaticDeps.logger.fatal().log("html inserted not handled", e);
    }
    return boundary;
  }
}
