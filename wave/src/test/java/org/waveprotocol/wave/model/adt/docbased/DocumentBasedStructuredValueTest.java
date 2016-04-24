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

import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.TestUtil.ValueContext;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Tests for the document-based structured value.
 *
 * @author anorth@google.com (Alex North)
 */

public class DocumentBasedStructuredValueTest extends TestCase {
  /** Key type for testing. */
  private static enum Key { NAME1, NAME2 }

  private static final String CONTAINER_TAG = "container";

  /**
   * An observer which allows setting and checking of expected events.
   */
  private static class MockStructuredValueObserver implements
      ObservableStructuredValue.Listener<Key, Integer> {
    Queue<Pair<Map<Key, Integer>, Map<Key, Integer>>> expectations =
        new LinkedList<Pair<Map<Key, Integer>, Map<Key, Integer>>>();

    public void expectValuesChanged(Map<Key, Integer> oldValues,
        Map<Key, Integer> newValues) {
      expectations.add(Pair.of(oldValues, newValues));
    }

    public void expectDeletion() {
      expectations.add(null);
    }

    public void checkExpectationsSatisfied() {
      assertTrue(expectations.isEmpty());
    }

    @Override
    public void onValuesChanged(Map<Key, ? extends Integer> oldValues,
        Map<Key, ? extends Integer> newValues) {
      Pair<Map<Key, Integer>, Map<Key, Integer>> expected = expectations.remove();
      assertEquals(expected.first, oldValues);
      assertEquals(expected.second, newValues);
    }

    public void onDeleted() {
      assertNull(expectations.remove());
    }
  }

  /** Value under test. */
  private ObservableStructuredValue<Key, Integer> value;
  private ValueContext<?, ?> context;

  public void testInitialiser() {
    Initializer init = DocumentBasedStructuredValue.createInitialiser(Serializer.INTEGER,
        CollectionUtils.immutableMap(Key.NAME1, 23, Key.NAME2, 42));
    TestUtil.assertInitializerValues(CollectionUtils.immutableMap(Key.NAME1.toString(), "23",
        Key.NAME2.toString(), "42"), init);

    init = DocumentBasedStructuredValue.createInitialiser(Serializer.INTEGER,
        CollectionUtils.immutableMap(Key.NAME1, (Integer) null));
    TestUtil.assertInitializerValues(Collections.<String, String> emptyMap(), init);
  }

  public void testEmptyAttributeIsNull() {
    createEmptyTarget();
    assertNull(value.get(Key.NAME1));
  }

  // Getting and setting.
  // Each test involving set() is performed once with set(String, String)
  // and once with set(Map).

  public void testSetOnEmptyStateIsReturnedByGet() {
    // set(String, String).
    createEmptyTarget();
    value.set(Key.NAME1, 42);
    assertEquals(new Integer(42), value.get(Key.NAME1));
    assertNull(value.get(Key.NAME2));

    // set(Map).
    createEmptyTarget();
    value.set(CollectionUtils.immutableMap(Key.NAME1, 42));
    assertEquals(new Integer(42), value.get(Key.NAME1));
    assertNull(value.get(Key.NAME2));
  }

  public void testSetOnEmptyStateInsertsIntoSubstrate() {
    // set(String, String).
    createEmptyTarget();
    value.set(Key.NAME1, 10);
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 10));
    value.set(Key.NAME2, 20);
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));

    // set(Map).
    createEmptyTarget();
    value.set(CollectionUtils.immutableMap(Key.NAME1, 10));
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 10));
    value.set(CollectionUtils.immutableMap(Key.NAME2, 20));
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
  }

  public void testSubstrateValueIsReflected() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10));
    assertEquals(new Integer(10), value.get(Key.NAME1));
    assertNull(value.get(Key.NAME2));
  }

  public void testSetReplacesValue() {
    // set(String, String).
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    value.set(Key.NAME1, 5);
    assertEquals(new Integer(5), value.get(Key.NAME1));
    assertEquals(new Integer(20), value.get(Key.NAME2));
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 20));

    // set(Map).
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    value.set(CollectionUtils.immutableMap(Key.NAME1, 5));
    assertEquals(new Integer(5), value.get(Key.NAME1));
    assertEquals(new Integer(20), value.get(Key.NAME2));
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 20));
  }

  public void testSetNullClearsValue() {
    // set(String, String).
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10));
    value.set(Key.NAME1, null);
    assertNull(value.get(Key.NAME1));
    assertSubstrateEquals(new HashMap<Key, Integer>());

    // set(Map).
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10));
    value.set(CollectionUtils.immutableMap(Key.NAME1, (Integer) null));
    assertNull(value.get(Key.NAME1));
    assertSubstrateEquals(new HashMap<Key, Integer>());
  }

  public void testRemoteSetReplacesValue() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    setValue(Key.NAME1, 5);

    assertEquals(new Integer(5), value.get(Key.NAME1));
    assertEquals(new Integer(20), value.get(Key.NAME2));
    assertSubstrateEquals(CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 20));
  }

  // Events.


  public void testDeletionProducesEvent() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    MockStructuredValueObserver observer = new MockStructuredValueObserver();
    value.addListener(observer);

    observer.expectDeletion();
    context.delete();
  }

  public void testSetSingleFromNullProducesEvent() {
    createEmptyTarget();
    MockStructuredValueObserver observer = new MockStructuredValueObserver();
    value.addListener(observer);

    observer.expectValuesChanged(CollectionUtils.immutableMap(Key.NAME1, (Integer) null),
        CollectionUtils.immutableMap(Key.NAME1, 42));
    value.set(Key.NAME1, 42);
    observer.checkExpectationsSatisfied();
  }

  public void testSetNullProducesEvent() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    MockStructuredValueObserver observer = new MockStructuredValueObserver();
    value.addListener(observer);

    observer.expectValuesChanged(CollectionUtils.immutableMap(Key.NAME1, 10),
        CollectionUtils.immutableMap(Key.NAME1, (Integer) null));
    value.set(Key.NAME1, null);
    observer.checkExpectationsSatisfied();
  }

  public void testSetManyProducesOneEvent() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    MockStructuredValueObserver observer = new MockStructuredValueObserver();
    value.addListener(observer);

    observer.expectValuesChanged(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20),
        CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 7));
    value.set(CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 7));
    observer.checkExpectationsSatisfied();
  }

  public void testRemoteSetManyProducesOneEvent() {
    createTargetOn(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20));
    MockStructuredValueObserver observer = new MockStructuredValueObserver();
    value.addListener(observer);

    observer.expectValuesChanged(CollectionUtils.immutableMap(Key.NAME1, 10, Key.NAME2, 20),
        CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 7));
    setValues(CollectionUtils.immutableMap(Key.NAME1, 5, Key.NAME2, 7));
    observer.checkExpectationsSatisfied();
  }

  // Helpers.

  /**
   * Initialises a target element representing an empty value.
   */
  private void createEmptyTarget() {
    createTargetOn(Collections.<Key, Integer> emptyMap());
  }

  /**
   * Initialises a target element with preset values.
   */
  private void createTargetOn(Map<Key, Integer> values) {
    context = substrate(values);
    value = createTargetOn(context);
  }

  private static <N> ObservableStructuredValue<Key, Integer> createTargetOn(
      ValueContext<N, ?> context) {
    return createTargetOn2(context);
  }

  private static <N, E extends N> ObservableStructuredValue<Key, Integer> createTargetOn2(
      ValueContext<N, E> context) {
    return DocumentBasedStructuredValue.create(DefaultDocumentEventRouter.create(context.doc),
        context.container, Serializer.INTEGER, Key.class);
  }

  /**
   * Creates a substrate TODO based on a list of entries.
   *
   * @return a map-context view of the document state
   */
  private static ValueContext<?, ?> substrate(Map<Key, Integer> values) {
    return substrate(BasicFactories.observableDocumentProvider().create("tagname",
        Collections.<String, String>emptyMap()), values);
  }

  /**
   * Populates a document with an initial map state TODO defined by an entry list.
   *
   * @return a map-context view of the document state.
   */
  private static <N, E extends N> ValueContext<N, E> substrate(
      ObservableMutableDocument<N, E, ?> doc, Map<Key, Integer> values) {
    // Insert container element.
    E container = doc.createChildElement(doc.getDocumentElement(), CONTAINER_TAG,
        Collections.<String,String>emptyMap());
    // Set attributes.
    for (Map.Entry<Key, Integer> entry : values.entrySet()) {
      doc.setElementAttribute(container, entry.getKey().toString(),
          Serializer.INTEGER.toString(entry.getValue()));
    }
    return new ValueContext<N, E>(doc, container);
  }

  /**
   * Sets the substrate value for a field.
   */
  private void setValue(Key name, Integer value) {
    setValue1(context, name, value);
  }

  private <N> void setValue1 (ValueContext<N, ?> context, Key name, Integer value) {
    setValue2(context, name, value);
  }

  private <N, E extends N> void setValue2(ValueContext<N, E> context, Key name, Integer value) {
    context.doc.setElementAttribute(context.container, name.toString(),
        Serializer.INTEGER.toString(value));
  }

  /**
   * Sets the substrate value for a number of fields simultaneously.
   */
  private void setValues(Map<Key, Integer> values) {
    setValues1(context, values);
  }

  private <N> void setValues1(ValueContext<N, ?> context, Map<Key, Integer> values) {
    setValues2(context, values);
  }


  private <N, E extends N> void setValues2(ValueContext<N, E> context,
      Map<Key, Integer> values) {
    Map<String, String> valueStrings = CollectionUtils.newHashMap();
    for (Map.Entry<Key, Integer> entry : values.entrySet()) {
      valueStrings.put(entry.getKey().toString(), Serializer.INTEGER.toString(entry.getValue()));
    }
    context.doc.updateElementAttributes(context.container, valueStrings);
  }

  /**
   * Asserts that the test-target's document substrate is in an expected state.
   *
   * @param expected map of entries describing the expected state
   */
  private void assertSubstrateEquals(Map<Key, Integer> expected) {
    ExtraAsserts.assertStructureEquivalent(substrate(expected).doc, context.doc);
  }
}
