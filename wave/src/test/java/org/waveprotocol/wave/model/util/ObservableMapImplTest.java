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


import junit.framework.Assert;
import junit.framework.TestCase;

import org.waveprotocol.wave.model.wave.ObservableMap;
import org.waveprotocol.wave.model.wave.ObservableMapImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Test case for {@link ObservableMapImpl}.
 *
 */

public final class ObservableMapImplTest extends TestCase {

  private static class MockListener<K, V> implements ObservableMap.Listener<K, V> {
    private final Queue<Pair<K, V>> added = CollectionUtils.createQueue();
    private final Queue<Pair<K, V>> removed = CollectionUtils.createQueue();

    private final ObservableMap.Listener<K, V> verifier = new ObservableMap.Listener<K, V>() {
      @Override
      public void onEntryAdded(K key, V value) {
        assertEquals(Pair.of(key, value), added.remove());
      }

      @Override
      public void onEntryRemoved(K key, V value) {
        assertEquals(Pair.of(key, value), removed.remove());
      }
    };

    @Override
    public void onEntryAdded(K key, V value) {
      added.add(Pair.of(key, value));
    }

    @Override
    public void onEntryRemoved(K key, V value) {
      removed.add(Pair.of(key, value));
    }

    ObservableMap.Listener<K, V> verify() {
      return verifier;
    }

    void verifyNoMoreInteractions() {
      assertTrue(added.isEmpty());
      assertTrue(removed.isEmpty());
    }
  }

  private static final Object a = new Object();
  private static final Object b = new Object();

  private ObservableMapImpl<String, Object> target;
  private MockListener<String, Object> listener;

  @Override
  protected void setUp() {
    target = ObservableMapImpl.create();
    listener = new MockListener<String, Object>();
  }

  private static <T> void assertEquals(Set<T> expected, Iterable<T> actual) {
    Set<T> actualSet = new HashSet<T>();
    for (T x : actual) {
      actualSet.add(x);
    }
    Assert.assertEquals(new HashSet<T>(expected), actualSet);
  }

  //
  // Basic structural tests.
  //

  public void testPutIsReturnedByGet() {
    target.put("a", a);
    assertEquals(a, target.get("a"));
  }

  public void testTwoPutsAreReturnedByTwoGets() {
    target.put("a", a);
    target.put("b", b);
    assertEquals(a, target.get("a"));
    assertEquals(b, target.get("b"));
  }

  public void testPutClobbers() {
    target.put("a", new Object());
    target.put("a", a);
    assertEquals(a, target.get("a"));
  }

  public void testRemoveIsNotReturnedByGet() {
    target.put("a", a);
    target.remove("a");
    assertNull(target.get("a"));
  }

  public void testTwoRemovesAreNotReturnedByTwoGets() {
    target.put("a", a);
    target.put("b", b);
    target.remove("a");
    target.remove("b");
    assertNull(target.get("a"));
    assertNull(target.get("b"));
  }

  public void testRemoveOnlyRemovesOne() {
    target.put("a", a);
    target.put("b", b);
    target.remove("a");
    assertEquals(b, target.get("b"));
  }

  //
  // Keys
  //

  public void testKeyCollectionWithOneKey() {
    target.put("a", new Object());
    assertEquals(Collections.singleton("a"), target.copyKeys());
  }

  public void testKeyCollectionWithManyKeys() {
    target.put("a", new Object());
    target.put("b", new Object());
    target.put("c", new Object());
    target.put("d", new Object());
    assertEquals(CollectionUtils.newHashSet("a", "b", "c", "d"), target.copyKeys());
  }

  public void testKeyCollectionWithManyKeysAfterMutations() {
    target.put("a", new Object());
    target.put("b", new Object());
    target.put("c", new Object());
    target.put("d", new Object());

    target.remove("c");
    target.remove("d");

    target.put("d", new Object());

    assertEquals(CollectionUtils.newHashSet("a", "b", "d"), target.copyKeys());
  }

  //
  // Events.
  //

  public void testSinglePutBroadcastsEvent() {
    target.addListener(listener);
    target.put("a", a);
    listener.verify().onEntryAdded("a", a);
    listener.verifyNoMoreInteractions();
  }

  public void testTwoPutsBroadcastTwoEvents() {
    target.addListener(listener);
    target.put("a", a);
    target.put("b", b);
    listener.verify().onEntryAdded("a", a);
    listener.verify().onEntryAdded("b", b);
    listener.verifyNoMoreInteractions();
  }

  public void testObserveAfterPutDoesNotBroadcastPriorEvent() {
    target.put("a", a);
    target.addListener(listener);
    target.put("b", b);
    listener.verify().onEntryAdded("b", b);
    listener.verifyNoMoreInteractions();
  }

  public void testRemoveBroadcastsEvent() {
    target.addListener(listener);
    target.put("a", a);
    target.remove("a");
    listener.verify().onEntryRemoved("a", a);
  }

  public void testClobberBroadcastsRemoveThenAdd() {
    target.put("x", a);
    target.addListener(listener);
    target.put("x", b);
    listener.verify().onEntryRemoved("x", a);
    listener.verify().onEntryAdded("x", b);
    listener.verifyNoMoreInteractions();
  }
}
