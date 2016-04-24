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
 * Tests for the {@link ObservableBasicMapImpl} class.
 *
 */

public abstract class ObservableBasicMapTestBase extends TestCase {
  /** A map listener that exposes the values it hears. */
  static class Listener implements ObservableBasicMap.Listener<String, Integer> {
    int callCount = 0;
    String lastKey = null;
    Integer lastNewValue = null;
    Integer lastOldValue = null;

    Listener() {
    }

    /** Checks just the number of calls to the listener. */
    void check(int callCount) {
      assertEquals(callCount, this.callCount);
    }

    /** Checks all stored values. */
    void check(int callCount, String lastKey, Integer lastOldValue, Integer lastNewValue) {
      check(callCount);
      assertEquals(lastKey, this.lastKey);
      assertEquals(lastOldValue, this.lastOldValue);
      assertEquals(lastNewValue, this.lastNewValue);
    }

    @Override
    public void onEntrySet(String key, Integer oldValue, Integer newValue) {
      callCount++;
      lastKey = key;
      lastOldValue = oldValue;
      lastNewValue = newValue;
    }
  }

  /**
   * Makes a HashSet from the given iterable. Checks that any item is only
   * present once.
   */
  private static Set<String> makeHashSet(Iterable<String> iterable) {
    Set<String> result = CollectionUtils.newHashSet();
    for (String s : iterable) {
      assertTrue(result.add(s));
    }
    return result;
  }

  /** The map under test. */
  protected ObservableBasicMap<String, Integer> map;

  /** Constructor. */
  public ObservableBasicMapTestBase() {
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createMap();
  }


  /**
   * Creates the ObservableBasicMap instance on which to run the tests specified
   * in this class. This method is called by {@link #setUp()} and should set the
   * {@link #map} variable.
   */
  abstract protected void createMap();

  /** Tests listeners get events. */
  public void testEvents() {
    Listener listener1 = new Listener();
    Listener listener2 = new Listener();
    map.addListener(listener1);

    // New values
    map.put("A", 100);
    listener1.check(1, "A", null, 100);
    map.put("B", 200);
    listener1.check(2, "B", null, 200);
    map.put("A", 300);
    listener1.check(3, "A", 100, 300);

    // No change
    map.put("B", 200);
    listener1.check(3);

    // Add a second listener - both should get same event
    map.addListener(listener2);
    listener2.check(0);
    map.put("B", 400);
    listener1.check(4, "B", 200, 400);
    listener2.check(1, "B", 200, 400);

    // Remove one listener
    map.removeListener(listener1);
    map.put("A", 500);
    listener1.check(4); // no more events here
    listener2.check(2, "A", 300, 500); // but one here
  }

  /** Tests simple getting and putting of values. */
  public void testGetPut() {
    // New keys ought to have null values
    assertNull(map.get("A"));
    assertNull(map.get("B"));

    // Put values in, and get them out again
    assertTrue(map.put("A", 1));
    assertTrue(map.put("B", 2));
    assertEquals(Integer.valueOf(1), map.get("A"));
    assertEquals(Integer.valueOf(2), map.get("B"));

    // Rewrite a value
    assertTrue(map.put("A", 3));
    assertEquals(Integer.valueOf(3), map.get("A"));

    // Overwrite with same value
    assertFalse(map.put("A", 3));
    assertEquals(Integer.valueOf(3), map.get("A"));
  }

  /** Tests functionality of keySet method. */
  public void testKeySet() {
    // Should be empty to begin with
    assertEquals(CollectionUtils.immutableSet(), makeHashSet(map.keySet()));

    // Test with adding data
    map.put("A", 1);
    map.put("B", 2);
    assertEquals(CollectionUtils.immutableSet("A", "B"), makeHashSet(map.keySet()));
    map.put("C", 3);
    assertEquals(CollectionUtils.immutableSet("A", "B", "C"), makeHashSet(map.keySet()));

    // Remove something
    map.remove("B");
    assertEquals(CollectionUtils.immutableSet("A", "C"), makeHashSet(map.keySet()));
  }


  /** Tests basic functionality of remove method. */
  public void testRemove() {
    assertNull(map.get("A"));
    map.put("A", 1);
    assertEquals(Integer.valueOf(1), map.get("A"));

    map.remove("A");
    assertNull(map.get("A"));
  }
}
