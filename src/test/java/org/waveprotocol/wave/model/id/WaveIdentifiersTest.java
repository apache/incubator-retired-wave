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

package org.waveprotocol.wave.model.id;


import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests for the wave identifiers utilities.
 *
 * @author anorth@google.com (Alex North)
 */

public class WaveIdentifiersTest extends TestCase {
  public void testEmptyIsInvalid() {
    assertFalse(WaveIdentifiers.isValidDomain(0, ""));
    assertFalse(WaveIdentifiers.isValidIdentifier(""));
  }

  public void testPlainAsciiIsValid() {
    assertTrue(WaveIdentifiers.isValidIdentifier("abcxyzABCXYZ0123456789"));
    assertTrue(WaveIdentifiers.isValidIdentifier(".-_~+@*"));
  }

  public void testUriGenDelimsAreInvalid() {
    // Gen-delims except "@".
    for (String s : Arrays.asList(":", "/", "?", "#", "[", "]")) {
      assertFalse(WaveIdentifiers.isValidIdentifier(s));
    }
  }

  public void testUriSubDelimsAreInvalid() {
    // Sub-delims except "+", "*".
    for (String s : Arrays.asList("!", "$", "&", "'", "(", ")", ",", ";", "=")) {
      assertFalse(WaveIdentifiers.isValidIdentifier(s));
    }
  }

  /**
   * Tests that all printable ASCII chars besides alphanumerics and URI
   * delimiters are invalid.
   */
  public void testDisallowedPunctuationIsInvalid() {
    // All other printable ASCII chars.
    for (String s : Arrays.asList(" ", "\"", "%", "<", ">", "[", "\\", "]", "^",
        "`", "{", "|", "}")) {
      assertFalse(WaveIdentifiers.isValidIdentifier(s));
    }
  }

  public void testIdWithMixedValidityIsInvalid() {
    assertFalse(WaveIdentifiers.isValidIdentifier("abc/def"));
    assertFalse(WaveIdentifiers.isValidIdentifier("abc!def"));
    assertFalse(WaveIdentifiers.isValidIdentifier("abc^def"));
  }

  /**
   * Tests that ASCII control points are invalid.
   */
  public void testControlsAreInvalid() {
    assertCodePointNotValidIdentifier(0x0000);
    assertCodePointNotValidIdentifier(0x0001);
    assertCodePointNotValidIdentifier(0x001F);
    // Printable ASCII: 0020 - 007E
    assertCodePointNotValidIdentifier(0x007F);
    assertCodePointNotValidIdentifier(0x0080);
    assertCodePointNotValidIdentifier(0x009F);
    // Valid UCS chars: 00A0 - ...
  }

  /**
   * Tests the boundaries of all ranges of valid UCS chars are valid.
   * See RFC 3987.
   */
  public void testUcsCharsAreValid() {
    // Valid UCS code point ranges {lo, hi} (inclusive).
    int[][] ranges = new int[][] {
        {0x00A0, 0xD7FF},
        {0xF900, 0xFDCF},
        {0xFDF0, 0xFFEF},
        {0x10000, 0x1FFFD},
        {0x20000, 0x2FFFD},
        {0x30000, 0x3FFFD},
        {0x40000, 0x4FFFD},
        {0x50000, 0x5FFFD},
        {0x60000, 0x6FFFD},
        {0x70000, 0x7FFFD},
        {0x80000, 0x8FFFD},
        {0x90000, 0x9FFFD},
        {0xA0000, 0xAFFFD},
        {0xB0000, 0xBFFFD},
        {0xC0000, 0xCFFFD},
        {0xD0000, 0xDFFFD},
        {0xE1000, 0xEFFFD},
    };

    for (int[] range : ranges) {
      int lo = range[0], hi = range[1];
      assertCodePointNotValidIdentifier(lo - 1);
      assertCodePointIsValidIdentifier(lo);
      assertCodePointIsValidIdentifier(lo + 1);

      assertCodePointIsValidIdentifier(hi - 1);
      assertCodePointIsValidIdentifier(hi);
      assertCodePointNotValidIdentifier(hi + 1);
    }
  }

  public void testPrivateUcsCharsAreInvalid() {
    // Private UCS code point ranges {lo, hi} (inclusive).
    int[][] ranges = new int[][] {
        {0xE000, 0xF8FF},
        {0xF0000, 0xFFFFD},
        {0x100000, 0x10FFFD},
    };

    for (int[] range : ranges) {
      int lo = range[0], hi = range[1];
      // (lo - 1) is not generally valid.
      assertCodePointNotValidIdentifier(lo);
      assertCodePointNotValidIdentifier(lo + 1);

      assertCodePointNotValidIdentifier(hi - 1);
      assertCodePointNotValidIdentifier(hi);
      // (hi + 1) is not generally valid.
    }
  }

  public void testTypicalIdsAreValid() {
    // Wave ids.
    assertTrue(WaveIdentifiers.isValidIdentifier("w+123abcABC"));
    assertTrue(WaveIdentifiers.isValidIdentifier("embed+39783"));
    assertTrue(WaveIdentifiers.isValidIdentifier("prof+fred@example.com"));
    assertTrue(WaveIdentifiers.isValidIdentifier("prof+j\u00F6rg@t\u016Bdali\u0146.lv"));

    // Wavelet ids
    assertTrue(WaveIdentifiers.isValidIdentifier("conv+root"));
    assertTrue(WaveIdentifiers.isValidIdentifier("user+fred@example.com"));
    assertTrue(WaveIdentifiers.isValidIdentifier("user+j\u00F6rg@t\u016Bdali\u0146.lv"));

    // Blip ids.
    assertTrue(WaveIdentifiers.isValidIdentifier("b+1a3c"));
    assertTrue(WaveIdentifiers.isValidIdentifier("spell+b~+1a3c"));

  }

  public void testTypicalBrokenIdsAreInvalid() {
    assertFalse(WaveIdentifiers.isValidIdentifier("http://example.com"));
    assertFalse(WaveIdentifiers.isValidIdentifier("conversation/root"));
    assertFalse(WaveIdentifiers.isValidIdentifier("contains space"));
    assertFalse(WaveIdentifiers.isValidIdentifier("w%252Bescaped"));
    assertFalse(WaveIdentifiers.isValidIdentifier("?foo=bar"));
    // NOTE(anorth): We may change the spec to allow this.
    assertFalse(WaveIdentifiers.isValidIdentifier("user+o'hallorhan@example.com"));
    // NOTE(anorth): Unfortunately we use this extensively at present.
    assertFalse(WaveIdentifiers.isValidIdentifier("m/read"));
  }

  public void testUnpairedSurrogateIsInvalid() {
    assertFalse(WaveIdentifiers.isValidIdentifier("" + Character.MIN_HIGH_SURROGATE));
    assertFalse(WaveIdentifiers.isValidIdentifier("" + Character.MAX_HIGH_SURROGATE));

    assertFalse(WaveIdentifiers.isValidIdentifier("" + Character.MIN_LOW_SURROGATE));
    assertFalse(WaveIdentifiers.isValidIdentifier("" + Character.MAX_LOW_SURROGATE));
  }

  private static void assertCodePointIsValidIdentifier(int cp) {
    assertTrue("should be valid in identifier: " + Integer.toHexString(cp),
        WaveIdentifiers.isValidIdentifier(new String(Character.toChars(cp))));
  }

  private static void assertCodePointNotValidIdentifier(int cp) {
    assertFalse("should not be valid in identifier: " + Integer.toHexString(cp),
        WaveIdentifiers.isValidIdentifier(new String(Character.toChars(cp))));
  }
}
