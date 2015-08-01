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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.waveprotocol.wave.client.paging.Traverser.MoveablePoint;

/**
 * Checks that blocks in a block tree have the correct page state with respect
 * to a viewport.
 *
 */
public final class ViewportRegionValidator {

  private final FakeBlock root;
  private final Region viewport;

  private ViewportRegionValidator(FakeBlock root, Region viewport) {
    this.root = root;
    this.viewport = viewport;
  }

  /**
   * Checks that blocks in a block tree have the correct page state with respect
   * to a viewport.
   *
   * @param root
   * @param viewport
   */
  public static void validate(FakeBlock root, Region viewport) {
    new ViewportRegionValidator(root, viewport).validate();
  }

  /**
   * Checks the page state of the block tree with respect to the viewport.
   */
  private void validate() {
    checkBlocks(root);
  }

  /**
   * Checks the page state of a block, and its descendants, with respect to the
   * viewport.
   *
   * @param block block to check
   */
  private void checkBlocks(FakeBlock block) {
    checkBlock(block);
    for (FakeBlock child = block.getFirstChild(); child != null; child = child.getNextSibling()) {
      checkBlocks(child);
    }
  }

  /**
   * Checks the page state of a block with respect to the viewport.
   *
   * @param block block to check
   */
  private void checkBlock(FakeBlock block) {
    if (block == root) {
      assertTrue(block.isPagedIn());
      return;
    }

    MoveablePoint start = SimpleMoveablePoint.startOf(block);
    MoveablePoint end = SimpleMoveablePoint.endOf(block);
    if (start.hasPrevious()) {
      start.previous();
    }
    if (end.hasNext()) {
      end.next();
    }
    Region blockRegion = RegionImpl.at(start.absoluteLocation(), end.absoluteLocation());
    // If a block has an edge on the viewport, then both paged in and paged out make sense.
    boolean exclude =
        blockRegion.getStart() == viewport.getEnd() || blockRegion.getEnd() == viewport.getStart();
    if (!exclude) {
      boolean shouldBeIn =
          blockRegion.getStart() < viewport.getEnd() && blockRegion.getEnd() > viewport.getStart();
      assertEquals(shouldBeIn, block.isPagedIn());
    }
  }
}
