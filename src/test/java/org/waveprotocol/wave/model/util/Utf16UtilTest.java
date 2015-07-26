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

package org.waveprotocol.wave.model.util;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class Utf16UtilTest extends TestCase {

  // relevant classes of strings:
  // invalid utf-16 (unpaired surrogates)
  // valid utf-16 without surrogates, but invalid unicode
  // valid utf-16 that uses surrogates, but invalid unicode (noncharacters)
  // valid utf-16 without surrogates, valid unicode, but not good
  // valid utf-16 with surrogates, valid unicode, but not good
  // valid utf-16 without surrogates, valid unicode, good
  // valid utf-16 with surrogates, valid unicode, good
  // valid utf-16 without surrogates, valid unicode, XML name

  public static final List<String> NOT_UTF16_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "abc\uD800def",
        "abc\uDC00def",
        "hk\uDC00\uD800o",
        "\uD800",
        "\uDC00",
        "\uD800a",
        "\uDC00a",
        "a\uD800",
        "a\uDC00",
        "\uDC00\uDC00",
        "\uD800\uD800",
        "\uD805\uDC05\uD801\uD7FF",
        "\uD805\uD7FF\uD801\uDC01",
        "\uD805\uDC05a\uD801\uD7FF"
    ));

  public static final List<String> BASIC_INVALID_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "\uFFFE",
        "\uFFFF",
        "\uFDD0"
    ));

  public static final List<String> EXTENDED_INVALID_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "\uDBFF\uDFFF",
        "\uD8FF\uDFFF"
    ));

  public static final List<String> BASIC_VALID_NOT_GOOD_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "a\u0000",
        "b\n",
        "c\b",
        "d\t",
        "e\0",
        "f\u007f",
        "g\u0080",
        "h\u200F",
        "i\u206B"
    ));

  public static final List<String> EXTENDED_VALID_NOT_GOOD_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        new String(Character.toChars(0x10FFFD)),
        new String(Character.toChars(0xE005B))
    ));

  public static final List<String> BASIC_GOOD_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "",
        "a",
        "kjagjf",
        "-Pr\uD7FF-"
    ));

  public static final List<String> EXTENDED_GOOD_STRINGS =
    Collections.unmodifiableList(Arrays.asList(
        "abc\uD800\uDC00def",
        "\uD800\uDC00",
        "\uD805\uDC05\uD801\uDC01",
        "\uD805\uDC05a\uD801\uDC01"
    ));

  public static final List<String> XML_NAMES = Collections.unmodifiableList(Arrays.asList(
      "a",
      "abc",
      "A",
      "f-",
      "\uD900\uDC00",
      "_\uD900\uDC00",
      "_\uD800\uDC00def",
      ":",
      ":::",
      new String(Character.toChars(0xE0080))
  ));

  public static final List<String> NOT_XML_NAMES = Collections.unmodifiableList(Arrays.asList(
      "",
      "a b",
      "-f",
      "abc\uD800def",
      "abc\uDC00def",
      "abc\uDC00\uD800def"
  ));


  @SuppressWarnings({"unchecked", "rawtypes"})
  public static List<String> concatenateLists(List... lists) {
    int len = 0;
    for (List l : lists) {
      len += l.size();
    }
    List<String> result = new ArrayList<String>(len);
    for (List l : lists) {
      for (Object o : l) {
        if (!(o instanceof String)) {
          throw new IllegalArgumentException("Not a list of strings");
        }
      }
      result.addAll(l);
    }
    return Collections.unmodifiableList(result);
  }

  @SuppressWarnings("deprecation")
  public void testContainsUnpairedSurrogates() {
    for (String s : BASIC_VALID_NOT_GOOD_STRINGS) {
      assertFalse(Utf16Util.containsUnpairedSurrogates(s));
    }
    for (String s : BASIC_GOOD_STRINGS) {
      assertFalse(Utf16Util.containsUnpairedSurrogates(s));
    }
    for (String s : EXTENDED_VALID_NOT_GOOD_STRINGS) {
      assertFalse(Utf16Util.containsUnpairedSurrogates(s));
    }
    for (String s : EXTENDED_GOOD_STRINGS) {
      assertFalse(Utf16Util.containsUnpairedSurrogates(s));
    }
    for (String s : NOT_UTF16_STRINGS) {
      assertTrue(Utf16Util.containsUnpairedSurrogates(s));
    }
  }

  public void testFirstSurrogate() {
    for (String s : BASIC_VALID_NOT_GOOD_STRINGS) {
      assertFalse(-1 != Utf16Util.firstSurrogate(s));
    }
    for (String s : EXTENDED_VALID_NOT_GOOD_STRINGS) {
      assertTrue(-1 != Utf16Util.firstSurrogate(s));
    }
    for (String s : EXTENDED_INVALID_STRINGS) {
      assertTrue(-1 != Utf16Util.firstSurrogate(s));
    }
    for (String s : NOT_UTF16_STRINGS) {
      assertTrue(-1 != Utf16Util.firstSurrogate(s));
    }
  }

  public void testIsXmlName() {
    for (String s : XML_NAMES) {
      System.err.println(s);
      assertTrue(Utf16Util.isXmlName(s));
    }
    for (String s : NOT_XML_NAMES) {
      System.err.println(s);
      assertFalse(Utf16Util.isXmlName(s));
    }
  }

  public void testTraverseUtf16String1() {
    final int[] call = { 0 };
    Utf16Util.traverseUtf16String("a\uD801b\uDC01\uD802", new CodePointHandler<Void>() {
      @Override
      public Void codePoint(int cp) {
        assertTrue(call[0] == 0 || call[0] == 2);
        call[0]++;
        return null;
      }

      @Override
      public Void unpairedSurrogate(char c) {
        assertTrue(call[0] == 1 || call[0] == 3 || call[0] == 4);
        call[0]++;
        return null;
      }

      @Override
      public Void endOfString() {
        assertEquals(5, call[0]);
        call[0]++;
        return null;
      }
    });
    assertEquals(6, call[0]);
  }

  public void testTraverseUtf16String2() {
    Utf16Util.traverseUtf16String("", new CodePointHandler<Void>() {
      @Override
      public Void codePoint(int cp) {
        fail();
        throw new AssertionError();
      }

      @Override
      public Void unpairedSurrogate(char c) {
        fail();
        throw new AssertionError();
      }

      @Override
      public Void endOfString() {
        // ok
        return null;
      }
    });
  }

  public void testGoodUtf16ForDataDocument() {
    assertTrue(Utf16Util.isGoodUtf16ForDataDocument("\0"));
  }

}
