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

/**
 * Utilities related to the document-based value classes
 *
 * @author anorth@google.com (Alex North)
 */
public final class ValueUtils {

  /**
   * @return true iff a and b are both null or are equal
   */
  public static <T> boolean equal(T a, T b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  /**
   * @return true iff a and b are not both null and are not equal
   */
  public static <T> boolean notEqual(T a, T b) {
    return (a == null) ? (b != null) : !a.equals(b);
  }

  /**
   * @return {@code value} if it is not null, {@code def} otherwise
   */
  public static <T> T valueOrDefault(T value, T def) {
    return value != null ? value : def;
  }

  /**
   * Returns the first {@code size} characters of the given string
   */
  public static String abbrev(String longString, int size) {
    if (longString == null) {
      return null;
    }

    return longString.length() <= size ? longString
        : new String(longString.substring(0, size));
  }
}
