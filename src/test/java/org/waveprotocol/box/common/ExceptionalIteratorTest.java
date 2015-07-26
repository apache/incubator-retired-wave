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

package org.waveprotocol.box.common;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the exceptional iterator.
 *
 * @author anorth@google.com (Alex North)
 */
public class ExceptionalIteratorTest extends TestCase {

  private static final List<Integer> items = Arrays.asList(1, 2, 3);

  public void testEmptyIteratorIsEmpty() throws Exception {
    ExceptionalIterator<?, Exception> empty = ExceptionalIterator.Empty.create();
    assertFalse(empty.hasNext());
  }

  public void testFailingIteratorFails() {
    Exception ex = new Exception("for testing");
    ExceptionalIterator<?, Exception> failing = ExceptionalIterator.Failing.create(ex);
    try {
      failing.hasNext();
      fail("Expected an exception");
    } catch (Exception e) {
      assertSame(ex, e);
    }
  }

  public void testFromIteratorNoException() throws Exception {
    ExceptionalIterator<Integer, Exception> itr =
        ExceptionalIterator.FromIterator.create(items.iterator());
    assertTrue(itr.hasNext());
    assertEquals(Integer.valueOf(1), itr.next());
    assertEquals(Integer.valueOf(2), itr.next());
    assertEquals(Integer.valueOf(3), itr.next());
    assertFalse(itr.hasNext());
  }

  public void testFromIteratorWithException() throws Exception {
    Exception ex = new Exception("for testing");

    ExceptionalIterator<Integer, Exception> itr =
        ExceptionalIterator.FromIterator.create(items.iterator(), ex);
    assertTrue(itr.hasNext());
    assertEquals(Integer.valueOf(1), itr.next());
    assertEquals(Integer.valueOf(2), itr.next());
    assertEquals(Integer.valueOf(3), itr.next());
    try {
      itr.hasNext();
      fail("Expected an exception");
    } catch (Exception e) {
      assertSame(ex, e);
    }
  }
}
