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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.paging.FakeBlock.Dimensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Builds a randomized tree.
 *
 */
public final class RandomTreeBuilder {

  private Random random = new Random(0);

  /** Probability that new blocks have zero size. */
  private double zeroProbability = 0.1;

  private FakeBlock root;

  private int counter;

  private RandomTreeBuilder() {
  }

  public static RandomTreeBuilder create() {
    return new RandomTreeBuilder();
  }

  public RandomTreeBuilder withSeed(int seed) {
    this.random = new Random(seed);
    return this;
  }

  public RandomTreeBuilder withZeroBlockProbability(double probability) {
    this.zeroProbability = probability;
    return this;
  }

  public FakeBlock build(int size) {
    Preconditions.checkArgument(size > 0);
    root = create(FakeBlock.root());
    List<FakeBlock> all = root.collect();
    for (int i = 1; i < size; i++) {
      all.add(create(choose(all).append()));
    }
    return root;
  }

  public List<FakeBlock> add(int n) {
    List<FakeBlock> all = root.collect();
    List<FakeBlock> added = new ArrayList<FakeBlock>();
    for (int i = 0; i < n; i++) {
      added.add(create(choose(all).append()));
      // 'all' is intentionally not updated, in order not to have overlapping adds.
    }
    return added;
  }

  public List<FakeBlock> choose(int min) {
    List<FakeBlock> all = root.collect();
    List<FakeBlock> topChosen = new ArrayList<FakeBlock>();
    Collection<FakeBlock> allChosen = new HashSet<FakeBlock>();
    while (allChosen.size() < min && all.size() > 1) {
      FakeBlock block = choose(all);

      // Don't allow root removal.
      if (block == root) {
        continue;
      }

      topChosen.add(block);
      allChosen.addAll(block.collect());
      all.removeAll(allChosen);
    }
    return topChosen;
  }

  private FakeBlock create(FakeBlock.Factory with) {
    boolean isZero = random.nextDouble() < zeroProbability;
    Dimensions childSize = isZero ? Dimensions.zero() : Dimensions.create(random);
    FakeBlock child = with.create(name(), childSize);
    return child;
  }

  private FakeBlock choose(List<FakeBlock> allBlocks) {
    return allBlocks.get(choose(random, allBlocks.size()));
  }

  /**
   * Chooses a number between zero (inclusive) and {@code total} (exclusive),
   * with a probability bias towards lower numbers.
   */
  private static int choose(Random random, int total) {
    // Decay function is of the form:
    //   d(x) = a(e^(3x) - 1)
    // with constraints d(0) = 0 and d(1) = 1.
    //
    double a = 1.0 / (Math.exp(3) - 1);
    double x = random.nextDouble();
    double d = a * (Math.exp(3 * x) - 1);

    return (int) (d * total);
  }

  /** @return a block name for the next block. */
  private String name() {
    return "" + counter++;
  }
}
