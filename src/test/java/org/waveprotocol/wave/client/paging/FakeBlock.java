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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple block tree for testing.
 *
 */
public final class FakeBlock extends AbstractTreeNode<FakeBlock> implements Block {

  interface Factory {
    FakeBlock create(String name, Dimensions size);
  }

  static class Dimensions {
    /** Maximum margin size. */
    private final static int MAX_MARGIN = 40;
    /** Maximum padding size. */
    private final static int MAX_PADDING = 40;
    /** Probability that a block will be an offset parent. */
    private final static double PARENT_PROBABILITY = 0.3;

    /** Randomizer for wobbling. */
    private final Random random;

    /** External space before and after this block. */
    int margin;
    /** Internal space at each end of this block. */
    int padding;
    /** True iff this block acts as a new origin for descendant positions. */
    boolean isOffsetParent;

    private Dimensions(Random random) {
      this.random = random;
    }

    /** @return a specification for an empty block. */
    static Dimensions zero() {
      return new Dimensions(null);
    }

    /** @return a specification for block of random size. */
    static Dimensions create(Random random) {
      Dimensions dimensions = new Dimensions(random);
      dimensions.wobble();
      return dimensions;
    }

    /**
     * Randomly adjusts this size specification.
     */
    void wobble() {
      if (random != null) {
        margin = random.nextInt(MAX_MARGIN);
        padding = random.nextInt(MAX_PADDING);
        isOffsetParent = random.nextDouble() < PARENT_PROBABILITY;
      }
    }
  }

  private final String name;
  private final Dimensions layout;

  private boolean pagedIn;

  private static final Factory ROOT = new Factory() {
    @Override
    public FakeBlock create(String name, Dimensions size) {
      return new FakeBlock(name, size);
    }
  };

  private FakeBlock(String name, Dimensions size) {
    this.name = name;
    this.layout = size;
  }

  @Override
  protected FakeBlock self() {
    return this;
  }

  public static FakeBlock.Factory root() {
    return ROOT;
  }

  private boolean hasParent() {
    return getParent() != null;
  }

  //
  // Covariant mutation.
  //

  public FakeBlock.Factory prepend() {
    return new FakeBlock.Factory() {
      @Override
      public FakeBlock create(String name, Dimensions size) {
        return prepend(new FakeBlock(name, size));
      }
    };
  }

  public FakeBlock.Factory append() {
    return new FakeBlock.Factory() {
      @Override
      public FakeBlock create(String name, Dimensions size) {
        return append(new FakeBlock(name, size));
      }
    };
  }

  public FakeBlock.Factory insertBefore(final FakeBlock reference) {
    return new FakeBlock.Factory() {
      @Override
      public FakeBlock create(String name, Dimensions size) {
        return insertBefore(reference, new FakeBlock(name, size));
      }
    };
  }

  public FakeBlock.Factory insertAfter(final FakeBlock reference) {
    return new FakeBlock.Factory() {
      @Override
      public FakeBlock create(String name, Dimensions size) {
        return insertAfter(reference, new FakeBlock(name, size));
      }
    };
  }

  public void removeFomParent() {
    Preconditions.checkState(hasParent());
    remove();
  }

  //
  // Paging state.
  //

  @Override
  public void pageIn() {
    Preconditions.checkState(!pagedIn);
    Preconditions.checkState(!hasParent() || getParent().isPagedIn());
    pagedIn = true;
    layout.wobble();
  }

  @Override
  public void pageOut() {
    Preconditions.checkState(pagedIn);
    Preconditions.checkState(hasParent() || getParent().isPagedIn());
    pagedIn = false;
    layout.wobble();
  }

  public boolean isPagedIn() {
    return pagedIn;
  }

  //
  // Dimensions.
  //

  /** @return the start of this block's margin box. */
  private double getOuterStart() {
    if (getPreviousSibling() != null) {
      return getPreviousSibling().getOuterEnd();
    } else if (getParent() != null) {
      return getParent().getInnerStart() - getParent().getChildrenOrigin();
    } else {
      return 0;
    }
  }

  /** @return the end of this block's margin box. */
  private double getOuterEnd() {
    return getEnd() + layout.margin;
  }

  private double getInnerStart() {
    return getStart() + layout.padding;
  }

  private double getInnerEnd() {
    if (getLastChild() != null) {
      return getLastChild().getOuterEnd() + getChildrenOrigin();
    } else {
      return getInnerStart();
    }
  }

  @Override
  public double getStart() {
    return getOuterStart() + layout.margin;
  }

  @Override
  public double getEnd() {
    return getInnerEnd() + layout.padding;
  }

  @Override
  public double getChildrenOrigin() {
    return layout.isOffsetParent ? getInnerStart() : 0;
  }

  //
  // Naming.
  //

  public String getName() {
    return name;
  }

  //
  // Random.
  //

  public List<FakeBlock> collect() {
    List<FakeBlock> all = new ArrayList<FakeBlock>();
    collectInto(all);
    return all;
  }

  private void collectInto(List<FakeBlock> all) {
    all.add(this);
    for (FakeBlock child = getFirstChild(); child != null; child = child.getNextSibling()) {
      child.collectInto(all);
    }
  }

  public String render() {
    return render(new StringBuffer()).toString();
  }

  public String renderWithNames() {
    return renderWithNames(new StringBuffer()).toString();
  }

  private StringBuffer render(StringBuffer sb) {
    sb.append(pagedIn ? "[" : "(");
    for (FakeBlock child = getFirstChild(); child != null; child = child.getNextSibling()) {
      child.render(sb);
    }
    sb.append(pagedIn ? "]" : ")");
    return sb;
  }

  private StringBuffer renderWithNames(StringBuffer sb) {
    sb.append(pagedIn ? "[ " : "( ");
    sb.append(name);
    for (FakeBlock child = getFirstChild(); child != null; child = child.getNextSibling()) {
      sb.append(" ");
      child.renderWithNames(sb);
    }
    sb.append(pagedIn ? " ]" : " )");
    return sb;
  }

  @Override
  public String toString() {
    return render();
  }
}
