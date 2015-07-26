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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.color;

import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * The Class ColorHelper is a utility for manage text colors of a wave
 * while editing a document via the toolbar or via shortcuts.
 *
 * @author vjrj@ourproject.org (Vicente J. Ruiz Jurado)
 */
public class ColorHelper {

  /**
   * On set color of document selection.
   *
   * @param editor the editor
   * @param button the button (to show a popup near it)
   */
  public static void onSetColor(EditorContext editor, ToolbarClickButton button) {
    showPopup(editor, button, "color", false);
  }

  /**
   * On set back color of document selection.
   *
   * @param editor the editor
   * @param button the button (to show a popup near it)
   */
  public static void onSetBackColor(final EditorContext editor, ToolbarClickButton button) {
    showPopup(editor, button, "backgroundColor", true);
  }

  /**
   * Show popup with a color picker and set/clear the color on the range.
   *
   * @param editor the editor
   * @param button the button
   * @param suffix the key suffix
   * @param allowNone the allow none color (in background color)
   */
  private static void showPopup(final EditorContext editor, ToolbarClickButton button,
      final String suffix, boolean allowNone) {
    FocusedRange focusedRange = editor.getSelectionHelper().getSelectionRange();
    if (focusedRange == null) {
      // Lets try to focus
      editor.focus(false);
    }
    focusedRange = editor.getSelectionHelper().getSelectionRange();
    if (focusedRange == null) {
      Window.alert(ComplexColorPicker.messages.selectSomeText());
      return;
    }
    final Range range = focusedRange.asRange();
    final ColorPopup popup = new ColorPopup(button.getButton().hackGetWidget().getElement(), allowNone);
    popup.show(new OnColorChooseListener() {
      @Override
      public void onColorChoose(String color) {
        EditorAnnotationUtil.
            setAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(),
                StyleAnnotationHandler.key(suffix), color, range.getStart(), range.getEnd());
        popup.hide();
        editor.focus(false);
      }

      @Override
      public void onNoneColorChoose() {
        EditorAnnotationUtil.
        clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(),
            new String[] {StyleAnnotationHandler.key(suffix)}, range.getStart(), range.getEnd());
        popup.hide();
        editor.focus(false);
      }});
  }
}
