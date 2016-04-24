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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.document.util.RangedAnnotationImpl;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Abstract test for implementations of Iterable<RangedAnnotation>
 *
 * @author ohler@google.com (Christian Ohler)
 */

public abstract class RangedAnnotationIterableTestBase extends TestCase {

  protected abstract RawAnnotationSet<Object> getNewSet();
  protected abstract Iterable<? extends RangedAnnotation<Object>> getIterable(
      RawAnnotationSet<Object> set, int start, int end,
      ReadableStringSet keys);

  protected static boolean equal(Object a, Object b) {
    if (a == null) {
      return b == null;
    }
    return a.equals(b);
  }

  protected static <V> void expectRanges(Iterable<? extends RangedAnnotation<V>> iterable,
      List<RangedAnnotation<V>> expectedRanges) {
    ArrayList<RangedAnnotation<V>> ranges = new ArrayList<RangedAnnotation<V>>(
        expectedRanges);
    for (RangedAnnotation<V> a : iterable) {
      boolean found = false;
      find:
      for (RangedAnnotation<V> b : ranges) {
        if (a.start() == b.start()
            && a.end() == b.end()
            && a.key().equals(b.key())
            && equal(a.value(), b.value())) {
          found = true;
          ranges.remove(b);
          break find;
        }
      }
      assertTrue("unexpected RangedAnnotation: " + a, found);
    }
    for (RangedAnnotation<V> b : ranges) {
      fail("ranged annotation expected but not found: " + b);
    }
  }

  @SuppressWarnings("unchecked")
  protected static <V> List<RangedAnnotation<V>> ranges(RangedAnnotation... ranges) {
    return Arrays.asList((RangedAnnotation<V>[]) ranges);
  }

  protected static final List<RangedAnnotation<Object>> NO_RANGES = Collections.emptyList();

  protected static ReadableStringSet strs(String... strings) {
    return CollectionUtils.newStringSet(strings);
  }

  protected static <V> RangedAnnotation<V> range(String key, V value, int start, int end) {
    return new RangedAnnotationImpl<V>(key, value, start, end);
  }

  public void test1() {
    RawAnnotationSet<Object> s = getNewSet();

    s.begin();
    s.insert(100);
    s.finish();

    expectRanges(getIterable(s, 40, 60, strs()), NO_RANGES);
    expectRanges(getIterable(s, 40, 60, strs("a", "b")),
        ranges(range("a", null, 0, 100),
            range("b", null, 0, 100)));
    expectRanges(getIterable(s, 0, 100, strs()), NO_RANGES);
    expectRanges(getIterable(s, 0, 100, strs("a", "b")),
        ranges(range("a", null, 0, 100),
            range("b", null, 0, 100)));

    s.begin();
    s.skip(10);
    s.startAnnotation("a", "1");
    s.skip(80);
    s.endAnnotation("a");
    s.finish();

    expectRanges(getIterable(s, 40, 60, strs()), NO_RANGES);
    expectRanges(getIterable(s, 40, 60, strs("a")),
        ranges(range("a", "1", 10, 90)));
    expectRanges(getIterable(s, 40, 60, strs("a", "b")),
        ranges(range("a", "1", 10, 90),
            range("b", null, 0, 100)));
    expectRanges(getIterable(s, 0, 100, strs()), NO_RANGES);
    expectRanges(getIterable(s, 0, 100, strs("a", "b")),
        ranges(range("a", null, 0, 10),
            range("a", "1", 10, 90),
            range("a", null, 90, 100),
            range("b", null, 0, 100)));

    s.begin();
    s.skip(20);
    s.startAnnotation("b", "2");
    s.skip(60);
    s.endAnnotation("b");
    s.finish();

    expectRanges(getIterable(s, 40, 60, strs()), NO_RANGES);
    expectRanges(getIterable(s, 40, 60, strs("a", "b")),
        ranges(range("a", "1", 10, 90),
            range("b", "2", 20, 80)));
    expectRanges(getIterable(s, 0, 80, strs()), NO_RANGES);
    expectRanges(getIterable(s, 0, 80, strs("a")),
        ranges(range("a", null, 0, 10),
            range("a", "1", 10, 90)));
    expectRanges(getIterable(s, 0, 80, strs("a", "b")),
        ranges(range("a", null, 0, 10),
            range("a", "1", 10, 90),
            range("b", null, 0, 20),
            range("b", "2", 20, 80)));

    s.begin();
    s.skip(25);
    s.startAnnotation("a", "3");
    s.skip(70);
    s.endAnnotation("a");
    s.finish();

    expectRanges(getIterable(s, 40, 60, strs()), NO_RANGES);
    expectRanges(getIterable(s, 40, 60, strs("a", "b")),
        ranges(range("a", "3", 25, 95),
            range("b", "2", 20, 80)));
    expectRanges(getIterable(s, 0, 100, strs()), NO_RANGES);
    expectRanges(getIterable(s, 0, 100, strs("a")),
        ranges(range("a", null, 0, 10),
            range("a", "1", 10, 25),
            range("a", "3", 25, 95),
            range("a", null, 95, 100)));
    expectRanges(getIterable(s, 0, 100, strs("a", "b")),
        ranges(range("a", null, 0, 10),
            range("a", "1", 10, 25),
            range("a", "3", 25, 95),
            range("a", null, 95, 100),
            range("b", null, 0, 20),
            range("b", "2", 20, 80),
            range("b", null, 80, 100)));
  }

  public void testBug1961653() {
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
    expectRanges(a.rangedAnnotations(0, a.size(), strs("a")), ranges(
        range("a", null, 0, 10),
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));
    expectRanges(a.rangedAnnotations(10, 26, strs("a")), ranges(
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));
    // a few more tests
    expectRanges(a.rangedAnnotations(9, 14, strs("a")), ranges(
        range("a", null, 0, 10),
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(9, 15, strs("a")), ranges(
        range("a", null, 0, 10),
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(9, 16, strs("a")), ranges(
        range("a", null, 0, 10),
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));
    expectRanges(a.rangedAnnotations(10, 14, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(10, 15, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(10, 16, strs("a")), ranges(
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));
    expectRanges(a.rangedAnnotations(11, 14, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(11, 15, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(11, 16, strs("a")), ranges(
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));

    expectRanges(a.rangedAnnotations(9, 9, strs("a")), ranges(
    ));
    expectRanges(a.rangedAnnotations(9, 10, strs("a")), ranges(
        range("a", null, 0, 10)
    ));
    expectRanges(a.rangedAnnotations(9, 11, strs("a")), ranges(
        range("a", null, 0, 10),
        range("a", "1", 10, 15)
    ));
    try {
      expectRanges(a.rangedAnnotations(10, 9, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectRanges(a.rangedAnnotations(10, 10, strs("a")), ranges(
    ));
    expectRanges(a.rangedAnnotations(10, 11, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    try {
      expectRanges(a.rangedAnnotations(11, 9, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      expectRanges(a.rangedAnnotations(11, 10, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectRanges(a.rangedAnnotations(11, 11, strs("a")), ranges(
    ));

    expectRanges(a.rangedAnnotations(14, 14, strs("a")), ranges(
    ));
    expectRanges(a.rangedAnnotations(14, 15, strs("a")), ranges(
        range("a", "1", 10, 15)
    ));
    expectRanges(a.rangedAnnotations(14, 16, strs("a")), ranges(
        range("a", "1", 10, 15),
        range("a", null, 15, 36)
    ));
    try {
      expectRanges(a.rangedAnnotations(15, 14, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectRanges(a.rangedAnnotations(15, 15, strs("a")), ranges(
    ));
    expectRanges(a.rangedAnnotations(15, 16, strs("a")), ranges(
        range("a", null, 15, 36)
    ));
    try {
      expectRanges(a.rangedAnnotations(16, 14, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    try {
      expectRanges(a.rangedAnnotations(16, 15, strs("a")), ranges(
      ));
      fail();
    } catch (IndexOutOfBoundsException e) {
      // ok
    }
    expectRanges(a.rangedAnnotations(16, 16, strs("a")), ranges(
    ));
  }

}
