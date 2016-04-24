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

package org.waveprotocol.wave.model.document.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ohler@google.com (Christian Ohler)
 */

public abstract class AnnotationIntervalIterableTestBase extends TestCase {

  protected abstract RawAnnotationSet<Object> getNewSet();
  protected abstract Iterable<AnnotationInterval<Object>> getIterable(RawAnnotationSet<Object> set,
      int start, int end, ReadableStringSet keys);

  protected static boolean equal(Object a, Object b) {
    if (a == null) {
      return b == null;
    }
    return a.equals(b);
  }

  protected static <V> void assertMapsEqual(ReadableStringMap<V> expected,
      final ReadableStringMap<V> actual) {
    final int[] bCount = { 0 };
    expected.each(new ReadableStringMap.ProcV<V>() {
      @Override
      public void apply(String key, V value) {
        bCount[0]++;
        assertTrue("Expected key not present: " + key, actual.containsKey(key));
        assertEquals("Value for key " + key, value, actual.getExisting(key));
      }
    });
    if (bCount[0] != actual.countEntries()) {
      fail("Expected " + bCount[0] + " entries, found " + actual.countEntries());
    }
  }

  protected static <V> void expectIntervals(Iterable<AnnotationInterval<V>> iterable,
      List<AnnotationInterval<V>> expectedIntervals) {
    List<AnnotationInterval<V>> actualIntervals = new ArrayList<AnnotationInterval<V>>();
    for (AnnotationInterval<V> i : iterable) {
      actualIntervals.add(new AnnotationIntervalImpl<V>(i));
    }
    assertEquals("Number of intervals", expectedIntervals.size(), actualIntervals.size());
    for (int i = 0; i < expectedIntervals.size(); i++) {
      AnnotationInterval<V> expected = expectedIntervals.get(i);
      AnnotationInterval<V> actual = actualIntervals.get(i);
      assertEquals("Start of interval " + i, expected.start(), actual.start());
      assertEquals("End of interval " + i, expected.end(), actual.end());
      assertMapsEqual(expected.annotations(), actual.annotations());
      assertMapsEqual(expected.diffFromLeft(), actual.diffFromLeft());
    }
  }

  @SuppressWarnings("unchecked")
  protected static <V> List<AnnotationInterval<V>> intervals(AnnotationInterval... intervals) {
    return Arrays.asList((AnnotationInterval<V>[]) intervals);
  }

  protected static final List<AnnotationInterval<Object>> NO_INTERVALS = Collections.emptyList();

  protected static ReadableStringSet strs(String ... strings) {
    return CollectionUtils.newStringSet(strings);
  }

  protected static <V> AnnotationInterval<V> interval(int start, int end,
      ReadableStringMap<V> annotations, ReadableStringMap<V> diffFromLeft) {
    return new AnnotationIntervalImpl<V>(start, end, annotations, diffFromLeft);
  }

  protected static ReadableStringMap<Object> map(Object... pairs) {
    Preconditions.checkArgument(pairs.length % 2 == 0,
        "Key-value pairs must come in pairs, found " + pairs.length);
    StringMap<Object> m = CollectionUtils.createStringMap();
    for (int i = 0; i < pairs.length; i += 2) {
      Preconditions.checkArgument(pairs[i] instanceof String,
          "Keys must be strings, found " + pairs[i]);
      String key = (String) pairs[i];
      Preconditions.checkArgument(!m.containsKey(key), "Duplicate key: " + key);
      m.put(key, pairs[i + 1]);
    }
    return m;
  }

  public void test1() throws OperationException {
    RawAnnotationSet<Object> s = getNewSet();

    s.begin();
    s.insert(100);
    s.finish();

    expectIntervals(getIterable(s, 40, 60, strs()), intervals(
        interval(40, 60, map(), map())));
    expectIntervals(getIterable(s, 40, 60, strs("a", "b")), intervals(
        interval(40, 60, map("a", null, "b", null), map())));
    expectIntervals(getIterable(s, 0, 100, strs()), intervals(
        interval(0, 100, map(), map())));
    expectIntervals(getIterable(s, 0, 100, strs("a", "b")), intervals(
        interval(0, 100, map("a", null, "b", null), map())));

    s.begin();
    s.skip(10);
    s.startAnnotation("a", "1");
    s.skip(80);
    s.endAnnotation("a");
    s.finish();

    expectIntervals(getIterable(s, 40, 60, strs()), intervals(
        interval(40, 60, map(), map())));
    expectIntervals(getIterable(s, 40, 60, strs("a")), intervals(
        interval(40, 60, map("a", "1"), map())));
    expectIntervals(getIterable(s, 40, 60, strs("a", "b")), intervals(
        interval(40, 60, map("a", "1", "b", null), map())));
    expectIntervals(getIterable(s, 0, 100, strs()), intervals(
        interval(0, 100, map(), map())));
    expectIntervals(getIterable(s, 0, 100, strs("a", "b")), intervals(
        interval(0, 10, map("a", null, "b", null), map()),
        interval(10, 90, map("a", "1", "b", null), map("a", "1")),
        interval(90, 100, map("a", null, "b", null), map("a", null))));

    s.begin();
    s.skip(20);
    s.startAnnotation("b", "2");
    s.skip(60);
    s.endAnnotation("b");
    s.finish();

    expectIntervals(getIterable(s, 40, 60, strs()), intervals(
        interval(40, 60, map(), map())));
    expectIntervals(getIterable(s, 40, 60, strs("a", "b")), intervals(
        interval(40, 60, map("a", "1", "b", "2"), map())));
    expectIntervals(getIterable(s, 0, 80, strs()), intervals(
        interval(0, 80, map(), map())));
    expectIntervals(getIterable(s, 0, 80, strs("a")), intervals(
        interval(0, 10, map("a", null), map()),
        interval(10, 80, map("a", "1"), map("a", "1"))));
    expectIntervals(getIterable(s, 0, 80, strs("a", "b")), intervals(
        interval(0, 10, map("a", null, "b", null), map()),
        interval(10, 20, map("a", "1", "b", null), map("a", "1")),
        interval(20, 80, map("a", "1", "b", "2"), map("b", "2"))));

    s.begin();
    s.skip(25);
    s.startAnnotation("a", "3");
    s.skip(70);
    s.endAnnotation("a");
    s.finish();

    expectIntervals(getIterable(s, 40, 60, strs()), intervals(
        interval(40, 60, map(), map())));
    expectIntervals(getIterable(s, 40, 60, strs("a", "b")), intervals(
        interval(40, 60, map("a", "3", "b", "2"), map())));
    expectIntervals(getIterable(s, 0, 100, strs()), intervals(
        interval(0, 100, map(), map())));
    expectIntervals(getIterable(s, 0, 100, strs("a")), intervals(
        interval(0, 10, map("a", null), map()),
        interval(10, 25, map("a", "1"), map("a", "1")),
        interval(25, 95, map("a", "3"), map("a", "3")),
        interval(95, 100, map("a", null), map("a", null))));
    expectIntervals(getIterable(s, 0, 100, strs("a", "b")), intervals(
        interval(0, 10, map("a", null, "b", null), map()),
        interval(10, 20, map("a", "1", "b", null), map("a", "1")),
        interval(20, 25, map("a", "1", "b", "2"), map("b", "2")),
        interval(25, 80, map("a", "3", "b", "2"), map("a", "3")),
        interval(80, 95, map("a", "3", "b", null), map("b", null)),
        interval(95, 100, map("a", null, "b", null), map("a", null))));
  }

  public void testEquivalentOfBug1961653() {
    RawAnnotationSet<Object> a = getNewSet();
    a.begin();
    a.insert(10);
    a.startAnnotation("a", "1");
    a.insert(5);
    a.endAnnotation("a");
    a.insert(21);
    a.finish();
    assertEquals(36, a.size());
    // tests from the bug report
    expectIntervals(a.annotationIntervals(0, a.size(), strs("a")), intervals(
        interval(0, 10, map("a", null), map()),
        interval(10, 15, map("a", "1"), map("a", "1")),
        interval(15, 36, map("a", null), map("a", null))));
    expectIntervals(a.annotationIntervals(10, 26, strs("a")), intervals(
        interval(10, 15, map("a", "1"), map("a", "1")),
        interval(15, 26, map("a", null), map("a", null))));
    // a few more tests
    expectIntervals(a.annotationIntervals(9, 14, strs("a")), intervals(
        interval(9, 10, map("a", null), map()),
        interval(10, 14, map("a", "1"), map("a", "1"))
    ));
    expectIntervals(a.annotationIntervals(9, 15, strs("a")), intervals(
        interval(9, 10, map("a", null), map()),
        interval(10, 15, map("a", "1"), map("a", "1"))
    ));
    expectIntervals(a.annotationIntervals(9, 16, strs("a")), intervals(
        interval(9, 10, map("a", null), map()),
        interval(10, 15, map("a", "1"), map("a", "1")),
        interval(15, 16, map("a", null), map("a", null))
    ));
    expectIntervals(a.annotationIntervals(10, 14, strs("a")), intervals(
        interval(10, 14, map("a", "1"), map("a", "1"))
    ));
    expectIntervals(a.annotationIntervals(10, 15, strs("a")), intervals(
        interval(10, 15, map("a", "1"), map("a", "1"))
    ));
    expectIntervals(a.annotationIntervals(10, 16, strs("a")), intervals(
        interval(10, 15, map("a", "1"), map("a", "1")),
        interval(15, 16, map("a", null), map("a", null))
    ));
    expectIntervals(a.annotationIntervals(11, 14, strs("a")), intervals(
        interval(11, 14, map("a", "1"), map())
    ));
    expectIntervals(a.annotationIntervals(11, 15, strs("a")), intervals(
        interval(11, 15, map("a", "1"), map())
    ));
    expectIntervals(a.annotationIntervals(11, 16, strs("a")), intervals(
        interval(11, 15, map("a", "1"), map()),
        interval(15, 16, map("a", null), map("a", null))
    ));

    expectIntervals(a.annotationIntervals(9, 9, strs("a")), intervals(
    ));
    expectIntervals(a.annotationIntervals(9, 10, strs("a")), intervals(
        interval(9, 10, map("a", null), map())
    ));
    expectIntervals(a.annotationIntervals(9, 11, strs("a")), intervals(
        interval(9, 10, map("a", null), map()),
        interval(10, 11, map("a", "1"), map("a", "1"))
    ));
    try {
      expectIntervals(a.annotationIntervals(10, 9, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectIntervals(a.annotationIntervals(10, 10, strs("a")), intervals(
    ));
    expectIntervals(a.annotationIntervals(10, 11, strs("a")), intervals(
        interval(10, 11, map("a", "1"), map("a", "1"))
    ));
    try {
      expectIntervals(a.annotationIntervals(11, 9, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      expectIntervals(a.annotationIntervals(11, 10, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectIntervals(a.annotationIntervals(11, 11, strs("a")), intervals(
    ));

    expectIntervals(a.annotationIntervals(14, 14, strs("a")), intervals(
    ));
    expectIntervals(a.annotationIntervals(14, 15, strs("a")), intervals(
        interval(14, 15, map("a", "1"), map())
    ));
    expectIntervals(a.annotationIntervals(14, 16, strs("a")), intervals(
        interval(14, 15, map("a", "1"), map()),
        interval(15, 16, map("a", null), map("a", null))
    ));
    try {
      expectIntervals(a.annotationIntervals(15, 14, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectIntervals(a.annotationIntervals(15, 15, strs("a")), intervals(
    ));
    expectIntervals(a.annotationIntervals(15, 16, strs("a")), intervals(
        interval(15, 16, map("a", null), map("a", null))
    ));
    try {
      expectIntervals(a.annotationIntervals(16, 14, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      expectIntervals(a.annotationIntervals(16, 15, strs("a")), intervals(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectIntervals(a.annotationIntervals(16, 16, strs("a")), intervals(
    ));
  }
}
