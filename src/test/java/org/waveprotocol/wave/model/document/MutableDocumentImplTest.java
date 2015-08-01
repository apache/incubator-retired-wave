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

package org.waveprotocol.wave.model.document;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableAnnotationSet.CompareRangedValueByStartThenEnd;
import org.waveprotocol.wave.model.document.MutableAnnotationSet.RangedValue;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;

import java.util.Collections;
import java.util.LinkedList;

/**
 * Test cases for MutableDocument implementations
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class MutableDocumentImplTest extends TestCase {

  /**
   * A parser for documents.
   */
  public static final DocumentTestCases.DocumentParser<
      IndexedDocument<Node, Element, Text>> documentParser =
      new DocumentTestCases.DocumentParser<IndexedDocument<
          Node, Element, Text>>() {

    public IndexedDocument<Node, Element, Text>
        parseDocument(String innerXml) {
      return DocProviders.POJO.parse(innerXml);
    }

    @Override
    public IndexedDocument<Node, Element, Text> copyDocument(
        IndexedDocument<Node, Element, Text> other) {
      return DocProviders.POJO.build(other.asOperation(), DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    }

    public String asString(IndexedDocument<Node, Element, Text> document) {
      return document.toXmlString();
    }

  };

  /** Indexed document to feed into a MutableDocument and apply ops to */
  IndexedDocument<Node, Element, Text> indexed;
  /** MutableDocument that gets tested */
  MutableDocumentImpl<Node, Element, Text> doc;
  /** Latest document mutation to have been applied */
  DocOp latestOp;

  /**
   * Creates and returns a sequencer which applies incoming ops to the given document
   */
  OperationSequencer<Nindo> createSequencer(
      final IndexedDocument<Node, Element, Text> document) {
    return new OperationSequencer<Nindo>() {
      @Override
      public void begin() {
      }

      @Override
      public void end() {
      }

      @Override
      public void consume(Nindo op) {
        try {
          latestOp = document.consumeAndReturnInvertible(op);
        } catch (OperationException oe) {
          throw new OperationRuntimeException("sequencer consume failed.", oe);
        }
      }
    };
  }

  /**
   * Tests for the delete range method.
   * Tests correct deletion behaviour, and returned point range value
   */
  public void testDeleteRange() {
    // deletes nothing
    String str = "123<b>asdf</b>34<x/>5";
    init(str);
    for (int i = 0; i <= 14; i++) {
      assertCollapsedAt(i, doc.deleteRange(l(i), l(i)));
      assertResult(str);
    }
    // delete start or end tag does nothing
    assertRangeAt(3, 4, doc.deleteRange(l(3), l(4)));
    assertResult(str);
    assertRangeAt(8, 9, doc.deleteRange(l(8), l(9)));
    assertResult(str);

    // text only
    init("12345678");
    assertCollapsedAt(1, doc.deleteRange(l(1), l(3))); // middle
    assertResult("145678");
    assertCollapsedAt(0, doc.deleteRange(l(0), l(1))); // start
    assertResult("45678");
    assertCollapsedAt(3, doc.deleteRange(l(3), l(5))); // end
    assertResult("456");
    assertCollapsedAt(0, doc.deleteRange(l(0), l(3))); // all
    assertResult("");

    // within single element, but multiple nodes being deleted
    init("123<b>5</b><i>6</i>78");
    assertCollapsedAt(3, doc.deleteRange(l(3), l(9))); // middle
    assertResult("12378");

    // into element
    init("123<b>456</b>");
    assertRangeAt(2, 3, doc.deleteRange(l(2), l(5)));
    assertResult("12<b>56</b>");

    // into elements, depth 2
    init("123<b>4<i>56</i></b>");
    assertRangeAt(2, 4, doc.deleteRange(l(2), l(7)));
    assertResult("12<b><i>6</i></b>");

    // out of element
    init("<b>123</b>456");
    assertRangeAt(3, 4, doc.deleteRange(l(3), l(6)));
    assertResult("<b>12</b>56");

    // out of element, depth 2
    init("<b><i>12</i>3</b>456");
    assertRangeAt(3, 5, doc.deleteRange(l(3), l(8)));
    assertResult("<b><i>1</i></b>56");

    // across elements
    init("<b>123</b>456<i>789</i>");
    assertRangeAt(3, 5, doc.deleteRange(l(3), l(10)));
    assertResult("<b>12</b><i>89</i>");

    // across elements with extra element in the middle
    init("<b>123</b>4<x></x>6<i>789</i>");
    assertRangeAt(3, 5, doc.deleteRange(l(3), l(11)));
    assertResult("<b>12</b><i>89</i>");

    // across elements with elements as the bounding content
    init("<b>123<x></x></b>456<i><y></y>789</i>");
    assertRangeAt(4, 6, doc.deleteRange(l(4), l(13)));
    assertResult("<b>123</b><i>789</i>");
  }

  public void testDeleteRangeIndices() {
    String str = "123<b>asdf</b>34<x/>5";
    init(str);
    for (int i = 0; i <= 14; i++) {
      assertCollapsedAt(i, doc.deleteRange(i, i));
      assertResult(str);
    }

    // delete start or end tag does nothing
    assertRangeAt(3, 4, doc.deleteRange(3, 4));
    assertResult(str);
    assertRangeAt(8, 9, doc.deleteRange(8, 9));
    assertResult(str);

    // text only
    init("12345678");
    assertCollapsedAt(1, doc.deleteRange(1, 3)); // middle
    assertResult("145678");
    assertCollapsedAt(0, doc.deleteRange(0, 1)); // start
    assertResult("45678");
    assertCollapsedAt(3, doc.deleteRange(3, 5)); // end
    assertResult("456");
    assertCollapsedAt(0, doc.deleteRange(0, 3)); // all
    assertResult("");
  }

  /**
   * Test basic get attribute.
   */
  public void testGetAttributes() {
    init("<p t=\"0\" s=\"hi\">hello</p>");
    Element e = (Element) doc.getFirstChild(doc.getDocumentElement());
    assertEquals("0", doc.getAttribute(e, "t"));
    assertEquals("hi", doc.getAttribute(e, "s"));
  }

  /**
   * Test set attribute overrides and removes old attributes, as opposed to
   * update.
   */
  public void testSetAttributes() {
    init("<p t=\"0\" s=\"hi\">hello</p>");
    Element e = (Element) doc.getFirstChild(doc.getDocumentElement());
    doc.setElementAttributes(e, new AttributesImpl("just", "this"));
    assertEquals(null, doc.getAttribute(e, "t"));
    assertEquals(null, doc.getAttribute(e, "s"));
    assertEquals("this", doc.getAttribute(e, "just"));
  }

  protected Point<Node> l(int location) {
    return doc.locate(location);
  }

  /** Init document state */
  protected void init(String initialContent) {
    indexed = DocProviders.POJO.parse(initialContent);

    // Get a mutable doc view of our target and hook it up with the
    // "remote" document as the sink of outgoing ops.
    doc = new MutableDocumentImpl<Node, Element, Text>(
        createSequencer(indexed), indexed);
  }

  /**
   * Check the content of both indexed documents is as expected
   * @param expectedContent
   */
  protected void assertResult(String expectedContent) {
    String result = DocOpUtil.toXmlString(indexed.asOperation());

    // Check the paste happened correctly
    assertEquals(expectedContent, result);

  }

  /**
   * Check the content of both indexed documents is as expected
   * @param expectedContent
   */
  protected void assertOperationResult(String expectedContent) {
    String result = DocOpUtil.toXmlString(indexed.asOperation());

    // Check the ops have been applied correctly
    assertEquals(expectedContent, result);
  }

  protected void assertCollapsedAt(
      int location, Range actual) {
    assertEquals(new Range(location, location), actual);
  }

  protected void assertCollapsedAt(
      int location, PointRange<Node> actual) {
    Point<Node> expected = l(location);
    assertEquals(new PointRange<Node>(expected, expected), actual);
  }

  protected void assertRangeAt(
      int start, int end, Range actual) {
    assertEquals(start, actual.getStart());
    assertEquals(end, actual.getEnd());
  }

  protected void assertRangeAt(
      int start, int end, PointRange<Node> actual) {
    PointRange<Node> expected = new PointRange<Node>(l(start), l(end));
    assertEquals(expected, actual);
  }

  /** Test a simple set annotation */
  public void testSetAnnotation() {
    init("<p>abcdef</p>");
    doc.setAnnotation(3, 6, "style/color", "stix");
    assertOperationResult(
        "<p>ab<?a \"style/color\"=\"stix\"?>cde<?a \"style/color\"?>f</p>");
  }

  /** Test adding two non-overlapping settings of the same annotation */
  public void testTwoNonOverlappingAnnotations() {
    init("<p>abcdef</p>");
    doc.setAnnotation(2, 4, "style/color", "lola");
    doc.setAnnotation(5, 6, "style/color", "lola");
    assertOperationResult(
        "<p>a<?a \"style/color\"=\"lola\"?>bc<?a \"style/color\"?>d" +
        "<?a \"style/color\"=\"lola\"?>e<?a \"style/color\"?>f</p>");
  }

  /** Test adding two overlapping settings of the same annotation */
  public void testTwoOverlappingAnnotations() {
    init("<p>abcdef</p>");
    doc.setAnnotation(2, 4, "style/color", "charlie");
    doc.setAnnotation(3, 6, "style/color", "charlie");
    assertOperationResult(
        "<p>a<?a \"style/color\"=\"charlie\"?>bcde<?a \"style/color\"?>f</p>");
  }

  /** Test adding an annotation over the whole document */
  public void testSetAnnotationOverWholeDocument() {
    init("<p>abcdef</p>");
    doc.setAnnotation(0, doc.size(), "style/color", "flim");
    assertOperationResult(
        "<?a \"style/color\"=\"flim\"?><p>abcdef</p><?a \"style/color\"?>");
  }

  /** Test a simple set and unset of an annotation */
  public void testSetAndUnsetAnnotation() {
    init("<p>abcdef</p>");
    doc.setAnnotation(3, 6, "style/color", "maisy");
    doc.setAnnotation(0, doc.size(), "style/color", null);
    assertOperationResult("<p>abcdef</p>");
  }

  /** Test that trying to add a zero range annotation does nothing */
  public void testZeroRangeSetAnnotation() {
    init("<p>abcdef</p>");
    doc.setAnnotation(3, 3, "style/color", "blum");
    assertOperationResult("<p>abcdef</p>");
  }

  /**
   * Test that trying to add an annotation with a negative start throws
   * an IndexOutOfBoundsException.
   */
  public void testNegativeStartSetAnnotationThrowsException() throws Exception {
    init("<p>abcdef</p>");
    try {
      doc.setAnnotation(-1, 4, "style/color", "frub");
      // Doh - no exception thrown. Fail the test
      assert false;
    } catch (IndexOutOfBoundsException iae) {
      // expected
    }
  }

  /**
   * Test that trying to add an annotation with an end past the size throws
   * an IndexOutOfBoundsException.
   */
  public void testSetAnnotationPastDocEndThrowsException() throws Exception {
    init("<p>abcdef</p>");
    try {
      doc.setAnnotation(1, doc.size() + 1, "style/color", "frub");
      // Doh - no exception thrown. Fail the test
      fail();
    } catch (IndexOutOfBoundsException iae) {
      // expected
    }
  }

  /**
   * Test that trying to add an annotation with a negative range throws
   * an IndexOutOfBoundsException.
   */
  public void testNegativeRangeSetAnnotationThrowsException() throws Exception {
    init("<p>abcdef</p>");
    try {
      doc.setAnnotation(4, 1, "style/color", "slarken");
      // Doh - no exception thrown. Fail the test
      fail();
    } catch (IndexOutOfBoundsException iae) {
      // expected
    }
  }

  /** Test a simple reset annotation */
  public void testResetAnnotation() {
    init("<p>abcdef</p>");
    doc.resetAnnotation(3, 6, "style/color", "pocoyo");
    assertOperationResult(
        "<p>ab<?a \"style/color\"=\"pocoyo\"?>cde<?a \"style/color\"?>f</p>");
  }

  /** Test a simple set and reset annotation */
  public void testSetAndResetAnnotation() {
    init("<p>abcdef</p>");
    doc.setAnnotation(0, doc.size(), "style/color", "pato");
    doc.resetAnnotation(3, 6, "style/color", "pato");
    assertOperationResult(
        "<p>ab<?a \"style/color\"=\"pato\"?>cde<?a \"style/color\"?>f</p>");
  }

  /**
   * Test that using a zero range reset annotation clears the annotation over
   * the whole document.
   */
  public void testZeroRangeResetAnnotationClearsDocument() {
    init("<p>abcdef</p>");
    doc.setAnnotation(1, 4, "style/color", "spot");
    doc.resetAnnotation(0, 0, "style/color", "spot");
    assertOperationResult("<p>abcdef</p>");

    init("<p>abcdef</p>");
    doc.setAnnotation(1, 4, "style/color", "spot");
    doc.resetAnnotation(2, 2, "style/color", "spot");
    assertOperationResult("<p>abcdef</p>");

    init("<p>abcdef</p>");
    doc.setAnnotation(1, 4, "style/color", "spot");
    doc.resetAnnotation(doc.size(), doc.size(), "style/color", "spot");
    assertOperationResult("<p>abcdef</p>");
  }

  /**
   * Test that trying to reset an annotation with a negative start throws
   * an IndexOutOfBoundsException.
   */
  public void testNegativeStartResetAnnotationThrowsException() throws Exception {
    init("<p>abcdef</p>");
    try {
      doc.resetAnnotation(-1, 4, "style/color", "frub");
      // Doh - no exception thrown. Fail the test
      assert false;
    } catch (IndexOutOfBoundsException iae) {
      // expected
    }
  }

  /**
   * Test that trying to reset an annotation with an end bigger than the document
   * an IndexOutOfBoundsException.
   */
  public void testResetAnnotationPastDocEndThrowsException() throws Exception {
    init("<p>abcdef</p>");
    try {
      doc.resetAnnotation(1, doc.size() + 1, "style/color", "frub");
      // Doh - no exception thrown. Fail the test
      assert false;
    } catch (IndexOutOfBoundsException iae) {
      // expected
    }
  }

  public void testMoveNodes() throws Exception {
    // simple move
    init("<root><before/><from/></root>");
    Element root = doc.getDocumentElement().getFirstChild().asElement();
    Node from = root.getLastChild();
    doc.moveSiblings(Point.start(doc, root), from, null);
    assertOperationResult("<root><from/><before/></root>");

    // move with attributes and children
    init("<root><before/> stuff <from> child <sub/></from> more <attr x=\"x\" y=\"z\"/> end</root>");
    root = doc.getDocumentElement().getFirstChild().asElement();
    Node stuff = root.getFirstChild().getNextSibling();
    from = stuff.getNextSibling();
    doc.moveSiblings(Point.before(doc, stuff), from, root.getLastChild());
    assertOperationResult(
        "<root><before/><from> child <sub/></from> more <attr x=\"x\" y=\"z\"/> stuff  end</root>");

    // move with annotations
    //    0     1  234  567   8   9    10 11     12
    init("<root><b>bo<i>ld</i></b><after/></root>");
    doc.setAnnotation(1, 9, "b", "B"); // around the bs
    doc.setAnnotation(4, 8, "i", "I"); // around the is
    doc.setAnnotation(0, 3, "s", "S"); // overlaps the start
    doc.setAnnotation(7, 12, "e", "E"); // overlaps the end, AND covers the new range

    /*
    <root><b>bo<i>ld</i></b><after/></root>
           B BB B BB  B   B
                I II  I
      S    S S
                      E   E    E E    E

    <root><after/><b>bo<i>ld</i></b></root>
                   B BB B BB  B   B
                        I II  I
      S            S S
             E E              E   E   E

    <?a "s"="S"?><root><?a "e"="E" "s"?><after/><?a "b"="B" "e" "s"="S"?><b>b<?a "s"?>o<?a "i"="I"?><i>ld<?a "e"="E"?></i><?a "i"?></b><?a "b"?></root><?a "e"?>
    */

    root = doc.getDocumentElement().getFirstChild().asElement();
    doc.moveSiblings(Point.end((Node) root), root.getFirstChild(), root.getLastChild());
    assertOperationResult("<?a \"s\"=\"S\"?><root><?a \"e\"=\"E\" \"s\"?><after/>"
        + "<?a \"b\"=\"B\" \"e\" \"s\"=\"S\"?><b>b<?a \"s\"?>o<?a \"i\"=\"I\"?><i>ld"
        + "<?a \"e\"=\"E\"?></i><?a \"i\"?></b><?a \"b\"?></root><?a \"e\"?>");
  }

  /**
   * Test we can atomically reset multiple annotations within a range.
   */
  @SuppressWarnings("deprecation") // resetAnnotationsInRange (method under test) is deprecated
  public void xtestSimpleResetAnnotations() {
    // TODO(user): Fix this test.

    init("<p>abcdef</p>");
    LinkedList<RangedValue<String>> annos = new LinkedList<RangedValue<String>>();
    annos.add(new RangedValue<String>(2, 4, "cyril"));
    annos.add(new RangedValue<String>(5, 6, "tallulah"));
    doc.resetAnnotationsInRange(0, doc.size(), "style/color", annos);
    assertOperationResult(
        "<p>a<?a \"style/color\"=\"cyril\"?>bc<?a \"style/color\"?>d" +
        "<?a \"style/color\"=\"tallulah\"?>e<?a \"style/color\"?>f</p>");
    // Just fail for now, so that we remember to come back to fix up this test.
    fail();

//    DocumentOperationChecker.Recorder recorder = new DocumentOperationChecker.Recorder();
//    recorder.begin();
//    recorder.skip(3);
//    recorder.startAnnotation("style/color", "cyril");
//    recorder.skip(2);
//    recorder.endAnnotation("style/color");
//    recorder.skip(1);
//    recorder.startAnnotation("style/color", "tallulah");
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.finish();
//    DocumentOperationChecker checker = recorder.finishRecording();
//    latestOp.apply(checker);
//    checker.checkCompleted();
  }

  /**
   * Test we can atomically extend an annotation to the right
   */
  @SuppressWarnings("deprecation") // resetAnnotationsInRange (method under test) is deprecated
  public void xtestExtendAnnotationsRight() {
    // TODO(user): Fix this test.

    // Test extending to the right
    init("<p>23456789</p>");
    LinkedList<RangedValue<String>> annos = new LinkedList<RangedValue<String>>();
    doc.setAnnotation(1, 2, "style/color", "marv");
    doc.setAnnotation(3, 4, "style/color", "eddie");
    annos.add(new RangedValue<String>(1, 2, "marv"));
    annos.add(new RangedValue<String>(3, 6, "eddie"));
    doc.resetAnnotationsInRange(1, 8, "style/color", annos);
    assertOperationResult(
        "<p>" +
        "<?a \"style/color\"=\"marv\"?>2<?a \"style/color\"?>" +
        "3" +
        "<?a \"style/color\"=\"eddie\"?>456<?a \"style/color\"?>" +
        "789" +
        "</p>");
    // Just fail for now, so that we remember to come back to fix up this test.
    fail();
//    DocumentOperationChecker.Recorder recorder = new DocumentOperationChecker.Recorder();
//    recorder.begin();
//    recorder.skip(5);
//    recorder.startAnnotation("style/color", "eddie");
//    recorder.skip(2);
//    recorder.endAnnotation("style/color");
//    recorder.finish();
//    DocumentOperationChecker checker = recorder.finishRecording();
//    latestOp.apply(checker);
//    checker.checkCompleted();
  }

  /**
   * Test we can atomically extend an annotation to the left
   */
  @SuppressWarnings("deprecation") // resetAnnotationsInRange (method under test) is deprecated
  public void xtestExtendAnnotationsLeft() {
    // TODO(user): Fix this test.

    init("<p>23456789</p>");
    LinkedList<RangedValue<String>> annos = new LinkedList<RangedValue<String>>();
    doc.setAnnotation(1, 2, "style/color", "lotta");
    doc.setAnnotation(3, 4, "style/color", "sizzles");
    annos.add(new RangedValue<String>(1, 2, "lotta"));
    annos.add(new RangedValue<String>(2, 4, "sizzles"));
    doc.resetAnnotationsInRange(0, 7, "style/color", annos);
    assertOperationResult(
        "<p>" +
        "<?a \"style/color\"=\"lotta\"?>2" +
        "<?a \"style/color\"=\"sizzles\"?>34<?a \"style/color\"?>" +
        "56789" +
        "</p>");
    // Just fail for now, so that we remember to come back to fix up this test.
    fail();
//    DocumentOperationChecker.Recorder recorder = new DocumentOperationChecker.Recorder();
//    recorder.begin();
//    recorder.skip(3);
//    recorder.startAnnotation("style/color", "sizzles");
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.finish();
//    DocumentOperationChecker checker = recorder.finishRecording();
//    latestOp.apply(checker);
//    checker.checkCompleted();
  }

  /**
   * Test we can atomically reset multiple annotations within a range that also clear
   * other existing annotations.
   */
  @SuppressWarnings("deprecation") // resetAnnotationsInRange (method under test) is deprecated
  public void xtestResetAnnotations() {
    // TODO(user): Fix this test.

    init("<p>23456789</p>");
    LinkedList<RangedValue<String>> annos = new LinkedList<RangedValue<String>>();
    doc.setAnnotation(0, 1, "style/color", "marv");
    doc.setAnnotation(3, 5, "style/color", "eddie");
    annos.add(new RangedValue<String>(2, 3, "charley"));
    annos.add(new RangedValue<String>(4, 6, "morten"));
    doc.resetAnnotationsInRange(0, 6, "style/color", annos);
    assertOperationResult(
        "<p>" +
        "2" +
        "<?a \"style/color\"=\"charley\"?>3<?a \"style/color\"?>" +
        "4" +
        "<?a \"style/color\"=\"morten\"?>56<?a \"style/color\"?>" +
        "789" +
        "</p></blip>");
    // Just fail for now, so that we remember to come back to fix up this test.
    fail();
//    DocumentOperationChecker.Recorder recorder = new DocumentOperationChecker.Recorder();
//    recorder.begin();
//    recorder.skip(1);
//    recorder.startAnnotation("style/color", null);
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.skip(1);
//    recorder.startAnnotation("style/color", "charley");
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.startAnnotation("style/color", null);
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    // TODO(user): optimise the below sequence to combine the two sets
//    // of the same value
//    recorder.startAnnotation("style/color", "morten");
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.startAnnotation("style/color", "morten");
//    recorder.skip(1);
//    recorder.endAnnotation("style/color");
//    recorder.finish();
//    DocumentOperationChecker checker = recorder.finishRecording();
//    latestOp.apply(checker);
//    checker.checkCompleted();
  }

  // TODO(danilatos): test all the other content manipulation methods.

  /**
   * Tests that createChildElement does as it says.
   */
  public void testCreateChildElement() {
    init("<p>first child</p>");
    Element root = doc.getDocumentElement();
    doc.createChildElement(root, "child", Collections.<String, String> emptyMap());
    assertOperationResult("<p>first child</p><child/>");
  }

  public void testCompareRangedValueByStartThenEndButIgnoreValue() {
    CompareRangedValueByStartThenEnd<String> comp =
      new CompareRangedValueByStartThenEnd<String>();
    {
      // first wholly to the left of second
      RangedValue<String> first = new RangedValue<String>(0, 3, "a");
      RangedValue<String> second = new RangedValue<String>(5, 6, null);
      assert(comp.compare(first, second) < 0);
    }
    {
      // End of first touching start of second
      RangedValue<String> first = new RangedValue<String>(0, 3, "a");
      RangedValue<String> second = new RangedValue<String>(3, 6, null);
      assert(comp.compare(first, second) < 0);
    }
    {
      // End of first within second
      RangedValue<String> first = new RangedValue<String>(0, 4, "a");
      RangedValue<String> second = new RangedValue<String>(3, 6, null);
      assert(comp.compare(first, second) < 0);
    }
    {
      // First and second start at the same place, first ends first
      RangedValue<String> first = new RangedValue<String>(3, 4, "a");
      RangedValue<String> second = new RangedValue<String>(3, 6, null);
      assert(comp.compare(first, second) < 0);
    }
    {
      // First equal to  second
      RangedValue<String> first = new RangedValue<String>(3, 4, "a");
      RangedValue<String> second = new RangedValue<String>(3, 4, null);
      assert(comp.compare(first, second) == 0);
    }
    {
      // First starts within second, ends are equal
      RangedValue<String> first = new RangedValue<String>(3, 4, "a");
      RangedValue<String> second = new RangedValue<String>(2, 4, null);
      assert(comp.compare(first, second) > 0);
    }
    {
      // First starts within second, first ends after second
      RangedValue<String> first = new RangedValue<String>(3, 5, "a");
      RangedValue<String> second = new RangedValue<String>(2, 4, null);
      assert(comp.compare(first, second) > 0);
    }
    {
      // First starts where second ends
      RangedValue<String> first = new RangedValue<String>(4, 5, "a");
      RangedValue<String> second = new RangedValue<String>(2, 4, null);
      assert(comp.compare(first, second) > 0);
    }
    {
      // First wholly to the right of second
      RangedValue<String> first = new RangedValue<String>(5, 6, "a");
      RangedValue<String> second = new RangedValue<String>(2, 4, null);
      assert(comp.compare(first, second) > 0);
    }
  }
}
