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

package org.waveprotocol.wave.client.editor.testtools;

import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * Some assertion methods useful for testing editor
 *
 */
public class EditorAssert {

  /**
   * A list of tagnames of inline elements
   */
  private static final String inlineTagNames = "b|u|i|span|a";

  /**
   * Returns whether the strings are equal after {@link #normalizeXml(String)}
   * Helper method (used in delayed assert) to predict the outcome of
   * {@link #assertXmlEquals(String, String, String)}
   *
   * @param expectedXml
   * @param actualXml
   * @return Whether the normalized xml strings are equivalent.
   */
  public static boolean xmlEquals(String expectedXml, String actualXml) {
    String expected = normalizeXml(expectedXml);
    String actual = normalizeXml(actualXml);
    if ((expected == null) && (actual == null)) {
      return true;
    }
    return (actual != null) && actual.equals(expected);
  }

  /**
   * Asserts that two xml strings are equal after {@link #normalizeXml(String)}
   *
   * @param msg
   * @param expectedXml
   * @param actualXml
   */
  public static void assertXmlEquals(
      String msg, String expectedXml, String actualXml) {
    String normalizedExpectedXml = normalizeXml(expectedXml);
    String normalizedActualXml = normalizeXml(actualXml);
    System.err.println("Expected:[" + normalizedExpectedXml + "]");
    System.err.println("Actual  :[" + normalizedActualXml + "]");
    assertEquals(msg, normalizedExpectedXml, normalizedActualXml);
  }

  /**
   * 'Normalises' an xml string to avoid fickleness in testing. Currently
   * we only perform a few normalisation steps:
   *
   * Self-close all empty elements, e.g., <p></p> -> <p/>
   * Replace " with '
   *
   * More can be added, e.g., to normalise whitespace, if + when needed.
   *
   * @param xml
   * @return normalised xml string
   */
  private static String normalizeXml(String xml) {
    if (xml == null) {
      return null;
    }
    int index = 0;
    int last = 0;
    StringBuilder out = new StringBuilder();
    while ((index = xml.indexOf("></", index)) != -1) {
      if (xml.charAt(index - 1) == '/') {
        index += 3;
        // Already self-closing.
        continue;
      }
      int open = xml.lastIndexOf('<', index);
      if (open != -1 && xml.charAt(open + 1) != '/') {
        out.append(xml.subSequence(last, index)).append("/>");
        index = last = xml.indexOf('>', index + 1) + 1;
      } else {
        index += 3;
      }
    }
    out.append(xml.subSequence(last, xml.length()));
    return out.toString().replaceAll("\"", "'");
  }

  /**
   * @param msg
   * @param expected
   * @param actual
   */
  private static void failLocationsDiffer(String msg, int expected, int actual) {
    fail(msg + ". expected=" + expected + ". actual=" + actual);
  }

  /**
   * @param msg
   * @param expected
   * @param actual
   */
  private static void failSelectionsDiffer(String msg, Range expected, Range actual) {
    fail(msg + ". expected=" + expected + ". actual=" + actual);
  }

  /**
   * Asserts that two selection ranges are equal modulo the fact that some browsers
   * move selections/carets we set outside or inside inline elements in
   * various ways, and (absent a way to fix that), we want tests still to pass.
   *
   * If for example our code sets <p>a<i>|bc</i>d</p>, some browsers may in
   * fact do <p>a|<i>bc</i>d</p>. Likewise, <p>a<i>bc|</i>d</p> may become
   * <p>a<i>bc</i>|d</p>. Some browsers do the opposite: <p>a|<i>bc</i>d</p>
   * becomes <p>a<i>|bc</i>d</p> and so on.
   *
   * This assert should fail only if the difference between the two points
   * cannot be explained by the problems above. Note that to pass the second
   * example above (i.e., [0, 1, 2] should be considered equal to [0, 2]) we
   * need the content present in order to check that the 2 index indeed sits at
   * the right end of the <i>bc</i> element.
   *
   * Note also that we allow these adjustments to the selection *only* around
   * inline elements. For example, <p>|</p> is *not* he same as <p></p>|. Again
   * we need the content to determine this, along with a white-list of inline
   * element tag names.
   *
   * @param msg
   * @param expected
   * @param actual
   * @param content
   */
  public static void assertSelectionEquals(
      String msg, Range expected, Range actual, String content) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    if (expected == null || actual == null ||
        actual.isCollapsed() != expected.isCollapsed()) {
      failSelectionsDiffer(msg, expected, actual);
    }
    assertLocationsEquals(msg, expected.getStart(), actual.getStart(), content);
    if (!expected.isCollapsed()) {
      assertLocationsEquals(msg, expected.getEnd(), actual.getEnd(), content);
    }
  }


  /**
   * Asserts that two locations are equal modulo the facts described
   * for {@link #assertSelectionEquals(String, Range, Range, String)}
   *
   * @param msg
   * @param expected
   * @param actual
   * @param content
   */
  public static void assertLocationsEquals(
      String msg, int expected, int actual, String content) {
    // First test for strict equality and nulls
    if (expected == actual) {
      return;
    }
    if (expected == -1 || actual == -1) {
      failLocationsDiffer(msg, expected, actual);
    }
    // We don't have strict equality, and neither location is -1.
    // Test if the left-most location can be made to match the right-most
    // one only by moving past tags of inline elements
    int left = location2Offset(
        (expected < actual) ? expected : actual, content);
    int right = location2Offset(
        (expected < actual) ? actual : expected, content);
    while (left != -1 && left < right) {
      left = maybeMovePastInlineTag(left, content);
    }
    if (left != right) {
      failLocationsDiffer(msg, expected, actual);
    }
  }

  /**
   * @param location e.g. 3
   * @param content e.g. <p>ab</p>
   * @return the char offset in content of location, e.g., 4.
   *    Calls fail if location is not inside content
   */
  private static int location2Offset(int location, String content) {
    int offset = 0;
    for (int i = 1; i < location; i++) {
      if (offset > content.length()) {
        fail("Location " + location + " is not in " + content);
      }
      if (content.charAt(offset) == '<') {
        offset = content.indexOf('>', offset);
        if (offset == -1) {
          fail("Missing '>' in " + content);
        }
      }
      offset++;
    }
    return offset;
  }

  /**
   * Attempts to move offset past an inline tag
   *
   * @param offset
   * @param content
   * @return the new offset, or -1 if offset is not immediately before an inline tag
   */
  private static int maybeMovePastInlineTag(int offset, String content) {
    if (content.charAt(offset) != '<') {
      return -1;
    }
    int tagStart = (content.charAt(offset + 1) == '/') ? offset + 2 : offset + 1;
    int space = content.indexOf(' ', tagStart);
    int close = content.indexOf('>', tagStart);
    int tagEnd = (space == -1) ? close : Math.min(space, close);
    String tag = content.substring(tagStart, tagEnd);
    return inlineTagNames.contains(tag) ? close + 1 : -1;
  }

  private static void assertEquals(String message, Object a, Object b) {
    if (!ValueUtils.equal(a, b)) {
      throw new AssertionError(message);
    }
  }

  private static void fail(String message) {
    throw new AssertionError(message);
  }
}
