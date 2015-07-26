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
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.toolbar.ButtonUpdater.Controller;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * A {@link ToolbarToggleButton.Listener} which applies a styling to a paragraph
 * using {@link Paragraph#apply} and synchronizes the toggle state of a button
 * with the styling applied to the current selection.
 * <p>
 * See {@link org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar}
 * for example uses.
 * 
 * @author kalman@google.com (Benjamin Kalman)
 */
public class ParagraphApplicationController implements ToolbarToggleButton.Listener, Controller {
  private final ToolbarToggleButton button;
  private final EditorContext editor;
  private final Paragraph.LineStyle style;

  public ParagraphApplicationController(ToolbarToggleButton button, EditorContext editor,
      Paragraph.LineStyle style) {
    this.button = button;
    this.editor = editor;
    this.style = style;
  }

  @Override
  public void onToggledOn() {
    setParagraphStyle(true);
  }

  @Override
  public void onToggledOff() {
    setParagraphStyle(false);
  }

  private void setParagraphStyle(final boolean isOn) {
    final Range range = editor.getSelectionHelper().getOrderedSelectionRange();
    if (range != null) {
      editor.undoableSequence(new Runnable() {
        @Override public void run() {
          Paragraph.apply(editor.getDocument(), range.getStart(), range.getEnd(), style, isOn);
        }
      });
    }
  }

  @Override
  public void update(Range range) {
    button.setToggledOn(Paragraph.appliesEntirely(editor.getDocument(), range.getStart(), range
        .getEnd(), style));
  }
}
