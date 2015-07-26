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

import java.util.Arrays;


public class CharBase64Test extends TestCase {
  private final byte [] emptyBytes = {};
  private final char [] emptyChars = {};
  private final String latin1 = "L\u00e1t\u00ef\u00f1-1\n";
  private final byte[] latin1Bytes = new byte[] { 76, -31, 116, -17, -15, 45, 49, 10 };
  private final String latin1Enc = "TOF07/EtMQo=";
  private final String latin1WSEnc = "TOF07_EtMQo=";  // web safe
  private final String latin1WSNPEnc = "TOF07_EtMQo"; // web safe, no padding

  private final byte[] randomBytes = {
    (byte)0x58, (byte)0x37, (byte)0xf8, (byte)0x77,
    (byte)0xd9, (byte)0x99, (byte)0x17, (byte)0x96
  };
  private final String randomEnc = "WDf4d9mZF5Y=";
  private final String randomWSNPEnc = "WDf4d9mZF5Y";

  public void testEncodings() throws Exception {
    assertArraysEqual(emptyChars, CharBase64.encode(emptyBytes).toCharArray());

    String enc = CharBase64.encode("fastening my ankle to a stone".getBytes());
    assertArraysEqual("ZmFzdGVuaW5nIG15IGFua2xlIHRvIGEgc3RvbmU=".toCharArray(),
                      enc.toCharArray());

    assertEquals(latin1Enc, CharBase64.encode(latin1Bytes));
    assertEquals(latin1Enc,
                 CharBase64.encode(offset5(latin1Bytes), 5, latin1Bytes.length,
                                   CharBase64.getAlphabet(), true));
    assertEquals(latin1WSEnc,
                 CharBase64.encodeWebSafe(latin1Bytes, true));
    assertEquals(latin1WSNPEnc,
                 CharBase64.encodeWebSafe(latin1Bytes, false));

    assertEquals(randomEnc, CharBase64.encode(randomBytes));
    assertEquals(randomWSNPEnc, CharBase64.encodeWebSafe(randomBytes, false));
  }

  public void testDecodings() throws Exception {
    assertArraysEqual(emptyBytes, CharBase64.decode(emptyChars));
    char [] d = "d2FzaCB5b3VyIGV5ZWxpZHMgaW4gdGhlIHJhaW4K".toCharArray();
    assertArraysEqual("wash your eyelids in the rain\n".getBytes(),
                      CharBase64.decode(d));

    assertArraysEqual(latin1Bytes, CharBase64.decode(latin1Enc));
    assertArraysEqual(
        latin1Bytes,
        CharBase64.decode(offset5(latin1Enc.toCharArray()), 5, latin1Enc.length()));
    assertArraysEqual(randomBytes, CharBase64.decode(randomEnc));
  }

  /** Test decodings that are incomplete. These are officially out of
   ** spec and 'mimencode -u' complains about EOF, but Google, particularly
   ** the web safe decoding, instead just assumes there should have been =
   ** padding at the end.
   **/
  public void testShortDecodings() throws Exception {
    byte [] A = "A\n".getBytes();
    assertArraysEqual(A, CharBase64.decode("QQo=".toCharArray()));
    assertArraysEqual(A, CharBase64.decode("QQo".toCharArray()));

    assertArraysEqual(A, CharBase64.decodeWebSafe("QQo=".toCharArray()));
    assertArraysEqual(A, CharBase64.decodeWebSafe("QQo".toCharArray()));

    byte [] seven0 = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    assertArraysEqual(seven0, CharBase64.decode("AAAAAAAAAA==".toCharArray()));
    assertArraysEqual(seven0, CharBase64.decode("AAAAAAAAAA=".toCharArray()));
    assertArraysEqual(seven0, CharBase64.decode("AAAAAAAAAA".toCharArray()));

    assertArraysEqual(seven0, CharBase64.decodeWebSafe("AAAAAAAAAA==".toCharArray()));
    assertArraysEqual(seven0, CharBase64.decodeWebSafe("AAAAAAAAAA=".toCharArray()));
    assertArraysEqual(seven0, CharBase64.decodeWebSafe("AAAAAAAAAA".toCharArray()));

  }

  public void testRoundTrip() throws Exception {
    String [] testStrings = {
      "", "abcd", "from time to time I am sublime", latin1
    };
    byte[][] testBytes = {
        new byte[0],
        new byte[] {97, 98, 99, 100},
        new byte[] {102, 114, 111, 109, 32, 116, 105, 109, 101, 32, 116, 111, 32, 116, 105, 109,
                    101, 32, 73, 32, 97, 109, 32, 115, 117, 98, 108, 105, 109, 101},
        latin1Bytes
    };

    for (int i = 0; i < testStrings.length; i++) {
      byte [] bytes = testBytes[i];
      String enc = CharBase64.encode(bytes);
      byte [] newBytes = CharBase64.decode(enc);
      String newEnc = CharBase64.encode(newBytes);
      assertArraysEqual(bytes, newBytes);
      assertEquals(enc, newEnc);
    }
  }

  public void testLineLength() throws Exception {
    String [] testStrings = {
      "", "abcdefgh", "abcdefghi", "abcdefghij", "abcdefghijk",
      "abcdefghijkl", "abcdefghijklm", "abcdefghijklmn",
      "01234567890123456789012345678901234567890123456789" +
      "01234567890123456789012345678901234567890123456789"
    };

    byte[][] testBytes = {
        new byte[0],
        new byte[] {98, 99, 100, 101, 102, 103, 104},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105, 106},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109},
        new byte[] {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110},
        new byte[] {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
                    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57}
    };

    for (int lineLength = 8; lineLength < 80; lineLength += 4) {
      for (int i = 0; i < testStrings.length; i++) {
        byte [] bytes = testBytes[i];
        char[] enc = CharBase64.encode(bytes, 0, bytes.length,
            CharBase64.getAlphabet(), lineLength);
        byte [] newBytes = CharBase64.decode(new String(enc));
        assertArraysEqual(bytes, newBytes);

        // Check for newlines
        for (int p = lineLength; p < enc.length; p += lineLength + 1) {
          assertEquals('\n', enc[p]);
        }
      }
    }
  }

  /**
   * Tests the base64 padding de-mangler. Up to three uncoded bytes are encoded
   * as 4 ASCII chars, with padding as necessary so length % 4 == 0; in these
   * cases we have the wrong number of padding bytes, but we expect the decoder
   * to handle them.
   */
  public void testDecodingStringsWithMismatchedPadding() throws Exception {
    assertDecodesToString("a", "YQ");
    assertDecodesToString("a", "YQ=");
    assertDecodesToString("a", "YQ=="); // Correct base64.
    assertDecodesToString("a", "YQ===");
    assertDecodesToString("a", "YQ====");

    assertDecodesToString("ab", "YWI");
    assertDecodesToString("ab", "YWI="); // Correct base64.
    assertDecodesToString("ab", "YWI==");
    assertDecodesToString("ab", "YWI===");

    assertDecodesToString("abc", "YWJj"); // Correct base64.
    assertDecodesToString("abc", "YWJj=");
    assertDecodesToString("abc", "YWJj==");
  }

  /** Test bad decodings - these are invalid. **/
  public void testDecodingInvalidStrings() {
    // These contain characters not in the decodabet.
    assertFailsToDecode("\u007f");
    assertFailsToDecode("Wf2!");
    // This sentence just isn't base64 encoded.
    assertFailsToDecode("let's not talk of love or chains!");
    // a 4n+1 length string is never legal base64
    assertFailsToDecode("12345");
  }

  /**
   * Tests badly padded base64 - padding must never occur in the first two bytes
   * and must always be at the end of the encoding or followed by more padding.
   */
  public void testDecodingStringsWithInvalidPadding() {
    assertFailsToDecode("====");
    assertFailsToDecode("=Wf2");
    assertFailsToDecode("Wf=2");
    assertFailsToDecode("Wf==2");
    assertFailsToDecode("Wf=2=");
  }

  private static void assertDecodesToString(String expected, String base64)
      throws Exception {
    assertEquals(expected, new String(CharBase64.decode(base64)));
  }

  private static void assertFailsToDecode(String base64) {
    try {
      CharBase64.decode(base64);
      fail("Should have thrown Base64DecoderException for " + base64);
    } catch (Base64DecoderException expected) {
    }
  }

  /** Returns a new array with five bytes of '@' followed by {@code bytes}. */
  private static byte[] offset5(byte[] bytes) {
    byte[] ret = new byte[5 + bytes.length];
    for (int i = 0; i < 5; ++i) {
      ret[i] = '@';
    }
    System.arraycopy(bytes, 0, ret, 5, bytes.length);
    return ret;
  }

  /** Returns a new array with five bytes of '@' followed by {@code bytes}. */
  private static char[] offset5(char[] chars) {
    char[] ret = new char[5 + chars.length];
    for (int i = 0; i < 5; ++i) {
      ret[i] = '@';
    }
    System.arraycopy(chars, 0, ret, 5, chars.length);
    return ret;
  }

  static void assertArraysEqual(byte[] expected, byte[] actual) {
    assertTrue("expected:<" + new String(expected) +
        "> but was:<" + new String(actual) + ">",
        Arrays.equals(expected, actual));
  }

  static void assertArraysEqual(char[] expected, char[] actual) {
    assertTrue("expected:<" + new String(expected) +
               "> but was:<" + new String(actual) + ">",
               Arrays.equals(expected, actual));
  }
}
