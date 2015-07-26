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

package org.waveprotocol.wave.model.document;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.Collections;
import java.util.Map;

/**
 * Tests a ModifiableReadableAnnotationSet
 *
 * TODO(danilatos): Many more thorough tests.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class AnnotationSetTestBase extends TestCase {

  protected abstract RawAnnotationSet<Object> getNewSet(AnnotationSetListener<Object> listener);

  protected RawAnnotationSet<Object> getNew() {
    return getNewSet(null);
  }

  public void testBlank() {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(7);
    m.finish();

    assertEquals(-1, m.firstAnnotationChange(0, 7, "hi", null));
    assertEquals(-1, m.lastAnnotationChange(0, 7, "hi", null));
    assertEquals(-1, m.firstAnnotationChange(1, 6, "hi", null));
    assertEquals(-1, m.lastAnnotationChange(1, 6, "hi", null));
  }

  public void testTwoInserts() {
    RawAnnotationSet<Object> m = getNew();
    m.begin();
    m.insert(1);
    m.insert(1);
    m.finish();

    assertEquals(-1, m.firstAnnotationChange(0, 2, "hi", null));
  }

  public void testAnAnnotation() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(7);
    m.finish();

    m.begin();
    m.skip(2);
    m.startAnnotation("a", "1");
    m.skip(3);
    m.endAnnotation("a");
    m.finish();

    // 0123456
    // ..111..

    assertEquals(-1, m.firstAnnotationChange(0, 7, "hi", null));
    assertEquals(-1, m.lastAnnotationChange(0, 7, "hi", null));
    assertEquals(-1, m.firstAnnotationChange(1, 6, "hi", null));
    assertEquals(-1, m.lastAnnotationChange(1, 6, "hi", null));

    assertEquals(2, m.firstAnnotationChange(0, 7, "a", null));
    assertEquals(5, m.lastAnnotationChange(0, 7, "a", null));
    assertEquals(2, m.firstAnnotationChange(2, 5, "a", null));
    assertEquals(5, m.lastAnnotationChange(2, 5, "a", null));

    assertEquals(0, m.firstAnnotationChange(0, 7, "a", "1"));
    assertEquals(7, m.lastAnnotationChange(0, 7, "a", "1"));
    assertEquals(-1, m.firstAnnotationChange(2, 5, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(2, 5, "a", "1"));
    assertEquals(5, m.firstAnnotationChange(2, 6, "a", "1"));
    assertEquals(2, m.lastAnnotationChange(1, 5, "a", "1"));


    assertEquals(-1, m.firstAnnotationChange(3, 4, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(3, 4, "a", "1"));
    assertEquals(3, m.firstAnnotationChange(3, 4, "a", "x"));
    assertEquals(4, m.lastAnnotationChange(3, 4, "a", "x"));

    assertEquals(-1, m.firstAnnotationChange(4, 5, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(2, 3, "a", "1"));

    assertEquals(-1, m.firstAnnotationChange(6, m.size(), "a", null));
    //assertEquals(-1, m.lastAnnotationChange(-1, 1, "a", null));

    assertEquals(-1, m.firstAnnotationChange(7, m.size(), "a", null));
    assertEquals(-1, m.lastAnnotationChange(7, m.size(), "a", null));
    //assertEquals(-1, m.firstAnnotationChange(-1, 0, "a", null));
    //assertEquals(-1, m.lastAnnotationChange(-1, 0, "a", null));
  }

  public void testOverlappingInOneGo() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(100);
    m.finish();

    m.begin();
    m.skip(11);
    m.startAnnotation("a", "1");
    m.skip(20);
    m.startAnnotation("b", "1");
    m.skip(10);
    m.endAnnotation("a");
    m.skip(15);
    m.endAnnotation("b");
    m.finish();

    // 0-11: {}
    // 11-31: {a=1}
    // 31-41: {a=1, b=1}
    // 31-56: {b=1}
    // 56-100: {}

    assertEquals(11, m.firstAnnotationChange(0, 100, "a", null));
    assertEquals(41, m.firstAnnotationChange(11, 100, "a", "1"));
    assertEquals(31, m.firstAnnotationChange(0, 100, "b", null));
    assertEquals(56, m.firstAnnotationChange(31, 100, "b", "1"));
  }

  public void testOverlappingFromSeparateModifications() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(100);
    m.finish();

    m.begin();
    m.skip(11);
    m.startAnnotation("a", "1");
    m.skip(20);
    m.skip(10);
    m.endAnnotation("a");
    m.skip(15);
    m.finish();

    m.begin();
    m.skip(11);
    m.skip(20);
    m.startAnnotation("b", "1");
    m.skip(10);
    m.skip(15);
    m.endAnnotation("b");
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 100, "a", null));
    assertEquals(41, m.firstAnnotationChange(11, 100, "a", "1"));
    assertEquals(31, m.firstAnnotationChange(0, 100, "b", null));
    assertEquals(56, m.firstAnnotationChange(31, 100, "b", "1"));

  }

  public void testOverlappingDuringCreation() {
    RawAnnotationSet<Object> m = getNew();

    //  0-11: {}
    // 11-31: {a=1}
    // 31-41: {a=1, b=1}
    // 41-56: {b=1}
    // 56-66: {}

    m.begin();
    m.insert(11);
    m.startAnnotation("a", "1");
    m.insert(20);
    m.startAnnotation("b", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.insert(15);
    m.endAnnotation("b");
    m.insert(10);
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 66, "a", null));
    assertEquals(41, m.firstAnnotationChange(11, 66, "a", "1"));
    assertEquals(31, m.firstAnnotationChange(0, 66, "b", null));
    assertEquals(56, m.firstAnnotationChange(31, 66, "b", "1"));

  }

  public void testCreateWithSpanningAnnotation() {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.finish();

    assertEquals(0, m.firstAnnotationChange(0, 10, "a", null));
    assertEquals(10, m.lastAnnotationChange(0, 10, "a", null));
    assertEquals(-1, m.firstAnnotationChange(0, 10, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(0, 10, "a", "1"));
  }

  public void testCreateWithInitialAnnotation() {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.insert(5);
    m.finish();

    assertEquals(0, m.firstAnnotationChange(0, 10, "a", null));
    assertEquals(10, m.lastAnnotationChange(0, 10, "a", null));
    assertEquals(0, m.firstAnnotationChange(0, 15, "a", null));
    assertEquals(10, m.lastAnnotationChange(0, 15, "a", null));
    assertEquals(-1, m.firstAnnotationChange(0, 10, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(0, 10, "a", "1"));
  }

  public void testCreateWithFinalAnnotation() {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(5);
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.finish();

    assertEquals(5, m.firstAnnotationChange(5, 15, "a", null));
    assertEquals(15, m.lastAnnotationChange(5, 15, "a", null));
    assertEquals(5, m.firstAnnotationChange(0, 15, "a", null));
    assertEquals(15, m.lastAnnotationChange(0, 15, "a", null));
    assertEquals(-1, m.firstAnnotationChange(5, 15, "a", "1"));
    assertEquals(-1, m.lastAnnotationChange(5, 15, "a", "1"));
  }

  public void testRemoval() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(100);
    m.finish();

    m.begin();
    m.skip(11);
    m.startAnnotation("a", "1");
    m.skip(20);
    m.startAnnotation("b", "1");
    m.skip(10);
    m.endAnnotation("a");
    m.skip(15);
    m.endAnnotation("b");
    m.finish();

    // 0-11: {}
    // 11-31: {a=1}
    // 31-41: {a=1, b=1}
    // 41-56: {b=1}

    m.begin();
    m.skip(23);
    m.startAnnotation("a", null);
    m.skip(2);
    m.endAnnotation("a");
    m.finish();

    // 0-11: {}
    // 11-23: {a=1}
    // 23-25: {}
    // 25-31: {a=1}
    // 31-41: {a=1, b=1}
    // 41-56: {b=1}

    assertEquals(23, m.firstAnnotationChange(11, 50, "a", "1"));
    assertEquals(25, m.firstAnnotationChange(23, 50, "a", null));
    assertEquals("1", m.getAnnotation(25, "a"));
  }

  public void testDocumentDelete() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(100);
    m.finish();

    m.begin();
    m.skip(11);
    m.startAnnotation("a", "1");
    m.skip(20);
    m.startAnnotation("b", "1");
    m.skip(10);
    m.endAnnotation("a");
    m.skip(15);
    m.endAnnotation("b");
    m.finish();

    // delete inside a range
    m.begin();
    m.skip(12);
    m.delete(1);
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 99, "a", null));
    assertEquals(40, m.firstAnnotationChange(11, 99, "a", "1"));
    assertEquals(30, m.firstAnnotationChange(0, 99, "b", null));
    assertEquals(55, m.firstAnnotationChange(31, 99, "b", "1"));

    // delete a contiguous range
    m.begin();
    m.skip(30);
    m.delete(10);
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 89, "a", null));
    assertEquals(30, m.firstAnnotationChange(11, 89, "a", "1"));
    assertEquals(30, m.firstAnnotationChange(0, 89, "b", null));
    assertEquals(45, m.firstAnnotationChange(31, 89, "b", "1"));
  }

  public void testDocumentInsert() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(100);
    m.finish();

    m.begin();
    m.skip(11);
    m.startAnnotation("a", "1");
    m.skip(20);
    m.startAnnotation("b", "1");
    m.skip(10);
    m.endAnnotation("a");
    m.skip(15);
    m.endAnnotation("b");
    m.finish();

    // insert inside a range
    m.begin();
    m.skip(12);
    m.insert(1);
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 100, "a", null));
    assertEquals(42, m.firstAnnotationChange(11, 100, "a", "1"));
    assertEquals(32, m.firstAnnotationChange(0, 100, "b", null));
    assertEquals(57, m.firstAnnotationChange(32, 100, "b", "1"));

    // insert at range boundary
    m.begin();
    m.skip(32);
    m.insert(5);
    m.finish();

    assertEquals(11, m.firstAnnotationChange(0, 100, "a", null));
    assertEquals(47, m.firstAnnotationChange(11, 100, "a", "1"));
    assertEquals(37, m.firstAnnotationChange(0, 100, "b", null));
    assertEquals(62, m.firstAnnotationChange(37, 100, "b", "1"));
  }

  public void testAnnotatedInsert() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(20);
    m.finish();

    m.begin();
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.skip(10);
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.skip(10);
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.finish();

    assertEquals(0, m.firstAnnotationChange(0, 50, "a", null));
    assertEquals(10, m.firstAnnotationChange(0, 50, "a", "1"));
    assertEquals(20, m.firstAnnotationChange(10, 50, "a", null));
    assertEquals(30, m.firstAnnotationChange(20, 50, "a", "1"));
    assertEquals(40, m.firstAnnotationChange(30, 50, "a", null));
    assertEquals(-1, m.firstAnnotationChange(40, 50, "a", "1"));
  }

  public void testAnnotatedInsertWithSameAsExistingAnnotation() throws OperationException {
    RawAnnotationSet<Object> m = getNew();

    m.begin();
    m.insert(10);
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.insert(20);
    m.finish();

    m.begin();
    m.skip(20);
    m.startAnnotation("a", "1");
    m.insert(10);
    m.endAnnotation("a");
    m.finish();

    assertEquals(10, m.firstAnnotationChange(0, 50, "a", null));
    assertEquals(30, m.lastAnnotationChange(0, 50, "a", null));
    assertEquals(-1, m.firstAnnotationChange(10, 30, "a", "1"));

    m.begin();
    m.skip(30);
    m.startAnnotation("a", "2");
    m.insert(10);
    m.endAnnotation("a");
    m.finish();

    assertEquals(10, m.firstAnnotationChange(0, 60, "a", null));
    assertEquals(40, m.lastAnnotationChange(0, 60, "a", null));
    assertEquals(-1, m.firstAnnotationChange(10, 30, "a", "1"));
    assertEquals(-1, m.firstAnnotationChange(30, 40, "a", "2"));

    assertEquals(10, m.firstAnnotationChange(0, 60, "a", null));
    assertEquals(40, m.lastAnnotationChange(0, 60, "a", null));
    assertEquals(30, m.firstAnnotationChange(10, 40, "a", "1"));
    assertEquals(30, m.lastAnnotationChange(10, 40, "a", "2"));
  }
//
//  public void testOperationExceptions() {
//    {
//      RawAnnotationSet<Object> m = getNew();
//      m.begin();
//      try {
//        m.skip(1);
//        fail();
//      } catch (OperationException e) {
//        // ok
//      }
//    }
//    {
//      RawAnnotationSet<Object> m = getNew();
//      m.begin();
//      try {
//        m.delete(1);
//        fail();
//      } catch (OperationException e) {
//        // ok
//      }
//    }
//  }

  public void testQueryExceptions() {
    RawAnnotationSet<Object> m = getNew();
    m.begin();
    m.insert(1);
    m.finish();

    // Shouldn't throw.
    m.getAnnotation(0, "a");

    try {
      m.getAnnotation(-1, "a");
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }

    try {
      m.getAnnotation(1, "a");
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }

    try {
      m.getAnnotation(0, null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    // Shouldn't throw.
    m.firstAnnotationChange(0, 1, "a", null);

    try {
      m.firstAnnotationChange(-1, 1, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.firstAnnotationChange(0, 2, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.firstAnnotationChange(1, 0, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.firstAnnotationChange(0, 1, null, null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }


    // Shouldn't throw.
    m.lastAnnotationChange(0, 1, "a", null);

    try {
      m.lastAnnotationChange(-1, 1, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.lastAnnotationChange(0, 2, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.lastAnnotationChange(1, 0, "a", null);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      m.lastAnnotationChange(0, 1, null, null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    ReadableStringSet acceptableSet = strs("a");
    ReadableStringSet emptySet = strs();

    // Shouldn't throw.
    m.annotationCursor(0, 0, emptySet);
    m.annotationCursor(0, 0, acceptableSet);

    try {
      m.annotationCursor(-1, 0, acceptableSet);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }

    try {
      m.annotationCursor(1, 0, acceptableSet);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }

    try {
      m.annotationCursor(0, 2, acceptableSet);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
  }
//
//  public void testDeleteInsert() throws OperationException {
//    RawAnnotationSet<Object> set = getNew();
//    FlatDocument doc = new FlatDocument((RawAnnotationSet) set);
//    DocumentMutationTestData input = new DocumentMutationTestData() {
//      public void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//          DocumentMutation.Builder result) {
//        initial.begin();
//        initial.startAnnotation("a", "1");
//        initial.characters("w");
//        initial.endAnnotation("a");
//        initial.finish();
//
//        mutation.begin();
//        mutation.deleteCharacters(1);
//        mutation.characters("r");
//        mutation.finish();
//
//        result.begin();
//        // ???
//        result.startAnnotation("a", "1");
//        result.characters("r");
//        result.endAnnotation("a");
//        result.finish();
//      }
//    };
//    doDocumentTest(input, doc);
//    // ???
//    assertEquals("1", set.getAnnotation(0, "a"));
//  }

  private interface IntervalCallback {
    void apply(int start, int end, Map<String, Object> annotations,
        Map<String, Object> diffFromLeft);
  }


  public void testIntervalIterator() {
    // IndexedDocumentImplTest contains some more tests that also exercise the
    // iterator methods of AnnotationTree.
    RawAnnotationSet<Object> m = getNew();
    m.begin();
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    IntervalCallback c = mock(IntervalCallback.class);
    for (AnnotationInterval<Object> i : m.annotationIntervals(0, m.size(), null)) {
      c.apply(i.start(), i.end(), CollectionUtils.newJavaMap(i.annotations()),
          CollectionUtils.newJavaMap(i.diffFromLeft()));
    }

    verify(c).apply(0, 1, Collections.<String, Object>singletonMap("a", "1"),
        Collections.<String, Object>singletonMap("a", "1"));
  }

  public void testIntervalIterator2()  {
    RawAnnotationSet<Object> m = getNew();
    m.begin();
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.insert(1);
    m.finish();

    IntervalCallback c = mock(IntervalCallback.class);
    for (AnnotationInterval<Object> i : m.annotationIntervals(0, m.size(), null)) {
      c.apply(i.start(), i.end(), CollectionUtils.newJavaMap(i.annotations()),
          CollectionUtils.newJavaMap(i.diffFromLeft()));
    }

    verify(c).apply(0, 1, Collections.<String, Object>singletonMap("a", "1"),
          Collections.<String, Object>singletonMap("a", "1"));
    verify(c).apply(1, 2, Collections.<String, Object>singletonMap("a", null),
        Collections.<String, Object>singletonMap("a", null));

  }
//
//  protected static <D extends ModifiableDocument & DocumentOperation> D doDocumentTest(
//      DocumentMutationTestData input, D doc) throws OperationException {
//    DocumentMutation.Builder a = new DocumentMutation.Builder();
//    DocumentMutation.Builder b = new DocumentMutation.Builder();
//    DocumentMutation.Builder c = new DocumentMutation.Builder();
//    input.build(a, b, c);
//    DocumentMutation initial = a.build();
//    DocumentMutation mutation = b.build();
//    DocumentMutation expected = c.build();
//
//    initial.apply(doc);
//    mutation.apply(doc);
//    DocumentMutation composition = DocumentMutationComposer2.compose(initial, mutation);
//    // The composition itself may contain redundant startAnnotations with a
//    // value of null.  So we apply it to an empty document before we compare.
//    FlatDocument simpleReference = new FlatDocument(
//        (RawAnnotationSet) new SimpleAnnotationSet(null));
//    composition.apply(simpleReference);
//    FlatDocument treeReference = new FlatDocument(new AnnotationTree<String>(
//        "a", "b", null));
//    composition.apply(treeReference);
//    try {
//      assertEquals(OperationXmlifier.xmlify(expected), OperationXmlifier.xmlify(doc));
//      assertEquals(OperationXmlifier.xmlify(treeReference), OperationXmlifier.xmlify(doc));
//      assertEquals(OperationXmlifier.xmlify(simpleReference), OperationXmlifier.xmlify(doc));
//    } catch (AssertionFailedError e) {
//      System.err.println("---");
//      System.err.println("expected:\n" + OperationXmlifier.xmlify(expected));
//      System.err.println("actual:\n" + OperationXmlifier.xmlify(doc));
//      System.err.println("simple ref:\n" + OperationXmlifier.xmlify(simpleReference));
//      System.err.println("tree ref:\n" + OperationXmlifier.xmlify(treeReference));
//      System.err.println("composition:\n" + OperationXmlifier.xmlify(composition));
//      throw e;
//    }
//    return doc;
//  }
//
//  /**
//   * An interface containing a method that builds the input and expected result for
//   * one test case.
//   */
//  protected interface DocumentMutationTestData {
//    void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//        DocumentMutation.Builder result);
//  }
//
//  public void testInsertAfterSkip() throws OperationException {
//    RawAnnotationSet<Object> set = getNew();
//    FlatDocument doc = new FlatDocument((RawAnnotationSet) set);
//    DocumentMutationTestData input = new DocumentMutationTestData() {
//      public void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//          DocumentMutation.Builder result) {
//        initial.begin();
//        initial.characters("p");
//        initial.finish();
//
//        mutation.begin();
//        mutation.startAnnotation("a", "1");
//        mutation.skip(1);
//        mutation.endAnnotation("a");
//        mutation.characters("y");
//        mutation.finish();
//
//        result.begin();
//        result.startAnnotation("a", "1");
//        result.characters("p");
//        result.endAnnotation("a");
//        result.characters("y");
//        result.finish();
//      }
//    };
//
//    doDocumentTest(input, doc);
//
//    assertEquals("1", set.getAnnotation(0, "a"));
//    assertEquals(null, set.getAnnotation(1, "a"));
//  }
//
//  /**
//   * Tests that integer overflow in skips is gracefully avoided.
//   */
//  public void testNoOverflowInSkips() {
//    RawAnnotationSet<Object> set = getNew();
//
//    set.begin();
//    set.insert(1);
//    try {
//      set.skip(Integer.MAX_VALUE);
//      fail("Expected exception was not thrown.");
//    } catch (OperationException e) {
//      // ok
//    }
//  }
//
//  /**
//   * Tests that integer overflow in deletions is gracefully avoided.
//   */
//  public void testNoOverflowInDeletions() {
//    RawAnnotationSet<Object> set = getNew();
//
//    set.begin();
//    set.insert(1);
//    try {
//      set.delete(Integer.MAX_VALUE);
//      fail("Expected exception was not thrown.");
//    } catch (OperationException e) {
//      // ok
//    }
//  }
//
//  public void testInsertAfterDelete() throws OperationException {
//    RawAnnotationSet<Object> set = getNew();
//    FlatDocument doc = new FlatDocument((RawAnnotationSet) set);
//    DocumentMutationTestData input = new DocumentMutationTestData() {
//      public void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//          DocumentMutation.Builder result) {
//        initial.begin();
//        initial.startAnnotation("e", "3");
//        initial.characters("q");
//        initial.endAnnotation("e");
//        initial.characters("y");
//        initial.finish();
//
//        mutation.begin();
//        mutation.skip(1);
//        mutation.deleteCharacters(1);
//        mutation.characters("w");
//        mutation.finish();
//
//        result.begin();
//        result.startAnnotation("e", "3");
//        result.characters("q");
//        result.endAnnotation("e");
//        result.characters("w");
//        result.finish();
//      }
//    };
//    doDocumentTest(input, doc);
//
//    assertEquals("3", set.getAnnotation(0, "e"));
//    assertEquals(null, set.getAnnotation(1, "e"));
//  }
//
//  public void testInsertAfterDelete2() throws OperationException {
//    RawAnnotationSet<Object> set = getNew();
//    FlatDocument doc = new FlatDocument((RawAnnotationSet) set);
//    DocumentMutationTestData input = new DocumentMutationTestData() {
//      public void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//          DocumentMutation.Builder result) {
//        initial.begin();
//        initial.startAnnotation("e", "3");
//        initial.characters("x");
//        initial.endAnnotation("e");
//        initial.characters("y");
//        initial.finish();
//
//        mutation.begin();
//        mutation.startAnnotation("e", null);
//        mutation.skip(1);
//        mutation.deleteCharacters(1);
//        mutation.endAnnotation("e");
//        mutation.characters("w");
//        mutation.finish();
//
//        result.begin();
//        result.characters("x");
//        result.characters("w");
//        result.finish();
//      }
//    };
//    doDocumentTest(input, doc);
//
//    assertEquals(null, set.getAnnotation(0, "e"));
//    assertEquals(null, set.getAnnotation(1, "e"));
//  }
//
//  public void testBug1() throws OperationException {
//    RawAnnotationSet<Object> set = getNew();
//    FlatDocument doc = new FlatDocument((RawAnnotationSet) set);
//    DocumentMutationTestData input = new DocumentMutationTestData() {
//      public void build(DocumentMutation.Builder initial, DocumentMutation.Builder mutation,
//          DocumentMutation.Builder result) {
//        initial.begin();
//        initial.characters("c");
//        initial.finish();
//
//        mutation.begin();
//        mutation.startAnnotation("b", "3");
//        mutation.deleteCharacters(1);
//        mutation.endAnnotation("b");
//        mutation.characters("a");
//        mutation.finish();
//
//        result.begin();
//        result.characters("a");
//        result.finish();
//      }
//    };
//
//    doDocumentTest(input, doc);
//  }

  @SuppressWarnings("unchecked")
  public void testListenerForInsertion() throws OperationException {
    final AnnotationSetListener<Object> listener = mock(AnnotationSetListener.class);
    RawAnnotationSet<Object> m = getNewSet(listener);

    m.begin();
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    verify(listener).onAnnotationChange(0, 1, "a", "1");

    m.begin();
    m.skip(1);
    m.insert(1);
    m.finish();
  }

  protected StringSet strs(String ... strings) {
    return CollectionUtils.newStringSet(strings);
  }
}
