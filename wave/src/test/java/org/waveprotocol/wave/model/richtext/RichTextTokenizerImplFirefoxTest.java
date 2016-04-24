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

package org.waveprotocol.wave.model.richtext;


import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ElementStyleView;
import org.waveprotocol.wave.model.document.util.RawElementStyleView;
import org.waveprotocol.wave.model.richtext.RichTextTokenizer;

/**
 * Tests the rich text tokenizer provided for Firefox, which does stripping
 * of extra whitespace.
 *
 */

public class RichTextTokenizerImplFirefoxTest extends RichTextTokenizerImplTest {
  public void testWhitespaceStrip() {
    RichTextTokenizer tokens = tokenize("  foo  ");
    assertEquals(RichTextTokenizer.Type.TEXT, tokens.next());
    assertEquals(tokens.getData(), "foo");
  }

  public void testNewlineStrip() {
    RichTextTokenizer tokens = tokenize("\nfoo\n");
    assertEquals(RichTextTokenizer.Type.TEXT, tokens.next());
    assertEquals(tokens.getData(), "foo");
  }

  public void testAllowedSpace() {
    RichTextTokenizer tokens = tokenize("Click <a>Here</a>");
    assertEquals(RichTextTokenizer.Type.TEXT, tokens.next());
    assertEquals(tokens.getData(), "Click ");
    assertEquals(RichTextTokenizer.Type.TEXT, tokens.next());
    assertEquals(tokens.getData(), "Here");
  }

  public void testNestedDivs() {
    RichTextTokenizer tokens = tokenize("<div><div><p>bar</p></div></div>");
    assertEquals(RichTextTokenizer.Type.NEW_LINE, tokens.next());
    assertEquals(RichTextTokenizer.Type.TEXT, tokens.next());
    assertEquals(tokens.getData(), "bar");
    assertEquals(RichTextTokenizer.Type.NEW_LINE, tokens.next());
    assertFalse(tokens.hasNext());
  }

  private ElementStyleView<Node, Element, Text> parse(String xmlString) {
    return new RawElementStyleView(
        RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER).parse(
            "<div>" + xmlString + "</div>"));
  }

  @Override protected RichTextTokenizer tokenize(String xmlString) {
    return new RichTextTokenizerImplFirefox<Node, Element, Text>(parse(xmlString));
  }
}
