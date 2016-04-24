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
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.util.Map;

/**
 * Implementation class for XmlStringBuilder, plus extension of its interface
 * for converting nodes to strings using a doc view. For details,
 *
 * @see XmlStringBuilder
 *
 */
public class XmlStringBuilderDoc<N, E extends N, T extends N> extends XmlStringBuilder {

  /**
   * The xml string itself
   */
  private final StringBuilder builder = new StringBuilder();

  /**
   * The "item length" of the xml string
   */
  private int length = 0;

  private final ReadableDocument<N, E, T> view;

  private final PermittedCharacters permittedChars;

  private XmlStringBuilderDoc(ReadableDocument<N, E, T> view, PermittedCharacters permittedChars) {
    this.view = view;
    this.permittedChars = permittedChars;
  }

  /**
   * Constructs empty xml
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createEmpty(ReadableDocument<N, E, T> view) {
    return new XmlStringBuilderDoc<N,E,T>(view, PermittedCharacters.ANY);
  }

  /**
   * Constructs empty xml with restriction on PermittedCharacters
   */
  public static <N, E extends N, T extends N> XmlStringBuilderDoc<N,E,T>
      createEmptyWithCharConstraints(ReadableDocument<N, E, T> view, PermittedCharacters permittedChars) {
    return new XmlStringBuilderDoc<N,E,T>(view, permittedChars);
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    builder.setLength(0);
    length = 0;
  }

  /** {@inheritDoc} */
  @Override
  public int getLength() {
    return length;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public XmlStringBuilderDoc<N,E,T> append(XmlStringBuilder xml) {
    // Note(user): The CharSequence assignment here is a workaround for some
    // eclipse problem: if we pass xml.builder directly to append, eclipse's
    // compiler OKs is although the method is private, because there's a
    // public method append(CharSequence), and StringBuilder implements
    // CharSequence. But then we get an IlelgalAccessError at runtime!
    // Note that casting (vs. assigning) xml.builder cases eclipse to tell
    // you the cast is unnecessary for the same faulty reason...
    if (xml instanceof XmlStringBuilderDoc) {
      CharSequence seq = ((XmlStringBuilderDoc<N,E,T>)xml).builder;
      builder.append(seq);
    } else {
      builder.append(xml.toString());
    }
    length += xml.getLength();
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public XmlStringBuilderDoc<N,E,T> wrap(String tagName) {
    checkValidTagName(tagName);
    builder.insert(0, "<" + tagName + ">");
    builder.append("</" + tagName + ">");
    length += 2;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public XmlStringBuilderDoc<N,E,T> wrap(String tagName, String... attribs) {
    checkValidTagName(tagName);
    Preconditions.checkArgument(attribs.length % 2 == 0, "Attribs must come in string pairs");
    String startTag =  "<" + tagName;
    for (int i = 0; i < attribs.length; i += 2) {
      if (attribs[i + 1] != null) {
        String attrName = attribs[i];
        checkValidAttributeName(attrName);
        startTag += " " + attrName + "=\"" + attrEscape(attribs[i + 1]) + "\"";
      }
    }
    startTag += ">";
    builder.insert(0, startTag);
    builder.append("</" + tagName + ">");
    length += 2;
    return this;
  }

  private void checkValidTagName(String tagName) {
    if (!Utf16Util.isXmlName(tagName)) {
      Preconditions.illegalArgument("Invalid tag name: '" + tagName + "'");
    }
  }

  private void checkValidAttributeName(String tagName) {
    if (!Utf16Util.isXmlName(tagName)) {
      Preconditions.illegalArgument("Invalid attribute name: '" + tagName + "'");
    }
  }

  /** {@inheritDoc} */
  @Override
  public XmlStringBuilderDoc<N,E,T> appendText(String text) {
    return appendText(text, PermittedCharacters.BLIP_TEXT);
  }

  /** {@inheritDoc} */
  @Override
  public XmlStringBuilderDoc<N,E,T> appendText(String text, PermittedCharacters permittedChars) {
    length += addText(permittedChars.coerceString(text));
    return this;
  }

  /**
   * Appends the "outerXML" representation of the node
   * @param node
   * @return self for convenience
   */
  public XmlStringBuilderDoc<N,E,T> appendNode(N node) {
    length += addNode(node);
    return this;
  }

  /**
   * Appends the "innerXML" representation of the element
   * @param element
   * @return self for convenience
   */
  public XmlStringBuilderDoc<N,E,T> appendChildXmlFragment(E element) {
    length += addChildXmlFragment(element);
    return this;
  }

  /**
   * Helper
   * @param element
   * @param selfClosing Whether this tag is self-closing
   * @return Opening tag of the given element as a String.
   */
  public String startTag(E element, boolean selfClosing) {
    return "<" + view.getTagName(element)
        + getAttributesString(element) + (selfClosing ? "/>" : ">");
  }

  /**
   * Helper
   * @param element
   * @return Closing tag of the given element as a String.
   */
  public String endTag(E element) {
    return "</" + view.getTagName(element) + ">";
  }

  /**
   * TODO(user): generalise this.
   * @param element
   * @return true if the element may self-close
   */
  private boolean isSelfClosing(E element) {
    return view.getTagName(element).equals("br");
  }

  /**
   * Worker for appendNode
   * @param node
   * @return item length of node
   */
  private int addNode(N node) {
    E element = view.asElement(node);
    if (element != null) {
      boolean selfClosing = isSelfClosing(element) && view.getFirstChild(element) == null;

      builder.append(startTag(element, selfClosing));
      int len = 2;
      if (!selfClosing) {
        len += addChildXmlFragment(element);
        builder.append(endTag(element));
      }

      return len;
    } else {
      String data = view.getData(view.asText(node));
      return addText(permittedChars.coerceString(data));
    }
  }

  /**
   * Worker
   * @param text
   * @return length of text
   */
  private int addText(String text) {
    builder.append(xmlEscape(text));
    return text.length();
  }

  /**
   * Worker
   * @param element
   * @return XML fragment describing children
   */
  private int addChildXmlFragment(E element) {
    int size = 0;
    for (N n = view.getFirstChild(element); n != null; n = view.getNextSibling(n)) {
      size += addNode(n);
    }
    return size;
  }

  /**
   * Worker to get the attributes from the element as a string
   * @param element
   * @return string of attributes, e.g., " name1='value1' name1='value2'"
   */
  private String getAttributesString(E element) {
    // TODO(danilatos): Investigate if it's worth optimising for ContentElement
    // StringMap attributes
    Map<String, String> attributes = view.getAttributes(element);
    String soFar = "";
    for (String key : attributes.keySet()) {
      soFar += " " + key + "=\"" + attrEscape(attributes.get(key)) + "\"";
    }
    return soFar;
  }

  /** Copied from Utils in util.client to avoid dependency issues */
  public static String xmlEscape(String xml) {
    return xml
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
  }

  /** Copied from Utils in util.client to avoid dependency issues */
  public static String attrEscape(String attrValue) {
    return xmlEscape(attrValue)
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&apos;");
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof XmlStringBuilderDoc) {
      // NOTE(danilatos): As of this writing, GWT implementation of toString() for StringBuilder
      // essentially caches the result, so it's not bad for performance to call it multiple times.
      // However, the JRE version creates a copy every time. The equals() method on StringBuilders
      // uses referential equality, so it is not usable. Need to be careful GWT doesn't change
      // their implementation to make this equals implementation here slow.
      return builder.toString().equals(((XmlStringBuilderDoc)o).builder.toString()) &&
          length == ((XmlStringBuilderDoc)o).length;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String getXmlString() {
    return builder.toString();
  }

  @Override
  public String toString() {
    return getXmlString();
  }

}
