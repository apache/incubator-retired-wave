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

/**
 * Tests for {@link ActiveRegion} based on random mutations to a random tree
 * structure.
 *
 */

public final class RandomizedMutationTest extends TestCase {

  /** Root of tree. */
  private FakeBlock root;

  /** Range of movement for viewport. */
  private RegionImpl content;

  /** Viewport. */
  private RegionImpl viewport;

  /** Target under test. */
  private ActiveRegion target;

  /** Builder for performing mutations. */
  private RandomTreeBuilder builder;

  /**
   * Sets up the random state of this test based on a seed.
   *
   * @param seed random seed
   */
  private void setUp(int seed) {
    builder = RandomTreeBuilder.create().withSeed(seed).withZeroBlockProbability(0.3);
    root = builder.build(20);
    target = ActiveRegion.over(root);

    // Define content as region that surrounds root by 10%.
    // Define viewport as being 25% of content.
    content = RegionImpl.at(root.getStart(), root.getEnd()).scale(1.1);
    viewport = RegionImpl.at(content).scale(0.25);
    viewport.moveBy(target.activate(viewport));
  }

  /**
   * Creates a moderate tree and does 30 rounds of block changes.
   */
  private void doTest() {
    ActiveRegionValidator.validate(root, target);
    ViewportRegionValidator.validate(root, viewport);

    for (int i = 0; i < 30; i++) {
      // Remove a bunch of subtrees.
      int total = 0;
      for (FakeBlock block : builder.choose(5)) {
        total += block.collect().size();
        target.onBeforeBlockRemoved(block);
        block.removeFomParent();
      }
      ActiveRegionValidator.validate(root, target);

      // Restore the number of blocks removed
      for (FakeBlock block : builder.add(total)) {
        target.onAfterBlockAdded(block);
      }
      ActiveRegionValidator.validate(root, target);

      // Activate the viewport again (expected behaviour when content changes).
      viewport.moveBy(target.activate(viewport));
      ActiveRegionValidator.validate(root, target);
      ViewportRegionValidator.validate(root, viewport);
    }
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
