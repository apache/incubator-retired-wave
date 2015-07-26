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

package org.waveprotocol.wave.model.experimental.schema;


import junit.framework.TestCase;

import java.util.Set;
import java.util.TreeSet;

/**
 * Tests for CharacterValidator.
 *
 */

public class CharacterValidatorTest extends TestCase {

  public void testCharacterWhitelisting() {
    CharacterValidator validator = CharacterValidator.allowedCharacters(stringToCodePoints("abcd"));
    assertEquals(-1, validator.validate("abcd"));
    assertEquals(-1, validator.validate("aaaa"));
    assertEquals(-1, validator.validate("bbbb"));
    assertEquals(-1, validator.validate("cccc"));
    assertEquals(-1, validator.validate("dddd"));
    assertEquals(4, validator.validate("abcde"));
    assertEquals(0, validator.validate("eabcd"));
    assertEquals(2, validator.validate("abecd"));
  }

  public void testCharacterBlacklisting() {
    CharacterValidator validator =
      CharacterValidator.disallowedCharacters(stringToCodePoints("abcd"));
    assertEquals(-1, validator.validate("efgh"));
    assertEquals(0, validator.validate("aaaa"));
    assertEquals(0, validator.validate("bbbb"));
    assertEquals(0, validator.validate("cccc"));
    assertEquals(0, validator.validate("dddd"));
    assertEquals(4, validator.validate("efgha"));
    assertEquals(0, validator.validate("aefgh"));
    assertEquals(2, validator.validate("efagh"));
  }

  /**
   * This converts a string into code points, where each character in the given
   * string must represent a full code point (so surrogate pairs are not
   * allowed).
   */
  private Set<Integer> stringToCodePoints(String s) {
    Set<Integer> codePoints = new TreeSet<Integer>();
    for (int i = 0; i < s.length(); ++i) {
      codePoints.add((int) s.charAt(i));
    }
    return codePoints;
  }

}
