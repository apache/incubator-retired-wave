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

import static org.waveprotocol.wave.model.document.util.LineContainers.PARAGRAPH_FULL_TAGNAME;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.PersistentContent;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Metadata useful for managing line rendering and behaviour within documents.
 * Additionally contains useful methods to manipulate this information.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author patcoleman@google.com (Pat Coleman)
 */
public class Line {
  /** Property used to attack line metadata to the elements. */
  private static final Property<Line> LINE = Property.immutable("line");

  /** Property used to indicate which line a linecontainer is rendering. */
  private static final Property<Line> FIRST_LINE = Property.mutable("fline");

  // Utilities (line element methods, then local paragraph ones, then line container ones).

  /** Retrieve the metadata from a line element. */
  public static Line fromLineElement(ContentElement line) {
    assert LineRendering.isLineElement(line)
        : "Not line element: " + line.getTagName();
    return line.getProperty(LINE);
  }

  /**
   * Retrieve the metadata from a paragraph element.
   * NOTE(danilatos): Might be something other than a paragraph or line.
   */
  public static Line fromParagraph(ContentElement paragraph) {
    assert !LineRendering.isLineElement(paragraph)
        : "Is but shouldn't be line element: " + paragraph.getTagName();
    return paragraph.getProperty(LINE);
  }

  /** Retrieve the first semantic line from a container. */
  public static Line getFirstLineOfContainer(ContentElement container) {
    assert LineRendering.isLineContainerElement(container)
        : "Not container: " + container.getTagName();
    return container.getProperty(FIRST_LINE);
  }

  /** Assign a first line to a line container. */
  public static void setFirstLineOfContainer(ContentElement container, Line first) {
    assert LineRendering.isLineContainerElement(container)
        : "Not container: " + container.getTagName();
    container.setProperty(FIRST_LINE, first);
  }

  // Metadata information
  public static final int DIRTY = -1;

  private final DocumentContext<ContentNode, ContentElement, ContentTextNode> cxt;
  private final ContentElement lineElement;
  private final ContentElement paragraph;

  // Linked list of adjacent lines
  private Line previous;
  private Line next;

  private int cachedNumberValue = DIRTY;

  /**
   * Given a document and a line element, this creates a local paragraph to hold the line's
   *   content, and wraps them up in a Line bundle to allow easier document traversal.
   */
  Line(DocumentContext<ContentNode, ContentElement, ContentTextNode> cxt,
      ContentElement lineElement) {
    assert LineRendering.isLineElement(lineElement);
    this.cxt = cxt;
    this.lineElement = lineElement;
    this.paragraph = cxt.annotatableContent().transparentCreate(PARAGRAPH_FULL_TAGNAME,
        cxt.document().getAttributes(lineElement),
        lineElement.getParentElement(), lineElement.getNextSibling());
    PersistentContent.makeHard(ContentElement.ELEMENT_MANAGER, paragraph);

    lineElement.setProperty(LINE, this);
    paragraph.setProperty(LINE, this);
  }

  // Linked list
  public Line next() {
    return next;
  }

  public Line previous() {
    return previous;
  }

  public void insertAfter(Line previousLine) {
    Preconditions.checkNotNull(previousLine, "Previous line must not be null");
    previous = previousLine;
    next = previous.next;
    previous.next = this;
    if (next != null) {
      next.previous = this;
    }
  }

  public void insertBefore(Line nextLine) {
    Preconditions.checkNotNull(nextLine, "Next line must not be null");
    next = nextLine;
    previous = next.previous;
    next.previous = this;
    if (previous != null) {
      previous.next = this;
    }
  }

  public void remove() {
    // delete if in tree:
    // NOTE(danilatos): This must come first so the rendering logic has
    // a chance to take into account the surrounding structure, before
    // the line is removed from the linked list.
    if (paragraph.getParentElement() != null) {
      cxt.annotatableContent().transparentDeepRemove(paragraph);
    }

    if (next != null) {
      next.previous = previous;
    }
    if (previous != null) {
      previous.next = next;
    }
    previous = next = null;
  }

  // Bundle exposure
  public MutableDocument<ContentNode, ContentElement, ContentTextNode> getMutableDoc() {
    return cxt.document();
  }

  public ContentElement getParagraph() {
    return paragraph;
  }

  public ContentElement getLineElement() {
    return lineElement;
  }

  // Replacement for element methods
  public String getAttribute(String name) {
    return lineElement.getAttribute(name);
  }

  public Map<String, String> getAttributes() {
    return getMutableDoc().getAttributes(lineElement);
  }

  // Behavioural modifiers
  public ParagraphBehaviour getBehaviour() {
    return ParagraphBehaviour.of(getAttribute(Paragraph.SUBTYPE_ATTR));
  }

  public boolean isDecimalListItem() {
    return Paragraph.isDecimalListItem(paragraph);
  }

  public int getIndent() {
    return lineElement == null ? 0 :
        Paragraph.getIndent(lineElement.getAttribute(Paragraph.INDENT_ATTR));
  }

  /**
   * Cache the number value for a bullet point. Use {@value #DIRTY} to clear
   * the cache.
   *
   * The value must only be set if it is accurate. It must be cleared when
   * inaccurate.
   */
  public void setCachedNumberValue(int value) {
    this.cachedNumberValue = value;
  }

  /**
   * Returns the current cached number value for a bullet point, or
   * {@value #DIRTY} if no value is cached. If there is a value, it should
   * always be usable without additional verification (see
   * {@link #setCachedNumberValue(int)})
   */
  public int getCachedNumberValue() {
    return cachedNumberValue;
  }

  @Override
  public String toString() {
    return "Line("
        + XmlStringBuilder.createNode(FullContentView.INSTANCE, lineElement).getXmlString() + ", "
        + cachedNumberValue + ")";
  }
}
