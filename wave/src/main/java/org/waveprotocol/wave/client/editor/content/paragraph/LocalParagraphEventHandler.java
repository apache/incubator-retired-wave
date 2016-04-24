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

import static org.waveprotocol.wave.client.editor.content.paragraph.ParagraphEventHandler.attributeKeptOnNewline;
import static org.waveprotocol.wave.client.editor.content.paragraph.ParagraphEventHandler.getBehaviour;
import static org.waveprotocol.wave.client.editor.content.paragraph.ParagraphEventHandler.indent;

import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LocalParagraphEventHandler extends NodeEventHandlerImpl {

  boolean isEmptyLine(ContentElement e) {
    // The line containing element e is considered empty if its line element is the last
    // element or if it is followed by another line element
    ContentElement lineElement = Line.fromParagraph(e).getLineElement();
    CMutableDocument doc = lineElement.getMutableDoc();
    ContentNode next = doc.getNextSibling(lineElement);
    return next == null
        || (next.asElement() != null && LineRendering.isLineElement(next.asElement()));
  }

  @Override
  public boolean handleEnter(ContentElement element, EditorEvent event) {
    ContentPoint contentPoint = event.getCaret();
    if (!contentPoint.isInTextNode() && contentPoint.getContainer() != element) {
      return true;
    }
    Point<ContentNode> point = contentPoint.asPoint();
    ContentNode nodeBefore = point.isInTextNode()
        ? point.getContainer() : Point.nodeBefore(FullContentView.INSTANCE, point.asElementPoint());
    Line line = Line.fromParagraph(element);

    // If user hits enter at the an empty list item - we de-indent or stop the list
    if (getBehaviour(element) == ParagraphBehaviour.LIST
        && isEmptyLine(element)) {
      return handleBackspaceAtBeginning(element, event);
    }

    Map<String, String> secondAttrs = new HashMap<String, String>();

    // TODO(patcoleman): use StringMap<String> instead?
    // copy whitelisted attributes
    CMutableDocument doc = element.getMutableDoc();
    Map<String, String> currentAttrs = line.getAttributes();
    if (currentAttrs != null) {
      for (Entry<String, String> entry : currentAttrs.entrySet()) {
        if (attributeKeptOnNewline(entry.getKey(), entry.getValue())) {
          secondAttrs.put(entry.getKey(), entry.getValue());
        }
      }
    }

    // rewrite to null if no attributes
    if (secondAttrs.isEmpty()) {
      secondAttrs = Attributes.EMPTY_MAP;
    }

    ContentElement newLineElement = doc.createElement(
        doc.locate(doc.getLocation(point)), LineContainers.LINE_TAGNAME, secondAttrs);

    ContentElement newLocalParagraph = Line.fromLineElement(newLineElement).getParagraph();
    element.getSelectionHelper().setCaret(
        Point.start(element.getRenderedContentView(), newLocalParagraph));

    return true;
  }

  @Override
  public boolean handleBackspaceAtBeginning(ContentElement paragraph, EditorEvent event) {
    Line line = Line.fromParagraph(paragraph);
    ContentElement lineElement = line.getLineElement();

    if (line.getIndent() > 0) {
      indent(lineElement, -1);
    } else {
      switch (line.getBehaviour()) {
        case LIST:
          lineElement.getMutableDoc().setElementAttribute(
              lineElement, Paragraph.SUBTYPE_ATTR, null);
          lineElement.getMutableDoc().setElementAttribute(
              lineElement, Paragraph.LIST_STYLE_ATTR, null);
          break;
        default:
          maybeRemove(line);
          break;
      }
    }
    return true;
  }

  @Override
  public boolean handleDeleteAtEnd(ContentElement p, EditorEvent event) {
    Line line = Line.fromParagraph(p);
    if (line.next() != null) {
      maybeRemove(line.next());
    }
    return true;
  }

  private void maybeRemove(Line line) {
    MutableDocument<ContentNode, ContentElement, ContentTextNode> doc = line.getMutableDoc();
    ContentElement lineElement = line.getLineElement();
    Line previousLine = line.previous();
    if (previousLine != null) {
      ContentElement prevLineElement = previousLine.getLineElement();
      ContentElement prevParagraph = previousLine.getParagraph();
      if (doc.getNextSibling(prevLineElement) == lineElement) {
        // If the previous line is empty
        Map<String, String> attrs = doc.getAttributes(lineElement);
        doc.setElementAttributes(prevLineElement, new AttributesImpl(attrs));
      }

      int at = doc.getLocation(Point.<ContentNode>end(prevParagraph));
      boolean needsAdjusting = prevParagraph.getFirstChild() == null;
      doc.deleteNode(lineElement);

      if (!needsAdjusting) {
        lineElement.getSelectionHelper().setCaret(doc.locate(at));
      } else {
        // NOTE(patcoleman): a special case for empty local paragraphs, these are skipped by locate
        lineElement.getSelectionHelper().setCaret(
            Point.<ContentNode, ContentElement>start(doc, prevParagraph));
      }
    }
  }
}
