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

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.ElementStyleView;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.RawElementStyleView;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.richtext.RichTextTokenizerImpl.Token;
import org.waveprotocol.wave.model.util.Pair;

/**
 * Tests logic of the rich text mutation builder.
 *
 */

public class RichTextMutationBuilderTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    LineContainers.setTopLevelContainerTagname("body");
  }

  public void testInline() {
    verifyMutations("{++\"foo\"; }", buildTokens(
        token(RichTextTokenizer.Type.TEXT, "foo")));

    verifyMutations("{++\"foo\"; ++\"bar\"; }", buildTokens(
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.TEXT, "bar")));
  }

  public void testSingleLine() {
    // This tests a special case in pasting single paragraphs.
    // Because a cursor is always in a <p> tag within the editor,
    // we don't want to close it and start a new one when pasting
    // <p>foo</p>. So we ignore the first and last newline, otherwise
    // pasting naively would result in something like:
    // <p></p>
    // <p>foo</p>
    // <p>|</p>
    verifyMutationsLC("{++\"foo\"; }",
        buildTokens(token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE)));
  }

  public void testMultipleLines() {
    verifyMutationsLC("{++\"foo\"; << line {}; >>; ++\"bar\"; }",
        buildTokens(token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "bar"),
            token(RichTextTokenizer.Type.NEW_LINE)));

    verifyMutationsLC("{++\"one\"; << line {}; >>; ++\"two\"; << line {}; >>; ++\"three\"; }",
        buildTokens(token(RichTextTokenizer.Type.TEXT, "one"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "two"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "three")));
  }

  public void testExtraSpaces() {
    verifyMutationsLC("{++\"foo\"; << line {}; >>; ++\"bar\"; }",
        buildTokens(token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "bar"),
            token(RichTextTokenizer.Type.NEW_LINE)));
  }

  public void testSplits() {
    verifyMutationsLC("{++\"foo\"; << line {}; >>; }",
        buildTokens(token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.NEW_LINE)));

    verifyMutationsLC("{++\"foo\"; << line {}; >>; ++\"bar\"; }",
        buildTokens(token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "bar"),
            token(RichTextTokenizer.Type.NEW_LINE)));

    verifyMutationsLC("{++\"foo\"; << line {}; >>; ++\"bar\"; << line {}; >>; ++\"baz\"; }",
        buildTokens(token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "bar"),
            token(RichTextTokenizer.Type.NEW_LINE),
            token(RichTextTokenizer.Type.TEXT, "baz")));
  }

  public void testHeading() {
    verifyMutationsLC("{++\"foo\"; }",
        buildTokens(token(RichTextTokenizer.Type.NEW_LINE, "h1"),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(RichTextTokenizer.Type.NEW_LINE)));
  }

  public void testLinks() {
    verifyMutations("{(( link/manual=\"http://www.google.com/\"; ++\"Goog\"; )) link/manual; }",
        buildTokens(token(RichTextTokenizer.Type.LINK_START, "http://www.google.com/"),
            token(RichTextTokenizer.Type.TEXT, "Goog"),
            token(RichTextTokenizer.Type.LINK_END)));

  }

  public void testStyles() {
    verifyStyle(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START,
        RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END, "style/fontWeight", "bold");
    verifyStyle(RichTextTokenizer.Type.STYLE_FONT_STYLE_START,
        RichTextTokenizer.Type.STYLE_FONT_STYLE_END, "style/fontStyle", "italic");
    verifyStyle(RichTextTokenizer.Type.STYLE_FONT_FAMILY_START,
        RichTextTokenizer.Type.STYLE_FONT_FAMILY_END,
        "style/fontFamily", "Times New Roman");
    verifyStyle(RichTextTokenizer.Type.STYLE_COLOR_START,
        RichTextTokenizer.Type.STYLE_COLOR_END,
        "style/color", "#FF00FF");
  }

  public void testInvalidStyles() {
    verifyMutations("{(( style/fontWeight=\"bold\"; ++\"foo\"; )) style/fontWeight; }",
        buildTokens(token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
            token(RichTextTokenizer.Type.TEXT, "foo")));

    verifyMutations("{(( style/fontWeight=\"bold\"; ++\"foo\"; )) style/fontWeight; }",
        buildTokens(token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
            token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
            token(RichTextTokenizer.Type.TEXT, "foo")));

    verifyMutations("{(( style/fontWeight=\"bold\"; )) style/fontWeight; }",
        buildTokens(token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_START, "bold"),
            token(RichTextTokenizer.Type.STYLE_FONT_WEIGHT_END)));
  }

  public void testList() {
//    verifyMutationsLC("{<< line {t=li}; >>; ++\"foo\"; }",
//        buildTokens(token(RichTextTokenizer.Type.LIST_ITEM),
//        token(RichTextTokenizer.Type.TEXT, "foo"),
//        token(RichTextTokenizer.Type.NEW_LINE)));

    verifyMutationsLC("{<< line {t=li}; >>; ++\"foo\"; << line {i=1}; >>; ++\"nested\"; "
        + "<< line {t=li}; >>; ++\"bar\"; }",
        buildTokens(
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "nested"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END)));

    verifyMutationsLC("{<< line {t=li}; >>; ++\"foo\"; << line {i=1}; >>; ++\"nested\"; "
        + "<< line {t=li}; >>; ++\"bar\"; << line {}; >>; ++\"after\"; }",
        buildTokens(
        token(RichTextTokenizer.Type.UNORDERED_LIST_START),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "foo"),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "nested"),
        token(RichTextTokenizer.Type.LIST_ITEM),
        token(RichTextTokenizer.Type.TEXT, "bar"),
        token(RichTextTokenizer.Type.UNORDERED_LIST_END),
        token(RichTextTokenizer.Type.NEW_LINE),
        token(RichTextTokenizer.Type.TEXT, "after")));
  }

  /**
   * Tests that html strings with styling is converted into correct annotations.
   * NOTE(user): This exercises code in document and tokenizer as well.
   */
  public void testAnnotations() {
    annotationTest("<?a 'style/fontWeight'='bold'?>abc<?a 'style/fontWeight'?>", "<b>abc</b>");
    annotationTest("<?a 'style/fontWeight'='bold'?>abcdef<?a 'style/fontWeight'?>",
        ("<b>ab<b>c</b>def</b>"));

    // Test that default styles are ignored.
    annotationTest("abcdef",
        "abc<span style='textDecoration: none;'>def</span>");
    annotationTest("abcd<?a 'style/textDecoration'='underline'?>e<?a 'style/textDecoration'?>f",
        "abc<span style='textDecoration: none;'>d<u>e</u>f</span>");
    annotationTest(
        "n" +
        "<?a 'style/textDecoration'='underline'?>u" +
        "<?a 'style/textDecoration'='overline'?>o" +
        "<?a 'style/textDecoration'='none'?>n" +
        "<?a 'style/textDecoration'='overline'?>o" +
        "<?a 'style/textDecoration'='underline'?>u" +
        "<?a 'style/textDecoration'?>n",

        "n" +
        "<u>u" +
        "<span style='textDecoration: overline;'>o" +
        "<span style='textDecoration: none;'>n" +
        "</span>o" +
        "</span>u" +
        "</u>n");
  }

  private ElementStyleView<Node, Element, Text> parseDocumentContents(String xmlString) {
    return new RawElementStyleView(
        RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER).parse(
            "<div>" + xmlString + "</div>"));
  }

  private Pair<Nindo, IndexedDocument<Node, Element, Text>> applyTokensToEmptyDoc(
      RichTextTokenizer tokens) {
    IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("<body><line/></body>");
    Point<Node> insertAt = doc.locate(3);

    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(3);
    new RichTextMutationBuilder().applyMutations(tokens, builder, doc, insertAt.getContainer());

    Nindo nindo = builder.build();
    try {
      doc.consumeAndReturnInvertible(nindo);
    } catch (OperationException e) {
      fail("Operation Exception " + e);
    }

    return new Pair<Nindo, IndexedDocument<Node,Element,Text>>(nindo, doc);
  }

  /**
   * Asserts that the expectedContent is produced from srcHtml.
   *
   * Single quotes are used as they do not require escaping and makes the
   * strings more readable.
   *
   * @param expectedContent expected content with annotation key/value in single
   *        quotes.
   * @param srcHtml source html with attributes in single quotes.
   */
  private void annotationTest(String expectedContent, String srcHtml) {
    // The parser implementation does not accept single quotes, so we must
    // convert.
    RichTextTokenizerImpl<Node, Element, Text> tokenizer =
        new RichTextTokenizerImpl<Node, Element, Text>(parseDocumentContents(srcHtml));

    Pair<Nindo, IndexedDocument<Node, Element, Text>> result = applyTokensToEmptyDoc(tokenizer);
    ReadableWDocument<Node, Element, Text> doc = result.second;

    DocCompare.equivalent(DocCompare.ALL, expectedContent, result.second);
  }

  private void verifyMutations(String opString, RichTextTokenizer tokens) {
    verifyMutationsLC(opString, tokens);
  }

  private void verifyStyle(RichTextTokenizer.Type start, RichTextTokenizer.Type end,
      String key, String value) {
    verifyMutations("{(( " + key + "=\"" + value + "\"; ++\"foo\"; )) " + key + "; }",
        buildTokens(token(start, value),
            token(RichTextTokenizer.Type.TEXT, "foo"),
            token(end)));
  }

  private void verifyMutationsLC(String opString, RichTextTokenizer tokens) {
    Pair<Nindo, IndexedDocument<Node, Element, Text>> result = applyTokensToEmptyDoc(tokens);
    Nindo nindo = Nindo.shift(-3, result.first);
    if (!opString.equals(nindo.toString())) {
      System.out.println("ACTUAL: " + nindo);
      System.out.println("EXPECT: " + opString);
      throw new AssertionError(opString + ", " + nindo);
    }
    assertEquals(opString, nindo.toString());
  }

  private RichTextTokenizer buildTokens(Token ... tokens) {
    MockRichTextTokenizer mockTokenizer = new MockRichTextTokenizer();
    for (Token token : tokens) {
      mockTokenizer.expectToken(token);
    }
    return mockTokenizer;
  }

  private static Token token(RichTextTokenizer.Type type, String data) {
    return new Token(type, data);
  }

  private static Token token(RichTextTokenizer.Type type) {
    return new Token(type, null);
  }
}
