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

package org.waveprotocol.wave.model.adt.docbased;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.adt.ObservableBasicMap.Listener;
import org.waveprotocol.wave.model.adt.docbased.TestUtil.ValueContext;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link DocumentBasedMonotonicMap}.
 *
 */

public class DocumentBasedMonotonicMapTest extends TestCase {

  private final static String KEY1 = "bEefFaCe*2";
  private final static String KEY2 = "caFeBabE*9";

  private final static String CONTAINER_TAG = "supplement";
  private final static String ENTRY_TAG = "read";
  private final static String KEY_ATTR = "blipId";
  private final static String VALUE_ATTR = "version";

  //
  // The ADT used in these tests to represent underlying map state is just a list of entries.
  // A Map can not be used, because one aspect to be tested is that the underlying document state
  // can transitionally contain duplicate entries, and the expected-value type must be able to
  // describe such states.
  //

  /**
   * A key-value pair.
   */
  private static class Entry<K, V> {
    final K key;
    final V value;

    Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  /**
   * Builder of a list of entries.
   */
  private static class ListBuilder<K, V> {
    private final List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>();

    ListBuilder <K, V> add(K key, V value) {
      entries.add(new Entry<K, V>(key, value));
      return this;
    }

    List<Entry<K, V>> build() {
      return entries;
    }
  }

  /**
   * Hack for simulating mutation events caused by a remote agent making
   * modifications to the document underlying the map.
   *
   * Used to be used to simulate document events. Documents now broadcast
   * events.
   *
   * TODO(user): delete all the scaffolding that's obviated by real events.
   */
  private final static class FungeStack<N, E extends N, K, C extends Comparable<C>> {
    private final ValueContext<N, E> context;
    private final DocumentBasedMonotonicMap<E, K, C> target;
    private final Serializer<K> keySerializer;
    private final Serializer<C> valueSerializer;

    public FungeStack(ValueContext<N, E> context,
        DocumentBasedMonotonicMap<E, K, C> target,
        Serializer<K> keySerializer, Serializer<C> valueSerializer) {
      this.context = context;
      this.target = target;
      this.keySerializer = keySerializer;
      this.valueSerializer = valueSerializer;
    }

    void addEntry(K key, C value) {
      String keyString = keySerializer.toString(key);
      String valueString = valueSerializer.toString(value);
      Attributes attrs = new AttributesImpl(KEY_ATTR, keyString, VALUE_ATTR, valueString);
      E child = context.doc.createChildElement(context.container, ENTRY_TAG, attrs);
    }

    void assertSubstrateEquals(ListBuilder<String, Integer> expected) {
      // Check that the monotonic map deleted the first entry.  The simplest
      // approach to check this is to check that the new substrate equals an
      // expected document structure (done via XML comparison).
      ExtraAsserts.assertStructureEquivalent(substrate(expected).doc, context.doc);
    }
  }

  /**
   * Mock listener for map events.
   */
  private static class MockListener<K, C> implements Listener<K, C> {
    private final ListBuilder<K, C> entries = new ListBuilder<K, C>();

    @Override
    public void onEntrySet(K key, C oldValue, C newValue) {
      entries.add(key, newValue);
    }

    public ListBuilder<K, C> getEntries() {
      return entries;
    }
  }

  /** Target state, containing the map being tested and its subtrate. */
  private FungeStack<?, ?, String, Integer> stack;

  //
  // Test-specific setup helpers.
  //

  /**
   * Creates an target map based on an initial list of entries.  The entries
   * are used to build a substrate document, and the target map is instantiated
   * on that substrate.
   */
  private void createTargetOn(ListBuilder<String, Integer> builder) {
    fungeCreateTargetOn(substrate(builder));
  }

  // Funge method to work around Sun JDK's laughably poor type inference.
  private <N> void fungeCreateTargetOn(ValueContext<N, ?> context) {
    createTargetOn(context);
  }

  /**
   * Creates a target map on a substrate.
   */
  private <N, E extends N> void createTargetOn(ValueContext<N, E> context) {
    DocumentBasedMonotonicMap<E, String, Integer> target =
      DocumentBasedMonotonicMap.create(DefaultDocumentEventRouter.create(context.doc),
          context.container, Serializer.STRING, Serializer.INTEGER, ENTRY_TAG, KEY_ATTR,
          VALUE_ATTR);

    // Eventually, the target map and the substrate should be sufficient state for all tests.
    // However, in order to simulate document events, the two need to be wrapped together in a
    // FungeStack so that Java knows that the element type-parameters match.
    stack = new FungeStack<N, E, String, Integer>(
        context, target, Serializer.STRING, Serializer.INTEGER);
  }

  /**
   * Creates a substrate based on a list of entries.
   *
   * @param values list of entries to include in the document state
   * @return a map-context view of the document state
   */
  private static ValueContext<?, ?> substrate(ListBuilder<String, Integer> values) {
    return substrate(BasicFactories.observableDocumentProvider().create("data",
        Collections.<String, String>emptyMap()), values);
  }

  /**
   * Populates a document with an initial map state defined by an entry list.
   *
   * @return a map-context view of the document state.
   */
  private static <N, E extends N> ValueContext<N, E> substrate(
      ObservableMutableDocument<N, E, ?> doc,
      ListBuilder<String, Integer> values) {
    // Insert container element
    E container = doc.createChildElement(doc.getDocumentElement(), CONTAINER_TAG,
        Collections.<String,String>emptyMap());

    // Insert entries
    for (Entry<String, Integer> e : values.build()) {
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put(KEY_ATTR, e.key);
      attrs.put(VALUE_ATTR, Serializer.INTEGER.toString(e.value));
      doc.createChildElement(container, ENTRY_TAG, new AttributesImpl(attrs));
    }

    return new ValueContext<N, E>(doc, container);
  }

  /**
   * Asserts that the test-target's document substrate is in an expected state.
   *
   * @param expected  list of entries describing the expected state
   */
  private void assertSubstrateEquals(ListBuilder<String, Integer> expected) {
    stack.assertSubstrateEquals(expected);
  }

  /**
   * Creates an empty map as the test target.
   */
  private void createEmptyMap() {
    createTargetOn(new ListBuilder<String, Integer>());
  }

  /**
   * Adds an entry to the target map's underlying state.  This simulates a
   * concurrent modification by some other agent.
   *
   * @param key
   * @param value
   */
  private void addEntry(String key, Integer value) {
    stack.addEntry(key, value);
  }

  /**
   * @return the target map being tested.
   */
  private DocumentBasedMonotonicMap<?, String, Integer> getTarget() {
    return stack.target;
  }

  public void testPutOnEmptyMapIsReturnedByGet() {
    createEmptyMap();
    getTarget().put(KEY1, 10);
    assertEquals(new Integer(10), getTarget().get(KEY1));
  }

  public void testPutOnEmptyMapInsertsIntoSubstrate() {
    createEmptyMap();
    getTarget().put(KEY1, 10);
    assertSubstrateEquals(new ListBuilder<String, Integer>().add(KEY1, 10));
  }

  public void testLoadLeavesOverridenEntriesInSubstrateButCleansOnWrite() {
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20)
        .add(KEY1, 30));

    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20)
        .add(KEY1, 30));

    getTarget().put(KEY2, 50);
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY2, 50)
        .add(KEY1, 30));
  }

  public void testPutOfALesserValueDoesNothing() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    getTarget().put(KEY1, 05);

    assertEquals(new Integer(10), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));
  }

  public void testPutOfAnEqualValueDoesNothing() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    getTarget().put(KEY2, 20);

    // If the substrate were to have been rewritten, we'd expect to find KEY2 at
    // the start of the document as a newly written entry. Therefore, we test
    // that it remains at its old location.
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));
  }

  public void testRemovePutWithLesserValues() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    getTarget().remove(KEY1);
    getTarget().put(KEY1, 5);

    assertEquals(new Integer(5), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 5)
        .add(KEY2, 20));
  }

  public void testPutOfAGreaterValueReplacesOld() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    getTarget().put(KEY1, 30);

    assertEquals(new Integer(30), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 30)
        .add(KEY2, 20));
  }

  public void testRemoteAddedLesserEntriesGetScheduledForRemoval() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 30)
        .add(KEY2, 20));

    // Add an entry remotely.
    addEntry(KEY1, 10);

    assertEquals(new Integer(30), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 30)
        .add(KEY2, 20)
        .add(KEY1, 10));

    getTarget().put(KEY2, 50);
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY2, 50)
        .add(KEY1, 30));
  }

  public void testRemoteAddedGreaterEntriesObviateOldEntry() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    // Add an entry remotely.
    addEntry(KEY1, 30);

    assertEquals(new Integer(30), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20)
        .add(KEY1, 30));

    // Mutate locally, expect cleanup
    getTarget().put(KEY1, 50);

    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 50)
        .add(KEY2, 20));
  }

  public void testPutOfNewEntryTriggersEvent() {
    createEmptyMap();

    MockListener<String, Integer> listener = new MockListener<String, Integer>();
    getTarget().addListener(listener);
    getTarget().put(KEY1, 10);

    List<Entry<String, Integer>> receivedEntries = listener.getEntries().build();
    assertEquals(1, receivedEntries.size());
  }

  public void testReplacementEntryTriggersSingleEvent() {
    createEmptyMap();
    getTarget().put(KEY1, 10);

    MockListener<String, Integer> listener = new MockListener<String, Integer>();
    getTarget().addListener(listener);

    getTarget().put(KEY1, 20);
    List<Entry<String, Integer>> receivedEntries = listener.getEntries().build();
    assertEquals(1, receivedEntries.size());
  }

}
