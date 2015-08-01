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

package org.waveprotocol.wave.client.editor.webdriver;

import com.google.gwt.dom.client.Element;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Utility that adds hooks for attaching functionality to an editor which can be triggered
 * through webdriver JSNI calls. @see EditorJSNIHelpers for the bridge calls.
 *
 * Call both EditorJsniHelpers.nativeSetupWebDriverTestPins()
 * and EditorWebDriverUtil.setDocumentSchema() during initialisation to set everything up.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class EditorWebDriverUtil {
  /**
   * Name of backref from main div to Editor object. Used by
   * {@link #webdriverEditorGetContent(Element)}
   */
  private static final String EDITOR_WEBDRIVER_PROPERTY = "__editor";

  private static DocumentSchema documentSchema = null;

  /**
   * Must be called with a non-null schema before any call to
   * {@link #webdriverEditorSetContent(Element,String)}.
   */
  public static void setDocumentSchema(DocumentSchema schema) {
    documentSchema = schema;
  }

  public static void register(Editor editor, Element container) {
    container.setPropertyObject(EDITOR_WEBDRIVER_PROPERTY, editor);
  }

  public static void unregister(Element container) {
    register(null, container);
  }

  /**
   * @param editorDiv
   * @return Editor owning given doc div
   */
  private static Editor getByEditorDiv(Element editorDiv) {
    return (editorDiv == null) ? null :
        (Editor) editorDiv.getPropertyObject(EDITOR_WEBDRIVER_PROPERTY);
  }

  /** Utility that flushes an editor div before getting the selected range. */
  private static Range getSelectionWithFlush(Element editorDiv) {
    Editor editor = getByEditorDiv(editorDiv);
    if (editor != null) {
      EditorTestingUtil.forceFlush(editor);
      return editor.getSelectionHelper().getOrderedSelectionRange();
    }
    return null;
  }

  /**
   * @param editorDiv
   * @return content of editor owning doc div
   */
  public static String webdriverEditorGetContent(Element editorDiv) {
    Editor editor = getByEditorDiv(editorDiv);
    if (editor != null) {
      // This must not be called synchronously in the same key event before
      // the dom is modified.
      EditorTestingUtil.forceFlush(editor);

      // NOTE(patcoleman): it seems empty strings get converted to null objects here by webdriver,
      //   this code removes the prefix that avoids that problem.
      // TODO(patcoleman): investigate where this happens with Simon.
      String content = XmlStringBuilder.innerXml(editor.getPersistentDocument()).toString();
      return content == null ? null : "_" + content;
    } else {
      return "Error in webdriverEditorGetContent";
    }
  }

  /**
   * @param editorDiv
   * @return local annotations of the editor owning div
   */
  public static String webdriverEditorGetLocalDiffAnnotations(Element editorDiv) {
    Editor editor = getByEditorDiv(editorDiv);
    if (editor == null) {
      return "Error in webdriverEditorGetContent";
    }
    // This must not be called synchronously in the same key event before the dom is modified.
    EditorTestingUtil.forceFlush(editor);

    StringBuilder ans = new StringBuilder("");
    ReadableStringSet keys = CollectionUtils.newStringSet(
        DiffHighlightingFilter.DIFF_INSERT_KEY,
        DiffHighlightingFilter.DIFF_DELETE_KEY);
    MutableAnnotationSet.Local annotations = editor.getContent().getLocalAnnotations();
    annotations.annotationIntervals(0, annotations.size(), keys);
    for (AnnotationInterval<Object> interval :
        annotations.annotationIntervals(0, annotations.size(), keys)) {

      boolean isInsertion = interval.annotations()
          .getExisting(DiffHighlightingFilter.DIFF_INSERT_KEY) != null;
      boolean isDeletion = interval.annotations()
          .getExisting(DiffHighlightingFilter.DIFF_DELETE_KEY) != null;
      char symbol;
      if (isInsertion && isDeletion) {
        symbol = '*';
      } else if (isInsertion) {
        symbol = '+';
      } else if (isDeletion) {
        symbol = '-';
      } else {
        symbol = '.';
      }
      for (int j = 0; j < interval.length(); j++) {
        ans.append(symbol);
      }
    }
    return ans.toString();
  }


  /**
   * @param editorDiv
   * @return start selection of editor owning doc div
   */
  public static int webdriverEditorGetStartSelection(Element editorDiv) {
    Range range = getSelectionWithFlush(editorDiv);
    return range == null ? -1 : range.getStart();
  }

  /**
   * Get the div of the editor's document (not the editor's div).
   * The editor must have a document, otherwise an exception will be thrown.
   *
   * @param editorDiv
   * @return div of editor's document
   */
  public static Element webdriverEditorGetDocDiv(Element editorDiv) {
    Editor editor = getByEditorDiv(editorDiv);
    return editor.getDocumentHtmlElement();
  }

  /**
   * @param editorDiv
   * @return end selection of editor owning doc div
   */
  public static int webdriverEditorGetEndSelection(Element editorDiv) {
    Range range = getSelectionWithFlush(editorDiv);
    return range == null ? -1 : range.getEnd();
  }

  /**
   * @param editorDiv editor
   * @param end end of selection range
   */
  public static void webdriverEditorSetSelection(Element editorDiv, int start, int end) {
    EditorContext editor = getByEditorDiv(editorDiv);
    editor.getSelectionHelper().setSelectionRange(new FocusedRange(start, end));
  }

  /**
   * @param editorDiv editor
   * @param content content to set
   */
  public static void webdriverEditorSetContent(Element editorDiv, String content) {
    Editor editor = getByEditorDiv(editorDiv);
    if (editor != null) {
      Preconditions.checkNotNull(documentSchema, "documentSchema is not set");
      editor.setContent(DocProviders.POJO.parse(content).asOperation(), documentSchema);
    }
  }

  /**
   * @param editorDiv
   * @return local content of editor owning doc div
   */
  public static String webdriverEditorGetLocalContent(Element editorDiv) {
    Editor editor = getByEditorDiv(editorDiv);
    if (editor != null) {
      // This must not be called synchronously in the same key event before
      // the dom is modified.
      EditorTestingUtil.forceFlush(editor);
      return XmlStringBuilder.innerXml(editor.getContent().getFullContentView()).toString();
    } else {
      return "Error in webdriverEditorGetLocalContent";
    }
  }
}
