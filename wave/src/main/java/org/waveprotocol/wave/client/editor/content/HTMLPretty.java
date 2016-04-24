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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.StringUtil;

import org.waveprotocol.wave.model.document.util.PointRange;

/**
 * Pretty-prints HTML w/ selection
 *
 */
public class HTMLPretty {

  /**
   * @param node Node to pretty print
   * @param selection Selection to mark in pretty print
   * @return A pretty-print HTML string, with '<' and '>' escaped
   */
  public static String print(Node node, PointRange<Node> selection) {
    return print(node, selection, 0, true);
  }

  /**
   * @param n Node to pretty print
   * @param selection Selection to mark in pretty print
   * @return A pretty-print string, in single line
   */
  public static String printSingleLine(Node n, PointRange<Node> selection) {
    return print(n, selection, 0, false);
  }

  /**
   * Produces a newline followed by &nbsp; for next line's indent
   *
   * @param indent
   * @return HTML string
   */
  private static String newLine(int indent) {
    String res = "<br/>";
    for (int i = 0; i < indent; i++) {
      res += "&nbsp;&nbsp";
    }
    return res;
  }

  /**
   * @param e
   * @param attribute
   * @return " attribute='value'"
   */
  private static String attribute(Element e, String attribute) {
    return attribute(e, attribute, attribute);
  }

  /**
   * @param e
   * @param attribute
   * @param attributeDisplayString
   * @return " string='value'"
   */
  private static String attribute(Element e, String attribute, String attributeDisplayString) {
    String value = e.getAttribute(attribute);
    return (value != null &&
        !(attribute.equals("contentEditable") && value.equals("inherit"))) ?
        " " + attributeDisplayString + "='" + value + "'" : "";
  }

  /**
   * @param e
   * @return attribute string of e's class attribute: " class='...'"
   * (Note, IE doesn't seem to treat this as a regular attribute)
   */
  private static String styleName(Element e) {
    String name = e.getClassName();
    return name.length() > 0 ? " class='" + e.getClassName() + "'" : "";
  }

  /**
   * @param e
   * @return Class and style attribute, if present
   * TODO(user): print all attributes
   */
  private static String attributes(Element e) {
    return styleName(e) +
        attribute(e, "src") +
        attribute(e, "contentEditable", "editable") +
        attribute(e, "sp");

  }

  /**
   * @param e
   * @param selfClosing Flag if tag should self close, e.g., <img src='...'/>
   * @param escape True if '<' and '>' should be escaped
   * @return Start tag, e.g., <span attribute='value'>
   */
  public static String startTag(Element e, boolean selfClosing, boolean escape) {
    return (escape ? "&lt;" : "<")
        + e.getTagName().toLowerCase() + attributes(e)
        + (selfClosing ? "/" : "")
        + (escape ? "&gt;" : ">");
  }

  /**
   * @param e
   * @param escape True if '<' and '>' should be escaped
   * @return Start tag, e.g., <span attribute='value'>
   */
  public static String endTag(Element e, boolean escape) {
    return (escape ? "&lt;/" : "</")
        + e.getTagName().toLowerCase()
        + (escape ? "&gt;" : ">");
  }

  /**
   * @param n Node to pretty print
   * @param selection Selection to mark in pretty print
   * @param indent Indentation level to print with.
   * @param multiLine True if output should be multi-line
   * @return A pretty-print HTML string (with '<' and '>' already escaped)
   */
  private static String print(Node n, PointRange<Node> selection, int indent, boolean multiLine) {

    // Inspect selection's relevance to this element
    boolean collapsed = selection != null && selection.isCollapsed();
    boolean printStartMarker =
        selection != null && selection.getFirst().getContainer().equals(n);
    boolean printEndMarker =
        selection != null && !collapsed && selection.getSecond().getContainer().equals(n);
    String startMarker =
        printStartMarker ? (collapsed ? "|" : "[") : "";
    String endMarker = printEndMarker ? "]" : "";
    if (DomHelper.isTextNode(n)) {
      // Print text node as 'value'
      String value = displayWhitespace(n.getNodeValue());
      int startOffset = printStartMarker ? selection.getFirst().getTextOffset() : 0;
      int endOffset = printEndMarker ? selection.getSecond().getTextOffset() : value.length();
      String ret = "'" + value.substring(0, startOffset)
          + startMarker
          + value.substring(startOffset, endOffset)
          + endMarker
          + value.substring(endOffset, value.length())
          + "'" ;
      return multiLine ? StringUtil.xmlEscape(ret) : ret;
    } else {
      Element e = n.cast();
      if (e.getChildCount() == 0) {
        // Print child-less element as self-closing tag
        return startTag(e, true, multiLine);
      } else {
        boolean singleLineHtml = multiLine &&
          (e.getChildCount() == 1 &&
            e.getFirstChild()
            .getChildCount() == 0);
        // Print element w/ children. One line each for start tag, child, end tag
        String pretty = startTag(e, false, multiLine);
        Node child = e.getFirstChild();
        Node startNodeAfter = selection.getFirst().getNodeAfter();
        Node endNodeAfter = selection.getSecond().getNodeAfter();
        while (child != null) {
          pretty += (multiLine && !singleLineHtml ? newLine(indent + 1) : "")
            + (printStartMarker && child.equals(startNodeAfter) ? startMarker : "")
            + (printEndMarker && child.equals(endNodeAfter) ? endMarker : "")
            + print(child, selection, indent + 1, multiLine);
          child = child.getNextSibling();
        }
        if (printEndMarker && endNodeAfter == null) {
          pretty += endMarker;
        }
        return pretty + (multiLine  && !singleLineHtml ? newLine(indent) : "")
            + endTag(e, multiLine);
      }
    }
  }

  /**
   * @param string
   * @return html string that displays white spaces;
   *   space -> a small square
   *   non-breaking space -> a small, solid square.
   *   TODO(user): other whitespace?
   */
  private static String displayWhitespace(String string) {
    return string.replaceAll("\u00A0", "\u25aa")
                 .replaceAll(" ", "\u25ab");
  }
}
