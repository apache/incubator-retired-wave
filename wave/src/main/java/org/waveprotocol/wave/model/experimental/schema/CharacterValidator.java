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

import org.waveprotocol.wave.model.util.Utf16Util;
import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.Set;

/**
 * A validator of characters.
 *
 */
final class CharacterValidator {

  private interface Predicate {
    boolean apply(int codePoint);
  }

  private static final class ValidatingHandler implements CodePointHandler<Integer> {

    private int count = 0;
    private final Predicate predicate;

    ValidatingHandler(Predicate predicate) {
      this.predicate = predicate;
    }

    @Override
    public Integer codePoint(int cp) {
      if (!predicate.apply(cp)) {
        return count;
      }
      ++count;
      return null;
    }

    @Override
    public Integer unpairedSurrogate(char c) {
      return count;
    }

    @Override
    public Integer endOfString() {
      return -1;
    }

  }

  private final Predicate predicate;

  private CharacterValidator(Predicate predicate) {
    this.predicate = predicate;
  }

  /**
   * Checks whether the given characters are valid.
   *
   * @param characters the characters to check
   * @return -1 if the characters are all valid, or otherwise the number of
   *         valid Unicode characters before the first invalid character
   */
  int validate(String characters) {
    return Utf16Util.traverseUtf16String(characters, new ValidatingHandler(predicate));
  }

  /**
   * Creates a <code>CharacterValidator</code> which blacklists characters.
   *
   * @param disallowedCharacters the blacklisted characters
   * @return the constructed <code>CharacterValidator</code>
   */
  static CharacterValidator disallowedCharacters(final Set<Integer> disallowedCharacters) {
    return new CharacterValidator(new Predicate() {
      @Override
      public boolean apply(int codePoint) {
        return !disallowedCharacters.contains(codePoint);
      }
    });
  }

  /**
   * Creates a <code>CharacterValidator</code> which whitelists characters.
   *
   * @param allowedCharacters the whitelisted characters
   * @return the constructed <code>CharacterValidator</code>
   */
  static CharacterValidator allowedCharacters(final Set<Integer> allowedCharacters) {
    return new CharacterValidator(new Predicate() {
      @Override
      public boolean apply(int codePoint) {
        return allowedCharacters.contains(codePoint);
      }
    });
  }

}
