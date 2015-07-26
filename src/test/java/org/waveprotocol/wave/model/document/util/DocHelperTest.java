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
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.DocHelper.NodeAction;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for the DocHelper class
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DocHelperTest extends TestCase {

  /**
   * Test the getText method
   */
  public void testGetText() {
    checkGetText("<x>abc</x>", 0, 0, "");
    checkGetText("<x>abc</x>", 1, 1, "");
    checkGetText("<x>abc</x>", 4, 4, "");
    checkGetText("<x>abc</x>", 1, 4, "abc");
    checkGetText("<x>abc</x>", 3, 4, "c");
    checkGetText("<x><y>abc</y></x>", 1, 6, "abc");
    checkGetText("<x><y>abc</y><z>def</z></x>", 1, 6, "abc");
    checkGetText("<x>a<b>b</b>c</x>", 1, 6, "abc");
    checkGetText("<x>abc<b></b></x>", 1, 4, "abc");
    checkGetText("<x>abc<b></b></x>", 1, 5, "abc");
    checkGetText("<x>abc<b></b></x>", 1, 6, "abc");
    checkGetText("<x>abc<b></b><c></c></x>", 1, 6, "abc");
    checkGetText("<x><a></a>abc<b></b></x>", 1, 6, "abc");
    checkGetText("<x>abc<b>x</b>def</x>", 1, 10, "abcxdef");
    checkGetText("<x>abc<b>x</b>def</x>", 2, 10, "bcxdef");
    checkGetText("<x>abc<b>x</b>def</x>", 1, 9, "abcxde");
    checkGetText("<x>abc<b>x</b>def</x>", 2, 9, "bcxde");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 1, 14, "abcdefghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 2, 14, "abcdefghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 3, 14, "bcdefghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 1, 13, "abcdefghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 1, 12, "abcdefgh");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 3, 12, "bcdefgh");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 7, 13, "efghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 2, 7, "abcd");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 2, 6, "abc");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 9, 14, "ghi");
    checkGetText("<x><a>abc</a>def<b>ghi</b></x>", 9, 13, "ghi");

    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("abc<x>def</x>");
    assertEquals("abcdef", DocHelper.getText(doc, doc, doc.getDocumentElement()));
  }

  private void checkGetText(String docXml, int start, int end, String expectedText) {
    IndexedDocument<?, ?, ?> doc = DocProviders.POJO.parse(docXml);
    assertEquals(expectedText, DocHelper.getText(doc, start, end));
  }
  /**
   * Test getLocation for DocHelper.
   * TODO(user): Write a more thorough test.
   */
  public void testGetLocation() throws OperationException {
    checkGetLocation("", 0, 0);
    checkGetLocation("hi", 0, 0);
    checkGetLocation("hi", 2, 2);
    checkGetLocation("hithere", 2, 2);
    checkGetLocation("hi<x/>", 2, 2);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 0, 0);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 1, 1);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 2, 2);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 3, 3);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 4, 4);
    checkGetLocation("<x><a><b>a</b>bc</a></x>", 9, 9);
    checkGetLocation("<x><a><b>a</b>bc</a></x><x/><a/>", 9, 9);
    checkGetLocation("<x><a><b>a</b>bc</a></x>hi", 9, 9);
  }

  private void checkGetLocation(String docXml, int nodeLocation, int expectedLocation)
      throws OperationException {

    // Persistent state for checkGetLocation.
    RawDocument<Node, Element, Text> fullDoc =
      RawDocumentImpl.PROVIDER.create("doc", Attributes.EMPTY_MAP);

    final Box<Point<Node>> pointBox = Box.create(null);
    PersistentContent<Node, Element, Text> persistentDoc =
      new PersistentContent<Node, Element, Text>(fullDoc, Element.ELEMENT_MANAGER) {
      @Override
      public void onBeforeFilter(Point<Node> point) {
        // catch the filter callback invocations.
        pointBox.boxed = point;
      }
    };

    IndexedDocument<Node, Element, Text> indexedDoc =
        new IndexedDocumentImpl<Node, Element, Text, Void>(persistentDoc, null,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    indexedDoc.consume(DocProviders.POJO.parse(docXml).asOperation());

    Point<Node> n = indexedDoc.locate(nodeLocation);

    if (!n.isInTextNode()) {
      Element newNode =
          persistentDoc.transparentCreate("abc", Collections.<String, String> emptyMap(),
              (Element) n.getContainer(), n.getNodeAfter());
      Element newNode2 =
          persistentDoc.transparentCreate("def", Collections.<String, String> emptyMap(), newNode,
              null);
      Element newNode3 =
          persistentDoc.transparentCreate("ghi", Collections.<String, String> emptyMap(), newNode,
              newNode2);

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode, null)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode, null));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode.getParentElement(), newNode)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode.getParentElement(), newNode));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode.getParentElement(), newNode.getNextSibling())));
      assertEquals(pointBox.boxed,
          Point.<Node> inElement(newNode.getParentElement(), newNode.getNextSibling()));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode2, null)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode2, null));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode2.getParentElement(), newNode2)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode2.getParentElement(), newNode2));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode3, null)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode3, null));

      assertEquals(expectedLocation, DocHelper.getFilteredLocation(indexedDoc, persistentDoc,
          Point.<Node> inElement(newNode3.getParentElement(), newNode3)));
      assertEquals(pointBox.boxed, Point.<Node> inElement(newNode3.getParentElement(), newNode3));
    }
  }

  /**
   * Test normalize point between two text nodes, i.e. "hello""world"
   */
  public void testNormalizePointBetweenTwoTextNodes() {
    MutableDocument<Node, Element, Text> doc = initializeMutableDoc();
    Element p = doc.asElement(doc.getFirstChild(doc.getDocumentElement()));
    assert p != null;

    doc.insertText(Point.<Node> end(p), "hello");
    insertTextInNewTextNodeHelper(doc, Point.<Node> end(p), "world");

    Text world = doc.asText(doc.getLastChild(p));
    Text hello = doc.asText(doc.getFirstChild(p));

    assertEquals(Point.inText(hello, hello.getLength()), DocHelper.normalizePoint(Point
        .<Node> inText(world, 0), doc));
    assertEquals(Point.<Node> inText(world, 1), DocHelper.normalizePoint(Point.<Node> inText(world,
        1), doc));
    assertEquals(Point.<Node> inText(world, 2), DocHelper.normalizePoint(Point.<Node> inText(world,
        2), doc));
    assertEquals(Point.<Node> inText(hello, 5), DocHelper.normalizePoint(Point.<Node> inText(hello,
        5), doc));
    assertEquals(Point.<Node> inText(hello, 4), DocHelper.normalizePoint(Point.<Node> inText(hello,
        4), doc));
  }

  /**
   * Test normalize points between an element and a text node <a>stuff</a>"hi"
   */
  public void testNormalizePointElementFollowedByTextNode() {
    MutableDocument<Node, Element, Text> doc = initializeMutableDoc();
    Element p = doc.asElement(doc.getFirstChild(doc.getDocumentElement()));
    assert p != null;

    Element aElement =
        doc.createElement(Point.start(doc, p), "a", Collections.<String, String> emptyMap());
    doc.insertText(Point.start(doc, aElement), "stuff");
    doc.insertText(Point.<Node> end(p), "hi");
    Text hi = doc.asText(doc.getLastChild(p));
    Text stuff = doc.asText(aElement.getFirstChild());

    assertEquals(Point.inText(hi, 0), DocHelper.normalizePoint(Point.<Node> inText(hi, 0), doc));
    assertEquals(Point.inText(hi, 0), DocHelper.normalizePoint(Point.<Node>inElement(p, hi), doc));
    // In the future, we might want to move the caret out from inline elements.
    assertEquals(Point.inText(stuff, stuff.getLength()), DocHelper.normalizePoint(Point
        .<Node> inText(stuff, stuff.getLength()), doc));
  }

  /**
   * Test normalize points between text node and element "hi"<a>stuff</a>
   */
  public void testNormalizePointTextNodeFollowedByElement() {
    MutableDocument<Node, Element, Text> doc = initializeMutableDoc();
    Element p = doc.asElement(doc.getFirstChild(doc.getDocumentElement()));
    assert p != null;

    doc.insertText(Point.<Node> end(p), "hi");
    Element aElement =
      doc.createElement(Point.<Node>end(p), "a", Collections.<String, String> emptyMap());
    doc.insertText(Point.start(doc, aElement), "stuff");

    Text hi = doc.asText(doc.getFirstChild(p));
    Text stuff = doc.asText(aElement.getFirstChild());

    assertEquals(Point.inText(hi, 2), DocHelper.normalizePoint(Point.<Node> inText(hi, 2), doc));
    assertEquals(Point.inText(hi, 2), DocHelper.normalizePoint(Point.<Node> inElement(p, aElement),
        doc));
    // In the future, we might want to move the caret out from inline elements.
    assertEquals(Point.inText(stuff, 0), DocHelper.normalizePoint(Point.<Node> inText(stuff, 0),
        doc));
    assertEquals(Point.inText(stuff, stuff.getLength()), DocHelper.normalizePoint(Point
        .<Node> inText(stuff, stuff.getLength()), doc));
  }

  private static MutableDocument<Node, Element, Text> initializeMutableDoc() {
    return DocProviders.MOJO.parse("<p></p>");
  }

  /**
   * Try to insert text into a new text node, by first inserting an element, and
   * then removing it after the new text is inserted.
   *
   * NOTE(user): We assume the document doesn't try to join the text nodes
   * after the dummy element is removed.
   *
   * @param <N>
   * @param at
   * @param text
   */
  // TODO(user): Move somewhere common for TextLocatorTest
  static <N, E extends N, T extends N> void insertTextInNewTextNodeHelper(
      MutableDocument<N, E, T> doc, Point<N> at, String text) {
    E e = doc.createElement(at, "a", Collections.<String, String> emptyMap());
    doc.insertText(Point.after(doc, e), text);
    doc.deleteNode(e);
  }

  /**
   * Some behavioural tests for aligning left over document views.
   */
  public void testLeftAlign() {
    TestDocumentContext<Node, Element, Text> cxt = createAlignTestCxt();
    LocalDocument<Node, Element, Text> doc = cxt.annotatableContent();
    Point<Node> at, other;

    // check when the point is already in the view
    at = Point.start(doc, doc.getDocumentElement().getFirstChild().asElement());
    other = DocHelper.leftAlign(at, doc, cxt.hardView());
    assertEquals(at, other); // no change

    // check when the point is to the left of shallow transparent elements
    at = Point.start(doc, doc.getDocumentElement().getFirstChild().getNextSibling().asElement());
    other = DocHelper.leftAlign(at, doc, cxt.hardView());
    assertEquals(at, other); // no change

    // check when the point is to the right of shallow transparent elements
    Element p2 = doc.getDocumentElement().getFirstChild().getNextSibling().asElement();
    at = Point.end((Node) p2);
    other = DocHelper.leftAlign(at, doc, cxt.hardView());
    assertEquals(Point.end(p2.getFirstChild()), other);
    // nb: normalization to text node occurs externally

    // check when the point is to the left of deep transparent elements
    at = Point.start(doc, doc.getDocumentElement().getLastChild().asElement());
    other = DocHelper.leftAlign(at, doc, cxt.hardView());
    assertEquals(at, other); // no change (nb: normalization to text node occurs externally)

    // check when the point is to the right of deep transparent elements
    at = Point.end(doc.getDocumentElement().getLastChild());
    other = DocHelper.leftAlign(at, doc, cxt.hardView());
    assertEquals(Point.before(doc, doc.getDocumentElement().getLastChild().getLastChild()), other);
  }

  // util for align tests above
  private TestDocumentContext<Node, Element, Text> createAlignTestCxt() {
    // creates the following, where '<t>' are soft nodes
    // <div>
    //   <p>A</p>
    //   <p> <t>Z</t> </p>
    //   <p> <t><t/></t> </p>
    // </div>
    String initialContent = "<p>A</p><p>Z</p><p></p>";
    TestDocumentContext<Node, Element, Text> cxt = ContextProviders.createTestPojoContext(
        DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
          DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    LocalDocument<Node, Element, Text> doc = cxt.annotatableContent();
    Text zText = doc.getDocumentElement().getFirstChild().getNextSibling().getFirstChild().asText();

    // First transparent part, moving the Z inside
    Element trans = doc.transparentCreate("S", Attributes.EMPTY_MAP,
        zText.getParentElement(), zText);
    cxt.annotatableContent().transparentMove(trans, zText, null, null);

    // Second transparent part, deep transparent (contains another transparent element)
    Element lastP = doc.getDocumentElement().getLastChild().asElement();
    trans = cxt.annotatableContent().transparentCreate("T", Attributes.EMPTY_MAP, lastP, null);
    cxt.annotatableContent().transparentCreate("U", Attributes.EMPTY_MAP, trans, null);
    return cxt;
  }

  /***/
  public void testGetNextSiblingElementBackwards() {
    ReadableWDocument<Node, Element, Text> doc = DocProviders.POJO.parse(
        "<div>abc<p>def<q>hij</q></p><p>def<q>hij</q></p></div>");
    Element previous = null;
    Node node = doc.getFirstChild(doc.getDocumentElement());
    while (node != null) {
      Element p = doc.asElement(node);
      if (p != null) {
        if (previous == null) {
          // p is the very first element among nodes of doc.
          assertNull(DocHelper.getPreviousSiblingElement(doc, p));
        } else {
          // p follows a previously seen previous element.
          assertSame(previous, DocHelper.getPreviousSiblingElement(doc, p));
        }
        previous = p;
      }
      node = doc.getNextSibling(node);
    }
    // TODO(user): The following fails. Uncomment and fix if it should work.
    // assertNull(DocHelper.getPreviousSiblingElement(doc, null));
  }

  public void testGetNextSiblingElementBackwardsForInvalid() {
    ReadableWDocument<Node, Element, Text> doc = DocProviders.POJO.parse(
        "<div>abc<p>def<q>hij</q></p><p>def<q>hij</q></p></div>");
    try {
      DocHelper.getPreviousSiblingElement(doc, null);
      fail("Should failed when fetching previous sibling of a null");
    } catch (Exception e) {
      // Success!
    }

    try {
      DocHelper.getPreviousSiblingElement(null, doc.getFirstChild(doc.getDocumentElement()));
      fail("Should failed when fetching previous sibling in a null document");
    } catch (Exception e) {
      // Success!
    }
  }

  /**
   * Tests the getItemSize method
   */
  public void testGetItemSize() {
    ReadableWDocument<Node, Element, Text> doc = DocProviders.POJO.parse(
        "<top>abc<p>def<q>hij</q></p><p>def<q>hij</q></p></top>");

    Element top = (Element) doc.getDocumentElement().getFirstChild();
    assertEquals(25, DocHelper.getItemSize(doc, top));

    Node text = doc.getFirstChild(top);
    assertEquals(3, DocHelper.getItemSize(doc, text));

    Node pWithSibling = doc.getNextSibling(text);
    assertEquals(10, DocHelper.getItemSize(doc, pWithSibling));

    Node pWithoutSibling = doc.getNextSibling(pWithSibling);
    assertEquals(10, DocHelper.getItemSize(doc, pWithoutSibling));
  }

  public void testGetElementWithTagName() {
    ReadableDocument<Node, Element, Text> doc;
    {
      // Nothing to find in empty doc.
      doc = getDoc("");
      assertNull(DocHelper.getElementWithTagName(doc, ""));
    }
    {
      // Container is excluded from search.
      doc = getDoc("<x><y></y></x>");
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      assertNull(DocHelper.getElementWithTagName(doc, "x", container));

      // Finds direct child match.
      Element expectedY = doc.getFirstChild(container).asElement();
      assertSame(expectedY, DocHelper.getElementWithTagName(doc, "y", container));

      // Finds deeper child match.
      assertSame(expectedY, DocHelper.getElementWithTagName(doc, "y"));
    }
    {
      doc = getDoc("<x><y></y><z></z></x>");
      // Finds a non-first-sibling match.
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      Element expectedZ = doc.getLastChild(container).asElement();
      assertSame(expectedZ, DocHelper.getElementWithTagName(doc, "z"));
    }
    {
      doc = getDoc("<x><y><z></z></y></x>");
      // Doesn't search above subtree.
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      Element y = doc.getFirstChild(container).asElement();
      assertNull(DocHelper.getElementWithTagName(doc, "x", y));
    }
    {
      doc = getDoc("<x><y></y></x><z></z>");
      // Doesn't search right of subtree.
      Element x = doc.getFirstChild(doc.getDocumentElement()).asElement();
      assertNull(DocHelper.getElementWithTagName(doc, "z", x));
    }
    {
      doc = getDoc("<y><x></x></y><x></x>");
      // Finds leftmost match.
      Element expectedX = doc.getFirstChild(doc.getFirstChild(
          doc.getDocumentElement())).asElement();
      assertSame(expectedX, DocHelper.getElementWithTagName(doc, "x"));
    }
  }

  public void testGetLastElementWithTagName() {
    ReadableDocument<Node, Element, Text> doc;
    {
      // Nothing to find in empty doc.
      doc = getDoc("");
      assertNull(DocHelper.getLastElementWithTagName(doc, ""));
    }
    {
      // Container is excluded from search.
      doc = getDoc("<x><y></y></x>");
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      assertNull(DocHelper.getLastElementWithTagName(doc, "x", container));

      // Finds direct child match.
      Element expectedY = doc.getFirstChild(container).asElement();
      assertSame(expectedY, DocHelper.getLastElementWithTagName(doc, "y", container));

      // Finds deeper child match.
      assertSame(expectedY, DocHelper.getLastElementWithTagName(doc, "y"));
    }
    {
      doc = getDoc("<x><y></y><z></z></x>");
      // Finds a non-last-sibling match.
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      Element expectedY = doc.getFirstChild(container).asElement();
      assertSame(expectedY, DocHelper.getLastElementWithTagName(doc, "y"));
    }
    {
      doc = getDoc("<x><y><z></z></y></x>");
      // Doesn't search above subtree.
      Element container = doc.getFirstChild(doc.getDocumentElement()).asElement();
      Element y = doc.getFirstChild(container).asElement();
      assertNull(DocHelper.getLastElementWithTagName(doc, "x", y));
    }
    {
      doc = getDoc("<z></z><x><y></y></x>");
      // Doesn't search left of subtree.
      Element x = doc.getLastChild(doc.getDocumentElement()).asElement();
      assertNull(DocHelper.getLastElementWithTagName(doc, "z", x));
    }
    {
      doc = getDoc("<x></x><y><x></x></y>");
      // Finds rightmost match.
      Element expectedX = doc.getFirstChild(doc.getLastChild(
          doc.getDocumentElement())).asElement();
      assertSame(expectedX, DocHelper.getLastElementWithTagName(doc, "x"));
    }
  }

  /**
   * Tests the testGetElementWithTagName which indirectly tests
   * {@link DocHelper#getElementWithTagName(ReadableDocument, String)} and
   * {@link DocHelper#getText(ReadableDocument, LocationMapper, Object)} .
   */
  public void testGetElementTextWithTagName() {
    checkGetElementTextWithTagName("<x>abc</x>", "x", "abc");
    checkGetElementTextWithTagName("<x>abc</x>", "z", null);
    checkGetElementTextWithTagName("<x><y>abc</y></x>",  "x", "abc");
    checkGetElementTextWithTagName("<x><y>abc</y></x>",  "y", "abc");
    checkGetElementTextWithTagName("<x><y>abc</y></x>",  "z", null);
    checkGetElementTextWithTagName("<x>a<b>b</b>c</x>", "x", "abc");
    checkGetElementTextWithTagName("<x>a<b>b</b>c</x>", "a", null);
    checkGetElementTextWithTagName("<x>a<b>b</b>c</x>", "b", "b");
    checkGetElementTextWithTagName("<x>abc<b></b></x>", "x", "abc");
    checkGetElementTextWithTagName("<x>abc<b></b><c></c></x>", "x", "abc");
    checkGetElementTextWithTagName("<x>abc<b></b><c></c></x>", "c", "");
    checkGetElementTextWithTagName("<x><a>abc</a>def<b>ghi</b></x>", "x", "abcdefghi");
    checkGetElementTextWithTagName("<x><a>abc</a>def<b>ghi</b></x>", "a", "abc");
    checkGetElementTextWithTagName("<x><a>abc</a>def<b>ghi</b></x>", "b", "ghi");
    checkGetElementTextWithTagName("<x><a>abc</a>def<b>ghi</b></x>", "c", null);
  }

  private void checkGetElementTextWithTagName(String docXml, String tagName,
      String expectedElementText) {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse(docXml);
    assertEquals(expectedElementText, DocHelper.getTextForElement(doc, doc, tagName));
  }

  public void testEnsureNodeBoundary() {
    checkEnsureNodeBoundary("ab", 1, 1, false);
    checkEnsureNodeBoundary("ab", 2, 2, true);
    checkEnsureNodeBoundary("ab", 3, -1, false);

    checkEnsureNodeBoundary("a^b", 3, 3, false);

    checkEnsureNodeBoundary("a<x>bc</x><y>de</y>", 2, 2, false);
    checkEnsureNodeBoundary("a<x>bc</x><y>de</y>", 5, 6, false);
    checkEnsureNodeBoundary("a<x><z>bc</z></x><y>de</y>", 6, 8, false);
    checkEnsureNodeBoundary("a<x><z>bc</z></x>", 6, -1, false);
    checkEnsureNodeBoundary("a<x><z></z></x>", 4, -1, false);
    checkEnsureNodeBoundary("a<x><y></y><z></z></x>", 4, 5, false);
    checkEnsureNodeBoundary("a<x><y></y><z></z></x>", 5, 5, false);
  }

  private void checkEnsureNodeBoundary(String initialContent,
      int boundaryLocation, int nextNodeLocation, boolean splitNecessary) {

    // Test the two methods at the same time
    for (boolean returnNextNode : new boolean[] {true, false}) {
      // There can be up to 3 points corresponding to a given location, automatically
      // test for all of them as input.
      for (int pointBias = 0; pointBias < 3; pointBias++) {
        IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse(
            "<doc>" + initialContent + "</doc>");

        splitTextNodes(doc);

        Point<Node> point = doc.locate(boundaryLocation);
        boolean detectSplitNecessary =
            point.isInTextNode() &&
            point.getTextOffset() > 0 &&
            point.getTextOffset() < ((Text) point.getContainer()).getLength();

        if (splitNecessary != detectSplitNecessary) {
          fail("Possible incorrect test case location params " +
              "- splitNecessary parameter inaccurate");
        }

        Point<Node> boundaryPoint = null;
        if (splitNecessary) {
          if (boundaryLocation != nextNodeLocation) {
            fail("Wrong test case - when a text node must be split," +
                " nextNodeLocation == boundaryLocation");
          }
          // There's only one possibility to test - so quit after this.
          pointBias = 50;
          boundaryPoint = point;
        } else {
          Node nodeBefore, nodeAfter, parent;
          if (point.isInTextNode()) {
            if (point.getTextOffset() == 0) {
              nodeAfter = point.getContainer();
              nodeBefore = doc.getPreviousSibling(nodeAfter);
            } else {
              nodeBefore = point.getContainer();
              nodeAfter = doc.getNextSibling(nodeBefore);
            }
            parent = point.getContainer().getParentElement();
          } else {
            nodeAfter = point.getNodeAfter();
            parent = point.getContainer();
            if (nodeAfter == null) {
              nodeBefore = point.getContainer().getLastChild();
            } else {
              nodeBefore = nodeAfter.getPreviousSibling();
            }
          }
          Text textNodeBefore = doc.asText(nodeBefore);
          Text textNodeAfter = doc.asText(nodeAfter);
          switch (pointBias) {
          case 0:
            if (textNodeBefore != null) {
              boundaryPoint = Point.<Node>inText(textNodeBefore, textNodeBefore.getLength());
            }
            break;
          case 1:
            if (textNodeAfter != null) {
              boundaryPoint = Point.<Node>inText(textNodeAfter, 0);
            }
            break;
          case 2:
            boundaryPoint = Point.inElement(parent, nodeAfter);
            break;
          }
        }

        if (boundaryPoint != null) {
          if (returnNextNode) {
            Node n = DocHelper.ensureNodeBoundaryReturnNextNode(boundaryPoint, doc, doc);
            if (nextNodeLocation >= 0) {
              assertEquals(nextNodeLocation, doc.getLocation(n));
            } else {
              assertNull(n);
            }
          } else {
            Point.El<Node> point2 = DocHelper.ensureNodeBoundary(boundaryPoint, doc, doc);
            assertEquals(boundaryLocation, doc.getLocation(point2));
            if (!splitNecessary && !boundaryPoint.isInTextNode()) {
              // Check no unecessary copying.
              assertSame(boundaryPoint, point2);
            }
          }
        }
      }
    }
  }

  public void testTransparentSlice() {
    final TestDocumentContext<Node, Element, Text> cxt1 = createSliceTestCxt();

    withTextNode(cxt1, "TT", new NodeAction<Text>() {
      @Override
      public void apply(Text node) {
        Point.El<Node> point = DocHelper.ensureNodeBoundary(
            DocHelper.transparentSlice(Point.<Node>inText(node, 1), cxt1),
            cxt1.getIndexedDoc(), cxt1.getIndexedDoc());
        assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b>" +
            " how^</a> are you</x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt1.annotatableContent()).toString());
        assertEquals("here^", ((Text)point.getNodeAfter()).getData());

        Element c = node.getParentElement();
        cxt1.annotatableContent().transparentMove(c.getParentElement(),
            c, c.getNextSibling(), null);
        point = DocHelper.ensureNodeBoundary(
            DocHelper.transparentSlice(Point.<Node>inText(node, 1), cxt1),
            cxt1.getIndexedDoc(), cxt1.getIndexedDoc());
        assertEquals("he^llo<x>t^<a><b>here^<c>TT</c></b></a>" +
            "<a> how^</a> are you</x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt1.annotatableContent()).toString());

        assertEquals("a", ((Element)point.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point.getNodeAfter().getPreviousSibling()).getTagName());

        Point<Node> point2 = DocHelper.transparentSlice(point, cxt1);
        assertEquals("a", ((Element)point2.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point2.getNodeAfter().getPreviousSibling()).getTagName());

        point2 = DocHelper.transparentSlice(
            Point.end(point2.getNodeAfter().getPreviousSibling()), cxt1);
        assertEquals("a", ((Element)point2.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point2.getNodeAfter().getPreviousSibling()).getTagName());

        Point<Node> point3 = DocHelper.ensureNodeBoundary(
            DocHelper.transparentSlice(Point.end(point2.getContainer()), cxt1),
            cxt1.getIndexedDoc(), cxt1.getIndexedDoc());
        assertEquals("x", ((Element)point3.getContainer()).getTagName());
        assertNull(point3.getNodeAfter());

        Element a = (Element) point2.getNodeAfter().getPreviousSibling();
        Element d = cxt1.annotatableContent().transparentCreate("d", Attributes.EMPTY_MAP,
            a, null);

        point2 = DocHelper.transparentSlice(Point.<Node>end(a), cxt1);
        assertEquals("a", ((Element)point2.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point2.getNodeAfter().getPreviousSibling()).getTagName());

        point2 = DocHelper.transparentSlice(Point.<Node>end(d), cxt1);
        assertEquals("a", ((Element)point2.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point2.getNodeAfter().getPreviousSibling()).getTagName());

        Element x = (Element) point2.getContainer();
        Element e = cxt1.annotatableContent().transparentCreate("e", Attributes.EMPTY_MAP,
            x, null);

        point2 = DocHelper.ensureNodeBoundary(
            DocHelper.transparentSlice(Point.<Node>end(x), cxt1),
            cxt1.getIndexedDoc(), cxt1.getIndexedDoc());
        assertEquals("x", ((Element)point2.getContainer()).getTagName());
        assertNull(point2.getNodeAfter());

        assertEquals("he^llo<x>t^<a><b>here^<c>TT</c></b><d></d></a>" +
            "<a> how^</a> are you<e></e></x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt1.annotatableContent()).toString());
      }
    });

    final TestDocumentContext<Node, Element, Text> cxt2 = createSliceTestCxt();

    withTextNode(cxt2, " how^", new NodeAction<Text>() {
      @Override
      public void apply(Text node) {
        Point<Node> point = DocHelper.transparentSlice(Point.<Node>inText(node, 0), cxt2);
        assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b></a>" +
            "<a> how^</a> are you</x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt2.annotatableContent()).toString());
        assertEquals("a", ((Element)point.getNodeAfter()).getTagName());
        assertEquals("a", ((Element)point.getNodeAfter().getPreviousSibling()).getTagName());
      }
    });

    final TestDocumentContext<Node, Element, Text> cxt3 = createSliceTestCxt();

    withTextNode(cxt3, "here^", new NodeAction<Text>() {
      @Override
      public void apply(Text node) {
        Point<Node> point = DocHelper.transparentSlice(Point.<Node>inText(node, 2), cxt3);
        assertEquals("he^llo<x>t^<a><b><c>TT</c>he</b></a><a><b>re^</b>" +
            " how^</a> are you</x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt3.annotatableContent()).toString());
        assertEquals("re^", ((Text)point.getNodeAfter().getFirstChild().getFirstChild()).getData());
        assertEquals("he", ((Text)point.getNodeAfter().getPreviousSibling()
            .getLastChild().getLastChild()).getData());
      }
    });

    final TestDocumentContext<Node, Element, Text> cxt4 = createSliceTestCxt();

    withTextNode(cxt4, "llo", new NodeAction<Text>() {
      @Override
      public void apply(Text node) {
        Point<Node> point = DocHelper.transparentSlice(Point.<Node>inText(node, 2), cxt4);
        assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b>" +
            " how^</a> are you</x>y^eah<p><r></r><q></q></p>",
            XmlStringBuilder.innerXml(cxt4.annotatableContent()).toString());
        assertEquals("llo", ((Text)point.getContainer()).getData());
        assertEquals(2, point.getTextOffset());
      }
    });

    final TestDocumentContext<Node, Element, Text> cxt5 = createSliceTestCxt();
    Node last = cxt5.getIndexedDoc().getDocumentElement().getLastChild();
    Point<Node> point = DocHelper.transparentSlice(Point.<Node>end(last), cxt5);
    assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b>" +
        " how^</a> are you</x>y^eah<p><r></r><q></q></p>",
        XmlStringBuilder.innerXml(cxt5.annotatableContent()).toString());
    assertSame(last, point.getContainer());
    assertNull(point.getNodeAfter());

    point = DocHelper.transparentSlice(Point.<Node>inElement(
        last, last.getLastChild()), cxt5);
    assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b>" +
        " how^</a> are you</x>y^eah<p><r></r><q></q></p>",
        XmlStringBuilder.innerXml(cxt5.annotatableContent()).toString());
    assertSame(last, point.getContainer());
    assertSame(last.getLastChild(), point.getNodeAfter());

  }

  public void testCountChildren() {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("<a/>asdf<b/><c/>");
    assertEquals(4, DocHelper.countChildren(doc, doc.getDocumentElement()));
  }

  public void testCountChildrenReturnsZeroWhenThereAreNoChildren() {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("");
    assertEquals(0, DocHelper.countChildren(doc, doc.getDocumentElement()));
  }

  private void withTextNode(TestDocumentContext<Node, Element, Text> cxt, final String data,
      final NodeAction<Text> action) {

    traverse(cxt, new NodeAction<Node>() {
      @Override
      public void apply(Node node) {
        if (node instanceof Text) {
          Text t = (Text) node;
          if (t.getData().equals(data)) {
            action.apply(t);
          }
        }
      }
    });
  }

  private void traverse(TestDocumentContext<Node, Element, Text> cxt, NodeAction<Node> action) {
    DocHelper.traverse(
        cxt.annotatableContent(), cxt.annotatableContent().getDocumentElement(), action);
  }

  private TestDocumentContext<Node, Element, Text> createSliceTestCxt() {
    String initialContent = "he^llo<x>t^here^ how^ are you</x>y^eah" +
        "<p><r></r><q></q></p>";
    TestDocumentContext<Node, Element, Text> cxt = ContextProviders.createTestPojoContext(
        DocProviders.POJO.parse(initialContent).asOperation(), null, null, null,
          DocumentSchema.NO_SCHEMA_CONSTRAINTS);

    List<Point<Node>> splitPoints = splitTextNodes(cxt.getIndexedDoc());

    Point<Node> t_here = splitPoints.get(1);
    Point<Node> there_ = splitPoints.get(2);
    Point<Node> how_ = splitPoints.get(3);

    Element a1 = cxt.annotatableContent().transparentCreate("a", Attributes.EMPTY_MAP,
        (Element) t_here.getContainer(), t_here.getNodeAfter());
    cxt.annotatableContent().transparentMove(
        a1, t_here.getNodeAfter(), how_.getNodeAfter(), null);

    Element a2 = cxt.annotatableContent().transparentCreate("b", Attributes.EMPTY_MAP,
        a1, t_here.getNodeAfter());
    cxt.annotatableContent().transparentMove(
        a2, t_here.getNodeAfter(), there_.getNodeAfter(), null);

    Element a3 = cxt.annotatableContent().transparentCreate("c", Attributes.EMPTY_MAP,
        a2, t_here.getNodeAfter());
    cxt.annotatableContent().transparentCreate("TT", a3, null);

    assertEquals("he^llo<x>t^<a><b><c>TT</c>here^</b> how^</a> are you</x>" +
        "y^eah<p><r></r><q></q></p>",
        XmlStringBuilder.innerXml(cxt.annotatableContent()).toString());

    return cxt;
  }

  private List<Point<Node>> splitTextNodes(IndexedDocument<Node, Element, Text> doc) {
    List<Point<Node>> splitPoints = new ArrayList<Point<Node>>();
    final List<Text> toSplit = new ArrayList<Text>();
    DocHelper.traverse(doc, doc.getDocumentElement(), new NodeAction<Node>() {
      public void apply(Node node) {
        if (node instanceof Text) {
          Text t = (Text) node;
          if (t.getData().contains("^")) {
            toSplit.add(t);
          }
        }
      }
    });

    for (Text t : toSplit) {
      while (t != null && t.getData().contains("^")) {
        t = doc.splitText(t, t.getData().indexOf("^") + 1);
        splitPoints.add(Point.before(doc, t));
      }
    }

    return splitPoints;
  }

  public void testGetNextNodeDepthFirst() {
    MutableDocument<Node, Element, Text> doc = getDoc(
        "<x>hello</x><y><yy>blah</yy>yeah</y><w/><z>final</z>");
    Element root = doc.getDocumentElement();
    Node x = root.getFirstChild();
    Node y = x.getNextSibling();
    Node w = y.getNextSibling();
    Node yy = y.getFirstChild();
    Node z = root.getLastChild();
    assertSame(y, DocHelper.getNextNodeDepthFirst(doc, x, null, false));
    assertSame(y, DocHelper.getNextNodeDepthFirst(doc, x, root, false));

    assertSame(x.getFirstChild(), DocHelper.getNextNodeDepthFirst(doc, x, x, true));
    assertSame(x.getFirstChild(), DocHelper.getNextNodeDepthFirst(doc, x, root, true));
    assertSame(x.getFirstChild(), DocHelper.getNextNodeDepthFirst(doc, x, null, true));
    assertSame(x.getFirstChild(), DocHelper.getPrevNodeDepthFirst(doc, x, x, true));
    assertSame(x.getFirstChild(), DocHelper.getPrevNodeDepthFirst(doc, x, root, true));
    assertSame(x.getFirstChild(), DocHelper.getPrevNodeDepthFirst(doc, x, null, true));

    assertSame(null, DocHelper.getNextNodeDepthFirst(doc, x, x, false));
    assertSame(null, DocHelper.getNextNodeDepthFirst(doc, w, w, true));
    assertSame(null, DocHelper.getNextNodeDepthFirst(doc, w, w, false));

    assertSame(y, DocHelper.getNextNodeDepthFirst(doc, x.getFirstChild(), null, true));
    assertSame(y, DocHelper.getNextNodeDepthFirst(doc, x.getFirstChild(), root, true));

    assertSame(x, DocHelper.getPrevNodeDepthFirst(doc, yy.getFirstChild(), root, true));

    assertSame(null, DocHelper.getNextNodeDepthFirst(doc, yy, y.getLastChild(), false));
  }

  public void testFindById() {
    MutableDocument<Node, Element, Text> doc = getDoc(
        "<x id=\"x\">hello</x><y id=\"y\"><yy id=\"y\">blah</yy>yeah</y><z>final</z>");

    Element root = doc.getDocumentElement();
    Node x = root.getFirstChild();
    Node z = root.getLastChild();
    Node y = z.getPreviousSibling();

    int firstLoc = doc.getLocation(x);
    assertSame(firstLoc, DocHelper.findLocationById(doc, "x"));
    assertSame(x, DocHelper.findElementById(doc, "x"));
    assertSame(firstLoc + 7, DocHelper.findLocationById(doc, "y"));
    assertSame(y, DocHelper.findElementById(doc, "y"));
    assertSame(-1, DocHelper.findLocationById(doc, "a"));
    assertSame(null, DocHelper.findElementById(doc, "a"));
  }

  public void testFindByIdFromElement() {
    MutableDocument<Node, Element, Text> doc = getDoc(
        "<x id=\"x\">hello</x>" +
        "<aroundy><y id=\"y\"><yy id=\"y\">blah</yy>yeah</y></aroundy>" +
        "<z>final</z>");

    Element root = doc.getDocumentElement();
    Node x = root.getFirstChild();
    Node z = root.getLastChild();
    Node aroundy = z.getPreviousSibling();
    Node y = aroundy.getFirstChild();
    Node yy = y.getFirstChild();

    assertSame(null, DocHelper.findElementById(doc, x.asElement(), "y"));
    assertSame(y, DocHelper.findElementById(doc, aroundy.asElement(), "y"));
    assertSame(y, DocHelper.findElementById(doc, y.asElement(), "y"));
    assertSame(null, DocHelper.findElementById(doc, z.asElement(), "y"));
    assertSame(yy, DocHelper.findElementById(doc, yy.asElement(), "y"));
  }

  public void testMatchingElement() {
    MutableDocument<Node, Element, Text> doc = getDoc("<x/>hello");
    Node n = doc.getDocumentElement().getFirstChild();
    assertFalse(DocHelper.isMatchingElement(doc, n.getNextSibling(), "x"));
    assertFalse(DocHelper.isMatchingElement(doc, n, "y"));
    assertTrue(DocHelper.isMatchingElement(doc, n, "x"));
  }

  final DocPredicate IS_X = new DocPredicate() {
    @Override
    public <N, E extends N, T extends N> boolean apply(ReadableDocument<N, E, T> doc, N node) {
      return DocHelper.isMatchingElement(doc, node, "x");
    }
  };

  public void testJumpOutJumpsReturnsNullWithNoMatch() {
    MutableDocument<Node, Element, Text> doc = getDoc("<w><y><z>abc</z>def</y>ghi</w>hello");
    Element z = DocHelper.getElementWithTagName(doc, "z");
    assertNull(DocHelper.jumpOut(doc, Point.start(doc, z), IS_X));

    doc = getDoc("<x><y><z>abc</z>def</y>ghi</x>hello");
    Element x = DocHelper.getElementWithTagName(doc, "x");
    assertNull(DocHelper.jumpOut(doc, Point.before(doc, x), IS_X));
    assertNull(DocHelper.jumpOut(doc, Point.after(doc, x), IS_X));
    assertNull(DocHelper.jumpOut(doc, Point.inText(x.getNextSibling(), 2), IS_X));
  }

  public void testJumpOutJumpsOutRightwards() {
    MutableDocument<Node, Element, Text> doc = getDoc("<x><y><z>abc</z>def</y>ghi</x>hello");
    Element x = DocHelper.getElementWithTagName(doc, "x");
    Element y = DocHelper.getElementWithTagName(doc, "y");
    Element z = DocHelper.getElementWithTagName(doc, "z");

    Point<Node> afterY = Point.after(doc, y);
    assertEquals(afterY, DocHelper.jumpOut(doc, Point.inText(z.getFirstChild(), 1), IS_X));
    assertEquals(afterY, DocHelper.jumpOut(doc, Point.start(doc, z), IS_X));
    assertEquals(afterY, DocHelper.jumpOut(doc, Point.start(doc, y), IS_X));
    assertEquals(afterY, DocHelper.jumpOut(doc, Point.<Node>end(y), IS_X));
    assertSame(afterY, DocHelper.jumpOut(doc, afterY, IS_X));
  }

  public void testJumpOutPreservesIdentityWherePossible() {
    MutableDocument<Node, Element, Text> doc = getDoc("<x/>hello");
    Node x = doc.getDocumentElement().getFirstChild();

    Point<Node> afterX = Point.after(doc, x);
    Point<Node> inText = Point.inText(x.getNextSibling(), 2);
    Point<Node> inX = Point.end(x);
    assertSame(afterX, DocHelper.jumpOut(doc, afterX, DocHelper.ROOT_PREDICATE));
    assertSame(inText, DocHelper.jumpOut(doc, inText, DocHelper.ROOT_PREDICATE));
    assertSame(inX, DocHelper.jumpOut(doc, inX, IS_X));
  }

  public void testExplicitCreateFailsOnOldDocument() {
    MutableDocument<Node, Element, Text> doc = getDoc("");

    DocHelper.createFirstTopLevelElement(doc, "foo");
  }

  public void testExpectedGetSucceedsOnOldEmptyDocument() {
    MutableDocument<Node, Element, Text> doc = getDoc("");
    try {
      Element top = DocHelper.expectAndGetFirstTopLevelElement(doc, "foo");
      fail("this test is not expected to work with new ops.");
    } catch (IllegalArgumentException ex) {
      // Success
    }
  }

  public void testGetOrCreateSucceedsOnOldEmptyDocument() {
    MutableDocument<Node, Element, Text> doc = getDoc("");

    Element top = DocHelper.getOrCreateFirstTopLevelElement(doc, "foo");
    assertEquals(doc.getFirstChild(doc.getDocumentElement()), top);
  }

  private MutableDocument<Node, Element, Text> getDoc(String innerXml) {
    return ContextProviders.createTestPojoContext(innerXml, null, null, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();
  }

  /** Tests for the isAncestor helper method. */
  public void testIsAncestor() {
    // build a simple tree
    //    _1
    // 0_/    3
    //   \_2_/
    //       \4
    ReadableWDocument<Node, Element, Text> doc = DocProviders.POJO.parse(
        "<A><B/><C><D/><E/></C></A>");
    Node[] nodes = new Node[5];
    nodes[0] = doc.getDocumentElement().getFirstChild();
    nodes[1] = nodes[0].getFirstChild();
    nodes[2] = nodes[1].getNextSibling();
    nodes[3] = nodes[2].getFirstChild();
    nodes[4] = nodes[3].getNextSibling();

    // check each pair:
    for (int i = 0; i < nodes.length; i++) {
      for (int j = 0; j < nodes.length; j++) {
        // find results:
        boolean resultExclusive = DocHelper.isAncestor(doc, nodes[i], nodes[j], false);
        boolean resultInclusive = DocHelper.isAncestor(doc, nodes[i], nodes[j], true);

        // calculate manually:
        boolean isParentExclusive = false;
        if (i == 0 || i == 2) {
          isParentExclusive = (j > i); // 0 and 2 are parents of everything lower.
        }
        boolean isParentInclusive = isParentExclusive || (i == j);

        // verify:
        assertEquals(isParentExclusive, resultExclusive);
        assertEquals(isParentInclusive, resultInclusive);
      }
    }

    // final check for null:
    assertFalse(DocHelper.isAncestor(doc, nodes[0], null, true));
    assertFalse(DocHelper.isAncestor(doc, nodes[0], null, false));
  }
}
