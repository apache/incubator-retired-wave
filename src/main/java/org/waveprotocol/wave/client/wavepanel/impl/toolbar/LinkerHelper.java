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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.common.util.WaveRefConstants;
import org.waveprotocol.wave.client.doodad.link.Link;
import org.waveprotocol.wave.client.doodad.link.Link.InvalidLinkException;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.i18n.LinkerMessages;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * The Class LinkerHelper is a utility for manage (create, remove, etc) links
 * while editing a document via the toolbar or via shortcuts
 */
public class LinkerHelper {
  private static final LinkerMessages messages = GWT.create(LinkerMessages.class);

  /**
   * Helper for insert links while editing a document
   *
   * @param editor the wave editor
   */
  public static void onCreateLink(EditorContext editor) {
    FocusedRange range = editor.getSelectionHelper().getSelectionRange();
    if (range == null || range.isCollapsed()) {
      Window.alert(messages.selectSomeText());
      return;
    }
    try {
      // We try to create a link with the current selection, if fails, we ask
      // for a link
      Range rg = range.asRange();
      String text = DocHelper.getText(editor.getDocument(), rg.getStart(), rg.getEnd());
      String linkAnnotationValue = Link.normalizeLink(text);
      EditorAnnotationUtil.setAnnotationOverSelection(editor, AnnotationConstants.LINK_PREFIX, linkAnnotationValue);
    } catch (InvalidLinkException e) {
      String rawLinkValue =
          Window.prompt(messages.enterLink(), WaveRefConstants.WAVE_URI_PREFIX);
      // user hit "ESC" or "cancel"
      if (rawLinkValue == null) {
        return;
      }
      try {
        String linkAnnotationValue = Link.normalizeLink(rawLinkValue);
        EditorAnnotationUtil.setAnnotationOverSelection(editor, AnnotationConstants.LINK_PREFIX, linkAnnotationValue);
      } catch (InvalidLinkException e2) {
        Window.alert(e2.getLocalizedMessage());
      }

    }

  }

  /**
   * Helper for remove links while editing a document
   *
   * @param editor the wave editor
   */
  public static void onClearLink(EditorContext editor) {
    if (editor.getSelectionHelper().getSelectionRange() != null) {
      EditorAnnotationUtil.clearAnnotationsOverSelection(editor, Link.LINK_KEYS);
    }
  }
}
