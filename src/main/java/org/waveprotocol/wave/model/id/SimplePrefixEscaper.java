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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Escapes and un-escapes characters by prefixing another character.
 *
 * @author zdwang@google.com (David Wang)
 */
public class SimplePrefixEscaper {

  /**
   * This is the default escaper that is used to prefix escape "+", "!" with "~".
   *
   * TODO: Consider using regex to simplify and optimise the implementation.
   */
  public static final SimplePrefixEscaper DEFAULT_ESCAPER = new SimplePrefixEscaper('~', '+', '!');

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private final Set<Character> needsEscaping = new HashSet<Character>();

  /** Character used to prefix an escaped character. */
  private final char prefix;

  public SimplePrefixEscaper(char prefix, char... needsEscaping) {
    this.prefix = prefix;
    this.needsEscaping.add(prefix);
    for (char s : needsEscaping) {
      this.needsEscaping.add(s);
    }
  }

  /**
   * Escapes instances of a char in a string by prefixing it with another char.
   *
   * @param toEscape string in which to escape it
   * @return the escaped string
   */
  public String escape(String toEscape) {
    StringBuilder cache = new StringBuilder(toEscape.length());
    for (int i = 0; i < toEscape.length(); i++) {
      if (needsEscaping.contains(toEscape.charAt(i))) {
        cache.append(prefix);
      }
      cache.append(toEscape.charAt(i));
    }

    return cache.toString();
  }

  /**
   * Un-escapes instances of a char in a string by replacing prefixed values
   * with just the value.
   *
   * @param toUnescape string from which to un-escape it
   * @return the un-escaped string
   */
  public String unescape(String toUnescape) {
    // At least the string is half as long.
    StringBuilder cache = new StringBuilder(toUnescape.length() / 2);
    for (int i = 0; i < toUnescape.length(); i++) {
      if (toUnescape.charAt(i) == prefix) {
        if (i + 1 >= toUnescape.length()) {
          throw new IllegalArgumentException("The value to unescape cannot be terminated with " +
              "the prefix: " + prefix);
        }

        if (!needsEscaping.contains(toUnescape.charAt(i + 1))) {
          throw new IllegalArgumentException("The value to unescape is not a properly escaped " +
              "value. The prefix charater is not followed by a character at needs prefixing: " +
              toUnescape);
        }

        // increment the index to the next character
        i++;
      } else if (needsEscaping.contains(toUnescape.charAt(i))) {
        throw new IllegalArgumentException("The value to unescape is not a properly escaped " +
            "value. Some chars are found unescaped: " + toUnescape);
      }

      cache.append(toUnescape.charAt(i));
    }

    return cache.toString();
  }

  /**
   * Join tokens together using a separator. If the separator appears in any
   * tokens it is escaped.
   *
   * @param separator the separator to join the tokens.
   * @param tokens tokens to join
   * @return joined tokens by separator
   */
  public String join(char separator, String... tokens) {
    if (separator == prefix) {
      throw new IllegalArgumentException("It's unsafe to join strings together using the prefix" +
          "char.");
    }

    // Doing this makes it unambiguous that "" is the join of a single empty token,
    // not the join of no tokens.
    if (tokens.length == 0) {
      throw new IllegalArgumentException("Must have at least 1 token to use join.");
    }

    if (!needsEscaping.contains(separator)) {
      throw new IllegalArgumentException("It's unsafe to join strings together using a " +
          "[separator:" + separator + "] that is not in the characters that are escaped.");
    }

    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < tokens.length; i++) {
      if (i > 0) {
        ret.append(separator);
      }
      ret.append(escape(tokens[i]));
    }
    return ret.toString();
  }

  /**
   * Splits a string on a separator. Any escaped separator characters appearing
   * in tokens are un-escaped. Any separator character by itself is a split
   * point.
   *
   * @param separator separator character
   * @param toSplit string to split
   * @return a list of unescaped tokens
   */
  public String[] split(char separator, String toSplit) {
    String[] ret = splitWithoutUnescaping(separator, toSplit);
    for (int i = 0; i < ret.length; i++) {
      ret[i] = unescape(ret[i]);
    }
    return ret;
  }


  /**
   * Splits a string on a separator. If the separator is escaped, it's ignored as a split point.
   * Any separator character by itself is a split point.
   *
   * @param separator separator character
   * @param toSplit string to split
   * @return a list of escaped (untouched) tokens
   */
  public String[] splitWithoutUnescaping(char separator, String toSplit) {
    if (separator == prefix) {
      throw new IllegalArgumentException("It's unsafe to split strings together the prefix char.");
    }

    ArrayList<String> ret = new ArrayList<String>();
    int start = 0;
    while (start <= toSplit.length()) {
      int end = start;

      while (end < toSplit.length() && toSplit.charAt(end) != separator) {
        // skip over escaped chars.
        end += toSplit.charAt(end) == prefix ? 2 : 1;
      }

      if (end >= toSplit.length()) {
        end = toSplit.length();
      }

      ret.add(toSplit.substring(start, end));
      start = end + 1;
      end = end + 1;
    }

    return ret.toArray(EMPTY_STRING_ARRAY);
  }

  /**
   * @param escapedValue The escaped string.
   * @return true if all occurrences of characters in needsEscaping, except the separator character,
   *    are escaped properly.
   */
  public boolean isEscapedProperly(char separator, String escapedValue) {
    for (int i = 0; i < escapedValue.length(); i++) {
      char c = escapedValue.charAt(i);

      if (c == prefix) {
        // The next character after the prefix is not a character that needs escaping.
        if (i >= escapedValue.length() - 1 || !needsEscaping.contains(escapedValue.charAt(i + 1))) {
          return false;
        } else {
          // skip over the escaped char
          i++;
        }
      } else if (c != separator && needsEscaping.contains(c)) {  // found unescaped char
        return false;
      }
    }
    return true;
  }

  /**
   * @param unescaped The unescaped value to be tested.
   * @return true if there are no characters that in the needsEscaping found in unescaped.
   */
  public boolean hasEscapeCharacters(String unescaped) {
    for (int i = 0; i < unescaped.length(); i++) {
      // THis is a character that needs to be escaped
      if (needsEscaping.contains(unescaped.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
