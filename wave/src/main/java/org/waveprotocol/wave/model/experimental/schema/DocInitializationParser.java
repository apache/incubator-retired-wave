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

package org.waveprotocol.wave.model.experimental.schema;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

// TODO(user): There is probably a better home for this class than the schema package.

/**
 * A parser that parses XML into a DocInitialization.
 *
 */
public final class DocInitializationParser {

  /**
   * An exception thrown by the parser if a problem is encountered.
   */
  public static final class ParseException extends Exception {
    private ParseException(Exception e) {
      super(e);
    }
  }

  private abstract static class EventHandler extends DefaultHandler {

    final DocInitializationBuilder builder = new DocInitializationBuilder();
    int depth = 0;

  }

  private static final class FullHandler extends EventHandler {

    @Override
    public void startElement(String uri, String localName, String qName,
        org.xml.sax.Attributes attributes) {
      if (depth > 0) {
        builder.elementStart(qName, convertAttributes(attributes));
      }
      ++depth;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      --depth;
      if (depth > 0) {
        builder.elementEnd();
      }
    }

    @Override
    public void characters(char ch[], int start, int length) {
      builder.characters(new String(ch, start, length));
    }

    private static Attributes convertAttributes(org.xml.sax.Attributes attributes) {
      List<Attribute> attributeList = new ArrayList<Attribute>();
      for (int i = 0; i < attributes.getLength(); ++i) {
        attributeList.add(new Attribute(attributes.getQName(i), attributes.getValue(i)));
      }
      return AttributesImpl.fromUnsortedAttributes(attributeList);
    }

  }

  private static final class NonCharacterHandler extends EventHandler {

    @Override
    public void startElement(String uri, String localName, String qName,
        org.xml.sax.Attributes attributes) {
      if (depth > 0) {
        builder.elementStart(qName, convertAttributes(attributes));
      }
      ++depth;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      --depth;
      if (depth > 0) {
        builder.elementEnd();
      }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
      // Ignore whitespace characters and throw an exception on non-whitespace characters.
      for (int i = start; i < start + length; ++i) {
        if (!Character.isWhitespace(ch[i])) {
          throw new SAXException("Non-whitespace character encountered: " + ch[i]);
        }
      }
    }

    private static Attributes convertAttributes(org.xml.sax.Attributes attributes) {
      List<Attribute> attributeList = new ArrayList<Attribute>();
      for (int i = 0; i < attributes.getLength(); ++i) {
        attributeList.add(new Attribute(attributes.getQName(i), attributes.getValue(i)));
      }
      return AttributesImpl.fromUnsortedAttributes(attributeList);
    }

  }

  private DocInitializationParser() {}

  /**
   * Parses an input stream into a {@link DocInitialization} object.
   */
  public static DocInitialization parse(InputStream stream)
      throws IOException, ParseException {
    return parse(stream, new FullHandler());
  }

  /**
   * Parses an input stream into a {@link DocInitialization} object,
   * ignoring whitespace and disallowing non-whitespace character data.
   */
  public static DocInitialization parseNonCharacterData(InputStream stream)
      throws IOException, ParseException {
    return parse(stream, new NonCharacterHandler());
  }

  private static DocInitialization parse(InputStream stream, EventHandler handler)
      throws IOException, ParseException {
    try {
      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      parser.parse(stream, handler);
    } catch (ParserConfigurationException e) {
      throw new ParseException(e);
    } catch (SAXException e) {
      throw new ParseException(e);
    }
    return handler.builder.build();
  }

}
