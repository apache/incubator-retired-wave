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

package org.waveprotocol.wave.communication;

import com.google.common.base.Preconditions;

/**
 * Trivial encode/decoder of byte arrays and char arrays.
 *
 * @author hearnden@google.com (David Hearnden)
 */
// The encoding used here can be replaced with any trusted encoding scheme
// (i.e., not 500+ lines of if statements and quirk explanations).
public final class Codec {
  private final static char[] digits =
      {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  private static char toDigit(int b) {
    assert 0 <= b && b <= 0xF;
    return digits[b];
  }

  private static int toNumber(char c) {
    if ('0' <= c && c <= '9') {
      return (c - '0');
    } else if ('A' <= c && c <= 'F') {
      return (c - 'A' + 0xA);
    } else {
      throw new IllegalArgumentException("Not a hexadecimal digit: " + c);
    }
  }

  /**
   * Encodes an arbitrary byte array as an array of characters.
   */
  public static String encode(byte[] decoded) {
    char[] encoded = new char[decoded.length * 2];
    for (int i = 0; i < decoded.length; i++) {
      byte b = decoded[i];
      encoded[i * 2 + 0] = toDigit((b >> 4) & 0xF);
      encoded[i * 2 + 1] = toDigit((b >> 0) & 0xF);
    }
    return new String(encoded);
  }

  /**
   * Decodes an {@link #encode(byte[]) encoded} character array to a byte array.
   *
   * @throws IllegalArgumentException if {@code encoded} is not a valid
   *         encoding.
   */
  public static byte[] decode(String s) {
    char [] encoded = s.toCharArray();
    Preconditions.checkArgument(encoded.length % 2 == 0);
    byte[] decoded = new byte[encoded.length / 2];
    for (int i = 0; i < decoded.length; i++) {
      char hi = encoded[i * 2 + 0];
      char lo = encoded[i * 2 + 1];
      decoded[i] = (byte) ((toNumber(hi) << 4) + toNumber(lo));
    }
    return decoded;
  }
}
