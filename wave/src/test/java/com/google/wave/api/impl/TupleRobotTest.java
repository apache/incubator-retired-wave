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
package com.google.wave.api.impl;

import junit.framework.TestCase;

/**
 * Test cases for {@link Tuple}.
 */
public class TupleRobotTest extends TestCase {

  private static final String BAR = "bar";
  private static final String FOO = "foo";

  private Tuple<String> tuple = Tuple.of(FOO, BAR);

  public void testSize() {
    assertEquals(2, tuple.size());
  }

  public void testGet() {
    assertEquals(FOO, tuple.get(0));
    assertEquals(BAR, tuple.get(1));

    try {
      tuple.get(-1);
      fail("Should have thrown IndexOutOfBoundsException when accessing index -1.");
    } catch (ArrayIndexOutOfBoundsException e) {
      // Expected.
    }

    try {
      tuple.get(2);
      fail("Should have thrown IndexOutOfBoundsException when accessing index that is greater " +
           "than the size of the tuple.");
    } catch (ArrayIndexOutOfBoundsException e) {
      // Expected.
    }
  }

  public void testEquals() {
    assertFalse(tuple.equals(null));
    assertFalse(tuple.equals(new String[] {FOO, BAR}));
    assertFalse(tuple.equals(Tuple.of(FOO)));

    assertTrue(tuple.equals(tuple));
    assertTrue(tuple.equals(Tuple.of(FOO, BAR)));
  }

  public void testHashCode() {
    assertTrue(tuple.hashCode() == tuple.hashCode());
    assertTrue(tuple.hashCode() == Tuple.of(FOO, BAR).hashCode());
    assertFalse(tuple.hashCode() == Tuple.of(BAR, FOO).hashCode());
  }
}
