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

package org.waveprotocol.wave.model.conversation;

import static org.waveprotocol.wave.model.document.util.DocHelper.findLocationById;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class TitleHelperTest extends TestCase {
  TestDocumentContext<Node, Element, Text> cxt;
  MutableDocument<Node, Element, Text> doc;

  static {
    // HACK(user): Force class-load of Blips class, which clobbers the
    // line-container static constant.
    // TODO(danilatos/anorth/plesner): Fix please.
    Blips.init();
  }

  public void testEmptyTitleWhenNoAnnotation() {
    assertEquals("", TitleHelper.extractTitle(getDoc(
        "<body><line/>Some text<line/>Some more text</body>")));

    assertEquals("", TitleHelper.extractTitle(getDoc(
        "Just text")));

    assertFalse(TitleHelper.hasExplicitTitle(doc));
  }

  public void testExplicitValueOverrides() {
    getDoc("<body><line/>Some text<line/>Some more text</body>");

    doc.setAnnotation(2, 7, TitleHelper.TITLE_KEY, "Blah");
    assertEquals("Blah", TitleHelper.extractTitle(doc));
    assertTrue(TitleHelper.hasExplicitTitle(doc));
  }

  public void testEmptyValueUsesEncompassedText() {
    getDoc("<body><line id=\"1\"/>Some text<line/>Some more text</body>");
    int startText = findLocationById(doc, "1") + 2;

    for (int i = 0; i <= startText; i++) {
      doc.resetAnnotation(i, startText + 7, TitleHelper.TITLE_KEY, "");
      assertEquals("Some te", TitleHelper.extractTitle(doc));
    }

    doc.resetAnnotation(startText + 1, startText + 7, TitleHelper.TITLE_KEY, "");
    assertEquals("ome te", TitleHelper.extractTitle(doc));

    doc.resetAnnotation(startText + 1, startText + 13, TitleHelper.TITLE_KEY, "");
    assertEquals("ome textSo", TitleHelper.extractTitle(doc));
    assertFalse(TitleHelper.hasExplicitTitle(doc));
  }

  public void testSettingTitleChangesExpliciticity() {
    getDoc("<body><line id=\"1\"/>Some text<line/>Some mor</body>");
    int startText = findLocationById(doc, "1") + 2;

    // Explicit set on blank doc
    TitleHelper.setExplicitTitle(doc, "Blah");
    assertEquals("Blah", TitleHelper.extractTitle(doc));
    assertTrue(TitleHelper.hasExplicitTitle(doc));

    // Change to implicit
    TitleHelper.setImplicitTitle(doc, startText + 1, startText + 7);
    assertEquals("ome te", TitleHelper.extractTitle(doc));
    assertFalse(TitleHelper.hasExplicitTitle(doc));

    // Back to explicit
    TitleHelper.setExplicitTitle(doc, "Blah2");
    assertEquals("Blah2", TitleHelper.extractTitle(doc));
    assertTrue(TitleHelper.hasExplicitTitle(doc));
  }

  public void testFindingImplicitTitleDoesntOverrideExplicit() {
    getDoc("<body><line/>Some text<line/>Some more text</body>");

    TitleHelper.setExplicitTitle(doc, "Blah");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Blah", TitleHelper.extractTitle(doc));
    assertTrue(TitleHelper.hasExplicitTitle(doc));
  }

  public void testFindingImplicitTitleObeysSentenceAndFirstLineBoundaries() {
    getDoc("<body><line id=\"1\"/>Some text<line/>Some more. text?</body>");
    int firstLineLocation = findLocationById(doc, "1");
    int startText = firstLineLocation + 2;

    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text", TitleHelper.extractTitle(doc));
    assertFalse(TitleHelper.hasExplicitTitle(doc));
    // We want the first line token to be part of the title, so it's easy to tell
    assertEquals(firstLineLocation,
        doc.firstAnnotationChange(0, doc.size(), TitleHelper.TITLE_KEY, null));

    getDoc("<body><line/>Some text.<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text.", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text!!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text!?.!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>The number 5.5 is good<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("The number 5.5 is good", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text. and more<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text.", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text!?.! and more!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    // Empty title
    getDoc("<body><line/><line/>Some text.and more<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("", TitleHelper.extractTitle(doc));
    assertEquals(firstLineLocation,
        doc.firstAnnotationChange(0, doc.size(), TitleHelper.TITLE_KEY, null));
    assertEquals(firstLineLocation + 2,
        doc.lastAnnotationChange(0, doc.size(), TitleHelper.TITLE_KEY, null));

    String firstLine = "Some 5.5 text!?.! and more!";
    // text node breaks
    for (int i = 1; i < firstLine.length(); i++) {
      getDoc("<body><line/>" + firstLine + "<line/>Some more text</body>");
      DocHelper.ensureNodeBoundary(doc.locate(startText + i), doc, cxt.textNodeOrganiser());
      TitleHelper.maybeFindAndSetImplicitTitle(doc);
      assertEquals("Some 5.5 text!?.!", TitleHelper.extractTitle(doc));

      for (int j = i + 1; j < firstLine.length(); j++) {
        getDoc("<body><line/>" + firstLine + "<line/>Some more text</body>");
        DocHelper.ensureNodeBoundary(doc.locate(startText + i), doc, cxt.textNodeOrganiser());
        DocHelper.ensureNodeBoundary(doc.locate(startText + j), doc, cxt.textNodeOrganiser());
        TitleHelper.maybeFindAndSetImplicitTitle(doc);
        assertEquals("Some 5.5 text!?.!", TitleHelper.extractTitle(doc));
      }
    }

    // embedded empty and non-empty elements
    getDoc("<body><line/>Some t<x/>ext!?.! and more!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text<x/>!?.! and more!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some text!?.! a<x/>nd more!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    getDoc("<body><line/>Some te<x>xt!?.! a</x>nd more!<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text!?.!", TitleHelper.extractTitle(doc));

    assertEquals(firstLineLocation,
        doc.firstAnnotationChange(0, doc.size(), TitleHelper.TITLE_KEY, null));
    assertFalse(TitleHelper.hasExplicitTitle(doc));
  }

  public void testFindImplicitTitle() {
    getDoc("<body><line/>Some text.<line/>Some more text</body>");
    Range range = TitleHelper.findImplicitTitle(doc);
    assertTrue(range.getEnd() > range.getStart());

    getDoc("<body></body>");
    range = TitleHelper.findImplicitTitle(doc);
    assertNull(range);

    getDoc("");
    range = TitleHelper.findImplicitTitle(doc);
    assertNull(range);
  }

  public void testFindingImplicitTitleClearsFromInvalidDocuments() {
    getDoc("<body><line/>Some text.<line/>Some more text</body>");
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("Some text.", TitleHelper.extractTitle(doc));

    doc.emptyElement(doc.getDocumentElement());
    doc.appendXml(XmlStringBuilder.createText("Blah. Blah").wrap("x"));
    TitleHelper.maybeFindAndSetImplicitTitle(doc);
    assertEquals("", TitleHelper.extractTitle(doc));
    assertFalse(TitleHelper.hasExplicitTitle(doc));
    assertEquals(-1, doc.firstAnnotationChange(0, doc.size(), TitleHelper.TITLE_KEY, null));
  }

  /** Tests that the complexity is not, say, exponential */
  public void testFindImplicitTitleComplexity() {
    getDoc("<body><line/>a!xa!!!!!!!!!!!!!!!!!!!!!!!!!!!!xxxxxxxxxxxxxxxxxxxxxxx<line/>foo</body>");
    // Should not time out:
    Range r = TitleHelper.findImplicitTitle(doc);
    // Just a sanity check.
    assertTrue(r.getEnd() > r.getStart());
  }

  /**
   * Test that the head of the document built by emptyDocumentWithTitle matches
   * the initial head in ConversationConstants.BLIP_INITIAL_HEAD.
   **/
  public void testEmptyDocumentWithTitle() {
    IndexedDocument<Node, Element, Text> d = DocProviders.POJO.build(
        TitleHelper.emptyDocumentWithTitle(),
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    String message = Blips.INITIAL_CONTENT.toString() +
        " expected, found " + XmlStringBuilder.innerXml(d).toString();
    assertTrue(message, DocCompare.equivalent(DocCompare.STRUCTURE,
        Blips.INITIAL_CONTENT.toString(), d));
  }

  private MutableDocument<Node, Element, Text> getDoc(String innerXml) {
    cxt = ContextProviders.createTestPojoContext(innerXml, null, null, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    return doc = cxt.document();
  }
}
