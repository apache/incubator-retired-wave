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

package org.waveprotocol.wave.client.editor.content.paragraph;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.NiceHtmlRenderer;
import org.waveprotocol.wave.client.editor.extract.PasteFormatRenderer;
import org.waveprotocol.wave.client.editor.extract.SelectionMatcher;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces "nice looking" html for paragraphs, with special handling for bullet
 * points and headings.
 *
 */
public class ParagraphNiceHtmlRenderer implements NiceHtmlRenderer {

  /**
   * Type of element used for indenting.
   */
  enum IndentType {
    BLOCKQUOTE {
      @Override public Element createElement() {
        return Document.get().createBlockQuoteElement();
      }
    },
    UL {
      @Override public Element createElement() {
        return Document.get().createULElement();
      }
    },
    OL {
      @Override public Element createElement() {
        return Document.get().createOLElement();
      }
    };

    abstract Element createElement();
  }

  /**
   * Maintains current nice HTML render information, as we are converting a
   * flattened linear structure into a nested structure.
   */
  static class HtmlStack {
    final Element topParent;
    final List<Element> elementStack = new ArrayList<Element>();

    HtmlStack(Element topParent) {
      this.topParent = topParent;
    }

    Element currentParent() {
      return elementStack.size() == 0 ? topParent : elementStack.get(elementStack.size() - 1);
    }

    /**
     * Updates the current indentation level by adding more nested elements of
     * the given type, or by popping elements off the stack. The element at the
     * top of the stack is where children are being appended.
     */
    void restack(IndentType t, int indentLevel) {
      assert indentLevel >= 0;
      while (indentLevel < stackDepth()) {
        pop();
      }
      int depth = indentLevel - stackDepth();
      for (int i = 0; i < depth; i++) {
        push(t.createElement());
      }
    }

    void startListItem() {
      push(Document.get().createLIElement());
    }

    void push(Element e) {
      currentParent().appendChild(e);
      elementStack.add(e);
    }

    void maybeCloseListItemForParagraph(int indent) {
      if (indent + 1 != stackDepth()) {
        maybeCloseListItem();
      }
    }

    void maybeCloseListItem() {
      if (currentParent().getTagName().equalsIgnoreCase("li")) {
        pop();
      }
    }

    void pop() {
      elementStack.remove(elementStack.size() - 1);
    }

    int stackDepth() {
      return elementStack.size();
    }
  }

  @Override
  public ContentNode renderSequence(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      ContentNode firstItem, ContentNode stopAt, final Element destParent,
      SelectionMatcher selectionMatcher) {

    Preconditions.checkArgument(firstItem instanceof ContentElement,
        "firstItem must be an instance of ContentElement ", firstItem.getClass());

    ContentNode prev = null;
    HtmlStack helper = new HtmlStack(destParent);

    for (ContentNode node = firstItem;
         node != null && node != stopAt;
         prev = node, node = view.getNextSibling(node)) {
      assert node instanceof ContentElement : "Expected node to be instance of ContentElement";
      ContentElement el = (ContentElement) node;

      int indent = Paragraph.getIndent(el);

      if (Paragraph.isHeading(el)) {
        helper.maybeCloseListItem();
        helper.restack(IndentType.BLOCKQUOTE, indent);
        renderHeadingItem(view, helper.currentParent(), el, selectionMatcher);
      } else if (Paragraph.isListItem(el)) {
        boolean isDecimal = Paragraph.isDecimalListItem(el);
        helper.maybeCloseListItem();
        // indent + 1 because list items are naturally indented & wrapped in
        // a container. e.g. at indent 0 we have 1 <ul>, but for paragraphs
        // we have 0 blockquotes.
        helper.restack(isDecimal ? IndentType.OL : IndentType.UL, indent + 1);
        // We start a list item and leave it open, so subsequent paragraphs
        // that line up (current indent + 1) can be just appended inside it
        // with line breaks.
        helper.startListItem();
        doInnards(view, helper.currentParent(), el, selectionMatcher);
      } else {
        // We only close open list items if the indent doesn't match.
        helper.maybeCloseListItemForParagraph(indent);
        helper.restack(IndentType.BLOCKQUOTE, indent);
        renderAndAppendParagraph(view, helper.currentParent(), el, selectionMatcher);
      }
    }

    return prev;
  }

  private ContentNode renderHeadingItem(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      Element dest, ContentElement src,
      SelectionMatcher selectionMatcher) {
    Element e = Document.get().createHElement(Paragraph.getHeadingSize(src));
    dest.appendChild(e);
    doInnards(view, e, src, selectionMatcher);
    return src;
  }

  private ContentNode renderAndAppendParagraph(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> contentView,
      Element dest, ContentNode child, SelectionMatcher selectionMatcher) {
    PasteFormatRenderer.renderChildren(contentView, dest, child,
        selectionMatcher);

    Element br = Document.get().createBRElement();
    dest.appendChild(br);

    selectionMatcher.maybeNoteHtml(child, br);
    return child;
  }

  private static void doInnards(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view, Element dest,
      ContentNode src, SelectionMatcher selectionMatcher) {
    PasteFormatRenderer.renderChildren(view, dest, src, selectionMatcher);
    selectionMatcher.maybeNoteHtml(src, dest);
  }
}
