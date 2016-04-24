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

package org.waveprotocol.wave.client.paging;


import junit.framework.TestCase;

import org.waveprotocol.wave.client.paging.Traverser.Point;
import org.waveprotocol.wave.client.paging.Traverser.SimplePoint;

/**
 * Tests for {@link Traverser} based on random tree structures and locations.
 *
 */

public final class RandomizedTraverserTest extends TestCase {
  /** Size of the block tree being tested. */
  private int size;

  /** Root of tree. */
  private Block root;

  /**
   * Builds the random tree.
   *
   * @param seed seed for randomizer
   * @param size number of blocks to build in the tree
   */
  private void setUp(int seed, int size) {
    this.size = size;
    this.root = RandomTreeBuilder.create().withSeed(seed).withZeroBlockProbability(0.3).build(size);
  }

  /**
   * Runs tests for the four locating methods in {@link Traverser}.
   */
  private void doTest() {
    // Test an even distribution of 2.size + 2 points over the range:
    // [start - E, end + E]
    double start = root.getStart() - 20;
    double end = root.getEnd() + 20;
    int count = size * 2 + 2;

    testStartWithin(start, end, count);
    testEndWithin(start, end, count);
    testStartAfter(start, end, count);
    testEndBefore(start, end, count);
  }

  /**
   * For an even distribution of {@code count} positions over the range {@code
   * [start, end]}, tests {@link Traverser#locateStartWithin(Block, double)}
   * against a trivial brute-force implementation.
   */
  private void testStartWithin(double start, double end, double count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);
      Point expected = locateStartWithin(root, position);
      Point actual = Traverser.locateStartWithin(root, position);
      assertEquals(expected, actual);
    }
  }

  /**
   * For an even distribution of {@code count} positions over the range {@code
   * [start, end]}, tests {@link Traverser#locateEndWithin(Block, double)}
   * against a trivial brute-force implementation.
   */
  private void testEndWithin(double start, double end, int count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);
      Point expected = locateEndWithin(root, position);
      Point actual = Traverser.locateEndWithin(root, position);
      assertEquals(expected, actual);
    }
  }

  /**
   * For the cross product of even distributions of {@code count} positions and
   * {@code count} points over the range {@code [start, end]}, tests
   * {@link Traverser#locateStartAfter(Point, double)} against a trivial
   * brute-force implementation.
   */
  private void testStartAfter(double start, double end, int count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);
      Point reference = locateEndWithin(root, position);
      if (reference != null) {
        testStartAfter(reference, start, end, count);
      }
    }
  }

  /**
   * For the cross product of even distributions of {@code count} positions and
   * {@code count} points over the range {@code [start, end]}, tests
   * {@link Traverser#locateEndBefore(Point, double)} against a trivial
   * brute-force implementation.
   */
  private void testEndBefore(double start, double end, int count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);
      Point reference = locateStartWithin(root, position);
      if (reference != null) {
        testEndBefore(reference, start, end, count);
      }
    }
  }

  /**
   * For an even distribution of {@code count} points over the range {@code
   * [start, end]}, tests {@link Traverser#locateStartAfter(Point, double)}
   * against a trivial brute-force implementation.
   */
  private void testStartAfter(Point ref, double start, double end, double count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);

      // expected = null means exception expected.
      Point expected = ref.absoluteLocation() < position ? locateStartWithin(root, position) : null;
      Point actual;
      try {
        actual = Traverser.locateStartAfter(ref, position);
        assertNotNull(actual);
      } catch (IllegalArgumentException e) {
        actual = null;
      }
      assertEquals(expected, actual);
    }
  }

  /**
   * For an even distribution of {@code count} points over the range {@code
   * [start, end]}, tests {@link Traverser#locateEndBefore(Point, double)}
   * against a trivial brute-force implementation.
   */
  private void testEndBefore(Point ref, double start, double end, double count) {
    for (int i = 0; i < count; i++) {
      double position = start + i * (end - start) / (count - 1);

      // expected = null means exception expected.
      Point expected = ref.absoluteLocation() > position ? locateEndWithin(root, position) : null;
      Point actual;
      try {
        actual = Traverser.locateEndBefore(ref, position);
        assertNotNull(actual);
      } catch (IllegalArgumentException e) {
        actual = null;
      }
      assertEquals(expected, actual);
    }
  }

  /** @return the rightmost point in a tree that is strictly before a position. */
  private Point locateStartWithin(Block block, double position) {
    return locateStartBefore(SimplePoint.startOf(block), position);
  }

  /** @return the leftmost point in a tree that is strictly after a position. */
  private Point locateEndWithin(Block block, double position) {
    return locateEndAfter(SimplePoint.endOf(block), position);
  }

  /**
   * @return the rightmost point in a tree, at or after a reference, that is
   *         strictly before a position.
   */
  private Point locateStartBefore(Point ref, double position) {
    SimpleMoveablePoint point = SimpleMoveablePoint.at(ref);
    if (point.absoluteLocation() >= position) {
      return null;
    } else {
      while (point.absoluteLocation() < position) {
        if (point.hasNext()) {
          point.next();
        } else {
          return point;
        }
      }
      point.previous();
      return point;
    }
  }

  /**
   * @return the leftmost point in a tree, at or before a reference, that is
   *         strictly after a position.
   */
  private Point locateEndAfter(Point ref, double position) {
    SimpleMoveablePoint point = SimpleMoveablePoint.at(ref);
    if (point.absoluteLocation() <= position) {
      return null;
    } else {
      while (point.absoluteLocation() > position) {
        if (point.hasPrevious()) {
          point.previous();
        } else {
          return point;
        }
      }
      // Backtrack one.
      point.next();
      return point;
    }
  }

  public void testRandom1() {
    setUp(1, 10);
    doTest();
  }

  public void testRandom2() {
    setUp(2, 20);
    doTest();
  }

  public void testRandom3() {
    setUp(3, 1);
    doTest();
  }

  public void testRandom4() {
    setUp(4, 10);
    doTest();
  }

  public void testRandom5() {
    setUp(5, 30);
    doTest();
  }
}
