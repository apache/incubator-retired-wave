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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.NiceHtmlRenderer;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;

/**
 * Predefined PasteFormatRenderers.
 *
 */
public class PasteFormatRenderers {
  /**
   * PasteFormatRenderer that deep clones a ContentNode's impl node.
   *
   * If the original selection was inside the content node, it'll set the
   * selection to fully contain the content node.
   */
  public static final NiceHtmlRenderer DEEP_CLONE_RENDERER = new NiceHtmlRenderer() {
    @Override
    public ContentNode renderSequence(
        ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
        ContentNode firstItem, ContentNode stopAt, Element dstParent,
        SelectionMatcher selectionMatcher) {
      if (firstItem instanceof ContentElement) {
        EditorStaticDeps.logger.trace().log(
            "deep cloning: " + ((ContentElement) firstItem).getTagName());
      }
      Node implNodelet = firstItem.getImplNodelet();
      Node clone = (implNodelet != null ? implNodelet.cloneNode(true) : null);
      if (clone != null) {
        dstParent.appendChild(clone);
        selectionMatcher.maybeNoteHtml(firstItem, clone);
      } else {
        selectionMatcher.noteSelectionInNode(firstItem, dstParent, true);
      }

      return firstItem;
    }
  };

  /**
   * PasteFormatRenderer that shallow clone's a ContentNode's impl node and then
   * invoke PasteFormatRender on the ContentNode's children.
   *
   * This is to be used on trivial ContentNodes that have only 1 impl node in
   * the DOM, and identical rendering in pretty and editor mode.
   */
  public static final NiceHtmlRenderer SHALLOW_CLONE_RENDERER = new NiceHtmlRenderer() {
    @Override
    public ContentNode renderSequence(
        ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
        ContentNode firstItem, ContentNode stopAt, Element dstParent,
        SelectionMatcher selectionMatcher) {
      Node implNodelet = firstItem.getImplNodelet();
      Node clone = implNodelet != null ? implNodelet.cloneNode(false) : null;

      if (clone != null) {
        dstParent.appendChild(clone);
        selectionMatcher.maybeNoteHtml(firstItem, clone);
      } else {
        selectionMatcher.noteSelectionInNode(firstItem, dstParent, false);
      }

      if (firstItem instanceof ContentElement) {
        final Element container;
        if (clone != null && clone instanceof Element) {
          container = (Element) clone;
        } else {
          container = dstParent;
        }
        PasteFormatRenderer.renderChildren(view, container, firstItem,
            selectionMatcher);
      }
      return firstItem;
    }
  };

  private PasteFormatRenderers() {}
}
