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
 * @author zdwang@google.com (David Wang)
 */

public class SimplePrefixEscaperTest extends TestCase {

  private final SimplePrefixEscaper escaper = SimplePrefixEscaper.DEFAULT_ESCAPER;

  /**
   * Test simple escaping and the unescaping.
   */
  public void testSimpleEscapingUnescaping() {
    utilTestEscapeUnescape("abc", "abc");

    // Middle of the string.
    utilTestEscapeUnescape("a~+~!~~bc", "a+!~bc");

    // Beginning and end of the string.
    utilTestEscapeUnescape("~!abc~+", "!abc+");

    // Empty
    utilTestEscapeUnescape("", "");

    // Single
    utilTestEscapeUnescape("~+", "+");
  }

  private void utilTestEscapeUnescape(String escaped, String unescaped) {
    assertEquals(escaped, escaper.escape(unescaped));
    assertEquals(unescaped, escaper.unescape(escaped));
  }

  /**
   * Test the unescaping of various illegally escaped strings.
   */
  public void testUnescapingFail() {
    // Escaping nothing
    utilTestUnescapeFail("~");

    // There are unescaped chars
    utilTestUnescapeFail("~~~");
    utilTestUnescapeFail("~~!");
    utilTestUnescapeFail("~!~");
    utilTestUnescapeFail("!~~");

    // Escaping something that does not require escaping
    utilTestUnescapeFail("~a");
  }

  private void utilTestUnescapeFail(String escaped) {
    try {
      escaper.unescape(escaped);
      fail("Not supposed to allow unescaping of malformed string.");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  /**
   * Test simple join and split.
   */
  public void testJoinSplit() {
    utilTestJoinSplit("a!b", '!', "a", "b");

    // Nothing to join
    utilTestJoinSplit("", '!', "");
    utilTestJoinSplit("!", '!', "", "");
    utilTestJoinSplit("ab", '!', "ab");

    // Needs escaping
    utilTestJoinSplit("~~!~!", '!', "~", "!");
    utilTestJoinSplit("~~!~~", '!', "~", "~");
    utilTestJoinSplit("~!!~!", '!', "!", "!");
  }

  private void utilTestJoinSplit(String joinedString, char separator, String... tokens) {
    assertEquals(joinedString, escaper.join(separator, tokens));
    assertEquals(Arrays.asList(tokens), Arrays.asList(escaper.split(separator, joinedString)));
  }

  public void testJoinFail() {
    // Empty tokens
    utilTestJoinFail('!');

    // Using prefix for join
    utilTestJoinFail('~', "a", "b");

    // Not using escape chars
    utilTestJoinFail('-', "a", "b");
  }

  private void utilTestJoinFail(char separator, String... tokens) {
    try {
      escaper.join(separator, tokens);
      fail("Not supposed to allow illegal joinning.");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  /**
   * Test strings that can't be split.
   */
  public void testSplitFail() {
    // Cannot split using prefix
    utilTestSplitFail('~', "a~b");

    // Invalid strings
    utilTestSplitFail('!', "~aa!b");
    utilTestSplitFail('!', "~");
    utilTestSplitFail('!', "ab~");
    utilTestSplitFail('!', "ab!+");
  }

  private void utilTestSplitFail(char separator, String toSplit) {
    try {
      escaper.split(separator, toSplit);
      fail("Not supposed to be able to split invalid strings.");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  public void testEscapedId() {
    utilTestEscapedId("google.com", "abc");
    utilTestEscapedId("google.com", "+c");
    utilTestEscapedId("google.com", "~~w~~a+v~!e~+~~");

    // bad domain
    utilTestEscapedIdFail("google.!com", "abc");

    // bad id
    utilTestEscapedIdFail("google.com", "ab+~c");
    utilTestEscapedIdFail("google.com", "ab!+!c");
    utilTestEscapedIdFail("google.com", "ab!c");
    utilTestEscapedIdFail("google.com", "!c");
    utilTestEscapedIdFail("google.com", "~~w~a+v~!e~+~~");
  }

  private void utilTestEscapedId(String domain, String id) {
    try {
      WaveId.ofLegacy(domain, id);
      WaveletId.ofLegacy(domain, id);
    } catch (IllegalArgumentException ex) {
      fail("Not supposed to throw exception: " + ex.getMessage());
    }
  }

  private void utilTestEscapedIdFail(String domain, String id) {
    try {
      WaveId.ofLegacy(domain, id);
      fail("Not supposed to be able to create invalid WaveId");
    } catch (IllegalArgumentException ex) {
      // Expected
    }

    try {
      WaveletId.ofLegacy(domain, id);
      fail("Not supposed to be able to create invalid WaveletId");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }
}
