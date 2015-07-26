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

package com.google.wave.api;

import org.apache.commons.codec.binary.Base64;

import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * A class that contains various utility methods.
 */
public final class Util {

  /**
   * A regular expression that defines reserved {@code proxy_for} id.
   */
  private static final Pattern RESERVED_PROXY_FOR_CHARS_REGEX =
      Pattern.compile("[\\s\u0000-\u001F@,:<>\u007F]");

  /**
   * Checks if the given string is a valid proxy id, by asserting whether the
   * string contains reserved characters or not. This check is to ensure that
   * when we concatenate the robot id and the proxy id, it doesn't result in an
   * invalid participant id.
   *
   * The reserved characters are:
   * <ul>
   *   <li>whitespaces</li>
   *   <li>non-printing characters: decimal 0 - 31 (hex 00 - 1F), and decimal
   *       127 (hex 7F)</li>
   *   <li>{@code @}, {@code ,}, {@code :}, {@code <}, {@code >}</li>
   * </ul>
   *
   * If you need to pass in an arbitrary string as the proxy id, please consider
   * encoding the string with a Base64 encoder (for example, {@link Base64}) or
   * a URL encoder (for example, {@link URLEncoder}).
   *
   * @param string the string to be checked.
   * @return {@code true} if the string doesn't contain any reserved characters.
   */
  public static boolean isValidProxyForId(String string) {
    return !RESERVED_PROXY_FOR_CHARS_REGEX.matcher(string).find();
  }

  /**
   * Checks if the given string is a valid proxy id. Please see
   * {@link #isValidProxyForId(String)} for more details on the assertion. This
   * method throws an {@link IllegalArgumentException} if the input string is
   * not a valid proxy id.
   *
   * @param string the string to check.
   * @throws IllegalArgumentException if the input string is not a valid proxy
   *     id.
   */
  public static void checkIsValidProxyForId(String string) {
    if (string != null && !isValidProxyForId(string)) {
      throw new IllegalArgumentException("The input string \"" + string + "\" is not a valid " +
          "proxy id. A valid proxy id may not contain whitespace characters, non-printing" +
          "characters (decimal 0 - 31, and decimal 127), @, :, <, >, and comma.");
    }
  }
  
  /**
   * Returns {@code true} if the given string is null, empty, or comprises only
   * whitespace characters.
   * 
   * @param string the string reference to check
   * @return {@code true} if {@code string} is null, empty, or consists of
   *     whitespace characters only
   */
  public static boolean isEmptyOrWhitespace(@Nullable String string) {
    return string == null || string.matches("\\s*");
  }
}
