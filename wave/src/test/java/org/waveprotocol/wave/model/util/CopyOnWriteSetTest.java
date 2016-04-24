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
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Test case for {@link CopyOnWriteSet}.
 *
 */

public class CopyOnWriteSetTest extends TestCase {

  /** Instance being tested.  Created in {@link #setUp()}. */
  private CopyOnWriteSet<String> set;

  /** Dummy items used in the tests. */
  private String a;
  private String b;
  private String c;
  private String d;


  @Override
  protected void setUp() throws Exception {
    set = CopyOnWriteSet.create();
    a = "a";
    b = "b";
    c = "c";
    d = "d";
  }

  /**
   * Creates a list of items in the order returned by an iterator.
   *
   * @param i  an iterator
   * @return a list containing the items returned by iterating over {@code i}.
   */
  private static <T> List<T> asList(Iterator<T> i) {
    List<T> list = new ArrayList<T>();
    while (i.hasNext()) {
      list.add(i.next());
    }
    return list;
  }


  /**
   * Creates a list of items in the order returned by an iterable.
   *
   * @param xs  an iterable
   * @return a list containing the items returned by iterating over {@code xs}.
   */
  private static <T> List<T> asList(Iterable<T> xs) {
    return asList(xs.iterator());
  }

  /**
   * Creates a set containing the items returned by iterating over an
   * iterable (this is used to compare a CopyOnWriteSet to a Set, since
   * CopyOnWriteSet does not implement equality with Sets).
   *
   * @param xs  a CopyOnWriteSet
   * @return a set containing the items returned by iterating over {@code xs}.
   */
  private static <T> Set<T> asSet(Iterable<T> xs) {
    return new HashSet<T>(asList(xs));
  }

  /**
   * Creates a set containing the items returned by iterating over a
   * CopyOnWriteSet (this is used to compare a CopyOnWriteSet to a Set, since
   * CopyOnWriteSet does not implement equality with Sets).
   *
   * @param i  a CopyOnWriteSet
   * @return a set containing the items returned by iterating over {@code xs}.
   */
  private static <T> Set<T> asSet(Iterator<T> i) {
    return new HashSet<T>(asList(i));
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

  public void testAddWhileIteratingDoesNotCauseCme() {
    set.add(a);
    set.add(b);
    set.add(c);

    try {
      // We attempt to add d on every iteration.
      for (String x : set) {
        set.add(d);
      }
    } catch (ConcurrentModificationException e) {
      fail("addition during iteration caused CME");
    }
  }

  public void testRemoveWhileIteratingDoesNotCauseCme() {
    set.add(a);
    set.add(b);
    set.add(c);
    set.add(d);

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
  }

  public void testAddIsDelayedWhileIterating() {
    set.add(a);
    set.add(b);

    Iterator<String> i = set.iterator();
    Iterator<String> j = set.iterator();
    // try to add c, and check that it should not go into the iterable collection yet
    set.add(c);
    assertEquals(setOf(a, b), asSet(i));

    // try again
    set.add(c);
    assertEquals(setOf(a, b), asSet(j));

    // Test that it has been added as far as next iterator is concerned
    assertEquals("wrong size", 3, asList(set).size());
    assertEquals("c was not added", setOf(a, b, c), asSet(set));
  }

  public void testRemoveIsDelayedWhileLocked() {
    set.add(a);
    set.add(b);
    set.add(c);
    set.add(d);

    Iterator<String> i = set.iterator();
    Iterator<String> j = set.iterator();
    Iterator<String> k = set.iterator();
    // try to remove c, and check that it should still be in the iterable collection yet
    set.remove(c);
    assertEquals(setOf(a, b, c, d), asSet(i));
    // try again
    set.remove(c);
    assertEquals(setOf(a, b, c, d), asSet(j));
    // try again with d
    set.remove(d);
    assertEquals(setOf(a, b, c, d), asSet(k));

    // unlock, and test that c and d get removed.
    assertEquals("wrong size", 2, asList(set).size());
    assertEquals("c and/or d were not removed", setOf(a, b), asSet(set));
  }

  public void testConstructionDoesNotCreateNewCollection() {
    CopyOnWriteSet<?> fragile = new CopyOnWriteSet<Object>(new CopyOnWriteSet.CollectionFactory() {
      @Override
      public <T> Collection<T> copy(Collection<T> xs) {
        fail("Not lazy");
        // Never reached
        return null;
      }
    });

    fragile.clear();
    assertTrue(fragile.isEmpty());
    assertEquals(0, fragile.size());
  }
}
