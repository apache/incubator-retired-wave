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

package org.waveprotocol.wave.model.document.indexed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.waveprotocol.wave.model.document.AnnotationSetTestBase;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Some basic tests for AnnotationTree.
 *
 * @author ohler@google.com (Christian Ohler)
 */

public class AnnotationTreeTest extends AnnotationSetTestBase {
  @Override
  protected AnnotationTree<Object> getNewSet(AnnotationSetListener<Object> listener) {
    return new AnnotationTree<Object>(new Object(), new Object(), listener);
  }

  CollectionFactory getFactory() {
    return CollectionUtils.getCollectionFactory();
  }

  void setAnnotation(RawAnnotationSet<Object> a, int start, int end, String key, Object value)
      throws OperationException {
    a.begin();
    if (start > 0) {
      a.skip(start);
    }
    a.startAnnotation(key, value);
    if (end - start > 0) {
      a.skip(end - start);
    }
    a.endAnnotation(key);
    a.finish();
  }

  void insert(RawAnnotationSet<Object> a, int firstShiftedIndex, int length)
      throws OperationException {
    a.begin();
    if (firstShiftedIndex > 0) {
      a.skip(firstShiftedIndex);
    }
    if (length > 0) {
      a.insert(length);
    }
    a.finish();
  }

  void delete(RawAnnotationSet<Object> a, int start, int length) throws OperationException {
    a.begin();
    if (start > 0) {
      a.skip(start);
    }
    if (length > 0) {
      a.delete(length);
    }
    a.finish();
  }

  public void testOffBalance() throws OperationException {
    AnnotationTree<Object> tree = new AnnotationTree<Object>(new Object(),
        new Object(), null);
    final int size = 15;
    insert(tree, 0, size);
    for (int i = 0; i < size - 1; i++) {
      setAnnotation(tree, i, i + 1, "a", "" + i);
      tree.checkSomeInvariants();
    }
  }

  public void testRemoveAll() throws OperationException {
    AnnotationTree<Object> tree = new AnnotationTree<Object>(new Object(),
        new Object(), null);
    insert(tree, 0, 1);
    setAnnotation(tree, 0, 1, "a", "0");
    setAnnotation(tree, 0, 1, "a", null);
    delete(tree, 0, 1);
  }

  public void testEraseMergeDuringSetAnnotation() throws OperationException {
    {
      AnnotationTree<Object> tree = new AnnotationTree<Object>(new Object(),
          new Object(), null);
      insert(tree, 0, 3);
      setAnnotation(tree, 0, 1, "a", "1");
      setAnnotation(tree, 1, 2, "a", "2");
      setAnnotation(tree, 2, 3, "a", "3");
      setAnnotation(tree, 0, 2, "a", "5");
    }
    {
      AnnotationTree<Object> tree = new AnnotationTree<Object>(new Object(),
          new Object(), null);
      insert(tree, 0, 3);
      setAnnotation(tree, 0, 1, "a", "1");
      setAnnotation(tree, 1, 2, "a", "2");
      setAnnotation(tree, 2, 3, "a", "3");
      setAnnotation(tree, 1, 3, "a", "5");
    }
  }

  public void testSplitAnnotations() throws OperationException {
    AnnotationTree<Object> tree = new AnnotationTree<Object>(new Object(),
        new Object(), null);

    // The test is that none of this throws an exception.

    tree.begin();
    tree.startAnnotation("a", "1");
    tree.insert(1);
    tree.startAnnotation("a", "2");
    tree.insert(5);
    tree.startAnnotation("a", "1");
    tree.insert(1);
    tree.endAnnotation("a");
    tree.finish();
    tree.checkSomeInvariants();

    // cut off one item on the left
    tree.begin();
    tree.skip(2);
    tree.startAnnotation("a", "3");
    tree.skip(4);
    tree.endAnnotation("a");
    tree.finish();
    tree.checkSomeInvariants();

    // cut off one item on the right
    tree.begin();
    tree.skip(2);
    tree.startAnnotation("a", "4");
    tree.skip(3);
    tree.endAnnotation("a");
    tree.finish();
    tree.checkSomeInvariants();

    // cut off one item on the left and one on the right
    tree.begin();
    tree.skip(3);
    tree.startAnnotation("a", "5");
    tree.skip(1);
    tree.endAnnotation("a");
    tree.finish();
    tree.checkSomeInvariants();
  }

  @SuppressWarnings("unchecked")
  public void testListenerBasics() throws OperationException {
    final AnnotationSetListener<Object> listener = mock(AnnotationSetListener.class);
    RawAnnotationSet<Object> m = getNewSet(listener);

    m.begin();
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.insert(1);
    m.startAnnotation("a", "2");
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.startAnnotation("a", "2");
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    m.begin();
    m.startAnnotation("a", "1");
    m.skip(6);
    m.endAnnotation("a");
    m.finish();

    verify(listener).onAnnotationChange(0, 1, "a", "1");
    verify(listener).onAnnotationChange(2, 3, "a", "2");
    verify(listener).onAnnotationChange(3, 4, "a", "1");
    verify(listener).onAnnotationChange(4, 5, "a", "2");
    verify(listener).onAnnotationChange(5, 6, "a", "1");
    // These assertions are too strict; the way the AnnotationSet splits its
    // notifications is actually undefined, and there would be several
    // alternatives here.
    verify(listener).onAnnotationChange(0, 6, "a", "1");

  }

  @SuppressWarnings("unchecked")
  public void testListenerBasics2() throws OperationException {
    final AnnotationSetListener<Object> listener = mock(AnnotationSetListener.class);
    RawAnnotationSet<Object> m = getNewSet(listener);

    m.begin();
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.startAnnotation("a", null);
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.startAnnotation("a", null);
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    m.begin();
    m.startAnnotation("a", "1");
    m.skip(6);
    m.endAnnotation("a");
    m.finish();

    verify(listener).onAnnotationChange(1, 2, "a", "1");
    verify(listener).onAnnotationChange(2, 3, "a", null);
    verify(listener).onAnnotationChange(3, 4, "a", "1");
    verify(listener).onAnnotationChange(4, 5, "a", null);
    verify(listener).onAnnotationChange(5, 6, "a", "1");
    // These assertions are too strict; the way the AnnotationSet splits its
    // notifications is actually undefined, and there would be several
    // alternatives here.
    verify(listener).onAnnotationChange(0, 6, "a", "1");

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testModificationFromListener() throws OperationException {
    final int callCounter[] = new int[] { 0 };
    // Chicken-and-egg problem: listener needs a reference to m in a final local
    // variable declared before it, m's constructor needs listener.
    final RawAnnotationSet<Object> m1[] = new RawAnnotationSet[1];
    AnnotationSetListener<Object> listener = new AnnotationSetListener<Object>() {
      @Override
      public void onAnnotationChange(int start, int end, String key, Object newValue) {
        switch (callCounter[0]) {
          case 0:
            assertEquals(1, start);
            assertEquals(2, end);
            assertEquals("a", key);
            assertEquals("1", newValue);
            break;
          case 1:
            assertEquals(2, start);
            assertEquals(3, end);
            assertEquals("a", key);
            assertEquals(null, newValue);
            break;
          case 2:
            assertEquals(0, start);
            assertEquals(3, end);
            assertEquals("a", key);
            assertEquals("1", newValue);
            m1[0].begin();
            m1[0].startAnnotation("b", "1");
            m1[0].skip(2);
            m1[0].endAnnotation("b");
            m1[0].finish();
            break;
          case 3:
            assertEquals(0, start);
            assertEquals(2, end);
            assertEquals("b", key);
            assertEquals("1", newValue);
            break;
          default:
            fail();
        }
        callCounter[0]++;
      }
    };
    RawAnnotationSet<Object> m = getNewSet(listener);
    m1[0] = m;

    m.begin();
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.startAnnotation("a", null);
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    m.begin();
    m.startAnnotation("a", "1");
    m.skip(3);
    m.endAnnotation("a");
    m.finish();

    assertEquals(4, callCounter[0]);
  }

  // The behavior tested here is not currently implemented in
  // SimpleAnnotationSet.
  public void testDoubleBeginFailsHard() {
    RawAnnotationSet<Object> m = getNew();
    m.begin();
    try {
      m.begin();
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  // The behavior tested here is not currently implemented in
  // SimpleAnnotationSet.
  public void testUnmatchedFinishFailsHard() {
    {
      RawAnnotationSet<Object> m = getNew();
      try {
        m.finish();
        fail();
      } catch (IllegalStateException e) {
        // ok
      }
    }
    {
      RawAnnotationSet<Object> m = getNew();
      m.begin();
      m.finish();
      try {
        m.finish();
        fail();
      } catch (IllegalStateException e) {
        // ok
      }
    }
  }

  public void testConstructorChecksArguments() {
    try {
      new AnnotationTree<String>(null, "a", null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      new AnnotationTree<String>("a", null, null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      new AnnotationTree<String>("a", "a", null);
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      new AnnotationTree<String>("b", "b", null);
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    // Should not throw.
    new AnnotationTree<String>("a", "b", null);
  }

  public void testCleanupKnownKeys() {
    AnnotationTree<Object> t = getNewSet(null);

    t.begin();
    t.startAnnotation("a", "1");
    t.insert(10);
    t.endAnnotation("a");
    t.finish();

    assertEquals(1, t.knownKeys().countEntries());

    t.begin();
    t.startAnnotation("a", null);
    t.skip(10);
    t.endAnnotation("a");
    t.finish();

    assertEquals(0, t.knownKeys().countEntries());

    t.begin();
    t.startAnnotation("a", null);
    t.skip(4);
    t.endAnnotation("a");
    t.finish();

    assertEquals(0, t.knownKeys().countEntries());

    t.begin();
    t.startAnnotation("a", "1");
    t.insert(10);
    t.startAnnotation("a", "2");
    t.skip(10);
    t.endAnnotation("a");
    t.finish();

    assertEquals(1, t.knownKeys().countEntries());

    t.begin();
    t.skip(2);
    t.startAnnotation("b", "1");
    t.skip(18);
    t.endAnnotation("b");
    t.finish();

    assertEquals(2, t.knownKeys().countEntries());

    t.begin();
    t.startAnnotation("b", null);
    t.skip(5);
    t.endAnnotation("b");
    t.delete(15);
    t.finish();

    assertEquals(1, t.knownKeys().countEntries());

    t.begin();
    t.delete(2);
    t.startAnnotation("a", null);
    t.skip(3);
    t.endAnnotation("a");
    t.finish();

    assertEquals(0, t.knownKeys().countEntries());
  }

}
