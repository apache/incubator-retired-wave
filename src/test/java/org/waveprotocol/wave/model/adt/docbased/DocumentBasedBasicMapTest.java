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
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link DocumentBasedBasicMap}, focussing particularly on
 * interaction with the underlying document storage.
 *
 */

public class DocumentBasedBasicMapTest extends TestCase {

  private static final String KEY1 = "bEefFaCe*2";
  private static final String KEY2 = "caFeBabE*9";

  private static final String CONTAINER_TAG = "supplement";
  private static final String ENTRY_TAG = "read";
  private static final String KEY_ATTR = "blipId";
  private static final String VALUE_ATTR = "version";

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
  private static final class FungeStack<N, E extends N, K, V> {
    private final ValueContext<N, E> context;
    private final DocumentBasedBasicMap<E, K, V> target;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

    public FungeStack(ValueContext<N, E> context,
        DocumentBasedBasicMap<E, K, V> target,
        Serializer<K> keySerializer, Serializer<V> valueSerializer) {
      this.context = context;
      this.target = target;
      this.keySerializer = keySerializer;
      this.valueSerializer = valueSerializer;
    }

    void addEntry(K key, V value) {
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put(KEY_ATTR, keySerializer.toString(key));
      attrs.put(VALUE_ATTR, valueSerializer.toString(value));
      E child = context.doc.createElement(Point.start(context.doc, context.container),
                                          ENTRY_TAG,
                                          new AttributesImpl(attrs));
    }

    void removeEntries(K key) {
      N curChild = context.doc.getFirstChild(context.container);
      String keyString = keySerializer.toString(key);
      E e = DocHelper.getFirstChildElement(context.doc, context.container);
      while (e != null) {
        if (ENTRY_TAG.equals(context.doc.getTagName(e))
            && keyString.equals(context.doc.getAttribute(e, KEY_ATTR))) {
              context.doc.deleteNode(e);
        }
        e = DocHelper.getNextSiblingElement(context.doc, e);
      }
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
  private static class MockListener<K, V> implements Listener<K, V> {
    private final ListBuilder<K, V> entries = new ListBuilder<K, V>();

    @Override
    public void onEntrySet(K key, V oldValue, V newValue) {
      entries.add(key, newValue);
    }

    public ListBuilder<K, V> getEntries() {
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
    DocumentBasedBasicMap<E, String, Integer> target =
      DocumentBasedBasicMap.create(DefaultDocumentEventRouter.create(context.doc),
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
        Collections.<String, String>emptyMap());

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
   * Remove entries with the specified keys from the target map's underlying state.
   * This simulates a concurrent modification by some other agent.
   */
  private void removeEntries(String key) {
    stack.removeEntries(key);
  }

  /**
   * @return the target map being tested.
   */
  private DocumentBasedBasicMap<?, String, Integer> getTarget() {
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

  public void testPutOfLaterValuesReplaceOld() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    getTarget().put(KEY1, 05);
    assertEquals(new Integer(05), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 05)
        .add(KEY2, 20));

    getTarget().put(KEY1, 30);
    assertEquals(new Integer(30), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 30)
        .add(KEY2, 20));
  }

  public void testRemoteAddedLaterValueObviatesOldEntry() {
    // Set up the target with some initial state.
    createTargetOn(new ListBuilder<String, Integer>()
        .add(KEY1, 30)
        .add(KEY2, 20));

    // Replace an entry remotely.
    removeEntries(KEY1);
    addEntry(KEY1, 10);

    assertEquals(new Integer(10), getTarget().get(KEY1));
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY1, 10)
        .add(KEY2, 20));

    // Mutate locally, expect cleanup.
    getTarget().put(KEY2, 50);
    assertSubstrateEquals(new ListBuilder<String, Integer>()
        .add(KEY2, 50)
        .add(KEY1, 10));
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

    getTarget().put(KEY1, 5);
    List<Entry<String, Integer>> receivedEntries = listener.getEntries().build();
    assertEquals(1, receivedEntries.size());
  }
}
