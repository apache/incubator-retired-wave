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
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.Tx;
import org.waveprotocol.wave.model.document.util.TextLocator;

/**
 * Test cases for TextLocator
 *
 */

public class TextLocatorTest extends TestCase {

  public void testFindCharacter() {
    checkFindCharacter("<x>abcd</x>", 1, "a", true, 1);
    checkFindCharacter("<x>abcd</x>", 1, "b", true, 2);
    checkFindCharacter("<x>abcd</x>", 4, "d", true, 4);

    checkFindCharacter("<x>abcd</x>", 3, "a", false, 1);
    checkFindCharacter("<x>abcd</x>", 3, "b", false, 2);
    checkFindCharacter("<x>abcd</x>", 5, "d", false, 4);

    checkFindCharacter("<x>ab<a/>cd</x>", 1, "a", true, 1);
    checkFindCharacter("<x>ab<a/>cd</x>", 1, "b", true, 2);

    checkFindCharacter("<x>ab<a/>cd</x>", 1, "c", true, 3);
    checkFindCharacter("<x>ab<a/>cd</x>", 1, "d", true, 3);

    // Test with split text node
    MutableDocument<Node, Element, Text> doc = DocProviders.MOJO.parse("<x>hello</x>");
    DocHelperTest.insertTextInNewTextNodeHelper(doc, doc.locate(6), "world");
    Tx<Node> helloStart = doc.locate(1).asTextPoint();
    assertEquals(3, doc.getLocation(TextLocator.findCharacter(doc, helloStart, "l", true)));
    assertEquals(6, doc.getLocation(TextLocator.findCharacter(doc, helloStart, "w", true)));
    assertEquals(1, doc.getLocation(TextLocator.findCharacter(doc, helloStart, "h",
        false)));
  }

  private void checkFindCharacter(String docXml, int start, String ch, boolean forward,
      int expectedLocation) {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse(docXml);
    Tx<Node> startPoint = DocHelper.normalizePoint(doc.locate(start), doc).asTextPoint();
    assert startPoint != null : "Invalid start point";
    Point<Node> found = TextLocator.findCharacter(doc, startPoint, ch, forward);
    assertNotNull(found);
    assertEquals(expectedLocation, doc.getLocation(found));
  }

  public void testFindWordBoundary() {
    checkWordBoundary("<x>a b</x>", 1, true, 2);
    checkWordBoundary("<x>ab bc</x>", 1, true, 3);

    checkWordBoundary("<x>ab bc de</x>", 3, true, 6);
    checkWordBoundary("<x>ab bc de</x>", 4, true, 6);
    checkWordBoundary("<x>ab   bc de</x>", 4, true, 8);

    checkWordBoundary("<x>hello</x>", 1, true, 6);
    checkWordBoundary("<x>hello<a/> hi</x>", 1, true, 6);
    checkWordBoundary("<x>hello</x>", 6, true, 6);

    checkWordBoundary("<x>hello</x>", 6, false, 1);
    checkWordBoundary("<x>a  b</x>", 5, false, 4);
    checkWordBoundary("<x>a  b</x>", 3, false, 1);
    checkWordBoundary("<x>a  bc  de</x>", 7, false, 4);
    checkWordBoundary("<x>a  bc  de</x>", 1, false, 1);

    // Test with split text node
    MutableDocument<Node, Element, Text> doc = DocProviders.MOJO.parse("<x>hello </x>");
    DocHelperTest.insertTextInNewTextNodeHelper(doc, doc.locate(7), "world");

    assertEquals(6, doc.getLocation(TextLocator.getWordBoundary(doc.locate(1).asTextPoint(), doc,
        true)));
    assertEquals(6, doc.getLocation(TextLocator.getWordBoundary(doc.locate(2).asTextPoint(), doc,
        true)));
    assertEquals(12, doc.getLocation(TextLocator.getWordBoundary(doc.locate(7).asTextPoint(), doc,
        true)));

    assertEquals(7, doc.getLocation(TextLocator.getWordBoundary(doc.locate(12).asTextPoint(), doc,
        false)));
    assertEquals(1, doc.getLocation(TextLocator.getWordBoundary(doc.locate(2).asTextPoint(), doc,
        false)));
  }

  private void checkWordBoundary(String docXml, int start, boolean forward, int expectedLocation) {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse(docXml);
    Tx<Node> startPoint = DocHelper.normalizePoint(doc.locate(start), doc).asTextPoint();
    assert startPoint != null : "Invalid start point";
    Point<Node> found =
      TextLocator.getWordBoundary(startPoint, doc, forward);
    if (found != null) {
      assertEquals(expectedLocation, doc.getLocation(found));
    } else {
      assertEquals(expectedLocation, -1);
    }
  }
}
