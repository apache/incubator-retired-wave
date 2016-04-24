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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.content.AgentAdapter;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.NiceHtmlRenderer;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;

/**
 * Renders content into a format suitable for copy and pasting.
 *
 * NOTE(user): This is still in very early stages, there's still many issues to
 * consider. We probably want to move logic on how to render each particular
 * doodad to their own renderer.
 *
 */
public final class PasteFormatRenderer {
  private PasteFormatRenderer() {}

  private static PasteFormatRenderer INSTANCE;

  public static final PasteFormatRenderer get() {
    if (INSTANCE == null) {
      INSTANCE = new PasteFormatRenderer();
    }
    return INSTANCE;
  }

  /**
   * Renders the html subtree
   *
   * @param view
   * @param node The node to copy.
   * @param selectionMatcher Used to keep track of html points that
-   *        correspond with specified content points.
   */
  Element renderTree(ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      ContentNode node, SelectionMatcher selectionMatcher) {
    Element holder = Document.get().createDivElement();
    selectionMatcher.setHtmlRootContainer(holder);
    renderSequence(view, node, node.getNextSibling(), holder, selectionMatcher);
    assert selectionMatcher.getHtmlStart() != null : "htmlStart is null.";
    assert selectionMatcher.getHtmlEnd() != null : "htmlEnd is null.";

    assert holder.isOrHasChild(selectionMatcher.getHtmlStart().getContainer()) :
      "selection start not attached.";
    assert holder.isOrHasChild(selectionMatcher.getHtmlEnd().getContainer()) :
      "selection end not attached";
    return holder;
  }

  /**
   * Renders all the children, appending them to parent.
   *
   * @param view
   * @param parent
   * @param contentParent
   * @param selectionMatcher
   */
  public static void renderChildren(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      Element parent,
      ContentNode contentParent, SelectionMatcher selectionMatcher) {
    ContentNode current = contentParent.getFirstChild();
    while (current != null) {
      ContentNode done = renderSequence(view, current, null, parent, selectionMatcher);
      current = view.getNextSibling(done);
    }
  }

  /**
   * Renders a sequence of nodes.
   *
   * This will render a sequence of nodes, and append the result to dstParent.
   *
   * @return the last node in the sequence rendered by this method.
   * @param view
   * @param firstItem
   * @param dstParent
   */
  private static ContentNode renderSequence(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      ContentNode firstItem, ContentNode stopAt, Element dstParent,
      SelectionMatcher selectionMatcher) {
    NiceHtmlRenderer semanticHandler = getSemanticHandler(firstItem);
    return semanticHandler.renderSequence(view, firstItem, stopAt, dstParent, selectionMatcher);
  }

  private static NiceHtmlRenderer getSemanticHandler(ContentNode node) {
    if (node instanceof AgentAdapter) {
      AgentAdapter element = (AgentAdapter) node;
      NiceHtmlRenderer handler = element.getRegistry().getNiceHtmlRenderer(element);
      return handler != null ? handler : PasteFormatRenderers.DEEP_CLONE_RENDERER;
    } else {
      return PasteFormatRenderers.DEEP_CLONE_RENDERER;
    }
  }
}
