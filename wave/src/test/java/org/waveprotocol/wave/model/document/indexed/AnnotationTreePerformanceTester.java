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

import org.waveprotocol.wave.model.document.ReadableAnnotationSet;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Random;

/**
 * Code that performs random operations on an AnnotationTree for profiling
 * purposes.  Not really a test case.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class AnnotationTreePerformanceTester {

  CollectionFactory getFactory() {
    return CollectionUtils.getCollectionFactory();
  }

  static final int INSERT_LENGTH_LIMIT = 20000;
  static final int DELETE_LENGTH_LIMIT = 10000;
  static final int MAX_LENGTH = 1000000;
  static final int NUM_KEYS = 50;
  static final int NUM_VALUES = 3;
  static final int NUM_UPDATES = 10000;
  static final int QUERIES_PER_UPDATE = 5;

  public void testPerformance() {
    Random random = new Random(2);

    for (int i = 0; i < 5; i++) {
      System.err.println("run " + i);
      testPerformance1(random);
    }
  }

  void testPerformance1(Random random) {
    RawAnnotationSet<Object> a = new SimpleAnnotationSet(null);
    RawAnnotationSet<Object> b = new AnnotationTree<Object>(new Object(),
        new Object(), null);

    String[] keys = new String[NUM_KEYS];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = "k" + i;
    }

    String[] values = new String[NUM_KEYS];
    for (int i = 0; i < values.length; i++) {
      values[i] = "v" + i;
    }

    int maxLength = 0;
    RawAnnotationSet<Object> c = b;
    for (int i = 0; i < NUM_UPDATES; i++) {
      applyRandomOperation(null, c, keys, values, random);
      maxLength = Math.max(maxLength, c.size());
      for (int j = 0; j < QUERIES_PER_UPDATE; j++) {
        performRandomQuery(c, keys, random);
      }
    }
    System.err.println("final length=" + c.size() + ", max length=" + maxLength);
  }


  void performRandomQuery(ReadableAnnotationSet<Object> a, String[] keys, Random random) {
    if (a.size() > 0) {
      int start = random.nextInt(a.size());
      int end = 1 + random.nextInt(a.size());
      if (start > end) {
        int swap = start;
        start = end;
        end = swap;
      }
      for (String key : keys) {
        a.firstAnnotationChange(start, end, key, "foo");
        a.lastAnnotationChange(start, end, key, "foo");

        Object value = a.getAnnotation(start, key);
        a.firstAnnotationChange(start, end, key, value);
        a.lastAnnotationChange(start, end, key, value);

        value = a.getAnnotation(end - 1, key);
        a.firstAnnotationChange(start, end, key, value);
        a.lastAnnotationChange(start, end, key, value);
      }
    }
  }

  void applyRandomOperation(RawAnnotationSet<Object> a, RawAnnotationSet<Object> b,
      String[] keys, String[] values, Random random) {
    if (a != null) {
      assert a.size() == b.size();
    }

    try {
      if (b.size() == 0) {
        applyRandomInsert(a, b, random);
      } else if (b.size() >= MAX_LENGTH) {
        switch (random.nextInt(2)) {
          case 0:
            applyRandomDelete(a, b, random);
            break;
          case 1:
            applyRandomSet(a, b, keys, values, random);
            break;
        }
      } else {
        switch (random.nextInt(3)) {
          case 0:
            applyRandomInsert(a, b, random);
            break;
          case 1:
            applyRandomDelete(a, b, random);
            break;
          case 2:
            applyRandomSet(a, b, keys, values, random);
            break;
        }
      }
    } catch (OperationException e) {
      throw new RuntimeException(e);
    }
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

  void applyRandomDelete(RawAnnotationSet<Object> a, RawAnnotationSet<Object> b,
      Random random) throws OperationException {
    int pos = random.nextInt(b.size());
    int length = random.nextInt(Math.min(DELETE_LENGTH_LIMIT, b.size() - pos));

    if (a != null) {
      delete(a, pos, length);
    }
    delete(b, pos, length);
  }

  void applyRandomInsert(RawAnnotationSet<Object> a, RawAnnotationSet<Object> b,
      Random random) throws OperationException {
    int pos = random.nextInt(b.size() + 1);
    int length = random.nextInt(INSERT_LENGTH_LIMIT);
    if (a != null) {
      insert(a, pos, length);
    }
    insert(b, pos, length);
  }

  void applyRandomSet(RawAnnotationSet<Object> a, RawAnnotationSet<Object> b,
      String[] keys, String[] values, Random random) throws OperationException {
    int start = random.nextInt(b.size());
    int end = 1 + random.nextInt(b.size());
    if (start > end) {
      int swap = start;
      start = end;
      end = swap;
    }
    String key = keys[random.nextInt(keys.length)];
    Object value = values[random.nextInt(values.length)];

    if (a != null) {
      setAnnotation(a, start, end, key, value);
    }
    setAnnotation(b, start, end, key, value);
  }

  public static void main(String[] args) {
    long startTime = System.currentTimeMillis();
    new AnnotationTreePerformanceTester().testPerformance();
    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;
    long seconds = time / 1000;
    System.out.println("time: " + seconds + "s");
  }
}
