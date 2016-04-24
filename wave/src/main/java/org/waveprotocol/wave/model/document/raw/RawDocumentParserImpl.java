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

package org.waveprotocol.wave.model.document.raw;

import org.waveprotocol.wave.model.document.parser.ItemType;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.parser.XmlParserFactory;
import org.waveprotocol.wave.model.document.parser.SafeXmlPullParser;
import org.waveprotocol.wave.model.document.util.DocumentParser;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Parses a string into a RawDocument
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author hearnden@google.com (David Hearnden)
 */
public class RawDocumentParserImpl<N, E extends N, T extends N, D extends RawDocument<N, E, T>>
    implements DocumentParser<D> {

  private final RawDocument.Factory<D> factory;

  /**
   * Creates a parser that uses a particular builder.
   *
   * @param factory  builder to use when parsing
   * @return a new parser.
   */
  public static <N, E extends N, T extends N, D extends RawDocument<N, E, T>>
      RawDocumentParserImpl<N, E, T, D> create(RawDocument.Factory<D> factory) {
    return new RawDocumentParserImpl<N, E, T, D>(factory);
  }

  /**
   * Creates a parser.
   *
   * @param factory  builder to use
   */
  private RawDocumentParserImpl(RawDocument.Factory<D> factory) {
    this.factory = factory;
  }

  /**
   * @param xmlString
   * @return parsed string
   */
  public D parse(String xmlString) {
    SafeXmlPullParser parser;
    try {
      parser = XmlParserFactory.buffered(xmlString);
    } catch (XmlParseException e) {
      throw new RuntimeException("Cannot parse xml: " + xmlString, e);
    }
    // TODO(ohler): This can be an infinite loop.  Fix that.
    while (parser.getCurrentType() != ItemType.START_ELEMENT) {
      parser.next();
    }

    D document =
        factory.create(parser.getTagName(), CollectionUtils.newJavaMap(parser.getAttributes()));
    parseChildren(parser, document, document.getDocumentElement());

    return document;
  }

  /**
   * Parses an element.
   *
   * @param parser  tokenizer
   * @param parentElement the parent element to attach the parsed node to
   * @return a new element.
   */
  private E parseElement(SafeXmlPullParser parser, D doc, E parentElement) {
    E element =
        doc.createElement(parser.getTagName(), CollectionUtils.newJavaMap(parser.getAttributes()),
        parentElement, null);
    parseChildren(parser, doc, element);
    return element;
  }

  private void parseChildren(SafeXmlPullParser parser, D doc, E element) {
    boolean done = false;
    do {
      N child = null;
      parser.next();

      switch (parser.getCurrentType()) {
        case TEXT:
          child = parseText(parser, doc, element);
          break;
        case START_ELEMENT:
          child = parseElement(parser, doc, element);
          break;
        case END_ELEMENT:
          done = true;
          break;
      }
      if (child != null) {
        doc.insertBefore(element, child, null);
      }
      // This is a bit of judgment call. If this happens, the document is
      // invalid, since the closing tag is missing. By exiting the loop when
      // parser is out of tokens we're silently repairing the invalid doc.
    } while (!done && parser.hasNext());
  }

  /**
   * Parses a text node.
   *
   * @param parser  tokenizer
   * @param parentElement the parent element to attach the parsed node to
   * @return a new text node.
   */
  private T parseText(SafeXmlPullParser parser, D doc, E parentElement) {
    String text = parser.getText();
    T child = null;
    if (text.length() > 0) {
      child = doc.createTextNode(text, parentElement, null);
    }
    return child;
  }
}
