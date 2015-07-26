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

/**
 * Tests for RegularExpressionChecker.
 *
 */

public class RegularExpressionCheckerTest extends TestCase {

  /**
   * Tests that valid regular expressions cause no exception to be thrown.
   */
  public void testValidRegularExpressions() throws InvalidSchemaException {
    checkValid("");
    checkValid("valid");
    checkValid("ab*c");
    checkValid("a.*b");
    checkValid("hello|world");
    checkValid("ab(cd|ef)gh");
    checkValid("ab(c*|.d)ef");
    checkValid("ab(c|d)*ef");
    checkValid("ab()*cd");
    checkValid("ab(cde(fg)(hi()j(k)lm)n)op");
  }

  /**
   * Tests that invalid regular expressions cause an
   * <code>InvalidSchemaException</code> to be thrown.
   */
  public void testInvalidRegularExpressions() {
    checkInvalid("*abcd");        // begins with '*'
    checkInvalid("?abcd");        // begins with '?'
    checkInvalid(")abcd");        // begins with ')'
    checkInvalid("ab(*cd)ef");    // unexpected '*'
    checkInvalid("ab(?cd)ef");    // unexpected '?'
    checkInvalid("ab(cd");        // unmatched '('
    checkInvalid("ab)cd");        // unexpected ')'
    checkInvalid("a((b(()()c)d"); // unmatched '('
    checkInvalid("a(b()())c))d"); // unexpected ')'
    checkInvalid("abcd\\");       // ends with '\\'
  }

  /**
   * A convenience method for checking valid regular expressions.
   */
  private static void checkValid(String re) throws InvalidSchemaException {
    RegularExpressionChecker.checkRegularExpression(re);
  }

  /**
   * Checks that a regular expression is invalid.
   */
  private static void checkInvalid(String re) {
    try {
      RegularExpressionChecker.checkRegularExpression(re);
      fail("The expected InvalidSchemaException was not thrown for: " + re);
    } catch (InvalidSchemaException e) {
    }
  }

}
