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

import static java.util.Arrays.asList;


import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * Test case for {@link ConcurrentList}.
 *
 */

public class ConcurrentListTest extends TestCase {

  /** Instance being tested.  Created in {@link #setUp()}. */
  private ConcurrentList<String> list;

  /** Dummy items used in the tests. */
  private String a;
  private String b;
  private String c;
  private String d;
  private String e;

  @Override
  protected void setUp() throws Exception {
    list = ConcurrentList.create();
    a = "a";
    b = "b";
    c = "c";
    d = "d";
    e = "e";
  }

  /**
   * Creates a list of items in the order returned by iterating over a
   * ConcurrentList.
   *
   * @param xs  a ConcurrentList
   * @return a list containing the items returned by iterating over {@code xs}.
   */
  private static <T> List<T> toList(ConcurrentList<T> xs) {
    List<T> list = new ArrayList<T>();
    for (T x : xs) {
      list.add(x);
    }
    return list;
  }

  public void testBasicAddAndRemove() {
    assertTrue(list.isEmpty());

    list.add(a);
    assertEquals("initial add failed", asList(a), toList(list));
    assertTrue(!list.isEmpty());

    list.add(b);
    assertEquals("second add failed", asList(b, a), toList(list));
    assertTrue(!list.isEmpty());

    list.remove(a);
    assertEquals("initial remove failed", asList(b), toList(list));
    assertTrue(!list.isEmpty());

    list.remove(b);
    assertEquals("second remove failed", Collections.<String>emptyList(), toList(list));
    assertTrue(list.isEmpty());
  }

  public void testAddWhileIteratingDoesNotCauseCme() {
    list.add(a);
    list.add(b);
    list.add(c);

    try {
      // We attempt to add d on every iteration.
      for (String x : list) {
        list.add(d);
      }
    } catch (ConcurrentModificationException e) {
      fail("addition during iteration caused CME");
    }
  }

  public void testRemoveWhileIteratingDoesNotCauseCme() {
    list.add(d);
    list.add(c);
    list.add(b);
    list.add(a);

    try {
      Iterator<String> i = list.iterator();

      // (* means iterator reference, [] means deleted
      // pre state:  a* b c d
      assertTrue(i.hasNext());
      assertEquals(a, i.next());
      // post state: a b* c d

      // pre state: a b* c d
      list.remove(c);
      assertEquals(asList(a, b, d), toList(list));
      // post state: a b* d

      // pre state: a b* d
      assertTrue(i.hasNext());
      assertEquals(b, i.next());
      // post state: a b d*

      // pre state: a b d*
      list.remove(d);
      assertEquals(asList(a, b), toList(list));
      // post state: a b [d*]
      assertFalse(i.hasNext());
    } catch (ConcurrentModificationException e) {
      fail("removal during iteration caused CME");
    }
  }

  public void testAddAndRemoveWhileIterating() {
    list.add(e);
    list.add(d);
    list.add(c);
    list.add(b);
    list.add(a);

    try {
      Iterator<String> i = list.iterator();

      //
      // pre state:  a* b c d e
      assertTrue(i.hasNext());
      assertEquals(a, i.next());
      // post state:  a b* c d e

      // pre state:  a b* c d e
      list.remove(e);
      list.add(e);
      assertEquals(asList(e, a, b, c, d), toList(list));
      // post state:  e a b* c d

      // pre state:  e a b* c d
      assertTrue(i.hasNext());
      list.remove(b);
      assertEquals(asList(e, a, c, d), toList(list));
      // post state:  e a [b*] c d
      list.remove(c);
      assertEquals(asList(e, a, d), toList(list));
      // post state:  e a [b*] [c] d
      assertTrue(i.hasNext());
      assertEquals(d, i.next());

      // post state: e a d
      assertFalse(i.hasNext());
    } catch (ConcurrentModificationException e) {
      fail("mutation during iteration caused CME");
    }
  }

  public void testHasNextDoesNotAffectNext() {
    list.add(d);
    list.add(c);
    list.add(b);
    list.add(a);

    try {
      Iterator<String> i = list.iterator();

      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertEquals(a, i.next());

      list.remove(b);
      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertEquals(c, i.next());

      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertTrue(i.hasNext());
      assertEquals(d, i.next());

      assertFalse(i.hasNext());
      assertFalse(i.hasNext());
      assertFalse(i.hasNext());
    } catch (ConcurrentModificationException e) {
      fail("mutation during iteration caused CME");
    }
  }

  public void testNextMatchesHasNext() {
    list.add(d);
    list.add(c);
    list.add(b);
    list.add(a);

    try {
      Iterator<String> i = list.iterator();

      // Even if we remove stuff, we expect next() to return what was pointed to at the time
      // of hasNext().
      // pre state: a* b c d
      assertTrue(i.hasNext());  // a
      list.remove(a);
      assertEquals(a, i.next());
      // post state: b* c d

      assertTrue(i.hasNext());  // b
      list.remove(b);
      list.remove(c);
      assertEquals(b, i.next());
      // post state: d*

      assertTrue(i.hasNext());  // d
      assertEquals(d, i.next());  // d
    } catch (ConcurrentModificationException e) {
      fail("mutation during iteration caused CME");
    }
  }

  public void testRemoveWhileIteratingAffectsIsEmpty() {
    list.add(c);
    list.add(b);
    list.add(a);

    try {
      Iterator<String> i = list.iterator();
      assertFalse(list.isEmpty());

      // pre state:  a* b c
      list.remove(c);
      i.next();
      assertFalse(list.isEmpty());
      // post state: a b*

      // pre state: a b*
      list.remove(b);
      list.remove(a);
      assertEquals(Collections.<String>emptyList(), toList(list));
      assertFalse(i.hasNext());
      assertTrue(list.isEmpty());
      // post state: [b*]
    } catch (ConcurrentModificationException e) {
      fail("removal during iteration caused CME");
    }
  }
}
