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


import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the serialized annotation format.
 *
 */
public class AnnotationParser {
  int offset = 0;
  String input;

  private AnnotationParser(String input) throws XmlParseException {
    this.input = input;
    ensure(Utf16Util.isValidUtf16(input),
      "Input is not valid UTF-16: ", input);
  }

  /**
   * Parses the annotation string into key value pairs.
   *
   * @param input - for processing instruction <a? annotationStr>, "input" is
   *        the annotationStr, it doesn't include the <a? or the terminating >
   * @return (key, value) pairs, annotation ends have a value of null.
   * @throws XmlParseException
   */
  public static List<Pair<String, String>> parseAnnotations(String input) throws XmlParseException {
    return new AnnotationParser(input).parse();
  }

  private String quotedValue() throws XmlParseException {
    if (input.charAt(offset) == '"') {
      return quotedValue('"');
    } else {
      return quotedValue('\'');
    }
  }

  private void ensure(boolean condition, Object ...messages) throws XmlParseException {
    if (!condition) {
      String[] toStr = new String[messages.length];
      for (int i = 0; i < messages.length; ++i) {
        toStr[i] = String.valueOf(messages[i]);
      }
      String concatenated = CollectionUtils.join(toStr);
      throw new XmlParseException(concatenated);
    }
  }

  private String quotedValue(char quoteChar) throws XmlParseException {
    ensure(input.charAt(offset) == quoteChar,
        "expected ", quoteChar, " at position ", offset, " in ", input);
    offset++;
    int start = offset;

    int nextQuote;
    while(true) {
      nextQuote = input.indexOf(quoteChar, offset);
      ensure(nextQuote != -1, "closing ", quoteChar, " not found");
      offset = nextQuote;
      if (input.charAt(nextQuote - 1) != '\\') {
        break;
      }
      offset++;
    }
    String value = input.substring(start, offset);
    offset++;

    return DocOpUtil.annotationUnEscape(DocOpUtil.xmlTextUnEscape(value));
  }

  private List<Pair<String, String>> parse() throws XmlParseException {
    List<Pair<String, String>> annotations = new ArrayList<Pair<String, String>>();
    while (offset < input.length()) {
      String name = quotedValue();
      ensure(!name.contains("?"), "Invalid char ?: ", input);
      ensure(!name.contains("@"), "Invalid char @: ", input);


      if (offset < input.length() && input.charAt(offset) == '=') {
        offset++;
        String value = quotedValue();
        annotations.add(new Pair<String, String> (name, value));
      } else {
        annotations.add(new Pair<String, String> (name, null));
      }
      while (offset < input.length() && input.charAt(offset) == ' ') {
        offset++;
      }
    }
    return annotations;
  }
}
