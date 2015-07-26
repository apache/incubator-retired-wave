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

import org.waveprotocol.wave.model.document.ReadableDocument;

import java.util.Map;

/**
 * Various pretty printing methods for dom structures
 *
 * TODO(user): include selection markers | []
 *
 * TODO(user): consider merging this with XmlStringBuilder, but
 * with care (this code will probably only exist in debug builds whereas
 * ContentXmlString will be in release builds as well...)
 *
 */
public class Pretty<N> {

  /**
   * A string builder for building up output
   */
  private StringBuilder builder = new StringBuilder();

  /**
   * Current indentation level
   */
  private int indent = 0;

  /**
   * Selection start + end
   */
  private Point.El<N> selStartEl = null, selEndEl = null;
  private Point.Tx<N> selStartTx = null, selEndTx = null;

  /**
   * Flag if selection is collapsed
   */
  private boolean collapsed = false;

  /**
   * Constructor
   */
  public Pretty() {}

  /**
   * Set selection for future prints
   *
   * @param selStart
   * @param selEnd
   * @return this
   */
  public Pretty<N> select(Point<N> selStart, Point<N> selEnd) {
    this.selStartEl = selStart != null ? selStart.asElementPoint() : null;
    this.selStartTx = selStart != null ? selStart.asTextPoint() : null;
    this.selEndEl = selEnd != null ? selEnd.asElementPoint() : null;
    this.selEndTx = selEnd != null ? selEnd.asTextPoint() : null;
    collapsed = selStart != null && selStart.equals(selEnd);
    return this;
  }

  /**
   * Clear selection
   *
   * @return this
   */
  public Pretty<N> noselect() {
    return select(null, null);
  }

  /**
   * @param doc
   * @return pretty-print string describing doc + selection
   */
  public <E extends N, T extends N> String print(ReadableDocument<N, E, T> doc) {
    clear();
    appendDocument(doc);
    return builder.toString();
  }

  /**
   * @param doc
   * @param node
   * @return pretty-print string describing node doc + selection
   */
  public <E extends N, T extends N> String print(ReadableDocument<N, E, T> doc, N node) {
    clear();
    appendNode(doc, node);
    return builder.toString();
  }


  /**
   * Clears the builder
   */
  private void clear() {
    indent = 0;
    if (builder.length() > 0) {
      builder = new StringBuilder();
    }
  }

  /**
   * Appends a char sequence to builder
   *
   * @param sequence
   */
  private void append(CharSequence sequence) {
    builder.append(sequence);
  }

  /**
   * @param c
   */
  private void append(char c) {
    builder.append(c);
  }

  /**
   * Appends a newline and spaces to indent next line
   */
  private void appendNewLine() {
    append("\n");
    for (int i = 0; i < indent; ++i) {
      append("  ");
    }
  }

  /**
   * Appends a document to builder
   *
   * @param doc
   */
  private <E extends N, T extends N>
      void appendDocument(ReadableDocument<N, E, T> doc) {
    appendElement(doc, doc.getDocumentElement());
  }

  /**
   * @param doc
   * @param node
   * @return true if node prefers to be output inline
   */
  private <E extends N, T extends N> boolean isInline(
      ReadableDocument<N, E, T> doc, N node) {
    E element = doc.asElement(node);
    T text = doc.asText(node);
    return text != null || "|b|u|i|".contains("|" + doc.getTagName(element).toLowerCase() + "|");
  }

  /**
   * Appends a node to builder
   *
   * @param doc
   * @param node
   */
  private <E extends N, T extends N> void appendNode(
      ReadableDocument<N, E, T> doc, N node) {
    E element = doc.asElement(node);
    if (element != null) {
      appendElement(doc, element);
      return;
    }
    T text = doc.asText(node);
    if (text != null) {
      appendText(doc, text);
      return;
    }
    assert(false);
  }

  /**
   * Appends element's tag name
   *
   * @param doc
   * @param element
   */
  private <E extends N, T extends N> void appendTagName(
      ReadableDocument<N, E, T> doc, E element) {
    append(doc.getTagName(element).toLowerCase());
  }

  /**
   * Appends element's attributes
   *
   * @param doc
   * @param element
   */
  private <E extends N, T extends N> void appendAttributes(
      ReadableDocument<N, E, T> doc, E element) {
    try {
      Map<String, String> attributes = doc.getAttributes(element);
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        append(" " + attribute.getKey() + "='" + attribute.getValue() + "'");
      }
    } catch (Exception e) {
      // TODO(user): remove this when + if HtmlViewImpl implements getAttributes
      for (String name : new String[] {"class", "src", "id", "type", "name", "for", "href",
          "target"}) {
        String value = doc.getAttribute(element, name);
        if (value != null && value.length() > 0) {
          append(" " + name + "='" + value + "'");
        }
      }
    }
  }

  /**
   * Appends an element start tag
   *
   * @param doc
   * @param element
   */
  private <E extends N, T extends N> void appendStartTag(
      ReadableDocument<N, E, T> doc, E element) {
    appendStartTag(doc, element, false);
  }

  /**
   * Appends a potentially self-closing element start tag
   *
   * @param doc
   * @param element
   * @param selfClosing
   */
  private <E extends N, T extends N> void appendStartTag(
      ReadableDocument<N, E, T> doc, E element, boolean selfClosing) {
    append("<");
    appendTagName(doc, element);
    appendAttributes(doc, element);
    append(selfClosing ? "/>" : ">");
  }

  /**
   * Appends an element end tag
   *
   * @param doc
   * @param element
   */
  private <E extends N, T extends N> void appendEndTag(
      ReadableDocument<N, E, T> doc, E element) {
    append("</");
    appendTagName(doc, element);
    append(">");
  }

  /**
   * Appends an element
   *
   * @param doc
   * @param element
   */
  private <E extends N, T extends N> void appendElement(
      ReadableDocument<N, E, T> doc, E element) {

    // Inspect selection's relevance to element
    boolean printStartMarker =
      selStartEl != null && doc.isSameNode(selStartEl.getContainer(), element);
    boolean printEndMarker =
      selEndEl != null && !collapsed && doc.isSameNode(selEndEl.getContainer(), element);
    String startMarker = collapsed ? "|" : "[";
    String endMarker = "]";

    // First deal with childless elements
    N firstChild = doc.getFirstChild(element);
    if (firstChild == null) {

      if (printStartMarker && selStartEl.getNodeAfter() == null) {
        appendStartTag(doc, element);
        append(startMarker);
        appendEndTag(doc, element);
      } else if (printEndMarker && selEndEl.getNodeAfter() == null) {
        appendStartTag(doc, element);
        append(endMarker);
        appendEndTag(doc, element);
      } else {
        appendStartTag(doc, element, true);
      }

    } else {

      // Start tag
      appendStartTag(doc, element);

      // Children
      N child = firstChild;
      boolean first = true;
      ++indent;
      while (child != null) {
        N next = doc.getNextSibling(child);
        if ((first && !isInline(doc, element)) || !isInline(doc, child)) {
          appendNewLine();
        }
        if (printStartMarker && doc.isSameNode(selStartEl.getNodeAfter(), child)) {
          append(startMarker);
        }
        appendNode(doc, child);
        if (printEndMarker && doc.isSameNode(selEndEl.getNodeAfter(), next)) {
          append(endMarker);
        }
        first = false;
        child = next;
      }
      --indent;

      // End tag
      if ((!isInline(doc, element)) || !isInline(doc, firstChild)) {
        appendNewLine();
      }
      appendEndTag(doc, element);
    }
  }

  /**
   * Appends a text node
   *
   * @param doc
   * @param text
   */
  private <E extends N, T extends N> void appendText(
      ReadableDocument<N, E, T> doc, T text) {

    // The text value to append
    String value = displayWhitespace(doc.getData(text));

    // Inspect selection's relevance to text node
    boolean printStartMarker =
      selStartTx != null && doc.isSameNode(selStartTx.getContainer(), text);
    boolean printEndMarker =
      selEndTx != null && !collapsed && doc.isSameNode(selEndTx.getContainer(), text);
    String startMarker =
        printStartMarker ? (collapsed ? "|" : "[") : "";
    String endMarker = printEndMarker ? "]" : "";
    int startOffset = printStartMarker
        ? Math.min(selStartTx.getTextOffset(), value.length()) : 0;
    int endOffset = printEndMarker
        ? Math.min(selEndTx.getTextOffset(), value.length()) : value.length();

    // Append text and selection markers
    append('\'');
    append(value.substring(0, startOffset));
    append(startMarker);
    append(value.substring(startOffset, endOffset));
    append(endMarker);
    append(value.substring(endOffset, value.length()));
    append('\'');
  }

  /**
   * @param xml
   * @return XML-escaped string
   */
  public static String xmlEscape(String xml) {
    return xml
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
  }

  /**
   * Notice that this function only escape entity reference and not character reference.
   * @param xml
   * @return the unescaped xml string.
   */
  public static String xmlUnescape(String xml) {
    return  xml.replaceAll("&lt;", "<")
        .replaceAll("&gt;", ">")
        .replaceAll("&quot;", "\"")
        .replaceAll("&apos;", "'")
        .replaceAll("&amp;", "&");
  }

  /**
   * @param attrValue
   * @return The escaped xml attribute value
   */
  public static String attrEscape(String attrValue) {
    return xmlEscape(attrValue)
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&apos;");
  }

  /**
   * Debug method.
   *
   * @param string
   * @return The input string as an html string that correctly displays
   *    xml special characters, and spaces
   */
  public static String stringToHtml(String string) {
    return displayWhitespace(xmlEscape(string));
  }

  /**
   * Debug method.
   *
   * @param string
   * @return html string that displays white spaces;
   *   space -> a small square
   *   non-breaking space -> a small, solid square.
   *   TODO(user): other whitespace?
   */
  public static String displayWhitespace(String string) {
    return string.replaceAll("\u00A0", "\u25aa")
      .replaceAll(" ", "\u25ab");
  }
}
