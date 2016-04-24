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

package org.waveprotocol.wave.model.document.parser;

import com.google.common.annotations.VisibleForTesting;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * Streaming xml parser implementation.
 *
 */
class StreamingXmlParser implements XmlPullParser {
  /**
   * Buffer for input string.
   */
  private static class Buffer {
    int position;
    private final String str;

    private Buffer(String str) {
      this.str = str;
    }

    private int peek() throws XmlParseException {
      if (position >= str.length()) {
        throw new XmlParseException("Reading past end of input.");
      }

      return str.codePointAt(position);
    }

    private void advanceCodeUnit(int i) {
      position += i;
    }

    private void advanceCodePoint() {
      if (Utf16Util.isHighSurrogate(str.charAt(position))) {
        position += 2;
      } else {
        position++;
      }
    }

    private boolean startsWith(String s) {
      if (str.length() < position + s.length()) {
        return false;
      }

      for (int i = 0; i < s.length(); ++i) {
        if (s.charAt(i) != str.charAt(i + position)) {
          return false;
        }
      }
      return true;
    }

    private int getPosition() {
      return position;
    }

    private String substring(int start, int end) {
      return str.substring(start, end);
    }

    private boolean hasMore() {
      boolean hasMore = position != str.length();
      return hasMore;
    }
  }

  /** Map containing the entities to be replaced. */
  final static ReadableStringMap<String> entities;
  static {
    StringMap<String> tmp = CollectionUtils.createStringMap();
    tmp.put("lt", "<");
    tmp.put("gt", ">");
    tmp.put("apos", "'");
    tmp.put("quot", "\"");
    tmp.put("amp", "&");
    entities = tmp;
  }

  private static final char openAngle = '<';
  private static final char closeAngle = '>';
  private static final String slashCloseAngle = "/>";
  private static final String openAngleSlash = "</";
  private static final char eq = '=';
  private static final char singleQuote = '\'';
  private static final char doubleQuote = '"';
  private static final String piStart = "<?";
  private static final String piEnd = "?>";
  private static final String charReferenceStart = "&#";
  private static final ReadableStringSet BASE_16_DIGITS =
      CollectionUtils.newStringSet(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "b", "c", "d", "e", "f",
        "A", "B", "C", "D", "E", "F");
  private static final ReadableStringSet BASE_10_DIGITS =
      CollectionUtils.newStringSet("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

  /** Input buffer */
  private final Buffer buffer;

  /**
   * Stack of elements thats been started.
   */
  private final Stack<String> elementTagStack = new Stack<String>();

  /**
   * Indicates if we are at the start of a self-closing tag.
   */
  private boolean atSelfClosingStart = false;

  private Item current;

  StreamingXmlParser(String xml) throws XmlParseException {
    // Ensure that the input is valid UTF-16 here, makes rest of the code
    // simpler
    if (Utf16Util.isValidUtf16(xml)) {
      buffer = new Buffer(xml);
    } else {
      throw new XmlParseException("Input is not valid UTF-16: " + xml);
    }
  }

  //// Production Rules

  private String whitespace() throws XmlParseException {
    int start = buffer.getPosition();
    if (!isWhiteSpaceChar(buffer.peek())) {
      return null;
    }

    while (buffer.hasMore() && isWhiteSpaceChar(buffer.peek())) {
      buffer.advanceCodeUnit(1);
    }
    int end = buffer.getPosition();
    return buffer.substring(start, end);
  }

  private boolean isXmlNameStartChar(int codePoint) throws XmlParseException {
    try {
      return Utf16Util.isXmlNameStartChar(codePoint);
    } catch (RuntimeException e) {
      throw new XmlParseException(e);
    }
  }

  private boolean isXmlNameChar(int codePoint) throws XmlParseException {
    try {
      return Utf16Util.isXmlNameChar(codePoint);
    } catch (RuntimeException e) {
      throw new XmlParseException(e);
    }
  }

  @VisibleForTesting
  String name() throws XmlParseException {
    String ret;
    int start = buffer.getPosition();
    if (!isXmlNameStartChar(buffer.peek())) {
      return null;
    }
    buffer.advanceCodeUnit(1);
    while (isXmlNameChar(buffer.peek())) {
      buffer.advanceCodeUnit(1);
    }
    int end = buffer.getPosition();
    return buffer.substring(start, end);
  }

  @VisibleForTesting
  StringMap<String> attrList() throws XmlParseException {
    StringMap<String> ret = null;
    whitespace();
    while (buffer.hasMore()) {
      Pair<String, String> attr = attr();
      if (attr == null) {
        break;
      } else {
        ret = (ret != null) ? ret : CollectionUtils.<String>createStringMap();
        ret.put(attr.first, attr.second);
      }
      whitespace();
    }
    return ret;
  }

  @VisibleForTesting
  Pair<String, String> attr() throws XmlParseException {
    String attrName = name();
    if (attrName == null) {
      return null;
    }
    ensure(match(eq), "Matching =");

    String attrValue = attrValue();
    ensure(attrValue != null, "Matching attr value");
    return new Pair<String, String>(attrName, attrValue);
  }

  private boolean contains(char except[], int c) {
    for (char i : except) {
      if (i == c) {
        return true;
      }
    }
    return false;
  }

  // AttValue ::= '"' ([^<&"] | Reference)* '"'
  //            | "'" ([^<&'] | Reference)* "'"
  @VisibleForTesting
  String attrValue() throws XmlParseException {
    if (match(doubleQuote)) {
      return attrValueInner(doubleQuote);
    } else if (match(singleQuote)) {
      return attrValueInner(singleQuote);
    } else {
      throw new XmlParseException("Cannot match opening quote " + buffer.getPosition());
    }
  }

  private String attrValueInner(char quote) throws XmlParseException {
    char except[] = {'<', '&', quote};

    StringBuilder ret = new StringBuilder();
    while (true) {
      int start = buffer.getPosition();
      while (!contains(except, buffer.peek())) {
        buffer.advanceCodePoint();
      }
      int end = buffer.getPosition();

      if (start != end) {
        assert start < end;
        ret.append(buffer.substring(start, end));
        continue;
      }

      String reference = reference();
      if (reference != null) {
        ret.append(reference);
        continue;
      }

      ensure(match(quote), "matching closing quote");
      String val = ret.toString();
      // Ensure that the sequence of character references form valid UTF-16
      ensure(Utf16Util.isValidUtf16(val), "Not valid UTF-16: " + val);
      return val;
    }
  }

  @VisibleForTesting
  String entityReference() throws XmlParseException {
    if (match('&')) {
      String entityName = name();
      ensure(entityName != null, "parsing entity name");
      ensure(match(';'), "matching ; on entity");
      String entityAsString = entities.get(entityName);
      if (entityAsString != null) {
        return entityAsString;
      } else {
        throw new XmlParseException("entity name not recognised: " + entityName);
      }
    } else {
      return null;
    }
  }

  @VisibleForTesting
  String reference() throws XmlParseException {
    String charReference = charReference();
    if (charReference != null) {
      return charReference;
    }

    String entityReference = entityReference();
    if (entityReference != null) {
      return entityReference;
    }
    return null;
  }

  @VisibleForTesting
  String charReference() throws XmlParseException {
    if (match(charReferenceStart)) {
      final int base;
      final ReadableStringSet validDigits;
      if (match('x')) {
        base = 16;
        validDigits = BASE_16_DIGITS;
      } else {
        base = 10;
        validDigits = BASE_10_DIGITS;
      }
      final int start = buffer.getPosition();
      while (validDigits.contains("" + (char) buffer.peek())) {
        buffer.advanceCodeUnit(1);
      }
      final int end = buffer.getPosition();
      ensure(start != end, "empty char reference");
      final int codePoint;
      final String number = buffer.substring(start, end);
      try {
        codePoint = Integer.parseInt(number, base);
      } catch (NumberFormatException e) {
        throw new XmlParseException("Could not parse number: " + number, e);
      }
      ensure(Utf16Util.isCodePoint(codePoint), "Not a codepoint: " + (base == 16 ? "0x" : "")
          + number);
      String ret = String.valueOf(Character.toChars(codePoint));
      ensure(match(';'), "Must match ; at end of charReference");
      return ret;
    }
    return null;
  }

  // element   ::=      '<' Name (S Attribute)* S? '>'
  //                |   '<' Name (S Attribute)* S? '/>'
  @VisibleForTesting
  Item startTag() throws XmlParseException {
    ensure(match(openAngle), "Matching <");
    String tagName = name();
    ensure(tagName != null, "Matching name");
    StringMap<String> attrList = attrList();
    whitespace();
    if (match(slashCloseAngle)) {
      atSelfClosingStart = true;
      return Item.elementStart(tagName, attrList);
    } else if (match(closeAngle)){
      elementTagStack.add(tagName);
      return Item.elementStart(tagName, attrList);
    } else {
      throw new XmlParseException("Matching > or />");
    }
  }

  @VisibleForTesting
  Item endTag() throws XmlParseException {
    ensure(match(openAngleSlash), "Matching </");
    String closingName = name();
    ensure(closingName != null, "Matching name");
    whitespace();
    ensure(match(closeAngle), "Matching >");
    ensure(!elementTagStack.isEmpty(), "no matching start tag");
    String matchingStart = elementTagStack.pop();
    ensure(matchingStart.equals(closingName),
        "start and end tag mismatch, start: " + matchingStart
        + " end: " + closingName);
    return Item.elementEnd(closingName);
  }

  @VisibleForTesting
  Item processingInstruction() throws XmlParseException {
    ensure(match(piStart), "Matching <?");
    String name = name();
    ensure(
        name != null,
        "Processing instruction target must start with a valid xml nameStartChar, but instead encountered: "
            + (char) buffer.peek());
    whitespace();
    int start = buffer.getPosition();
    while (buffer.hasMore() && !buffer.startsWith(piEnd)) {
      buffer.advanceCodeUnit(1);
    }
    int end = buffer.getPosition();
    ensure(match(piEnd), "Matching ?>");

    return Item.processingInstruction(name, buffer.substring(start, end));
  }

  @VisibleForTesting
  String charData() throws XmlParseException {
    char except[] = {'<', '&'};
    int start = buffer.getPosition();
    while (buffer.hasMore()) {
      if (!contains(except, buffer.peek())) {
        buffer.advanceCodePoint();
      } else {
        break;
      }
    }
    int end = buffer.getPosition();
    return start == end ? null : buffer.substring(start, end);
  }

  //// Utility functions
  private boolean match(String t) {
    if (buffer.startsWith(t)) {
      buffer.advanceCodeUnit(t.length());
      return true;
    } else {
      return false;
    }
  }

  private boolean match(char t) {
    if (!buffer.hasMore()) {
      return false;
    }
    try {
      if (buffer.peek() == t) {
        buffer.advanceCodeUnit(1);
        return true;
      } else {
        return false;
      }
    } catch (XmlParseException e) {
      throw new RuntimeException("Impossible, as buffer has more: ", e);
    }
  }

  // S ::= (#x20 | #x9 | #xD | #xA)+
  private boolean isWhiteSpaceChar(int c) {
    return '\u0020' == c ||
           '\u0009' == c ||
           '\n' == c ||
           '\r' == c;
  }

  private void ensure(boolean condition, Object... messages) throws XmlParseException {
    if (!condition) {
      StringBuilder builder = new StringBuilder();
      builder.append("message: ");
      for (Object m : messages) {
        builder.append((String)m);
      }
      builder.append(", at position: ");
      builder.append(buffer.getPosition());
      throw new XmlParseException(builder.toString());
    }
  }

  @VisibleForTesting
  Item getTextChunk() throws XmlParseException {
    StringBuffer b = new StringBuffer();
    while (true) {
      String c = charData();
      if (c != null) {
        b.append(c);
        continue;
      }
      String r = reference();
      if (r != null) {
        b.append(r);
        continue;
      }
      if (b.length() > 0) {
        String val = b.toString();
        // Ensure that the sequence of character references form valid UTF-16
        ensure(Utf16Util.isValidUtf16(val), "Not valid UTF-16: " + val);
        return Item.text(val);
      } else {
        return null;
      }
    }
  }

  @VisibleForTesting
  Item getItem() throws XmlParseException {
    if (buffer.startsWith(piStart)) {
      return processingInstruction();
    } else if (buffer.startsWith(openAngleSlash)) {
      return endTag();
    } else if (buffer.peek() == openAngle) {
      return startTag();
    } else {
      return getTextChunk();
    }
  }

  @Override
  public boolean hasNext() {
    return buffer.hasMore() || atSelfClosingStart;
  }

  @Override
  public StringMap<String> getAttributes() throws IllegalStateException {
    return current.getAttributes();
  }

  @Override
  public ItemType getCurrentType() {
    return current == null ? null : current.type;
  }

  @Override
  public String getProcessingInstructionName() throws IllegalStateException {
    return current.getProcessingInstructionName();
  }

  @Override
  public String getProcessingInstructionValue() throws IllegalStateException {
    return current.getProcessingInstructionValue();
  }

  @Override
  public String getTagName() throws IllegalStateException {
    return current.getTagName();
  }

  @Override
  public String getText() throws IllegalStateException {
    return current.getText();
  }

  @Override
  public ItemType next() throws NoSuchElementException, XmlParseException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    if (atSelfClosingStart) {
      atSelfClosingStart = false;
      current = current.startElementToEndElement();
    } else {
      if (buffer.hasMore()) {
        current = getItem();
      } else {
        current = null;
      }
    }

    // Check that all tags are closed after input is fully consumed.
    if (!hasNext()) {
      ensure(elementTagStack.isEmpty(), "Start tags not closed, " + elementTagStack.size());
    }

    return getCurrentType();
  }

  /**
   * @return the current item. This returns a new item each time, no need to
   *         make defensive copies.
   */
  Item getCurrentItem() {
    return current.copy();
  }
}
