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

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.Pair;

import java.util.List;

/**
 * Tests for AnnotationParser
 *
 */
public class AnnotationParserTest extends TestCase {

  public void testAnnotationParser() {
    try {
      List<Pair<String, String>> parsedAnnotations = AnnotationParser.parseAnnotations(
          "\"hello\"=\"world\" \"a&amp;1\" \"b2\"=\"\\\"c\\\"\"");
      assertEquals("hello", parsedAnnotations.get(0).first);
      assertEquals("world", parsedAnnotations.get(0).second);

      assertEquals("a&1", parsedAnnotations.get(1).first);
      assertNull(parsedAnnotations.get(1).second);

      assertEquals("b2", parsedAnnotations.get(2).first);
      assertEquals("\"c\"", parsedAnnotations.get(2).second);
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }

    try {
      List<Pair<String, String>> parsedAnnotations = AnnotationParser.parseAnnotations(
          "'ab'='c'");
      assertEquals("ab", parsedAnnotations.get(0).first);
      assertEquals("c", parsedAnnotations.get(0).second);
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }

    try {
      List<Pair<String, String>> parsedAnnotations = AnnotationParser.parseAnnotations("");
      assertEquals(0, parsedAnnotations.size());
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }
  }

  public void testInvalidAnnotations() {
    // extra space after =
    try {
      List<Pair<String, String>> parsedAnnotations =
          AnnotationParser.parseAnnotations("\"hello\"= \"world\" ");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
      System.out.println(e.getMessage());
    }

    // missing closing "
    try {
      List<Pair<String, String>> parsedAnnotations =
          AnnotationParser.parseAnnotations("\"hello\"=\"world");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
    }

    // missing opening '
    try {
      List<Pair<String, String>> parsedAnnotations = AnnotationParser.parseAnnotations(
          "ab'='c'");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
    }

    // missing closing " (it got escaped)
    try {
      List<Pair<String, String>> parsedAnnotations =
          AnnotationParser.parseAnnotations("\"hello\"=\"world\\\"");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
    }

    // ? is invalid char in annotation key
    try {
      List<Pair<String, String>> parsedAnnotations =
          AnnotationParser.parseAnnotations("\"?hello\"=\"world\"");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
    }

    // @ is invalid char in annotation key
    try {
      List<Pair<String, String>> parsedAnnotations =
          AnnotationParser.parseAnnotations("\"@hello\"=\"world\"");
      fail("Should've thrown exception");
    } catch (XmlParseException e) {
    }
  }
}
