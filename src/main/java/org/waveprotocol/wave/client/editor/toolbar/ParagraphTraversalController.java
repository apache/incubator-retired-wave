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

package org.waveprotocol.wave.client.editor.toolbar;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * A {@link ToolbarClickButton.Listener} which applies a styling to a
 * paragraph using {@link Paragraph#traverse}.
 * <p>
 * See {@link org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar}
 * for example uses.
 * 
 * @author kalman@google.com (Benjamin Kalman)
 */
public class ParagraphTraversalController implements ToolbarClickButton.Listener {
  private final EditorContext editor;
  private final ContentElement.Action action;

  public ParagraphTraversalController(EditorContext editor, ContentElement.Action action) {
    this.editor = editor;
    this.action = action;
  }

  @Override
  public void onClicked() {
    final Range range = editor.getSelectionHelper().getOrderedSelectionRange();
    if (range != null) {
      editor.undoableSequence(new Runnable(){
        @Override public void run() {
          LocationMapper<ContentNode> locator = editor.getDocument();
          Paragraph.traverse(locator, range.getStart(), range.getEnd(), action);
        }
      });
    }
  }
}