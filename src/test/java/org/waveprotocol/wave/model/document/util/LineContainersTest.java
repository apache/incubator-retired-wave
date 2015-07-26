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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.LineContainers.RoundDirection;
import org.waveprotocol.wave.model.document.util.LineContainers.Rounding;
import org.waveprotocol.wave.model.schema.AbstractXmlSchemaConstraints;

import java.util.Collections;
import java.util.List;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class LineContainersTest extends TestCase {

  private final DocumentSchema SCHEMA = new AbstractXmlSchemaConstraints() {
    {
      addChildren(null, "body");

      addChildren("body", "line", "input");
      containsBlipText("body");
      containsBlipText("input");

      addRequiredInitial("body", Collections.singletonList("line"));

      addAttrWithValues("line", "t", "h1", "h2", "h3", "h4", "li");
    }
  };

  private TestDocumentContext<Node, Element, Text> cxt;
  private MutableDocument<Node, Element, Text> doc;

  @Override
  protected void setUp() throws Exception {
    LineContainers.setTopLevelContainerTagname("body");
  }

  public void testTopLevelContainerTagName() {
    LineContainers.setTopLevelContainerTagname("blah");
    assertEquals("blah", LineContainers.topLevelContainerTagname());
    LineContainers.setTopLevelContainerTagname("body");
    assertEquals("body", LineContainers.topLevelContainerTagname());
    try {
      LineContainers.setTopLevelContainerTagname(null);
      fail("null should be rejected");
    } catch (RuntimeException e) {
      // ok
    }
  }

  public void testWrappers() {
    assertEquals("<line></line>foo", LineContainers.debugLineWrap("foo"));
    assertEquals("<body></body>", LineContainers.debugContainerWrap());
    assertEquals("<body><line></line>foo</body>", LineContainers.debugContainerWrap("foo"));
    assertEquals("<body><line></line>foo<line></line>bar</body>",
        LineContainers.debugContainerWrap("foo", "bar"));
  }

  private class LineTestState {
    {
      getDocWithoutSchema("<body><line/>abc<x>def<line id=\"bad\"/></x>."
          + "<line id=\"2\"/><line id=\"3\"/>ghi</body>jkl");
    }
    Element lc = DocHelper.getElementWithTagName(doc, "body");
    Point<Node> beforeLine = Point.before(doc, DocHelper.getElementWithTagName(doc, "line"));
    Point<Node> afterLine = Point.after(doc, DocHelper.getElementWithTagName(doc, "line"));
    Point<Node> inAbc = Point.inText(afterLine.getNodeAfter(), 2);
    Element x = DocHelper.getElementWithTagName(doc, "x");
    Point<Node> inX = Point.start(doc, x);
    Point<Node> afterX = Point.after(doc, x);
    Point<Node> beforeInvalidLine = Point.before(doc, DocHelper.findElementById(doc, "bad"));
    Point<Node> beforeLine2 = Point.before(doc, DocHelper.findElementById(doc, "2"));
    Point<Node> beforeLine3 = Point.before(doc, DocHelper.findElementById(doc, "3"));
    Point<Node> endLc = Point.<Node>end(lc);
    Point<Node> inGhi = Point.inText(endLc.getContainer().getLastChild(), 2);
    Point<Node> endGhi = Point.inText(endLc.getContainer().getLastChild(), 3);
  }

  public void testEndOfLineCheck() {
    LineTestState s = new LineTestState();

    // Consider having this be satisfied:
    // assertFalse(LineContainers.isAtLineEnd(doc, s.beforeLine));

    assertTrue(LineContainers.isAtLineEnd(doc, s.beforeLine2));
    assertTrue(LineContainers.isAtLineEnd(doc, s.beforeLine3));
    assertFalse(LineContainers.isAtLineEnd(doc, s.inAbc));
    assertFalse(LineContainers.isAtLineEnd(doc, s.inX));
    s.afterX = doc.deleteRange(s.afterX, s.beforeLine2).getSecond();
    assertTrue(LineContainers.isAtLineEnd(doc, s.afterX));
    assertFalse(LineContainers.isAtLineEnd(doc, s.inGhi));
    assertTrue(LineContainers.isAtLineEnd(doc, s.endGhi));
  }

  public void testStartOfLineCheck() {
    LineTestState s = new LineTestState();
    assertTrue(LineContainers.isAtLineStart(doc, s.afterLine));
    assertFalse(LineContainers.isAtLineStart(doc, s.beforeLine2));
    assertTrue(LineContainers.isAtLineStart(doc, s.beforeLine3));
    assertFalse(LineContainers.isAtLineStart(doc, s.inAbc));
    assertFalse(LineContainers.isAtLineStart(doc, s.inX));
  }

  public void testEmptyLineCheck() {
    LineTestState s = new LineTestState();
    assertFalse(LineContainers.isAtEmptyLine(doc, s.afterLine));
    assertFalse(LineContainers.isAtEmptyLine(doc, s.beforeLine2));
    assertTrue(LineContainers.isAtEmptyLine(doc, s.beforeLine3));
    assertFalse(LineContainers.isAtEmptyLine(doc, s.inAbc));
    assertFalse(LineContainers.isAtEmptyLine(doc, s.inX));
  }

  public void testRounding() {
    LineTestState s = new LineTestState();

    // at node boundaries
    assertSame(s.beforeLine, LineContainers.roundLocation(
        doc, Rounding.NONE, s.beforeLine, RoundDirection.RIGHT));
    assertSame(s.afterLine, LineContainers.roundLocation(
        doc, Rounding.NONE, s.afterLine, RoundDirection.RIGHT));
    // in text
    assertSame(s.inAbc, LineContainers.roundLocation(
        doc, Rounding.NONE, s.inAbc, RoundDirection.RIGHT));
    // in a nested element, check it does not jump out
    assertSame(s.inX, LineContainers.roundLocation(
        doc, Rounding.NONE, s.inX, RoundDirection.RIGHT));

    try {
      LineContainers.roundLocation(doc, Rounding.WORD, s.afterLine, RoundDirection.RIGHT);
      fail("You forgot to write a unit test when implementing word rounding!");
    } catch (UnsupportedOperationException e) {
      // ok
    }

    try {
      LineContainers.roundLocation(doc, Rounding.SENTENCE, s.afterLine, RoundDirection.RIGHT);
      fail("You forgot to write a unit test when implementing sentence rounding!");
    } catch (UnsupportedOperationException e) {
      // ok
    }

    // just before the first line
    assertEquals(s.beforeLine, LineContainers.roundLocation(
        doc, Rounding.LINE, s.beforeLine, RoundDirection.RIGHT));
    // just after the preceding line
    assertEquals(s.beforeLine2, LineContainers.roundLocation(
        doc, Rounding.LINE, s.afterLine, RoundDirection.RIGHT));
    // in text
    assertEquals(s.beforeLine2, LineContainers.roundLocation(
        doc, Rounding.LINE, s.inAbc, RoundDirection.RIGHT));
    // *does* jump out
    assertEquals(s.beforeLine2, LineContainers.roundLocation(
        doc, Rounding.LINE, s.inX, RoundDirection.RIGHT));
    // ignore lines not direct children of the line container
    assertEquals(s.beforeLine2, LineContainers.roundLocation(
        doc, Rounding.LINE, s.beforeInvalidLine, RoundDirection.RIGHT));
    // just before a line with preceding content
    assertEquals(s.beforeLine2, LineContainers.roundLocation(
        doc, Rounding.LINE, s.beforeLine2, RoundDirection.RIGHT));
    // just before a line with an immediately preceeding line
    assertEquals(s.beforeLine3, LineContainers.roundLocation(
        doc, Rounding.LINE, s.beforeLine3, RoundDirection.RIGHT));
    // inside text at the end
    assertEquals(s.endLc, LineContainers.roundLocation(
        doc, Rounding.LINE, s.inGhi, RoundDirection.RIGHT));
    // just before the end
    assertEquals(s.endLc, LineContainers.roundLocation(
        doc, Rounding.LINE, s.endLc, RoundDirection.RIGHT));
    // outside of line container is invalid
    assertNull(LineContainers.roundLocation(
        doc, Rounding.LINE, Point.before(doc, s.lc), RoundDirection.RIGHT));
    assertNull(LineContainers.roundLocation(
        doc, Rounding.LINE, Point.after(doc, s.lc), RoundDirection.RIGHT));
    assertNull(LineContainers.roundLocation(
        doc, Rounding.LINE,
        Point.inText(s.lc.getNextSibling(), 2), RoundDirection.RIGHT));
  }

  public void testInsertLine() {
    LineTestState s;

    // at node boundaries
    s = new LineTestState();
    checkInsertLine(s.beforeLine, Rounding.NONE, s.beforeLine);
    s = new LineTestState();
    checkInsertLine(s.afterLine, Rounding.NONE, s.afterLine);
    // in text
    s = new LineTestState();
    checkInsertLine(s.inAbc, Rounding.NONE, s.inAbc);
    // in a nested element, check it *does* jump out
    s = new LineTestState();
    checkInsertLine(s.afterX, Rounding.NONE, s.inX);

    // just before the first line
    s = new LineTestState();
    checkInsertLine(s.beforeLine, Rounding.LINE, s.beforeLine);
    // just after the preceding line
    s = new LineTestState();
    checkInsertLine(s.beforeLine2, Rounding.LINE, s.afterLine);
    // in text
    s = new LineTestState();
    checkInsertLine(s.beforeLine2, Rounding.LINE, s.inAbc);
    // *does* jump out
    s = new LineTestState();
    checkInsertLine(s.beforeLine2, Rounding.LINE, s.inX);
    // ignore lines not direct children of the line container
    s = new LineTestState();
    checkInsertLine(s.beforeLine2, Rounding.LINE, s.beforeInvalidLine);
    // just before a line with preceding content
    s = new LineTestState();
    checkInsertLine(s.beforeLine2, Rounding.LINE, s.beforeLine2);
    // just before a line with an immediately preceeding line
    s = new LineTestState();
    checkInsertLine(s.beforeLine3, Rounding.LINE, s.beforeLine3);
    // inside text at the end
    s = new LineTestState();
    checkInsertLine(s.endLc, Rounding.LINE, s.inGhi);
    // just before the end
    s = new LineTestState();
    checkInsertLine(s.endLc, Rounding.LINE, s.endLc);

    // append
    s = new LineTestState();
    checkAppendLine(s.endLc, null);
    s = new LineTestState();
    checkAppendLine(s.endLc, XmlStringBuilder.createText("blah").wrap("y"));

    getDocWithoutSchema("");
    LineContainers.appendLine(doc, XmlStringBuilder.createText("blah").wrap("y"));
    assertEquals("<body><line></line><y>blah</y></body>",
        XmlStringBuilder.innerXml(doc).toString());

    getDocWithoutSchema("<x>abc</x>");
    LineContainers.appendLine(doc, XmlStringBuilder.createText("blah").wrap("y"));
    assertEquals("<x>abc</x><body><line></line><y>blah</y></body>",
        XmlStringBuilder.innerXml(doc).toString());

    // outside of line container is invalid
    try {
      s = new LineTestState();
      LineContainers.insertLine(doc, Rounding.LINE, Point.before(doc, s.lc));
      fail("Expected invalid location exception");
    } catch (IllegalArgumentException iae) {
      // ok
    }
    try {
      s = new LineTestState();
      LineContainers.insertLine(doc, Rounding.LINE, Point.after(doc, s.lc));
      fail("Expected invalid location exception");
    } catch (IllegalArgumentException iae) {
      // ok
    }
    try {
      s = new LineTestState();
      LineContainers.insertLine(doc, Rounding.LINE,
          Point.inText(s.lc.getNextSibling(), 2));
      fail("Expected invalid location exception");
    } catch (IllegalArgumentException iae) {
      // ok
    }
  }

  public void testAppendLineWithAttributes() {
    getDocWithSchema("");
    LineContainers.appendLine(doc, XmlStringBuilder.createText("hi"),
        new AttributesImpl("t", "h2"));
    assertEquals("h2", doc.getAttribute(DocHelper.getElementWithTagName(doc, "line"), "t"));

    getDocWithSchema("<body><line/>abc</body>");
    LineContainers.appendLine(doc, XmlStringBuilder.createText("hi"),
        new AttributesImpl("id", "2", "t", "h2"));
    assertEquals("h2", doc.getAttribute(DocHelper.findElementById(doc, "2"), "t"));
  }

  public void testAppendObeysSchema() {
    getDocWithSchema("");
    LineContainers.appendLine(doc, XmlStringBuilder.createText("hi"));
  }

  public void testInsertInto() {
    getDocWithSchema("<body><line/>abc</body>");
    Point<Node> afterLine = Point.after(doc, DocHelper.getElementWithTagName(doc, "line"));
    LineContainers.insertInto(doc, afterLine,
        XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line><input>blah</input>abc</body>",
        XmlStringBuilder.innerXml(doc).toString());

    getDocWithSchema("<body><line/>abc</body>");
    Point<Node> beforeLine = Point.before(doc, DocHelper.getElementWithTagName(doc, "line"));
    LineContainers.insertInto(doc, beforeLine,
        XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line><input>blah</input><line></line>abc</body>",
        XmlStringBuilder.innerXml(doc).toString());
  }

  public void testInsertContentIntoLineStart() {
    getDocWithSchema("<body><line/>abc</body>");
    LineContainers.insertContentIntoLineStart(doc, DocHelper.getElementWithTagName(doc, "line"),
        XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line><input>blah</input>abc</body>",
        XmlStringBuilder.innerXml(doc).toString());
  }

  public void testInsertContentIntoLineEnd() {
    getDocWithSchema("<body><line/>abc</body>");
    LineContainers.insertContentIntoLineEnd(doc, DocHelper.getElementWithTagName(doc, "line"),
        XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line>abc<input>blah</input></body>",
        XmlStringBuilder.innerXml(doc).toString());
  }

  public void testAppendToLastLine() {
    getDocWithSchema("<body><line/>abc</body>");
    LineContainers.appendToLastLine(doc, XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line>abc<input>blah</input></body>",
        XmlStringBuilder.innerXml(doc).toString());

    getDocWithoutSchema("");
    LineContainers.appendToLastLine(doc, XmlStringBuilder.createText("blah").wrap("input"));
    assertEquals("<body><line></line><input>blah</input></body>",
        XmlStringBuilder.innerXml(doc).toString());
  }

  public void testGetRelatedLineElement() {
    LineTestState s = new LineTestState();
    Element line1 = DocHelper.getElementWithTagName(doc, "line");
    Element line2 = DocHelper.findElementById(doc, "2");
    Element line3 = DocHelper.findElementById(doc, "3");

    getDocWithoutSchema("<lc><line/>abc<x>def<line id=\"bad\"/></x>."
        + "<line id=\"2\"/><line id=\"3\"/>ghi</lc>jkl");

    assertNull(LineContainers.getRelatedLineElement(doc, Point.start(doc, s.lc)));
    assertNull(LineContainers.getRelatedLineElement(doc, s.beforeLine));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.afterLine));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.inAbc));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.inX));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.afterX));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.beforeInvalidLine));
    assertSame(line1, LineContainers.getRelatedLineElement(doc, s.beforeLine2));
    assertSame(line2, LineContainers.getRelatedLineElement(doc, s.beforeLine3));
    assertSame(line3, LineContainers.getRelatedLineElement(doc, s.inGhi));
    assertSame(line3, LineContainers.getRelatedLineElement(doc, Point.end((Node)s.lc)));
  }

  public void testGetLineRanges() {
    getDocWithSchema("");
    List<Range> ranges = LineContainers.getLineRanges(doc);
    assertEquals(0, ranges.size());

    getDocWithSchema("<body><line/></body>");
    checkLineRanges("");

    getDocWithSchema("<body><line/>abc</body>");
    checkLineRanges("abc");

    getDocWithSchema("<body><line/>abc<input/>def</body>");
    checkLineRanges("abcdef");

    getDocWithSchema("<body><line/>abc<input>_</input>def</body>");
    checkLineRanges("abc_def");

    getDocWithSchema("<body><line/>abc<line/>def</body>");
    checkLineRanges("abc", "def");

    getDocWithSchema("<body><line/>a day<line/> late and <line/>a dollar<line/> short</body>");
    checkLineRanges("a day", " late and ", "a dollar", " short");
  }

  public void testDeleteLine() {
    getDocWithSchema("<body><line/>foo</body>");
    LineContainers.deleteLine(doc, DocHelper.getElementWithTagName(doc, "line"));
    assertEquals("<body><line></line></body>", XmlStringBuilder.innerXml(doc).toString());

    getDocWithoutSchema("<body><line><x/></line>foo</body>");
    LineContainers.deleteLine(doc, DocHelper.getElementWithTagName(doc, "line"));
    assertEquals("<body><line></line></body>", XmlStringBuilder.innerXml(doc).toString());

    getDocWithSchema("<body><line/>foo<line/>bar</body>");
    LineContainers.deleteLine(doc, DocHelper.getElementWithTagName(doc, "line"));
    assertEquals("<body><line></line>bar</body>", XmlStringBuilder.innerXml(doc).toString());

    getDocWithSchema("<body><line/>foo<line id=\"2\"/>bar<line/>baz</body>");
    LineContainers.deleteLine(doc, DocHelper.findElementById(doc, "2"));
    assertEquals("<body><line></line>foo<line></line>baz</body>",
        XmlStringBuilder.innerXml(doc).toString());

    getDocWithoutSchema("<x/><body><line/>foo</body>");
    try {
      LineContainers.deleteLine(doc, DocHelper.getElementWithTagName(doc, "x"));
      fail("Did not reject non-line element");
    } catch (IllegalArgumentException e) {
      // ok
    }
    assertEquals("<x></x><body><line></line>foo</body>", XmlStringBuilder.innerXml(doc).toString());
  }

  private void checkLineRanges(String ... expectedLines) {
    List<Range> ranges = LineContainers.getLineRanges(doc);
    assertEquals(expectedLines.length, ranges.size());

    int i = 0;
    for (Range r : ranges) {
      Point<Node> start = doc.locate(r.getStart());
      Point<Node> end = doc.locate(r.getStart());
      String expectedLine = expectedLines[i++];
      assertEquals(expectedLine, DocHelper.getText(doc, doc, r.getStart(), r.getEnd()));
    }
  }

  // Delete this after 2009/09/15
  public void testRejectsParagraphs() {
    getDocWithoutSchema("<p>blah</p>");
    try {
      LineContainers.appendLine(doc, XmlStringBuilder.createText("yep"));
      fail("Did not reject paragraph doc");
    } catch (IllegalArgumentException e) {
      // ok
    }
  }

  private void checkInsertLine(Point<Node> expectedLocation,
      Rounding rounding, Point<Node> location) {
    checkInsertLineish(expectedLocation, rounding, location, false, 0);
  }

  private void checkInsertLineish(Point<Node> expectedPoint,
      Rounding rounding, Point<Node> location, boolean useParagraphs, int add) {
    int expectedLocation = doc.getLocation(expectedPoint) + add;

    Element el = LineContainers.insertLine(doc, rounding, location);

    assertTrue(LineContainers.isLineElement(doc, el));
    assertEquals(expectedLocation, doc.getLocation(el));
  }

  private void checkAppendLine(Point<Node> expectedPoint, XmlStringBuilder content) {
    checkAppendLineish(expectedPoint, content, false);
  }

  private void checkAppendLineish(Point<Node> expectedPoint, XmlStringBuilder content,
      boolean useParagraphs) {
    int expectedLocation = doc.getLocation(expectedPoint);

    Element el = LineContainers.appendLine(doc, content);

    if (content == null) {
      content = XmlStringBuilder.createEmpty();
    }

    assertTrue(LineContainers.isLineElement(doc, el));
    XmlStringBuilderDoc<Node, Element, Text> siblingContent = XmlStringBuilder.createEmpty(doc);
    for (Node n = el.getNextSibling(); n != null; n = n.getNextSibling()) {
      siblingContent.appendNode(n);
    }
    assertEquals(content, siblingContent);
    assertEquals(expectedLocation, doc.getLocation(el));
  }

  private MutableDocument<Node, Element, Text> getDocWithoutSchema(String innerXml) {
    cxt = ContextProviders.createTestPojoContext(innerXml, null, null, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    return doc = cxt.document();
  }

  private MutableDocument<Node, Element, Text> getDocWithSchema(String innerXml) {
    cxt = ContextProviders.createTestPojoContext(innerXml, null, null, null,
        new DocumentSchema() {
          @Override
          public List<String> getRequiredInitialChildren(String typeOrNull) {
            return SCHEMA.getRequiredInitialChildren(typeOrNull);
          }

          @Override
          public boolean permitsAttribute(String type, String attributeName) {
            return (type.equals("line") && attributeName.equals("id"))
                || SCHEMA.permitsAttribute(type, attributeName);
          }

          @Override
          public boolean permitsAttribute(String type, String attributeName,
              String attributeValue) {
            return (type.equals("line") && attributeName.equals("id"))
                || SCHEMA.permitsAttribute(type, attributeName, attributeValue);
          }

          @Override
          public boolean permitsChild(String parentTypeOrNull, String childType) {
            return SCHEMA.permitsChild(parentTypeOrNull, childType);
          }

          @Override
          public PermittedCharacters permittedCharacters(String typeOrNull) {
            return SCHEMA.permittedCharacters(typeOrNull);
          }
    });
    return doc = cxt.document();
  }
}
