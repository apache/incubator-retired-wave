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

import org.waveprotocol.wave.client.paging.FakeBlock.Dimensions;
import org.waveprotocol.wave.client.paging.Traverser.MoveablePoint;
import org.waveprotocol.wave.client.paging.Traverser.Point;
import org.waveprotocol.wave.client.paging.Traverser.SimplePoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link MoveablePoint}.
 *
 */

public final class PointTest extends TestCase {

  private FakeBlock root;

  @Override
  protected void setUp() {
    root = FakeBlock.root().create("a", Dimensions.zero());
  }

  private FakeBlock appendChild(FakeBlock parent, String name) {
    return parent.append().create(name, Dimensions.zero());
  }

  private List<Point> buildComplex() {
    //         A
    //        / \
    //       B   H
    //      / \   \
    //     C   D   I
    //        /|\
    //       E F G
    //
    FakeBlock a = root;
    FakeBlock b = appendChild(a, "b");
    FakeBlock c = appendChild(b, "c");
    FakeBlock d = appendChild(b, "d");
    FakeBlock e = appendChild(d, "e");
    FakeBlock f = appendChild(d, "f");
    FakeBlock g = appendChild(d, "g");
    FakeBlock h = appendChild(a, "h");
    FakeBlock i = appendChild(h, "i");

    // Expected point ordering.
    return Arrays.<Point>asList(
        SimplePoint.startOf(a),
        SimplePoint.startOf(b),
        SimplePoint.startOf(c),
        SimplePoint.endOf(c),
        SimplePoint.startOf(d),
        SimplePoint.startOf(e),
        SimplePoint.endOf(e),
        SimplePoint.startOf(f),
        SimplePoint.endOf(f),
        SimplePoint.startOf(g),
        SimplePoint.endOf(g),
        SimplePoint.endOf(d),
        SimplePoint.endOf(b),
        SimplePoint.startOf(h),
        SimplePoint.startOf(i),
        SimplePoint.endOf(i),
        SimplePoint.endOf(h),
        SimplePoint.endOf(a));
  }

  //
  // Movement.
  //

  public void testComplexNext() {
    List<Point> expected = buildComplex();
    List<Point> actual = new ArrayList<Point>();

    MoveablePoint point = SimpleMoveablePoint.startOf(root);
    actual.add(SimplePoint.at(point));
    while (point.hasNext()) {
      point.next();
      actual.add(SimplePoint.at(point));
    }

    assertEquals(expected, actual);
  }

  public void testComplexPrevious() {
    List<Point> expected = buildComplex();
    Collections.reverse(expected);
    List<Point> actual = new ArrayList<Point>();

    MoveablePoint point = SimpleMoveablePoint.endOf(root);
    actual.add(SimplePoint.at(point));
    while (point.hasPrevious()) {
      point.previous();
      actual.add(SimplePoint.at(point));
    }

    assertEquals(expected, actual);
  }

  //
  // Comparison.
  //

  public void testAllCompares() {
    Point [] points = buildComplex().toArray(new Point [] {});
    for (int i = 0; i < points.length; i++) {
      for (int j = 0; j < points.length; j++) {
        assertEquals(new Integer(i).compareTo(new Integer(j)), points[i].compareTo(points[j]));
      }
    }
  }
}
