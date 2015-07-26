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

package org.waveprotocol.wave.model.adt;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Set;

/**
 * Base class defining common tests for the {@link ObservableBasicSet} class.
 *
 */

public abstract class ObservableBasicSetTestBase extends TestCase {

  /** A set listener that exposes the values it hears. */
  static class Listener implements ObservableBasicSet.Listener<String> {
    int addCount = 0;
    String lastValue = null;
    int removeCount = 0;

    Listener() {
    }

    /** Checks just the number of calls to the listener. */
    void check(int addCount, int removeCount) {
      assertEquals(addCount, this.addCount);
      assertEquals(removeCount, this.removeCount);
    }

    /** Checks all stored values. */
    void check(int addCount, int removeCount, String lastValue) {
      check(addCount, removeCount);
      assertEquals(lastValue, this.lastValue);
    }

    @Override
    public void onValueAdded(String newValue) {
      lastValue = newValue;
      addCount++;
    }

    @Override
    public void onValueRemoved(String oldValue) {
      lastValue = oldValue;
      removeCount++;
    }
  }

  /**
   * Makes a HashSet from the given iterable. Checks that any item is only
   * present once.
   */
  private static Set<String> makeHashSet(BasicSet<String> basicSet) {
    Set<String> result = CollectionUtils.newHashSet();
    for (String s : basicSet.getValues()) {
      if (result.contains(s)) {
        fail("Value " + s + " already present in set");
      }
      result.add(s);
    }
    return result;
  }

  /** The set on which tests are being run. */
  protected ObservableBasicSet<String> set;

  /** Constructor. */
  public ObservableBasicSetTestBase() {
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createEmptyMap();
  }

  /**
   * Creates the ObservableBasicSet instance on which to run the tests specified
   * in this class. This method is called by {@link #setUp()} and should set the
   * {@link #set} variable.
   */
  abstract protected void createEmptyMap();

  /** Tests simple add and remove functionality. */
  public void testAddRemoveContains() {
    assertTrue(makeHashSet(set).isEmpty());

    set.add("A");
    set.add("B");
    set.add("C");
    assertEquals(CollectionUtils.immutableSet("A", "B", "C"), makeHashSet(set));
    assertTrue(set.contains("B"));
    assertFalse(set.contains("D"));

    set.add("B");
    assertEquals(CollectionUtils.immutableSet("A", "B", "C"), makeHashSet(set));

    set.remove("B");
    assertEquals(CollectionUtils.immutableSet("A", "C"), makeHashSet(set));
    assertFalse(set.contains("B"));

    set.remove("C");
    set.remove("A");
    assertTrue(makeHashSet(set).isEmpty());
  }

  /** Tests the clear method. */
  public void testClear() {
    Listener listener = new Listener();
    set.addListener(listener);

    set.add("A");
    set.add("B");
    set.add("C");
    assertEquals(CollectionUtils.immutableSet("A", "B", "C"), makeHashSet(set));
    listener.check(3, 0);

    set.clear();
    assertTrue(makeHashSet(set).isEmpty());
    listener.check(3, 3);
  }

  /** Tests Listeners. */
  public void testEvents() {
    Listener listener1 = new Listener();
    Listener listener2 = new Listener();

    set.addListener(listener1);
    listener1.check(0, 0);

    set.add("A");
    listener1.check(1, 0, "A");
    set.add("B");
    listener1.check(2, 0, "B");
    set.add("A");
    listener1.check(2, 0);

    set.remove("A");
    listener1.check(2, 1, "A");

    set.addListener(listener2);
    listener2.check(0, 0);
    set.add("B");
    listener1.check(2, 1);
    listener2.check(0, 0);

    set.add("C");
    listener1.check(3, 1, "C");
    listener2.check(1, 0, "C");

    set.add("A");
    listener1.check(4, 1, "A");
    listener2.check(2, 0, "A");

    set.removeListener(listener1);
    set.remove("A");
    listener1.check(4, 1);
    listener2.check(2, 1, "A");

    set.add("D");
    listener1.check(4, 1);
    listener2.check(3, 1, "D");
  }
}
