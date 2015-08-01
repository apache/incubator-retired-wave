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

package org.waveprotocol.wave.client.editor.util;

import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;

import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Pretty;

/**
 * Collection of decoration utilities that enable conversion between editor content documents and
 * java Strings for easy comparison in logging/testing.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class EditorDocFormatter {
  /** Number of spaces to indent when pretty printing the document contents. */
  private static final int DOCUMENT_STRINGIFY_INDENT = 2;

  /** Private constructor for utility class. */
  private EditorDocFormatter () {}

  ///
  /// Pretty-print formatting methods
  ///

  /** @return a String version the persistent dom tree in an editor. */
  public static String formatContentDomString(Editor editor) {
    boolean inconsistent = true;
    if (EditorTestingUtil.isConsistent(editor)) {
      if (editor instanceof EditorImpl) {
        // specialise for editor impls to have the behaviour of the document but no selection
        // if the editor is not consistent.
        EditorImpl ed = (EditorImpl)editor;
        Point<ContentNode> selStart = null;
        Point<ContentNode> selEnd = null;

        PointRange<Node> seln = ed.getOrderedHtmlSelection();
        try {
          selStart = seln == null ? null :
              ed.getNodeManager().nodeletPointToWrapperPoint(seln.getFirst());
          selEnd = seln == null ? null :
            ed.getNodeManager().nodeletPointToWrapperPoint(seln.getSecond());
          return new Pretty<ContentNode>().select(selStart, selEnd).print(
              editor.getContent().getFullContentView());
        } catch (HtmlInserted e) {
          // fall through to error case
        } catch (HtmlMissing e) {
          // fall through to error case
        }
      } else {
        return null;
      }
    }

    EditorStaticDeps.logger.error().logPlainText("EditorDocFormatter called with inconsistent Doc");
    return null;
  }

  /** @return a String version of the HTML dom inside an editor. */
  public static String formatImplDomString(Editor editor) {
    if (EditorTestingUtil.isConsistent(editor)) {
      // Get editor, null selection if outside the editor:
      PointRange<Node> selection = NativeSelectionUtil.getOrdered();
      if (selection != null) {
        Node editorHtml = editor.getWidget().getElement();
        if (!editorHtml.isOrHasChild(selection.getFirst().getContainer()) ||
             editorHtml.isOrHasChild(selection.getSecond().getContainer())) {
          selection = null; // outside!
        }
      }
      HtmlView view = editor.getContent().getRawHtmlView();
      Point<Node> selStart = (selection != null) ? selection.getFirst() : null;
      Point<Node> selEnd = (selection != null) ? selection.getSecond() : null;
      return new Pretty<Node>().select(selStart, selEnd).print(view);
    } else {
      EditorStaticDeps.logger.error().logPlainText(
          "EditorDocFormatter called with inconsistent Doc");
      return null;
    }
  }

  /** @return a string version of the persistent dom tree in an editor. */
  public static String formatPersistentDomString(Editor editor) {
    if (EditorTestingUtil.isConsistent(editor)) {
      return DocOpUtil.toPrettyXmlString(editor.getDocumentInitialization(),
          DOCUMENT_STRINGIFY_INDENT);
    } else {
      EditorStaticDeps.logger.error().logPlainText(
          "EditorDocFormatter called with inconsistent Doc");
      return null;
    }
  }
}
