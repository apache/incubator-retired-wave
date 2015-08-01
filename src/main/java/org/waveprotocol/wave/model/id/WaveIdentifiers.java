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

import org.waveprotocol.wave.model.util.Utf16Util;
import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.Arrays;

/**
 * Utilities for working with identifiers compliant with the new specification.
 *
 * @author anorth@google.com (Alex North)
 * @see "http://code.google.com/p/wave-protocol/source/browse/spec/waveid/waveidspec.rst"
 */
public final class WaveIdentifiers {

  /**
   * Boolean array defining ASCII chars allowed in an identifier.
   * Entries correspond to character values.
   */
  private static final boolean[] SAFE_ASCII_CHARS;

  static {
    SAFE_ASCII_CHARS = new boolean[0x7F];
    for (char c = 'A'; c <= 'Z'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c = 'a'; c <= 'z'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c = '0'; c <= '9'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c : Arrays.asList('-', '.', '_', '~', '+', '*', '@')) {
      SAFE_ASCII_CHARS[c] = true;
    }
  }

  private static final CodePointHandler<Boolean> GOOD_UTF16_FOR_ID =
      new CodePointHandler<Boolean>() {
        @Override
        public Boolean codePoint(int cp) {
          if (!Utf16Util.isCodePointValid(cp)) {
            return false;
          }
          if (cp < SAFE_ASCII_CHARS.length && !SAFE_ASCII_CHARS[cp]) {
            return false;
          }
          if (cp >= SAFE_ASCII_CHARS.length && !isUcsChar(cp)) {
            return false;
          }
          return null;
        }

        @Override
        public Boolean endOfString() {
          return true;
        }

        @Override
        public Boolean unpairedSurrogate(char c) {
          return false;
        }
  };

  /**
   * Checks whether a UTF-16 string is a valid wave identifier.
   */
  public static boolean isValidIdentifier(String id) {
    return !id.isEmpty() && Utf16Util.traverseUtf16String(id, GOOD_UTF16_FOR_ID);
  }

  /**
   * Checks if the given string has a valid host name specified, starting at the
   * given start index. This method implements a check for a valid domain as
   * specified by RFC 1035, Section 2.3.1. It essentially checks if the domain
   * matches the following regular expression:
   * <tt>[a-z0-9]([a-z0-9\-]*[a-z0-9])(\.[a-z0-9]([a-z0-9\-]*[a-z0-9]))*</tt>.
   * Please note that the specification does not restrict TLDs, and therefore
   * my.arbitrary.domain passes the check. We also allow labels to start with
   * a digit to allow for domains such as 76.com. Furthermore, we allow only
   * strings specified by the subdomain non-terminal,to avoid allowing empty
   * string, which can be derived from the domain non-terminal.
   */
  public static boolean isValidDomain(int start, String x) {
    // TODO(user): Make sure we accept only valid TLDs.
    int index = start;
    int length = x.length() - start;
    if (length > 253 || length < 1) {
      return false;
    }
    while (index < x.length()) {
      char c = x.charAt(index);
      // A label must being with a letter or a digit.
      if (('a' > c || c > 'z') && ('0' > c || c > '9')) {
        return false;
      }
      char d = c;
      while (++index < x.length()) {
        c = x.charAt(index);
        // Subsequent characters may be letters, digits or the dash.
        if (('a' > c || c > 'z') && ('0' > c || c > '9') && (c != '-')) {
          break;
        }
        d = c;
      }
      if (index >= x.length()) {
        return d != '-';
      }
      // Labels must be separated by dots, and may not end with the dash.
      if ('.' != c || d == '-') {
        return false;
      }
      ++index;
    }
    // The domain ended in a dot, legal but we do not approve.
    return false;
  }

  /**
   * Checks whether an int value is a valid UCS code-point above 0x7F as defined
   * in RFC 3987.
   */
  private static boolean isUcsChar(int c) {
    return (c >= 0xA0 && c <= 0xD7FF) || (c >= 0xF900 && c <= 0xFDCF)
        || (c >= 0xFDF0 && c <= 0xFFEF) || (c >= 0x10000 && c <= 0x1FFFD)
        || (c >= 0x20000 && c <= 0x2FFFD) || (c >= 0x30000 && c <= 0x3FFFD)
        || (c >= 0x40000 && c <= 0x4FFFD) || (c >= 0x50000 && c <= 0x5FFFD)
        || (c >= 0x60000 && c <= 0x6FFFD) || (c >= 0x70000 && c <= 0x7FFFD)
        || (c >= 0x80000 && c <= 0x8FFFD) || (c >= 0x90000 && c <= 0x9FFFD)
        || (c >= 0xA0000 && c <= 0xAFFFD) || (c >= 0xB0000 && c <= 0xBFFFD)
        || (c >= 0xC0000 && c <= 0xCFFFD) || (c >= 0xD0000 && c <= 0xDFFFD)
        || (c >= 0xE1000 && c <= 0xEFFFD);
  }

  private WaveIdentifiers() {
  }
}
