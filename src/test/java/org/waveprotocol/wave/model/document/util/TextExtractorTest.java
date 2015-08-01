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

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * Test cases for the text extractor.
 *
 */

public class TextExtractorTest extends TestCase {

  public void testOnlyText() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hello</x>");
    String text = extractAll(doc);
    assertEquals("Hello", text);
  }

  public void testTextSplitByAnElement() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y></y>lo</x>");
    String text = extractAll(doc);
    assertEquals("Hello", text);
  }

  public void testTextSplitByTwoElements() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y></y>lo Wor<z></z>ld</x>");
    String text = extractAll(doc);
    assertEquals("Hello World", text);
  }

  public void testTextNestedInAnElement() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y>lo</y> Wor<z>l</z>d</x>");
    String text = extractAll(doc);
    assertEquals("Hello World", text);
  }
  private static <N> String extractAll(ReadableDocument<N, ?, ?> doc) {
    return extractAll1(doc);
  }

  private static <N, E extends N, T extends N> String extractAll1(ReadableDocument<N, E, T> doc) {
    return TextExtractor.extractInnerText(doc, doc.getDocumentElement());
  }
}
