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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ElementStyleView;
import org.waveprotocol.wave.model.document.util.RawElementStyleView;
import org.waveprotocol.wave.model.richtext.RichTextTokenizerImpl.Token;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests the rich text tokenizer.
 *
 */

public class RichTextTokenizerImplTest extends TestCase {
  public void testEmpty() {
    RichTextTokenizer tokens = tokenize("");
    assertFalse(tokens.hasNext());
  }

  public void testUnknownElements() {
    verifyTokens(tokenize("<p><x/>foo<d/></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testSpaces() {
    verifyTokens(tokenize("" + '\u00a0' + '\u00a0'),
        token(RichTextTokenizer.Type.TEXT, "  "));
  }

  public void testLineBreaks() {
    verifyTokens(tokenize("<br/>foo<br/>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"));

    verifyTokens(tokenize("<p><br/><br/><br/></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>foo</p>bar<br/><p>baz</p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "baz"),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testInline() {
    verifyTokens(tokenize("foo"),
        token(RichTextTokenizer.Type.TEXT, "foo"));

    verifyTokens(tokenize("foo<p>bar</p>"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("foo<p>bar</p>baz"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "baz"));

  }

  public void testParagraph() {
    verifyTokens(tokenize("<p>foo</p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>foo</p><p>bar</p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>foo<br/></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>foo<br/><br/></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>foo</p>bar<p>baz</p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "baz"),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testNestedParagraphs() {
    verifyTokens(tokenize("<p><p>foo</p></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<div><div><p>foo</p></div></div>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testStyle() {
    verifyTokens(tokenize("<p><b>foo</b></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testMultipleStyles() {
    verifyTokens(tokenize("<p><b><i>foo</i></b></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.STYLE_FONT_STYLE_START, "italic"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_FONT_STYLE_END),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testNestedStyle() {
    verifyTokens(tokenize("<p><b><b>foo</b></b></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p><b style=\"color: red\">out<em>foo</em></b></p>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.STYLE_COLOR_START, "red"),
        token(RichTextTokenizer.Type.TEXT, "out"),
        token(RichTextTokenizer.Type.STYLE_FONT_STYLE_START, "italic"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_FONT_STYLE_END),
        token(RichTextTokenizer.Type.STYLE_COLOR_END),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  public void testHeaders() {
    for (int i = 1; i <= 4; ++i) {
      String tagName = "h" + i;
      verifyTokens(tokenize("<" + tagName + ">foo</" + tagName + ">"),
          token(RichTextTokenizer.Type.NEW_LINE, tagName),
          token(RichTextTokenizer.Type.TEXT, "foo"),
          token(RichTextTokenizer.Type.NEW_LINE));
    }
    verifyTokens(tokenize("<h0>foo</h0>"),
        token(RichTextTokenizer.Type.TEXT, "foo"));
  }

  public void testStyles() {
    verifySimpleTag("b", RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START,
        RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END, "bold");
    verifySimpleTag("strong", RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START,
        RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END, "bold");
    verifySimpleTag("i", RichTextTokenizer.Type.STYLE_FONT_STYLE_START,
        RichTextTokenizer.Type.STYLE_FONT_STYLE_END, "italic");
    verifySimpleTag("em", RichTextTokenizer.Type.STYLE_FONT_STYLE_START,
        RichTextTokenizer.Type.STYLE_FONT_STYLE_END, "italic");
    verifyTokens(tokenize("<span style=\"color: #FF00FF\">foo</span>"),
        token(RichTextTokenizer.Type.STYLE_COLOR_START, "#FF00FF"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_COLOR_END));
  }

  public void testTextDecoration() {
    // NOTE(user): using "textDecoration" rather than "text-decoration" as the
    // test goes through our own parser rather than the browser.
    verifyTokens(tokenize("<span style=\"textDecoration: underline\">foo</span>"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_START, "underline"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_END));
    verifyTokens(tokenize("<u>foo</u>"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_START, "underline"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_END));
    verifyTokens(tokenize("<u style=\"textDecoration: overline\">foo</u>"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_START, "overline"),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.STYLE_TEXT_DECORATION_END));
  }

  public void testLinks() {
    verifyTokens(tokenize("<a href=\"http://www.google.com/\">Goog</a>"),
        token(RichTextTokenizer.Type.LINK_START, "http://www.google.com/"),
        token(RichTextTokenizer.Type.TEXT, "Goog"),
        token(RichTextTokenizer.Type.LINK_END));

    verifyTokens(tokenize("<a>Goog</a>"),
        token(RichTextTokenizer.Type.TEXT, "Goog"));
  }

  public void testAttributeCombinations() {
    verifyTokens(tokenize("<a href=\"http://www.google.com/\" style=\"color: #FF00FF\">Goog</a>"),
        token(RichTextTokenizer.Type.LINK_START, "http://www.google.com/"),
        token(RichTextTokenizer.Type.STYLE_COLOR_START, "#FF00FF"),
        token(RichTextTokenizer.Type.TEXT, "Goog"),
        token(RichTextTokenizer.Type.STYLE_COLOR_END),
        token(RichTextTokenizer.Type.LINK_END));
  }

  public void testListSimple() {
    verifyTokens(tokenize("<ul><li>foo</li><li>bar</li></ul>"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END));

    verifyTokens(tokenize("<ul><li>foo</li><li>bar</li></ul>abc"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "abc"));

    verifyTokens(tokenize("<ul><li>foo</li><li>bar</li></ul>"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END));

    verifyTokens(tokenize("<ol><li/>foo</ol>"),
        token(RichTextTokenizer.Type.ORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.ORDERED_LIST_END));

    verifyTokens(tokenize("<ol><li/>fo<b>o</b></ol>"),
        token(RichTextTokenizer.Type.ORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "fo"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.TEXT, "o"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END),
        token(RichTextTokenizer.Type.ORDERED_LIST_END));

    verifyTokens(tokenize("<ol><li>fo</li></ol><b>o</b>"),
        token(RichTextTokenizer.Type.ORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "fo"),
        token(RichTextTokenizer.Type.ORDERED_LIST_END),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "o"),
        token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END));
  }

  public void testListComplex() {
    Attributes listAttribute = new AttributesImpl(Collections.singletonMap("t", "li"));
    verifyTokens(tokenize("<ol><li>fo</li></ol><p>hello</p>"),
        token(RichTextTokenizer.Type.ORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "fo"),
        token(RichTextTokenizer.Type.ORDERED_LIST_END),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "hello"),
        token(RichTextTokenizer.Type.NEW_LINE));

    verifyTokens(tokenize("<p>hello</p><ol><li>fo</li></ol>"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "hello"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.ORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "fo"),
        token(RichTextTokenizer.Type.ORDERED_LIST_END));

    verifyTokens(tokenize("<ul><li/>foo<li/>bar</ul>"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END));

// TODO(danilatos): New tests, enable them once code fixed up further.

//    verifyTokens(tokenize("<ul><li>foo<div>nested</div></li><li>bar</li></ul>"),
//        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
//        token(RichTextTokenizer.Type.LIST_ITEM),
//        token(RichTextTokenizer.Type.TEXT, "foo"),
//        token(RichTextTokenizer.Type.NEW_LINE),
//        token(RichTextTokenizer.Type.TEXT, "nested"),
//        token(RichTextTokenizer.Type.LIST_ITEM),
//        token(RichTextTokenizer.Type.TEXT, "bar"),
//        token(RichTextTokenizer.Type.UNORDERED_LIST_END));

//    verifyTokens(tokenize("<ul><li><h3>heading</h3>stuff</li><li>bar</li></ul>"),
//        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
//        token(RichTextTokenizer.Type.NEW_LINE, "h3"),
//        token(RichTextTokenizer.Type.TEXT, "heading"),
//        token(RichTextTokenizer.Type.NEW_LINE),
//        token(RichTextTokenizer.Type.TEXT, "stuff"),
//        token(RichTextTokenizer.Type.LIST_ITEM),
//        token(RichTextTokenizer.Type.TEXT, "bar"),
//        token(RichTextTokenizer.Type.UNORDERED_LIST_END));
  }

  public void testTableSimple() {
    // TODO(patcoleman): temporary table parsing, this should be fixed to table tokens
    // once supported in the document schema.
    String tableData = "<table><tbody>" +
                          "<tr><th>h1</th><th>h2</th></tr>" +
                          "<tr><td>c1</td><td>c2</td></tr>" +
                       "</tbody></table>";

    verifyTokens(tokenize(tableData),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, " "),
        token(RichTextTokenizer.Type.TEXT, "h1"),
        token(RichTextTokenizer.Type.TEXT, " "),
        token(RichTextTokenizer.Type.TEXT, "h2"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, " "),
        token(RichTextTokenizer.Type.TEXT, "c1"),
        token(RichTextTokenizer.Type.TEXT, " "),
        token(RichTextTokenizer.Type.TEXT, "c2"),
        token(RichTextTokenizer.Type.NEW_LINE));
  }

  private void verifyTokens(RichTextTokenizer tokenizer,
      Token ... tokens) {
    for (Token token : tokens) {
      if (!tokenizer.hasNext() ||
          !ValueUtils.equal(token.getType(), tokenizer.next()) ||
          !ValueUtils.equal(token.getData(), tokenizer.getData())) {
        fail("\n" + Arrays.<Token>asList(tokens) + " vs \n" + tokenizer);
      }
    }
    if (tokenizer.hasNext()) {
      fail("\n" + Arrays.<Token>asList(tokens) + " vs \n" + tokenizer);
    }
  }

  private void verifySimpleTag(String tagName, RichTextTokenizer.Type start,
      RichTextTokenizer.Type end, String data) {
    verifyTokens(tokenize("<" + tagName + ">foo</" + tagName + ">"),
        token(start, data),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(end));
  }

  private ElementStyleView<Node, Element, Text> parseDocumentContents(String xmlString) {
    return new RawElementStyleView(
        RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER).parse(
            "<div>" + xmlString + "</div>"));
  }

  protected RichTextTokenizer tokenize(String xmlString) {
    return new RichTextTokenizerImpl<Node, Element, Text>(parseDocumentContents(xmlString));
  }

  private Token token(RichTextTokenizer.Type type) {
    return new Token(type, null);
  }

  private Token token(RichTextTokenizer.Type type, String data) {
    return new Token(type, data);
  }
}
