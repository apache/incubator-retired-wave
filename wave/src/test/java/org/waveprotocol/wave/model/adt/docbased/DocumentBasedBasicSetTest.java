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

import org.waveprotocol.wave.model.adt.BasicSet;
import org.waveprotocol.wave.model.adt.docbased.TestUtil.ValueContext;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.ExtraAsserts;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Test cases for storage layer of {@link DocumentBasedBasicSet}.
 *
 */

public class DocumentBasedBasicSetTest extends TestCase {

  private final static String CONTAINER_TAG = "folders";
  private final static String ENTRY_TAG = "folder";
  private final static String VALUE_ATTR = "i";

  /**
   * Hack for simulating mutation events caused by a remote agent making
   * modifications to the document underlying the map.  If wave documents ever
   * support events, this class can disappear, because it really just serves to
   * bind the map's element type-parameter with the substrate document's
   * element type-parameter, in order that
   * {@link DocumentBasedMonotonicMap#onElementAdded(Object)} may be faked since
   * the underlying document implementation doesn't support it.
   */
  private final static class FungeStack<N, E extends N, C extends Comparable<C>> {
    private final ValueContext<N, E> context;
    private final DocumentBasedBasicSet<E, C> target;
    private final Serializer<C> serializer;

    public FungeStack(ValueContext<N, E> context,
        DocumentBasedBasicSet<E, C> target, Serializer<C> serializer) {
      this.context = context;
      this.target = target;
      this.serializer = serializer;
    }

    void setValue(C value) {
      context.doc.setElementAttribute(context.container, VALUE_ATTR, serializer.toString(value));
    }

    void assertSubstrateEquals(int expected) {
      // Check that the monotonic map deleted the first entry.  The simplest
      // approach to check this is to check that the new substrate equals an
      // expected document structure (done via XML comparison).
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
  private void createTargetOn(int ... state) {
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
    DocumentBasedBasicSet<E, Integer> target =
      DocumentBasedBasicSet.create(DefaultDocumentEventRouter.create(context.doc),
          context.container, Serializer.INTEGER, ENTRY_TAG, VALUE_ATTR);

    // Eventually, the target map and the substrate should be sufficient state for all tests.
    // However, in order to simulate document events, the two need to be wrapped together in a
    // FungeStack so that Java knows that the element type-parameters match.
    stack = new FungeStack<N, E, Integer>(context, target, Serializer.INTEGER);
  }

  /**
   * Creates a substrate based on a list of entries.
   *
   * @param value list of entries to include in the document state
   * @return a map-context view of the document state
   */
  private static ValueContext<?, ?> substrate(int ... value) {
    return substrate(BasicFactories.observableDocumentProvider().create("data",
        Collections.<String, String>emptyMap()), value);
  }

  /**
   * Populates a document with an initial map state defined by an entry list.
   *
   * @return a map-context view of the document state.
   */
  private static <N, E extends N> ValueContext<N, E> substrate(
      ObservableMutableDocument<N, E, ?> doc,
      int ... values) {
    // Insert container element
    E container = doc.createChildElement(doc.getDocumentElement(), CONTAINER_TAG,
        Collections.<String,String>emptyMap());
    for (int value : values) {
      Attributes attrs = new AttributesImpl(VALUE_ATTR, Serializer.INTEGER.toString(value));
      doc.createChildElement(container, ENTRY_TAG, attrs);
    }
    return new ValueContext<N, E>(doc, container);
  }

  /**
   * Asserts that the test-target's document substrate is in an expected state.
   *
   * @param expected  list of entries describing the expected state
   */
  private void assertSubstrateEquals(int expected) {
    stack.assertSubstrateEquals(expected);
  }

  /**
   * Creates an empty map as the test target.
   */
  private void createEmptyMap() {
    createTargetOn();
  }

  /**
   * @return the target map being tested.
   */
  private BasicSet<Integer> getTarget() {
    return stack.target;
  }

  private void assertTargetEquals(int ... values) {
    // NOTE(user): can't seem to get autoboxing to autoconvert int[] to Integer[], and Integer[]
    //   is required by all the Collection<Integer> constructors.
    Set<Integer> expectedSet = new HashSet<Integer>();
    Set<Integer> actualSet = new HashSet<Integer>();

    for (int value : values) {
      expectedSet.add(value);
    }
    for (Integer value : getTarget().getValues()) {
      boolean duplicate = !actualSet.add(value);
      assertFalse(duplicate);
    }

    assertEquals(expectedSet, actualSet);
  }

  public void testAddOnEmptyStateIsReturnedByGet() {
    createEmptyMap();
    getTarget().add(10);

    assertTargetEquals(10);
  }

  public void testAddOnEmptyStateInsertsIntoSubstrate() {
    createEmptyMap();
    getTarget().add(10);
    assertSubstrateEquals(10);
  }

  public void testSingleSubstrateValueIsReflected() {
    createTargetOn(10);
    assertTargetEquals(10);
  }

  public void testMultipleSubstrateValuesAreReflected() {
    createTargetOn(30, 10);
    assertTargetEquals(10, 30);
  }

  public void testDuplicateSubstrateValuesAreNotExposed() {
    createTargetOn(10, 10);
    assertTargetEquals(10);
  }
}
