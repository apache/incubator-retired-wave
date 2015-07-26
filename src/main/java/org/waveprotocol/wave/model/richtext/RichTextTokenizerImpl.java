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

import org.waveprotocol.wave.model.document.util.ElementStyleView;
import org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type.TypeGroup;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * W3C implementation of the RichTextTokenizer.
 *
 * TODO(user): Optimize by changing this to a SAX-like implementation
 * instead of DOM. Ino ther words, iteratively visit every node instead of
 * pre-processing into a token list.
 *
 * TODO(user): Turn the static "isThis, isThat" helper functions
 * into a more data-driven mapping system.
 *
 * TODO(user): Add support for lists, headers, images and custom
 * style tags on block elements.
 *
 */
public class RichTextTokenizerImpl<N, E extends N, T extends N> implements RichTextTokenizer {
  /**
   * Internal token class, exposed via get methods on the parent class.
   */
  static class Token {
    private final Type type;

    private final String data;

    Token(Type type) {
      this(type, null);
    }

    Token(Type type, String data) {
      this.type = type;
      this.data = data;
    }

    Type getType() {
      return type;
    }

    String getData() {
      return data;
    }

    @Override
    public String toString() {
      return "(" + type + "," + data + ")";
    }
  }

  /**
   * Contains data required to extract a particular type of style token from an
   * element.
   */
  private static class StyleTokenExtractor {
    final Type tokenStartType;
    final Type tokenEndType;
    final StringMap<String> tagToValue;
    final String stylePropertyName;

    private StyleTokenExtractor(Type tokenStartType, Type tokenEndType,
        StringMap<String> tagToValue, String stylePropertyName) {
      this.tokenStartType = tokenStartType;
      this.tokenEndType = tokenEndType;
      this.tagToValue = tagToValue;
      this.stylePropertyName = stylePropertyName;
    }
  }

  /**
   * Mapping from tag names to corresponding token value.
   */
  private static final StringMap<String> fontWeightMap;
  private static final StringMap<String> fontStyleMap;
  private static final StringMap<String> textDecorationMap;

  static {
    fontWeightMap = CollectionUtils.createStringMap();
    fontWeightMap.put("b", "bold");
    fontWeightMap.put("strong", "bold");

    fontStyleMap = CollectionUtils.createStringMap();
    fontStyleMap.put("i", "italic");
    fontStyleMap.put("em", "italic");

    textDecorationMap = CollectionUtils.createStringMap();
    textDecorationMap.put("u", "underline");
  }

  private static final StyleTokenExtractor FONT_WEIGHT_HANDLER =
      new StyleTokenExtractor(Type.STYLE_FONT_WEIGHT_START, Type.STYLE_FONT_WEIGHT_END,
          fontWeightMap, "fontWeight");

  private static final StyleTokenExtractor FONT_STYLE_HANDLER =
      new StyleTokenExtractor(Type.STYLE_FONT_STYLE_START, Type.STYLE_FONT_STYLE_END, fontStyleMap,
          "fontStyle");

  private static final StyleTokenExtractor TEXT_DECORATION_HANDLER =
      new StyleTokenExtractor(Type.STYLE_TEXT_DECORATION_START, Type.STYLE_TEXT_DECORATION_END,
          textDecorationMap, "textDecoration");

  private final List<Token> tokenList;

  private final int[] activeTokenCounts;

  protected final ElementStyleView<N, E, T> document;

  private int tokenIndex = -1;

  private boolean endBlockPending = false;

  private E root = null;

  private boolean mergeNextNewLine = false;

  /**
   * Creates a tokenizer and parses the inner contents of an Element.
   *
   * @param doc The readable document that will be parsed.
   */
  public RichTextTokenizerImpl(ElementStyleView<N, E, T> doc) {
    document = doc;
    tokenList = new ArrayList<Token>();
    activeTokenCounts = new int[Type.values().length];
    for (int i = 0; i < activeTokenCounts.length; ++i) {
      activeTokenCounts[i] = 0;
    }
    process(doc.getDocumentElement());
  }

  private RichTextTokenizerImpl(RichTextTokenizerImpl<N, E, T> o) {
    activeTokenCounts = new int[o.activeTokenCounts.length];
    for (int i = 0; i < o.activeTokenCounts.length; ++i) {
      activeTokenCounts[i] = o.activeTokenCounts[i];
    }
    tokenList = new ArrayList<Token>(o.tokenList);
    document = o.document;
    tokenIndex = o.tokenIndex;
    endBlockPending = o.endBlockPending;
    root = o.root;
    mergeNextNewLine = o.mergeNextNewLine;
  }

  @Override
  public RichTextTokenizer copy() {
    return new RichTextTokenizerImpl<N, E, T>(this);
  }

  @Override
  public boolean hasNext() {
    return tokenIndex < tokenList.size() - 1;
  }

  @Override
  public Type next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    ++tokenIndex;
    return getCurrentToken().getType();
  }

  @Override
  public Type getCurrentType() {
    return getCurrentToken().getType();
  }

  @Override
  public String getData() {
    return getCurrentToken().getData();
  }

  private Token getCurrentToken() {
    if (tokenIndex >= tokenList.size()) {
      throw new IllegalStateException("No token available.");
    }
    return tokenList.get(tokenIndex);
  }

  final protected void process(E container) {
    if (container == null) {
      throw new IllegalArgumentException();
    }
    root = container;
    tokenList.clear();
    tokenIndex = -1;

    N prevNode = null;
    for (N child = document.getFirstChild(root); child != null;
        child = document.getNextSibling(child)) {
      processNode(child, prevNode);
      prevNode = child;
    }

    // Sanity check, all counters should be zero.
    for (int j = 0 ; j < activeTokenCounts.length; ++j) {
      assert activeTokenCounts[j] == 0;
    }
  }

  private void processNode(N node, N leftSibling) {
    T textNode = document.asText(node);
    if (textNode != null) {
      processTextNode(textNode, leftSibling);
      return;
    }

    E element = asElement(node);
    if (element != null) {
      processElement(element, leftSibling);
    }
  }

  final void processTextNode(T textNode, N leftSibling) {
    if (endBlockPending) {
      maybeInsertNewline();
      endBlockPending = false;
    }
    processTextNodeInner(textNode, leftSibling);
  }

  protected void processTextNodeInner(T textNode, N leftSibling) {
    String text = document.getData(textNode);
    if (!text.isEmpty()) {
      addTextToken(text);
    }
  }

  final void addTextToken(String text) {
    StringBuilder builder = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); ++i) {
      char ch = text.charAt(i);
      if (ch == '\u00a0') {
        builder.append(' ');
      } else if (ch == '\t') {
        builder.append("    ");
      } else if (ch == '\n') {
        if (builder.length() != 0) {
          addToken(new Token(Type.TEXT, builder.toString()));
        }
        addToken(new Token(Type.NEW_LINE));
        builder = new StringBuilder(text.length() - i);
      } else {
        builder.append(ch);
      }
    }
    if (builder.length() != 0) {
      addToken(new Token(Type.TEXT, builder.toString()));
    }
  }

  private void processElement(E element, N leftSibling) {
    String tagName = document.getTagName(element).toLowerCase();

    List<Type> closingTypeStack = new ArrayList<Type>(); // must only contain annotation closures
    boolean maybeEndParagraph = false;

    boolean setEndBlockPending = false;

    if (isBlockElement(tagName)) {
      if (isListItem(tagName)) {
        addToken(new Token(Type.LIST_ITEM));
      } else {
        // TODO(user): This will always ensure that there cannot be
        // nested paragraphs. However, because <p> and <div> are treated the
        // same, nested divs will result in extra paragraphs. The solution
        // is to flatten the divs when no renderable content exists.
        if (!ignorableBlock(element, tagName)) {
          maybeInsertNewline();
          maybeEndParagraph = true;
          setEndBlockPending = true;
        }
      }
    } else if (isNewline(tagName)) {
      // Special case - if this is the last line break in a paragraph, just
      // ignore it.
      if (!isLastLinebreak(element)) {
        addToken(new Token(Type.NEW_LINE));
        mergeNextNewLine = false;
      }
    } else if (isHeading(tagName)) {
      addToken(new Token(Type.NEW_LINE, tagName));
      maybeEndParagraph = true;
      setEndBlockPending = true;

    } else if (isList(tagName)) {
      putIfNotNull(closingTypeStack, handleListElement(element, tagName));
      maybeEndParagraph = false;
      setEndBlockPending = true;

    } else if (isTableRelated(tagName)) {
      // TODO(patcoleman): temporary table rendering, replace with real tables once supported.
      if (isTable(tagName)) {
        addToken(new Token(Type.NEW_LINE));
      } else if (isTableRow(tagName)) {
        maybeEndParagraph = true;
        setEndBlockPending = true;
      } else if (isTableCell(tagName)) {
        addToken(new Token(Type.TEXT, " "));
      }

    } else {
      putIfNotNull(closingTypeStack, handleLinkElement(element, tagName));
    }

    handleStyleElements(element, tagName, closingTypeStack);

    // Recursively iterate children.
    N prevNode = null;
    for (N child = document.getFirstChild(element); child != null;
        child = document.getNextSibling(child)) {
      processNode(child, prevNode);
      prevNode = child;
    }

    while (!closingTypeStack.isEmpty()) {
      Type closingType = closingTypeStack.remove(closingTypeStack.size() - 1);
      addToken(new Token(closingType));
      decrementTypeCounter(closingType);
    }

    endBlockPending |= setEndBlockPending;

    if (maybeEndParagraph && endBlockPending) {
      maybeInsertNewline();
      endBlockPending = false;
    }
  }

  private <E> void putIfNotNull(List<E> list, E item) {
    if (item != null) {
      list.add(item);
    }
  }

  private boolean maybeInsertNewline() {
    // Only add the newline if we didn't just end a block with a close tag.
    if (!mergeNextNewLine) {
      addToken(new Token(Type.NEW_LINE));
      return true;
    }
    return false;
  }

  /**
   * Checks the element for various style properties in its tag name or css styles.
   * All styles found generate a starting token, plus add an end token to the closing stack.
   */
  private void handleStyleElements(E el, String tagName, List<Type> closeStack) {
    Type startType = null;
    Type endType = null;
    String data = null;

    // Styles supported here: bold, italic, font colour, background colour, font family.
    maybeExtractStyleToken(el, tagName, closeStack, FONT_WEIGHT_HANDLER);
    maybeExtractStyleToken(el, tagName, closeStack, FONT_STYLE_HANDLER);
    maybeExtractStyleToken(el, tagName, closeStack, TEXT_DECORATION_HANDLER);

    if (isColor(el)) {
      String value = document.getStylePropertyValue(el, "color");
      addTokenOrIncrement(true, new Token(Type.STYLE_COLOR_START, value), Type.STYLE_COLOR_END);
      closeStack.add(Type.STYLE_COLOR_END);
    }
    if (isBackgroundColor(el)) {
      String value = document.getStylePropertyValue(el, "backgroundColor");
      addTokenOrIncrement(true,
          new Token(Type.STYLE_BG_COLOR_START, value), Type.STYLE_BG_COLOR_END);
      closeStack.add(Type.STYLE_BG_COLOR_END);
    }
    if (isFontFamily(el)) {
      String value = document.getStylePropertyValue(el, "fontFamily");
      addTokenOrIncrement(true, new Token(Type.STYLE_FONT_FAMILY_START, value),
          Type.STYLE_FONT_FAMILY_END);
      closeStack.add(Type.STYLE_FONT_FAMILY_END);
    }
  }

  private Type handleLinkElement(E el, String tagName) {
    if (isLink(tagName)) {
      String attr = document.getAttribute(el, "href");
      if (attr != null) {
        addTokenOrIncrement(new Token(Type.LINK_START, attr), Type.LINK_END);
        return Type.LINK_END;
      }
    }
    return null;
  }

  private Type handleListElement(E element, String tagName) {
    if (isOrderedList(tagName)) {
      addTokenOrIncrement(true, new Token(Type.ORDERED_LIST_START), Type.ORDERED_LIST_END);
      return Type.ORDERED_LIST_END;
    } else if (isUnorderedList(tagName)) {
      addTokenOrIncrement(true, new Token(Type.UNORDERED_LIST_START), Type.UNORDERED_LIST_END);
      return Type.UNORDERED_LIST_END;
    } else {
      return null;
    }
  }

  private void addToken(Token token) {
    if (token.getType().isStructural()) {
      mergeNextNewLine = token.getType().group() == TypeGroup.BLOCK;
    }
    tokenList.add(token);
  }

  private void addTokenOrIncrement(Token token, Type endType) {
    addTokenOrIncrement(false, token, endType);
  }

  private void addTokenOrIncrement(boolean replace, Token token, Type endType) {
    if (replace || !isTypeInUse(endType)) {
      addToken(token);
    }
    incrementTypeCounter(endType);
  }

  private E asElement(N node) {
    if (node != null) {
      return document.asElement(node);
    }
    return null;
  }

  /**
   * Checks if this is the last line break before a new paragraph.
   */
  private boolean isLastLinebreak(E element) {
    N sibling = document.getNextSibling(element);
    // If we are not the last child and do not border a block element,
    // then we cannot ignore this linebreak.
    if (sibling != null) {
      E el = asElement(sibling);
      if (el == null || !isBlockElement(document.getTagName(el))) {
        return false;
      }
    }

    // Make sure we're not deeply nested.
    return getDepthFromBlock(element) == 1;
  }

  private boolean ignorableBlock(E element, String tagName) {
    // Ignore empty divs.
    if ("div".equalsIgnoreCase(tagName)) {
      if (document.getFirstChild(element) == null) {
        return true;
      }
      /*
      int offsetHeight = element.getOffsetHeight();
      if (element.getOffsetHeight() == 0) {
        return true;
      }
      */
    }
    return false;
  }

  private int getDepthFromBlock(N node) {
    int depth = 1;
    E e = document.getParentElement(node);
    while (e != null) {
      if (e == root || isBlockElement(document.getTagName(e))) {
        break;
      }
      depth++;
      e = document.getParentElement(e);
    }
    return depth;
  }

  private boolean isTypeInUse(Type type) {
    return activeTokenCounts[type.ordinal()] > 0;
  }

  private void incrementTypeCounter(Type type) {
    ++activeTokenCounts[type.ordinal()];
  }

  private boolean decrementTypeCounter(Type type) {
    int newValue = --activeTokenCounts[type.ordinal()];
    assert newValue >= 0;
    return newValue == 0;
  }

  private static boolean isBlockElement(String tagName) {
    return "p".equalsIgnoreCase(tagName) ||
        "div".equalsIgnoreCase(tagName) || isListItem(tagName);
  }

  private static boolean isNewline(String tagName) {
    return "br".equalsIgnoreCase(tagName);
  }

  private void maybeExtractStyleToken(E el, String tagName, List<Type> closeStack,
      StyleTokenExtractor tokenHandler) {
    String styleProperty = getStyleProperty(el, tagName, tokenHandler);

    if (styleProperty != null) {
      addTokenOrIncrement(true, new Token(tokenHandler.tokenStartType, styleProperty),
          tokenHandler.tokenEndType);
      closeStack.add(tokenHandler.tokenEndType);
    }
  }

  private String getStyleProperty(E el, String tagName, StyleTokenExtractor tokenHandler) {
    String value = document.getStylePropertyValue(el, tokenHandler.stylePropertyName);
    if (value != null && !value.isEmpty()) {
      return value;
    }

    String lowerCaseTag = tagName.toLowerCase();
    return tokenHandler.tagToValue.containsKey(lowerCaseTag) ? tokenHandler.tagToValue
        .get(lowerCaseTag) : null;
  }

  private boolean isColor(E el) {
    return isStylePropertySet(el, "color");
  }

  private boolean isBackgroundColor(E el) {
    return isStylePropertySet(el, "backgroundColor");
  }

  private boolean isFontFamily(E el) {
    return isStylePropertySet(el, "fontFamily");
  }

  private static boolean isHeading(String tagName) {
    if (tagName.length() == 2) {
      if (tagName.charAt(0) == 'h' || tagName.charAt(0) == 'H') {
        int size = tagName.charAt(1) - '0';
        if (size >= 1 && size <= 4) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isLink(String tagName) {
    return "a".equalsIgnoreCase(tagName);
  }

  private static boolean isListItem(String tagName) {
    return "li".equalsIgnoreCase(tagName);
  }

  private static boolean isList(String tagName) {
    return isOrderedList(tagName) || isUnorderedList(tagName);
  }

  private static boolean isOrderedList(String tagName) {
    return "ol".equalsIgnoreCase(tagName);
  }

  private static boolean isUnorderedList(String tagName) {
    return "ul".equalsIgnoreCase(tagName);
  }

  private static boolean isTable(String tagName) {
    return "table".equalsIgnoreCase(tagName);
  }

  private static boolean isTableRow(String tagName) {
    return "tr".equalsIgnoreCase(tagName);
  }

  private static boolean isTableCell(String tagName) {
    return "th".equalsIgnoreCase(tagName) || "td".equalsIgnoreCase(tagName);
  }

  private static boolean isTableRelated(String tagName) {
    // TODO(patcoleman): fix up table implementation once tables supported in the editor.
    // When this happens, also extract out strings into symbolic constants.
    return isTable(tagName) || isTableRow(tagName) || isTableCell(tagName) ||
           "thead".equalsIgnoreCase(tagName) ||
           "tbody".equalsIgnoreCase(tagName);
  }

  private boolean isStylePropertySet(E el, String property) {
    String value = document.getStylePropertyValue(el, property);
    return value != null && !value.isEmpty();
  }

  @Override
  public String toString() {
    return tokenList.toString();
  }
}
