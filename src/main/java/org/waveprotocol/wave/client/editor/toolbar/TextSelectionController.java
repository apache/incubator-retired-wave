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
import org.waveprotocol.wave.client.editor.toolbar.ButtonUpdater.Controller;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * A {@link ToolbarToggleButton.Listener} which applies a styling to a selection
 * of text and synchronizes the toggle state of a button with the styling
 * applied to the current selection.
 * <p>
 * See {@link org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar}
 * for example uses.
 * 
 * @author kalman@google.com (Benjamin Kalman)
 */
public class TextSelectionController implements ToolbarToggleButton.Listener, Controller {
  private final ToolbarToggleButton button;
  private final EditorContext editor;
  private final String annotationName;
  private final String annotationValue;

  public TextSelectionController(ToolbarToggleButton button, EditorContext editor,
      String annotationName, String annotationValue) {
    this.button = button;
    this.editor = editor;
    this.annotationName = annotationName;
    this.annotationValue = annotationValue;
  }

  @Override
  public void onToggledOff() {
    setSelectionAnnotation(null);
  }

  @Override
  public void onToggledOn() {
    setSelectionAnnotation(annotationValue);
  }

  private void setSelectionAnnotation(final String style) {
    if (editor.getSelectionHelper().getSelectionPoints() != null) {
      editor.undoableSequence(new Runnable() {
        @Override public void run() {
          EditorAnnotationUtil.setAnnotationOverSelection(editor, annotationName, style);
        }
      });
    }
  }

  @Override
  public void update(Range selectionRange) {
    if (annotationValue != null) {
      String value =
          EditorAnnotationUtil.getAnnotationOverRangeIfFull(editor.getDocument(),
              editor.getCaretAnnotations(), annotationName, selectionRange.getStart(),
              selectionRange.getEnd());
      button.setToggledOn(annotationValue.equals(value));
    }
  }
}
