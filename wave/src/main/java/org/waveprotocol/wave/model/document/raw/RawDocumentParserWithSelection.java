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
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Parses a string into a RawDocument
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class RawDocumentParserWithSelection<N, E extends N, T extends N,
                                            D extends RawDocument<N, E, T>> {
  private final RawDocument.Factory<D> factory;

  /**
   */
  public interface SelectionParsingListener<N> {
    /**
     */
    void onStartSelection(Point<N> start);

    /**
     */
    void onEndSelection(Point<N> end);
  }

  /**
   * Creates a parser that uses a builder.
   *
   * @param factory  factory to use when parsing
   * @return a new parser.
   */
  public static <N, E extends N, T extends N, D extends RawDocument<N, E, T>>
      RawDocumentParserWithSelection<N, E, T, D> create(RawDocument.Factory<D> factory) {
    return new RawDocumentParserWithSelection<N, E, T, D>(factory);
  }

  private RawDocumentParserWithSelection(RawDocument.Factory<D> factory) {
    this.factory = factory;
  }

  /**
   * @param xmlString
   * @return parsed string
   */
  public RawDocument<N, E, T> parse(String xmlString) {
    return parseIntoDocument(xmlString, null);
  }


  /**
   * @param xmlString
   * @param selectionListener
   * @return new document
   */
  public D parseIntoDocument(
      String xmlString, SelectionParsingListener<N> selectionListener) {
    SafeXmlPullParser parser;
    try {
      parser = XmlParserFactory.buffered(xmlString);
    } catch (XmlParseException e) {
      throw new RuntimeException("Cannot parse xml: " + xmlString, e);
    }

    while (parser.getCurrentType() != ItemType.START_ELEMENT) {
      parser.next();
    }

    D doc = factory.create(parser.getTagName(), CollectionUtils.newJavaMap(parser.getAttributes()));

    parseChildren(parser, selectionListener, doc, doc.getDocumentElement());

    return doc;
  }

  @SuppressWarnings("cast")  // TODO(danilatos): Figure out how to avoid cast to Point<N>
  private E parseElement(SafeXmlPullParser parser, SelectionParsingListener<N> selectionListener,
      D doc, E parent, N nodeAfter) {
    return parseChildren(parser, selectionListener, doc,
        doc.createElement(parser.getTagName(),
            CollectionUtils.newJavaMap(parser.getAttributes()), parent, nodeAfter));
  }

  private E parseChildren(SafeXmlPullParser parser, SelectionParsingListener<N> selectionListener,
      D doc, E element) {

    int start = -1;
    int end = -1;
    boolean startBefore = false;
    boolean endBefore = false;
    N startNodeAfter = null;
    N endNodeAfter = null;

    N recentChild = null;

    parser.next();

    while (true) {

      ItemType type = parser.getCurrentType();

      if (type == ItemType.END_ELEMENT) {
        break;
      }

      switch (type) {
        case START_ELEMENT:
          recentChild = parseElement(parser, selectionListener, doc, element, null);
          break;
        case TEXT:
          String text = parser.getText();

          if (selectionListener != null) {
            start = text.indexOf('|');
            end = start;
            if (start >= 0) {
              text = removeChar(text, start);
            } else {
              start = text.indexOf('[');
              if (start >= 0) {
                text = removeChar(text, start);
              }
              end = text.indexOf(']');
              if (end >= 0) {
                text = removeChar(text, end);
              }
            }

            assert end == -1 || end >= start;
          }

          if (text.length() > 0) {
            recentChild = doc.createTextNode(text, element, null);

            if (start >= 0) {
              selectionListener.onStartSelection(Point.inText(recentChild, start));
            }
            if (end >= 0) {
              selectionListener.onEndSelection(Point.inText(recentChild, end));
            }
          } else {
            if (start >= 0) {
              startBefore = true;
            }
            if (end >= 0) {
              endBefore = true;
            }
          }

          parser.next();

          break;
      }

      if (startBefore && start == -1) {
        startNodeAfter = recentChild;
        startBefore = false;
      }
      if (endBefore && end == -1) {
        endNodeAfter = recentChild;
        endBefore = false;
      }

      start = -1;
      end = -1;
    }

    if (startBefore || startNodeAfter != null) {
      selectionListener.onStartSelection(Point.<N>inElement(element, startNodeAfter));
    }
    if (endBefore || endNodeAfter != null) {
      selectionListener.onEndSelection(Point.<N>inElement(element, endNodeAfter));
    }

    parser.next();
    return element;
  }

  private static String removeChar(String str, int pos) {
    return str.substring(0, pos) + str.substring(pos + 1);
  }
}
