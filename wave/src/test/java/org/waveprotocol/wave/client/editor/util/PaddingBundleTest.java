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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

/**
 * Tests numerous cases for the padding calculations.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class PaddingBundleTest extends TestCase {
  private static IndexedDocument<Node, Element, Text> createContent(String doc) {
    return DocProviders.POJO.parse("<body><line></line>" + doc + "</body>");
  }

  public static void CHECK(PaddingBundle bundle, String text, boolean start, boolean end) {
    assertEquals(bundle.getText(), text);
    assertEquals(bundle.isAddedStart(), start);
    assertEquals(bundle.isAddedEnd(), end);
  }

  /** Make sure correct padding is calculated for insertion into an empty document. */
  public void testEmptyDocumentInsertion() {
    IndexedDocument<Node, Element, Text> document = createContent("");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 3, 3);
    CHECK(bundle, "word ", false, true);
  }

  /** Make sure correct padding is calculated for insertion over text in a document. */
  public void testReplaceTextInsertion() {
    IndexedDocument<Node, Element, Text> document = createContent("stuff");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 3, 8);
    CHECK(bundle, "word ", false, true);
  }

  /** Make sure correct padding is calculated for insertion in the middle of a word. */
  public void testInsertionWithinWord() {
    IndexedDocument<Node, Element, Text> document = createContent("stuff");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 5, 6);
    CHECK(bundle, " word ", true, true);
  }

  /** Make sure correct padding is calculated for already-padded insertion inside a word. */
  public void testPaddedInsertionWithinWord() {
    IndexedDocument<Node, Element, Text> document = createContent("stuff");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, " word ", 5, 6);
    CHECK(bundle, " word ", false, false);
  }

  /** Make sure correct padding is calculated for insertion across inline tags. */
  public void testInsertionWithinWordAcrossTags() {
    IndexedDocument<Node, Element, Text> document = createContent("stuf<b>f</b>");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 7, 7);
    CHECK(bundle, " word ", true, true);
  }

  /** Make sure correct padding is calculated for insertion at the start of a paragraph. */
  public void testInsertionAtStart() {
    IndexedDocument<Node, Element, Text> document = createContent("stuff");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 3, 3);
    CHECK(bundle, "word ", false, true);
  }

  /** Make sure correct padding is calculated for insertion at the end of a paragraph. */
  public void testInsertionAtEnd() {
    IndexedDocument<Node, Element, Text> document = createContent("stuff");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "word", 8, 8);
    CHECK(bundle, " word ", true, true);
  }

  /** Make sure correct padding is calculated for insertion just before a word. */
  public void testInsertionBeforeWord() {
    IndexedDocument<Node, Element, Text> document = createContent("seize the day");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "carpediem", 9, 9);
    CHECK(bundle, "carpediem ", false, true);
  }

  /** Make sure correct padding is calculated for insertion just after a word. */
  public void testInsertionAfterWord() {
    IndexedDocument<Node, Element, Text> document = createContent("seize the day");
    PaddingBundle bundle = PaddingBundle.applyPadding(document, "carpediem", 12, 12);
    CHECK(bundle, " carpediem", true, false);
  }
}
