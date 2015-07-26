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

import org.waveprotocol.wave.model.adt.MonotonicValue;
import org.waveprotocol.wave.model.adt.ObservableMonotonicValue;
import org.waveprotocol.wave.model.adt.docbased.TestUtil.ValueContext;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link DocumentBasedMonotonicValue}.
 *
 */

public class DocumentBasedMonotonicValueTest extends TestCase {

  private final static String CONTAINER_TAG = "supplement";
  private final static String ENTRY_TAG = "all";
  private final static String VALUE_ATTR = "read";

  private static final class MockListener<C extends Comparable<C>> implements
      ObservableMonotonicValue.Listener<C>{

    private final List<C> expectedValues;
    private int index = 0;

    public MockListener() {
      this.expectedValues = new ArrayList<C>();
    }

    @Override
    public void onSet(C oldValue, C newValue) {
      if (index == expectedValues.size() - 1) {
        fail("Unexpected event #" + (index + 1) + " " + oldValue + " -> " + newValue);
      }
      assertEquals(expectedValues.get(index), oldValue);
      index++;
      assertEquals(expectedValues.get(index), newValue);
    }

    void finished() {
      assertEquals(expectedValues.size() - 1, index);
    }

    C getLast() {
      if (expectedValues.isEmpty()) {
        throw new RuntimeException("Must have at least one expected value, which would be initial");
      }
      return expectedValues.get(expectedValues.size() - 1);
    }

    @SuppressWarnings("unchecked")
    void expect(C ... expectedValues) {
      this.expectedValues.addAll(Arrays.asList(expectedValues));
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
  private final static class FungeStack<N, E extends N, C extends Comparable<C>> {
    private final ValueContext<N, E> context;
    private final DocumentBasedMonotonicValue<E, C> target;
    private final Serializer<C> serializer;
    private final MockListener<C> listener = new MockListener<C>();

    public FungeStack(ValueContext<N, E> context,
        DocumentBasedMonotonicValue<E, C> target, Serializer<C> serializer) {
      this.context = context;
      this.target = target;
      this.serializer = serializer;
      target.addListener(listener);
    }

    void addEntry(C value) {
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put(VALUE_ATTR, serializer.toString(value));
      E child =
          context.doc.createChildElement(context.container, ENTRY_TAG, new AttributesImpl(attrs));
    }

    void assertSubstrateEquals(Integer ... expected) {
      ExtraAsserts.assertStructureEquivalent(substrate(expected).doc, context.doc);
    }
  }

  /** Target state, containing the map being tested and its subtrate. */
  private FungeStack<?, ?, Integer> stack;

  //
  // Test-specific setup helpers.
  //

  /**
   * Creates an target map based on an initial list of entries.  The entries
   * are used to build a substrate document, and the target map is instantiated
   * on that substrate.
   */
  private void createTargetOn(Integer ... state) {
    fungeCreateTargetOn(substrate(state));
  }

  // Funge method to work around Sun JDK's laughably poor type inference.
  private <N> void fungeCreateTargetOn(ValueContext<N, ?> context) {
    createTargetOn(context);
  }

  /**
   * Creates a target map on a substrate.
   */
  private <N, E extends N> void createTargetOn(ValueContext<N, E> context) {
    DocumentBasedMonotonicValue<E, Integer> target =
      DocumentBasedMonotonicValue.create(DefaultDocumentEventRouter.create(context.doc),
          context.container, Serializer.INTEGER, ENTRY_TAG, VALUE_ATTR);

    // Eventually, the target map and the substrate should be sufficient state for all tests.
    // However, in order to simulate document events, the two need to be wrapped together in a
    // FungeStack so that Java knows that the element type-parameters match.
    stack = new FungeStack<N, E, Integer>(context, target, Serializer.INTEGER);
  }

  /**
   * Creates a substrate based on a list of entries.
   *
   * @param values list of entries to include in the document state
   * @return a map-context view of the document state
   */
  private static ValueContext<?, ?> substrate(Integer ... values) {
    return substrate(BasicFactories.observableDocumentProvider().create("data",
        Collections.<String, String>emptyMap()),
        values);
  }

  /**
   * Populates a document with an initial map state defined by an entry list.
   *
   * @return a map-context view of the document state.
   */
  private static <N, E extends N> ValueContext<N, E> substrate(
      ObservableMutableDocument<N, E, ?> doc,
      Integer ... values) {
    // Insert container element
    E container = doc.createChildElement(doc.getDocumentElement(), CONTAINER_TAG,
        Collections.<String,String>emptyMap());

    // Insert entries
    for (Integer x : values) {
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put(VALUE_ATTR, Serializer.INTEGER.toString(x));
      doc.createChildElement(container, ENTRY_TAG, new AttributesImpl(attrs));
    }

    return new ValueContext<N, E>(doc, container);
  }

  /**
   * Asserts that the test-target's document substrate is in an expected state.
   *
   * @param expected  list of entries describing the expected state
   */
  private void assertSubstrateEquals(Integer ... expected) {
    stack.assertSubstrateEquals(expected);
  }

  /**
   * Creates an empty map as the test target.
   */
  private void createEmptyValue() {
    createTargetOn();
  }

  /**
   * Adds an entry to the target value's underlying state.  This simulates a
   * concurrent modification by some other agent.
   *
   * @param value
   */
  private void addEntry(int value) {
    stack.addEntry(value);
  }

  /**
   * @return the target map being tested.
   */
  private MonotonicValue<Integer> getTarget() {
    return stack.target;
  }

  /**
   * Sets up event handling mock
   * @param values history of values, including the initial one
   */
  private void expectHistory(Integer ... values) {
    stack.listener.expect(values);
  }

  private void check() {
    stack.listener.finished();
    assertEquals(new Integer(stack.listener.getLast()), getTarget().get());
  }

  /**
   * Checks the value is in the expected state, and that it is represented by
   * the given set of numbers.
   *
   * @param substrateValues
   */
  private void checkDirty(Integer ... substrateValues) {
    check();
    assertSubstrateEquals(substrateValues);
  }

  /**
   * Check the value is in the expected state, and that it is represented
   * cleanly; i.e., only one element in the substrate.
   */
  private void checkClean() {
    check();
    assertSubstrateEquals(stack.listener.getLast());
  }

  public void testSetOnEmptyStateIsReturnedByGetAndInsertsIntoSubstrate() {
    createEmptyValue();
    expectHistory(null, 10);
    getTarget().set(10);
    checkClean();
  }

  public void testLeavesObsoleteEntriesOnLoadButCleansOnWrite() {
    // Set up the target with some initial state.
    createTargetOn(10, 30, 20);
    assertSubstrateEquals(10, 30, 20);

    expectHistory(30, 40);
    getTarget().set(40);
    checkClean();
  }

  public void testSetOfALesserValuesDoesNothing() {
    // Set up the target with some initial state.
    createTargetOn(10);
    getTarget().set(5);

    assertEquals(new Integer(10), getTarget().get());
    assertSubstrateEquals(10);
  }

  public void testSetOfAGreaterValuesReplacesOld() {
    // Set up the target with some initial state.
    createTargetOn(10);
    expectHistory(10, 20);
    getTarget().set(20);
    checkClean();
  }

  public void testRemoteAddedLesserEntriesScheduledForRemoval() {
    // Set up the target with some initial state.

    createTargetOn(20);
    expectHistory(20);

    // Add an entry remotely.
    addEntry(10);
    checkDirty(20, 10);

    // Perform mutation, expecting cleanup.
    expectHistory(30);
    getTarget().set(30);
    checkClean();
  }

  public void testRemoteAddedGreaterEntriesReplaceOldEntryButIsNotCleaned() {
    // Set up the target with some initial state.
    createTargetOn(10);
    expectHistory(10, 20);

    // Add an entry remotely.
    addEntry(20);
    checkDirty(10, 20);
  }
}
