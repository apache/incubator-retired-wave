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

package org.waveprotocol.wave.client.clipboard;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Safari implementation of the paste buffer. In Safari, we need to have the
 * paste occur inside of a text node in order to consistently get the
 * leading and trailing inline text. If that is not present, we sometimes
 * cannot tell the difference between a new paragraph or inline text.
 *
 * Also works for Firefox >= 15
 *
 */
class PasteBufferImplSafariAndNewFirefox extends PasteBufferImpl {

  private boolean markersStripped = false;

  private static final char MARKER_CHAR = '\u007F';
  private static final String MARKER_NODE_STRING = String.valueOf(MARKER_CHAR) + MARKER_CHAR;

  /**
   * Use this to get the paste contents (from innerHTML). Note, this
   * implementation will strip the extra markers injected for Safari.
   *
   * @return The pasted contents.
   */
  @Override
  public Element getPasteContainer() {
    stripMarkers();
    return element;
  }

  @Override
  public void prepareForPaste() {
    element.setInnerHTML("");
    element.appendChild(Document.get().createTextNode(MARKER_NODE_STRING));
    NativeSelectionUtil.setCaret(Point.inText(element.getFirstChild(), 1));
    markersStripped = false;
  }

  private void stripMarkers() {
    if (markersStripped) {
      return;
    }

    // Remove the leading and trailing markers.
    maybeStripMarker(element.getFirstChild(), element, true);
    maybeStripMarker(element.getLastChild(), element, false);
    markersStripped = true;
  }

  // TODO(user): Remove this when we can confirm this no longer happens.
  private void logEndNotFound(String detail) {
    Clipboard.LOG.error().log("end not found: " + detail);
  }

  private void maybeStripMarker(Node node, Element parent, boolean leading) {
    if (node == null) {
      logEndNotFound("node is null");
      return;
    }
    if (DomHelper.isTextNode(node)) {
      Text textNode = node.cast();
      String text = textNode.getData();
      if (!text.isEmpty()) {
        if (leading) {
          if (text.charAt(0) == MARKER_CHAR) {
            textNode.setData(text.substring(1));
          }
        } else {
          if (text.charAt(text.length() - 1) == MARKER_CHAR) {
            textNode.setData(text.substring(0, text.length() - 1));
          } else {
            logEndNotFound("last character is not marker");
          }
        }
      } else {
        logEndNotFound("text node is empty");
      }
      if (textNode.getData().isEmpty()) {
        parent.removeChild(textNode);
      }
    } else {
      // In some cases, Safari will put the marker inside of a div, so this
      // traverses down the left or right side of the tree to find it.
      // For example: x<div><span>pasted</span>x</div>
      maybeStripMarker(leading ? node.getFirstChild() : node.getLastChild(), node.<Element> cast(),
          leading);
    }
  }
}
