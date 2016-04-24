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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test case for {@link ConcurrentSet}.
 *
 */

public class ConcurrentSetTest extends TestCase {

  /** Instance being tested.  Created in {@link #setUp()}. */
  private ConcurrentSet<String> set;

  /** Dummy items used in the tests. */
  private String a;
  private String b;
  private String c;
  private String d;


  @Override
  protected void setUp() throws Exception {
    set = ConcurrentSet.create();
    a = "a";
    b = "b";
    c = "c";
    d = "d";
  }

  /**
   * Creates a list of items in the order returned by iterating over a
   * ConcurrentSet (this is used to ensure set semantics are maintained).
   *
   * @param xs  a ConcurrentSet
   * @return a list containing the items returned by iterating over {@code xs}.
   */
  private static <T> List<T> asList(ConcurrentSet<T> xs) {
    List<T> list = new ArrayList<T>();
    for (T x : xs) {
      list.add(x);
    }
    return list;
  }

  /**
   * Creates a set containing the items returned by iterating over a
   * ConcurrentSet (this is used to compare a ConcurrentSet to a Set, since
   * ConcurrentSet does not implement equality with Sets).
   *
   * @param xs  a ConcurrentSet
   * @return a set containing the items returned by iterating over {@code xs}.
   */
  private static <T> Set<T> asSet(ConcurrentSet<T> xs) {
    return new HashSet<T>(asList(xs));
  }

  /**
   * Creates a Set.
   *
   * @param xs  items to put in the set
   * @return a set containing {@code xs}.
   */
  @SuppressWarnings("unchecked")
  private static <T> Set<T> setOf(T ... xs) {
    return new HashSet<T>(Arrays.asList(xs));
  }

  public void testBasicAddAndRemove() {
    set.add(a);
    assertEquals("initial add failed", setOf(a), asSet(set));

    set.add(b);
    assertEquals("second add failed", setOf(a, b), asSet(set));

    set.add(b);
    assertEquals("b added twice", 2, asList(set).size());

    set.remove(a);
    assertEquals("initial remove failed", setOf(b), asSet(set));

    set.remove(b);
    assertEquals("second remove failed", Collections.<String>emptySet(), asSet(set));
  }

  public void testAddWhileLockedDoesNotCauseCme() {
    set.add(a);
    set.add(b);
    set.add(c);

    set.lock();
    try {
      // We attempt to add d on every iteration.
      for (String x : set) {
        set.add(d);
      }
    } catch (ConcurrentModificationException e) {
      fail("addition during iteration caused CME");
    }
    set.unlock();
  }

  public void testRemoveWhileLockedDoesNotCauseCme() {
    set.add(a);
    set.add(b);
    set.add(c);
    set.add(d);

    set.lock();
    try {
      // Removals to attempt on every iteration.
      String [] removals = new String [] {b, d, c, a};
      int toRemove = 0;
      for (String x : set) {
        set.remove(removals[toRemove++]);
      }
    } catch (ConcurrentModificationException e) {
      fail("removal during iteration caused CME");
    }
    set.unlock();
  }

  public void testAddIsDelayedWhileLocked() {
    set.add(a);
    set.add(b);

    set.lock();
    // try to add c, and check that it should not go into the iterable collection yet
    set.add(c);
    assertEquals(setOf(a, b), asSet(set));
    // try again
    set.add(c);
    assertEquals(setOf(a, b), asSet(set));
    // try again with d
    set.add(d);
    assertEquals(setOf(a, b), asSet(set));

    // unlock, and test that c and d have been added, but only once each
    set.unlock();
    assertEquals("wrong size", 4, asList(set).size());
    assertEquals("c and/or d were not added", setOf(a, b, c, d), asSet(set));
  }

  public void testRemoveIsDelayedWhileLocked() {
    set.add(a);
    set.add(b);
    set.add(c);
    set.add(d);

    set.lock();
    // try to remove c, and check that it should still be in the iterable collection yet
    set.remove(c);
    assertEquals(setOf(a, b, c, d), asSet(set));
    // try again with d
    set.remove(d);
    assertEquals(setOf(a, b, c, d), asSet(set));

    // unlock, and test that c and d get removed.
    set.unlock();
    assertEquals("wrong size", 2, asList(set).size());
    assertEquals("c and/or d were not removed", setOf(a, b), asSet(set));
  }

  public void testAddAfterRemoveWhileLockedHasNoEffect() {
    set.add(a);
    set.add(b);
    set.add(c);

    set.lock();
    set.remove(b);
    set.add(b);
    set.unlock();

    assertEquals(setOf(a, b, c), asSet(set));
  }

  public void testRemoveAfterAddWhileLockedHasNoEffect() {
    set.add(a);
    set.add(b);

    set.lock();
    set.add(c);
    set.remove(c);
    set.unlock();

    assertEquals(setOf(a, b), asSet(set));
  }
}
