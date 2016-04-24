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
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

import java.util.Iterator;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DocIterateTest extends TestCase {

  public void testIteration() {
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "<x>hello</x><y><yy>blah</yy>yeah</y><z>final</z>", null, null, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();

    int i;
    Element root = doc.getDocumentElement();
    Node x = root.getFirstChild();
    Node z = root.getLastChild();
    Node y = z.getPreviousSibling();

    for (Node end : new Node[]{z, root, null}) {
      i = 0;
      for (Node n : DocIterate.deep(doc, z.getFirstChild(), end)) {
        assertSame(z.getFirstChild(), n);
        i++;
      }
      assertEquals(1, i);

      i = 0;
      for (Node n : DocIterate.deep(doc, z, end)) {
        assertSame(i == 0 ? z : z.getFirstChild(), n);
        i++;
      }
      assertEquals(2, i);

      i = 0;
      for (Node n : DocIterate.deep(doc, root, end)) {
        switch (i) {
          case 0: assertSame(root, n); break;
          case 3: assertSame(y, n); break;
          case 4: assertSame(y.getFirstChild(), n); break;
          case 5: assertSame(y.getFirstChild().getFirstChild(), n); break;
          case 6: assertSame(y.getFirstChild().getNextSibling(), n); break;
          default: assertNotNull(n); break;
        }
        i++;
      }
      assertEquals(end == z ? 7 : 9, i);
    }

    i = 0;
    for (Element e : DocIterate.deepElements(doc, root, root)) {
      i++;
    }
    assertEquals(5, i);

    i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "x")) {
      i++;
    }
    assertEquals(1, i);
  }

  private MutableDocument<Node, Element, Text> makeTestDocument() {
    return ContextProviders.createTestPojoContext(
        "<foo id=\"0\"/><x>hello</x><y><foo id=\"1\">hello</foo>yeah</y><z>sup</z><foo id=\"2\"/>",
        null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();
  }

  public void testElementsByTagNameIteration() {
    MutableDocument<Node, Element, Text> doc = makeTestDocument();

    int i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "foo")) {
      assertEquals(String.valueOf(i), e.getAttribute("id"));
      i++;
    }
    assertEquals(3, i);

    i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "missing")) {
      i++;
    }
    assertEquals(0, i);
  }

  public void testElementsByTagName_startNode() {
    MutableDocument<Node, Element, Text> doc = makeTestDocument();

    Element secondFoo = DocHelper.findElementById(doc, "1");
    Iterator<Element> iter = DocIterate.deepElementsWithTagName(
        doc, "foo", secondFoo, null).iterator();
    assertTrue(iter.hasNext());
    assertEquals("1", iter.next().getAttribute("id"));
    assertEquals("2", iter.next().getAttribute("id"));
    assertFalse(iter.hasNext());
  }

  public void testElementsByTagName_endNode() {
    MutableDocument<Node, Element, Text> doc = makeTestDocument();

    Element secondFoo = DocHelper.findElementById(doc, "1");
    Iterator<Element> iter = DocIterate.deepElementsWithTagName(
        doc, "foo", DocHelper.getElementWithTagName(doc, "foo"), secondFoo).iterator();
    assertTrue(iter.hasNext());
    assertEquals("0", iter.next().getAttribute("id"));
    assertEquals("1", iter.next().getAttribute("id"));
    assertFalse(iter.hasNext());
  }

  public void testElementsByTagName_nullStartNode() {
    MutableDocument<Node, Element, Text> doc = makeTestDocument();

    Element secondFoo = DocHelper.findElementById(doc, "1");
    Iterator<Element> iter = DocIterate.deepElementsWithTagName(
        doc, "foo", null, secondFoo).iterator();
    assertFalse(iter.hasNext());
  }
}
