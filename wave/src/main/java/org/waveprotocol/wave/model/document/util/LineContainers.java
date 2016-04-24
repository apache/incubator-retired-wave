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

package org.waveprotocol.wave.model.document.util;


import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocument.Action;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Constants and utilities for line containers
 *
 * TODO(danilatos): This should become a wrapper on the document, with the static
 * methods no longer being static, and the tag dependencies being injected.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class LineContainers {

  /**
   * True if line containers should be used - false if still using old
   * paragraphs.
   *
   * NOTE(danilatos): Setting this to true might break
   * AggressiveSelectionHelperTest. It will need updating.
   */
  public static final boolean USE_LINE_CONTAINERS_BY_DEFAULT = true;

  public static final String LINE_TAGNAME = "line";

  public static final String PARAGRAPH_NS = "l";
  public static final String PARAGRAPH_TAGNAME = "p";
  public static final String PARAGRAPH_FULL_TAGNAME = PARAGRAPH_NS + ":" + PARAGRAPH_TAGNAME;

  // TODO(danilatos): Convert this class to no longer be static,
  // and then these would be member variables.
  private static String topLevelContainerTagname;
  private static final Set<String> lineContainerTagnames = new HashSet<String>();

  /**
   * Sets the tag name for the top level line container
   *
   * MUST be set before attempting to append a line to an empty document
   *
   * @param tagName
   */
  public static void setTopLevelContainerTagname(String tagName) {
    Preconditions.checkNotNull(tagName, "Top level tag name must not be null");
    topLevelContainerTagname = tagName;
    registerLineContainerTagname(tagName);
  }

  /**
   * Default tag name for the top level line container
   *
   * Implicitly created when appending lines to empty documents
   */
  public static String topLevelContainerTagname() {
    Preconditions.checkState(topLevelContainerTagname != null,
        "Top level line container tag name not set!");
    return topLevelContainerTagname;
  }

  /**
   * Register a tag name as a line container, to recognise all
   * elements with that tag name as being able to contain line elements.
   *
   * @param tagName
   */
  public static void registerLineContainerTagname(String tagName) {
    lineContainerTagnames.add(tagName);
  }

  /**
   * Defines a text rounding granularity
   */
  // TODO(danilatos/mtsui): Should this be unified with either of the two MoveUnit enums?
  // This currently does not include any display logic (such as visual line vs logical line,
  // or page), which is a bit different to the other move units, which contain both visual
  // and logical units.
  public enum Rounding {
    /** No rounding */
    NONE,
    /** Round to word boundary */
    WORD,
    /** Round to sentence boundary */
    SENTENCE,
    /** Round to line boundary */
    LINE
  }

  /**
   * Defines in which direction rounding is to be applied.
   */
  public enum RoundDirection {
    LEFT,
    RIGHT
  }

  /**
   * @param doc
   * @param rounding
   * @param location
   * @param direction Whether the rounding goes leftwards or rightwards
   * @return the given location rounded rightwards to the requested granularity
   */
  public static <N, E extends N, T extends N> Point<N> roundLocation(
      ReadableDocument<N, E, T> doc, Rounding rounding, Point<N> location,
      RoundDirection direction) {
    Preconditions.checkNotNull(direction, "Rounding direction cannot be null.");

    switch (rounding) {
      case NONE:
        return location;
      case WORD:
      case SENTENCE:
        // TODO(mtsui/danilatos): Use and/or unify with TextLocator
        throw new UnsupportedOperationException("Not implemented");
      case LINE:
        checkNotParagraphDocument(doc);

        Point<N> point = jumpOutToContainer(doc, location);
        if (point == null) {
          return null;
        }

        E el = Point.enclosingElement(doc, point);
        if (direction == RoundDirection.RIGHT) { // round to the right
          N nodeAfter = point.isInTextNode()
              ? doc.getNextSibling(point.getContainer()) : point.getNodeAfter();
          while (nodeAfter != null && !isLineElement(doc, nodeAfter)) {
            nodeAfter = doc.getNextSibling(nodeAfter);
          }
          return Point.<N>inElement(el, nodeAfter);
        } else { // otherwise, round left (backwards)
          N nodeBefore = point.isInTextNode() ? doc.getPreviousSibling(point.getContainer())
              : Point.nodeBefore(doc, point.asElementPoint());
          while (nodeBefore != null && !isLineElement(doc, nodeBefore)) {
            nodeBefore = doc.getPreviousSibling(nodeBefore);
          }
          return nodeBefore == null ? null : Point.before(doc, nodeBefore);
        }
      default:
        throw new AssertionError("Missing rounding implementations");
    }
  }

  /**
   * Predicates a node being a line container
   */
  public static final DocPredicate LINE_CONTAINER_PREDICATE = new DocPredicate() {
    @Override
    public <N, E extends N, T extends N> boolean apply(ReadableDocument<N, E, T> doc, N node) {
      return isLineContainer(doc, node);
    }
  };

  /**
   * Jumps the point out to the enclosing line container, if any
   *
   * @see DocHelper#jumpOut(ReadableDocument, Point, DocPredicate)
   */
  public static <N, E extends N, T extends N> Point<N> jumpOutToContainer(
      ReadableDocument<N, E, T> doc, Point<N> location) {
    return DocHelper.jumpOut(doc, location, LINE_CONTAINER_PREDICATE);
  }

  /**
   * Finds the last line element that is before a given location, which should be within a
   * line container element.
   *
   * @param doc
   * @param at
   * @return The line element or null if not found.
   */
  public static <N, E extends N, T extends N> E getRelatedLineElement(
      ReadableDocument<N, E, T> doc, Point<N> at) {
    Point<N> atStart = roundLocation(doc, Rounding.LINE, at, RoundDirection.LEFT);

    // atStart should now have the lineContainer as the parent and the line element as nodeAfter:
    if (atStart == null || atStart.getNodeAfter() == null) {
      return null; // nothing found
    }

    return doc.asElement(atStart.getNodeAfter());
  }

  /**
   * @param doc
   * @param point
   * @return true if the given location is at the end of a line
   */
  public static <N, E extends N, T extends N> boolean isAtLineEnd(
      ReadableWDocument<N, E, T> doc, Point<N> point) {
    return doc.getLocation(point) == doc.getLocation(LineContainers.roundLocation(
        doc, Rounding.LINE, point, RoundDirection.RIGHT));
  }

  /**
   * @param doc
   * @param point
   * @return true if the given location is at the start of a line
   */
  public static <N, E extends N, T extends N> boolean isAtLineStart(
      ReadableWDocument<N, E, T> doc, Point<N> point) {
    E elementBefore = point == null ? null : Point.elementBefore(doc, point);
    return elementBefore != null ? isLineElement(doc, elementBefore) : false;
  }

  /**
   * @param doc
   * @param point
   * @return true if the given location is at an empty line
   */
  public static <N, E extends N, T extends N> boolean isAtEmptyLine(
      ReadableWDocument<N, E, T> doc, Point<N> point) {
    return isAtLineStart(doc, point) && isAtLineEnd(doc, point);
  }

  /**
   * Inserts content into a point that is within a line. If the point is not
   * within a line, will create a new one at the next available location.
   *
   * @param doc the document to insert into.
   * @param point the point within a line to insert.
   * @param content the content to insert.
   * @return the node that was inserted into.
   */
  public static <N, E extends N, T extends N> N insertInto(MutableDocument<N, E, T> doc,
      Point<N> point, XmlStringBuilder content) {

    checkNotParagraphDocument(doc);

    E lc = null;
    for (E el : DocIterate.deepElementsReverse(doc, doc.getDocumentElement(), null)) {
      if (isLineContainer(doc, el)) {
        lc = el;
        break;
      }
    }
    if (lc != null) {
      // This garbage code attempts to figure out if the current location
      // is after a line declaration. Has to be an easier way, but this is
      // quick and dirty.
      int location = doc.getLocation(point);
      // Find the first line.
      for (N child = doc.getFirstChild(lc); child != null; child = doc.getNextSibling(lc)) {
        if (isLineElement(doc, child)) {
          if (doc.getLocation(child) < location) {
            return doc.insertXml(point, content);
          }
        }
      }
    }

    // Just insert a line here.
    return insertContentOnNewLine(doc, Rounding.NONE, point, content);
  }

  /**
   * Deletes a line inside of a line container. Takes care to not invalidate
   * the schema by leaving an empty line container. If the line to be deleted
   * is the last one, then it will be emptied and left alone instead.
   *
   * @param doc
   * @param line the element marking the start of the line to remove.
   */
  public static <N, E extends N, T extends N> void deleteLine(MutableDocument<N, E, T> doc,
      E line) {
    checkNotParagraphDocument(doc);
    if (!isLineElement(doc, line)) {
      Preconditions.illegalArgument("Not a line element: " + line);
    }

    E lc = doc.getParentElement(line);
    if (!isLineContainer(doc, lc)) {
      Preconditions.illegalArgument("Not a line container: " + lc);
    }

    boolean isFirstLine = doc.getFirstChild(lc) == line;

    Point<N> deleteEndPoint =
        roundLocation(doc, Rounding.LINE, Point.after(doc, line), RoundDirection.RIGHT);
    // If this is not the first line or there is another line, then we can
    // delete this one. Otherwise, empty it and leave it.
    Point<N> deleteStartPoint = null;
    if (!isFirstLine || isLineElement(doc, deleteEndPoint.getNodeAfter())) {
      deleteStartPoint = Point.before(doc, line);
    } else {
      doc.emptyElement(line);
      deleteStartPoint = Point.after(doc, line);
    }

    doc.deleteRange(deleteStartPoint, deleteEndPoint);
  }

  /**
   * For a given document, will linearly scan all lines and return a list of
   * ranges representing each. The start point of each range will be the first
   * point after the end line element and the end point will be the point
   * before the next line (or before the end tag of the line container in the
   * case of the last line).
   *
   * @param doc
   * @return list of ranges representing each line.
   */
  public static <N, E extends N, T extends N> List<Range> getLineRanges(
      MutableDocument<N, E, T> doc) {
    checkNotParagraphDocument(doc);

    List<Range> lines = new ArrayList<Range>();
    N root = doc.getDocumentElement();
    for (N lc = doc.getFirstChild(root); lc != null; lc = doc.getNextSibling(lc)) {
      if (isLineContainer(doc, lc)) {
        int start = -1;
        for (N line = doc.getFirstChild(lc); line != null; line = doc.getNextSibling(line)) {
          if (isLineElement(doc, line)) {
            if (start > 0) {
              int end = doc.getLocation(Point.before(doc, line));
              lines.add(new Range(start, end));
            }
            start = doc.getLocation(Point.after(doc, line));
          }
        }
        if (start > 0) {
          lines.add(new Range(start, doc.getLocation(Point.end(lc))));
        }
      }
    }

    return lines;
  }

  /**
   * Inserts a line at the given location
   *
   * @param doc
   * @param rounding rightwards rounding to apply to the given location
   * @param location
   * @return the new line element
   *
   * Temporarily supports paragraphs as well
   */
  public static <N, E extends N, T extends N> E insertLine(final MutableDocument<N, E, T> doc,
      Rounding rounding, Point<N> location) {
    return insertLine(doc, rounding, location, Attributes.EMPTY_MAP);
  }

  /**
   * Inserts a line at the given location
   *
   * @param doc
   * @param rounding rightwards rounding to apply to the given location
   * @param location
   * @param attributes
   * @return the new line element
   *
   * Temporarily supports paragraphs as well
   */
  public static <N, E extends N, T extends N> E insertLine(final MutableDocument<N, E, T> doc,
      Rounding rounding, Point<N> location, Attributes attributes) {

    Preconditions.checkNotNull(rounding, "rounding must not be null");

    location = roundLocation(doc, rounding, location, RoundDirection.RIGHT);
    Preconditions.checkArgument(location != null, "location is not a valid place to insert a line");

    checkNotParagraphDocument(doc);

    // Make sure this is a valid place to insert the line, even if it means
    // dishonouring the rounding. Line rounding should already have done this.
    if (rounding != Rounding.LINE) {
      location = jumpOutToContainer(doc, location);
    }

    return doc.createElement(location, LINE_TAGNAME, attributes);
  }

  public static <N, E extends N, T extends N> N insertContentOnNewLine(
      final MutableDocument<N, E, T> doc,
      Rounding rounding, Point<N> location, XmlStringBuilder initialContent) {
    return insertContentIntoLineStart(doc, insertLine(doc, rounding, location), initialContent);
  }

  public static <N, E extends N, T extends N> N appendContentOnNewLine(
      MutableDocument<N, E, T> doc, XmlStringBuilder initialContent) {
    // TODO(user): This is redundant to appendLine with content. Remove.
    return insertContentIntoLineStart(doc, appendLine(doc, null), initialContent);
  }

  /**
   * Inserts content into the end of the line specified by the element.
   *
   * @param doc
   * @param line the line element to insert into
   * @param content the content to insert
   * @return the node that was inserted into.
   */
  public static <N, E extends N, T extends N> N insertContentIntoLineEnd(
      final MutableDocument<N, E, T> doc, E line, XmlStringBuilder content) {
    // Find the next line and insert just before it.
    Point<N> point = roundLocation(doc, Rounding.LINE, Point.start(doc, line),
        RoundDirection.RIGHT);
    if (point == null) {
      throw new AssertionError("Not a valid line location.");
    }
    return doc.insertXml(point, content);
  }

  /**
   * Inserts content into the start of the line specified by the element.
   *
   * @param doc
   * @param line the line element to insert into
   * @param initialContent the content to insert
   * @return the node that was inserted.
   */
  public static <N, E extends N, T extends N> N insertContentIntoLineStart(
      final MutableDocument<N, E, T> doc, E line, XmlStringBuilder initialContent) {
    doc.insertXml(Point.after(doc, line), initialContent);
    return doc.getNextSibling(line);
  }

  public static void properAppendLine(final MutableDocument<?, ?, ?> doc,
      final XmlStringBuilder content) {
    doc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        appendLine(doc, content);
      }
    });
  }

  public static <N, E extends N, T extends N> E appendLine(MutableDocument<N, E, T> doc,
      XmlStringBuilder content) {
    return appendLine(doc, content, Attributes.EMPTY_MAP);
  }

  /**
   * Appends a line to the last line container of the document
   *
   * If the document has no line containers, one will be created at the end of
   * the document.
   *
   * Temporarily also supports old style paragraphs for old documents, in which
   * case the new paragraph is returned
   *
   * @param doc
   * @param content optional content for the new line, may be null
   * @return the line token representing the start of the new line
   */
  public static <N, E extends N, T extends N> E appendLine(final MutableDocument<N, E, T> doc,
      XmlStringBuilder content, Attributes attributes) {

    checkNotParagraphDocument(doc);

    E lc = null;
    for (E el : DocIterate.deepElementsReverse(doc, doc.getDocumentElement(), null)) {
      if (isLineContainer(doc, el)) {
        lc = el;
        break;
      }
    }

    if (lc == null) {
      // Create the <body><line></line></body> in one go.
      lc = doc.appendXml(XmlStringBuilder.createEmpty().wrap(LINE_TAGNAME).wrap(
          topLevelContainerTagname()));

      // Add the content before </body>
      if (content != null && content.getLength() > 0) {
        doc.insertXml(Point.<N>end(lc), content);
      }
      E line = doc.asElement(doc.getFirstChild(lc));
      assert line != null;
      if (attributes != null) {
        doc.setElementAttributes(line, attributes);
      }
      return line;
    } else {
      return appendLine(doc, lc, content, attributes);
    }
  }

  /**
   * Finds the last valid line and appends to the end of it.
   *
   * @param doc the document to insert into.
   * @param content the content to append.
   */
  public static <N, E extends N, T extends N> E appendToLastLine(MutableDocument<N, E, T> doc,
      XmlStringBuilder content) {
    checkNotParagraphDocument(doc);

    // TODO(user): Don't duplicate the code below.
    for (E el : DocIterate.deepElementsReverse(doc, doc.getDocumentElement(), null)) {
      if (isLineContainer(doc, el)) {
        // TODO(user): Check for at least a line tag? I'm assuming
        // there is one...
        Point<N> point = Point.inElement((N) el, null);
        if (point != null) {
          return doc.insertXml(point, content);
        }
      }
    }

    // Looks like no line to add to, just append.
    return appendLine(doc, content);
  }

  public static <N, E extends N, T extends N> E appendLine(final MutableDocument<N, E, T> doc,
      E lineContainer, XmlStringBuilder content) {
    return appendLine(doc, lineContainer, content, Attributes.EMPTY_MAP);
  }

  /**
   * Appends a line to the given line container
   *
   * @param doc
   * @param lineContainer
   * @param content optional content for the new line, may be null
   * @param attributes optional attributes, may be null.
   * @return the line token representing the start of the new line
   */
  public static <N, E extends N, T extends N> E appendLine(final MutableDocument<N, E, T> doc,
      E lineContainer, XmlStringBuilder content, Attributes attributes) {
    E line = doc.createChildElement(lineContainer, LINE_TAGNAME, attributes);
    if (content != null && content.getLength() > 0) {
      doc.insertXml(Point.<N>end(lineContainer), content);
    }
    return line;
  }

  /**
   * Returns true iff the tagname is a line container.
   *
   *         NOTE(danilatos): In the future, the match may involve more than
   *         just a tag name check. Other element types, such as table cells,
   *         might be line containers.
   *
   * @param tagname tagname to check
   * @return true iff the tagname is a line container
   */
  public static boolean isLineContainerTagname(String tagname) {
    return lineContainerTagnames.contains(tagname);
  }

  /**
   * @param doc
   * @return true if the given document is an old-style-paragraph document
   */
  @Deprecated
  private static <N, E extends N, T extends N> boolean isUnsupportedParagraphDocument(
      ReadableDocument<N, E, T> doc) {
    if (doc.getFirstChild(doc.getDocumentElement()) == null) {
      // If the document is empty, check what the default global option is
      return !USE_LINE_CONTAINERS_BY_DEFAULT;
    }
    // Testing all children in the case of special <input> tags
    N root = doc.getDocumentElement();
    for (N child = doc.getFirstChild(root); child != null; child = doc.getNextSibling(child)) {
      if (isUnsupportedParagraphElement(doc, child)) {
        return true;
      }
    }
    return false;
  }

  /** For temporary assertion purposes */
  public static <N, E extends N, T extends N> void checkNotParagraphDocument(
      ReadableDocument<N, E, T> doc) {
    Preconditions.checkArgument(!isUnsupportedParagraphDocument(doc),
        "Paragraph docs no longer supported");
  }

  /**
   * @param doc
   * @param node
   * @return true if the node is a line container element
   *
   *         NOTE(danilatos): In the future, the match may involve more than
   *         just a tag name check. Other element types, such as table cells,
   *         might be line containers.
   */
  public static <N, E extends N> boolean isLineContainer(
      final ReadableDocument<N, E, ?> doc, N node) {
    E el = doc.asElement(node);
    if (el != null) {
      return isLineContainerTagname(doc.getTagName(el));
    } else {
      return false;
    }
  }

  /**
   * @param doc
   * @param node
   * @return true if the node is a line token element
   */
  public static <N, E extends N> boolean isLineElement(
      final ReadableDocument<N, E, ?> doc, N node) {
    return DocHelper.isMatchingElement(doc, node, LINE_TAGNAME);
  }

  /**
   * @param doc
   * @param element a line element
   * @return true if the element is the first line in the document
   */
  public static <N, E extends N> boolean isFirstLine(
      final ReadableDocument<N, E, ?> doc, E element) {
    Preconditions.checkArgument(isLineElement(doc, element), "not a line element");
    return DocHelper.getPreviousSiblingElement(doc, element) == null;
  }

  /** to be deleted */
  @Deprecated
  public static <N, E extends N> boolean isUnsupportedParagraphElement(
      final ReadableDocument<N, E, ?> doc, N node) {
    return DocHelper.isMatchingElement(doc, node, PARAGRAPH_TAGNAME);
  }

  /**
   * Used for testing purposes, wraps content with correct tags.
   *
   * @param lines the lines to wrap.
   * @return the wrapped content.
   */
  public static String debugLineWrap(String ... lines) {
    StringBuilder body = new StringBuilder();
    for (String line : lines) {
      body.append("<" + LINE_TAGNAME + "></" + LINE_TAGNAME + ">" + line);
    }
    return body.toString();
  }

  /**
   * Used for testing purposes, wraps content with correct tags.
   *
   * @param lines the lines to wrap. if null, will not add a new line.
   * @return the wrapped content.
   */
  public static String debugContainerWrap(String ... lines) {
    return "<" + topLevelContainerTagname + ">" + debugLineWrap(lines)
        + "</" + topLevelContainerTagname + ">";
  }

  private LineContainers() {
  }
}
