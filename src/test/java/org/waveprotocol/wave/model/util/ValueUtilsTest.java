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

/**
 * Tests for value utility methods.
 *
 * @author anorth@google.com (Alex North)
 */

public class ValueUtilsTest extends TestCase {
  public void testNullValuesEqual() {
    assertTrue(ValueUtils.equal(null, null));
    assertFalse(ValueUtils.notEqual(null, null));
  }

  public void testOneNullNotEqual() {
    assertFalse(ValueUtils.equal(null, "foo"));
    assertTrue(ValueUtils.notEqual(null, "foo"));

    assertFalse(ValueUtils.equal("foo", null));
    assertTrue(ValueUtils.notEqual("foo", null));
  }

  public void testEqualValues() {
    assertTrue(ValueUtils.equal(foo(), foo()));
    assertFalse(ValueUtils.notEqual(foo(), foo()));
  }

  public void testUnequalValues() {
    assertFalse(ValueUtils.equal(foo(), "bar"));
    assertTrue(ValueUtils.notEqual(foo(), "bar"));
  }

  public void testDefaultValues() {
    assertEquals("aa", ValueUtils.valueOrDefault("aa", "bb"));
    assertEquals("bb", ValueUtils.valueOrDefault(null, "bb"));
    String empty = null;
    assertEquals(null, ValueUtils.valueOrDefault(null, empty));
  }

  /**
   * Creates a new instance of a string equal to "foo". Use this to
   * exercise String.equals() rather than relying on reference equality
   * of interned strings.
   */
  private String foo() {
    return new String("foo");
  }
}
