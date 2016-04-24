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

import java.util.Random;

/**
 * Tests for {@link ActiveRegion} based on random tree structures and locations.
 *
 */

public final class RandomizedActivationLargeTest extends TestCase {

  /** Randomizer. */
  private Random random;

  /** Root of tree. */
  private FakeBlock root;

  /** Range of movement for viewport. */
  private RegionImpl content;

  /** Viewport. */
  private RegionImpl viewport;

  /** Target under test. */
  private ActiveRegion target;

  /**
   * Sets up the random state of this test based on a seed.
   *
   * @param seed random seed
   */
  private void setUp(int seed) {
    random = new Random(seed);
    root = RandomTreeBuilder.create().withSeed(seed).withZeroBlockProbability(0.3).build(50);
    target = ActiveRegion.over(root);

    // Define content as region that surrounds root by 10%.
    // Define viewport as being 15% of content.
    content = RegionImpl.at(root.getStart(), root.getEnd()).scale(1.1);
    viewport = RegionImpl.at(content).scale(0.15);
  }

  /**
   * Creates a biggish tree and makes 30 viewport movements.
   */
  private void doTest() {
    for (int i = 0; i < 30; i++) {
      moveViewport();
      viewport.moveBy(target.activate(viewport));
      ActiveRegionValidator.validate(root, target);
      ViewportRegionValidator.validate(root, viewport);
    }
  }

  /**
   * Moves the viewport top to a random location in the content region.
   */
  private void moveViewport() {
    double newTop = content.getStart() + random.nextDouble() * content.getSize();
    double shift = newTop - viewport.getStart();
    viewport.moveBy(shift);
  }

  /**
   * For 50 different seeds, runs the randomized test.
   */
  public void testManyRandoms() {
    for (int i = 0; i < 50; i++) {
      setUp(i);
      doTest();
    }
  }
}
