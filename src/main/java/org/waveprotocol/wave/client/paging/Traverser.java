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

import org.waveprotocol.wave.client.common.util.LinkedSequence;

/**
 * Defines strategies for traversing {@link Point points} in a block tree.
 *
 */
public final class Traverser {

  /** A side of a block. */
  public enum BlockSide {
    /** The start of a block. */
    START {
      @Override
      double of(Block b) {
        return b.getStart();
      }
    },
    /** The end of a block. */
    END {
      @Override
      double of(Block b) {
        return b.getEnd();
      }
    };

    /** @return the location of this side of a block. */
    abstract double of(Block b);
  }

  /**
   * A side of a particular block.
   */
  public static abstract class Point implements Comparable<Point> {
    /** A number to represent an unset value for {@link #cachedOrigin}. */
    protected static final double UNSET = Double.NEGATIVE_INFINITY; // NaN does not work.
    /** The side of this point. */
    BlockSide side;
    /** The block of this point. */
    Block block;
    /** Cached value of {@link #origin()}. */
    double cachedOrigin = UNSET;

    public Point(BlockSide side, Block block) {
      this.side = side;
      this.block = block;
    }

    /** @return the location of this point, relative to its block's parent. */
    final double location() {
      return side.of(block);
    }

    /** @return the absolute location of this point. */
    final double absoluteLocation() {
      double ret = origin() + location();
      return ret;
    }

    /**
     * @return the absolute location of the origin, relative to which
     *         {@link #location()} is expressed.
     */
    private double origin() {
      if (cachedOrigin == UNSET) {
        double origin = 0.0;
        Block parent = block.getParent();
        while (parent != null) {
          origin += parent.getChildrenOrigin();
          parent = parent.getParent();
        }
        cachedOrigin = origin;
      }
      return cachedOrigin;
    }

    /**
     * Invalidates any cached origin on this point. This method needs to be
     * called if any part of the block tree may have changed since call to
     * {@link #absoluteLocation()}.
     */
    void invalidateCachedOrigin() {
      cachedOrigin = UNSET;
    }

    /**
     * {@inheritDoc}
     *
     * The algorithm used to compare points is based on the sibling ordering of
     * children in the lowest common ancestor. It is not constant time. For thin
     * narrow trees, or wide short trees, comparing widely separated points may
     * approach linear complexity.
     */
    @Override
    public int compareTo(Point other) {
      if (this.block == other.block) {
        return this.side == other.side ? 0 : this.side == BlockSide.START ? -1 : 1;
      } else {
        // Build the LCA triple: the lowest common ancestor (lca) of this and
        // other, the child of the LCA that is an ancestor of this (thisColca),
        // and the child of the LCA that is an ancestor of other (otherColca).
        Block lca = null;
        Block thisColca = null;
        Block otherColca = null;

        // The lca is found by collecting all ancestors of this, then finding
        // the first ancestor of other that is in that collection. The two
        // colcas are found by backtracking one in the ancestor chain of each.
        LinkedSequence<Block> thisAncestors = LinkedSequence.create();
        for (Block block = this.block; block != null; block = block.getParent()) {
          thisAncestors.append(block);
        }
        for (Block block = other.block; block != null; block = block.getParent()) {
          if (thisAncestors.contains(block)) {
            lca = block;
            break;
          }
          otherColca = block;
        }

        if (lca == null) {
          throw new IllegalArgumentException("no common ancestor of " + this + " and " + other);
        } else {
          thisColca = thisAncestors.getPrevious(lca);
        }

        // thisColca and otherColca both null implies that this.block =
        // other.block, which is in a separate branch.
        assert !(thisColca == null && otherColca == null);

        if (thisColca == null) {
          // Other point's block is a descendant of this point's block.
          return this.side == BlockSide.START ? -1 : 1;
        } else if (otherColca == null) {
          // This point's block is a descendant of other point's block.
          return other.side == BlockSide.START ? 1 : -1;
        } else {
          assert thisColca.getParent() == otherColca.getParent() && thisColca != otherColca;
          // Sibling ordering of thisColca and otherColca defines the ordering of these two points.
          Block sibling = thisColca.getNextSibling();
          while (sibling != null) {
            if (sibling == otherColca) {
              // This is before Other.
              return -1;
            }
            sibling = sibling.getNextSibling();
          }
          // Other is before This.
          return 1;
        }
      }
    }

    public BlockSide getSide() {
      return side;
    }

    public Block getBlock() {
      return block;
    }

    @Override
    public final boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof Point)) {
        return false;
      } else {
        Point other = (Point) obj;
        return this.block == other.block && this.side == other.side;
      }
    }

    @Override
    public final int hashCode() {
      return 37 * block.hashCode() + side.hashCode();
    }

    @Override
    public final String toString() {
      return side + " of " + block;
    }
  }

  /**
   * A point that is intended to be immutable, since this class exposes no
   * mutators.
   */
  public static final class SimplePoint extends Point {

    protected SimplePoint(BlockSide side, Block block) {
      super(side, block);
    }

    static SimplePoint startOf(Block block) {
      return new SimplePoint(BlockSide.START, block);
    }

    static SimplePoint endOf(Block block) {
      return new SimplePoint(BlockSide.END, block);
    }

    public static SimplePoint at(Point point) {
      return new SimplePoint(point.side, point.block);
    }
  }

  /**
   * A mutable point that can be {@link #set(Point) placed} at a point, or at
   * {@link #clear() no point}, and {@link #next() moved} forwards and
   * backwards.
   */
  static abstract class MoveablePoint extends Point {

    MoveablePoint(BlockSide side, Block block) {
      super(side, block);
    }

    /** @return true if this point is non-vacuous. */
    boolean isActive() {
      return block != null;
    }

    /**
     * Places this point.
     */
    void set(Point point) {
      this.side = point.side;
      this.block = point.block;
      invalidateCachedOrigin();
    }

    /**
     * Unplaces this point.
     */
    void clear() {
      this.side = null;
      this.block = null;
      invalidateCachedOrigin();
    }

    /**
     * Shifts the cached origin, if there is one.
     */
    private void shiftOrigin(double shift) {
      if (cachedOrigin != UNSET) {
        cachedOrigin += shift;
      }
    }

    /** @return true if there is a {@link #next()} point. */
    protected final boolean hasNext() {
      return !(side == BlockSide.END && isRoot(block));
    }

    /** @return true if there is a {@link #previous()} point. */
    protected final boolean hasPrevious() {
      return !(side == BlockSide.START && isRoot(block));
    }

    /**
     * Moves to the next point.
     */
    protected void next() {
      Block next;
      switch (side) {
        case START:
          if ((next = block.getFirstChild()) != null) {
            //           ______________
            // this --> |   _____      |
            // next ------>|     | ... |
            //
            shiftOrigin(block.getChildrenOrigin());
            block = next;
          } else {
            //           _____
            // this --> |     | <-- next
            //
            side = BlockSide.END;
          }
          break;
        case END:
          if ((next = block.getNextSibling()) != null) {
            //   _____                      _____
            //  |     | <-- this  next --> |     |
            //
            side = BlockSide.START;
            block = next;
          } else if ((next = block.getParent()) != null) {
            //  ______________
            // |      _____   | <-- next
            // | ... |     |<------ this
            //
            block = next;
            shiftOrigin(-block.getChildrenOrigin());
          } else {
            //  _____
            // |     | <-- this
            //
            // Can not go any further.  Was not prefixed with hasNext().
            throw new IllegalStateException("next() called without hasNext()");
          }
          break;
        default:
          // Unreachable (switch handles every value).
          throw new RuntimeException();
      }
    }

    /**
     * Moves to the previous point.
     *
     * @throws IllegalStateException if there is no previous point.
     */
    protected void previous() {
      Block prev;
      switch (side) {
        case END:
          if ((prev = block.getLastChild()) != null) {
            //  ______________
            // |      _____  <|  this
            // | ... |    <|  |  prev
            //
            shiftOrigin(block.getChildrenOrigin());
            block = prev;
          } else {
            //         _____
            // prev  <|    <|  this
            //
            side = BlockSide.START;
          }
          break;
        case START:
          if ((prev = block.getPreviousSibling()) != null) {
            //         _____     _____
            // prev   |    <|  <|     |  this
            //
            block = prev;
            side = BlockSide.END;
          } else if ((prev = block.getParent()) != null) {
            //         ______________
            // prev  <|   _____      |
            // this   | <|     | ... |
            //
            block = prev;
            shiftOrigin(-block.getChildrenOrigin());
          } else {
            //         _____
            // this  <|     |
            //
            // Can not go any further.  Was not prefixed with hasPrevious().
            throw new IllegalStateException("previous() called without hasPrevious()");
          }
          break;
        default:
          // Unreachable (switch handles every value).
          throw new RuntimeException();
      }
    }
  }

  private Traverser() {
  }

  /**
   * Finds the rightmost point in a tree that is strictly before a position.
   *
   * @param root root of a block tree
   * @param position location (in absolute space)
   * @return what the method says.
   * @throws IllegalArgumentException if {@code root} is not a root block.
   */
  public static Point locateStartWithin(Block root, double position) {
    Preconditions.checkArgument(isRoot(root), "Not a root block");
    return locateStart(root, root, position);
  }

  /**
   * Finds the leftmost point in a tree that is strictly after a position.
   *
   * @param root root of a block tree
   * @param position location (in absolute space)
   * @return what the method says.
   * @throws IllegalArgumentException if {@code root} is not a root block.
   */
  public static Point locateEndWithin(Block root, double position) {
    Preconditions.checkArgument(isRoot(root), "Not a root block");
    return locateEnd(root, root, position);
  }

  /**
   * Finds the last point, at or after a given point, located strictly before a
   * position.
   *
   * @param point
   * @param position position (in absolute space)
   * @return what the method says (never null).
   * @throws IllegalArgumentException if {@code point} is not strictly before
   *         {@code position}.
   */
  public static Point locateStartAfter(Point point, double position) {
    //
    // The gist of this method is:
    //
    //   do {
    //     point.next();
    //   } while (point.location() < position);
    //   point.previous();
    //
    // but optimized to skip subtrees.
    //

    // Rebase position.
    position -= point.origin();
    Preconditions.checkArgument(point.location() < position);
    Block block = point.block;

    if (block.getEnd() >= position) {
      //   [ block ]
      //     |<--->|
      // The answer is a point between start and end of this block.
      return locateStart(block, block, position);
    } else {
      // Walk up the tree, so that entire subtrees of next-siblings can be skipped.
      // Invariant: block.getEnd() < position
      Block parent;
      double parentPosition;
      while ((parent = block.getParent()) != null
          && parent.getEnd() < (parentPosition = parent.getChildrenOrigin() + position)) {
        //    _______________
        //   |    _____     <|  next
        //   |   |    <| ... |  this
        //                            |<--- position
        block = parent;
        position = parentPosition;
      }

      if (parent == null) {
        // No parent means block == root, and there is nothing after its end.
        return SimplePoint.endOf(block);
      } else {
        // End of block is before position, but there may be a subsequent point
        // that is also before position. If so, it must be before end of parent,
        // which is known to be after position.
        Block next = block.getNextSibling();
        Point answer = next != null ? locateStart(next, parent.getLastChild(), position) : null;
        return answer != null ? answer : SimplePoint.endOf(block);
      }
    }
  }

  /**
   * Finds the first point, at or before a given point, located strictly after a
   * position.
   *
   * @param point
   * @param position (in absolute space)
   * @return what the method says (never null).
   * @throws IllegalArgumentException if {@code point} is not strictly after
   *         {@code position}.
   */
  public static Point locateEndBefore(Point point, double position) {
    //
    // The gist of this method is:
    //
    //   do {
    //     point.previous();
    //   } while (point.location() > position);
    //   point.next();
    //
    // but optimized to skip subtrees.
    //

    // Rebase position.
    position -= point.origin();
    Preconditions.checkArgument(point.location() > position);
    Block block = point.block;

    if (block.getStart() <= position) {
      //   [ block ]
      //   |<--->|
      // The answer is a point between start and end of this block.
      return locateEnd(block, block, position);
    } else {
      // Walk up the tree, so that entire subtrees of next-siblings can be skipped.
      // Invariant: block.getStart() > position
      Block parent;
      double parentPosition;
      while ((parent = block.getParent()) != null
          && parent.getStart() > (parentPosition = parent.getChildrenOrigin() + position)) {
        //         _______________
        // next   |>     _____    |
        // this   | ... |>    |   |  this
        //    |<--- position
        block = parent;
        position = parentPosition;
      }

      if (parent == null) {
        // No parent means block == root, and there is nothing before its start.
        return SimplePoint.startOf(block);
      } else {
        // Start of block is after position, but there may be a previous point
        // that is also after position. If so, it must be after start of parent,
        // which is known to be before position.
        Block prev = block.getPreviousSibling();
        Point answer = prev != null ? locateEnd(parent.getFirstChild(), prev, position) : null;
        return answer != null ? answer : SimplePoint.startOf(block);
      }
    }
  }

  /**
   * Finds the rightmost point in a list of siblings that is strictly before a
   * position.
   *
   * @param first first child in a sibling list
   * @param last last child in a sibling list
   * @param position search position
   * @return what the method says.
   */
  private static Point locateStart(Block first, Block last, double position) {
    if (first == null) {
      assert last == null;
      return null;
    }

    double fromFirst = position - first.getStart();
    double toLast = last.getEnd() - position;

    // Check boundaries.
    if (fromFirst <= 0) {
      //      [first] ... [last]
      //   <--|
      // No point is strictly before position.
      return null;
    } else if (toLast < 0) {
      //      [first] ... [last]
      //                         |-->
      // Very last point is the rightmost point before position.
      return SimplePoint.endOf(last);
    }

    //
    // [first]  ...  [last]
    //   |<-------------->|
    // Find the oldest sibling with a start before position. That sibling is
    // the best candidate of all siblings, so the answer must be in the subtree
    // of that sibling.
    //
    Block oldest;
    if (first == last) {
      oldest = first;
    } else if (fromFirst <= toLast) {
      // Search, forwards from first, for the first block that starts at, or
      // after, position, then backtrack one.
      oldest = first;
      Block next = oldest.getNextSibling();
      while (next != null && next.getStart() < position) {
        oldest = next;
        next = oldest.getNextSibling();
      }
    } else {
      // Search, backwards from last, for the first block that starts before
      // position. Given that first starts before position, then it is known
      // that this will not terminate with null.
      oldest = last;
      while (!(oldest.getStart() < position)) {
        oldest = oldest.getPreviousSibling();
      }
    }
    // All three branches above maintain the invariant:
    assert oldest.getStart() < position;

    // ...]   [oldest]   [...
    //           |<----->|
    // The answer is within the subtree of oldest.
    if (oldest.getEnd() < position) {
      return SimplePoint.endOf(oldest);
    } else {
      //   [oldest]
      //     |<-->|
      // (Note: this method is tail-recursive, so could be optimized into a loop
      // if desired).
      double childPosition = position - oldest.getChildrenOrigin();
      Point answer = locateStart(oldest.getFirstChild(), oldest.getLastChild(), childPosition);
      return answer != null ? answer : SimplePoint.startOf(oldest);
    }
  }

  /**
   * Finds the leftmost point in a list of siblings that is strictly after a
   * position.
   *
   * @param first first child in a sibling list
   * @param last last child in a sibling list
   * @param position search position
   * @return what the method says.
   */
  private static Point locateEnd(Block first, Block last, double position) {
    if (first == null) {
      assert last == null;
      return null;
    }

    double fromFirst = position - first.getStart();
    double toLast = last.getEnd() - position;

    // Check boundaries.
    if (toLast <= 0) {
      //      [first] ... [last]
      //                       |-->
      // No point is strictly after position.
      return null;
    } else if (fromFirst < 0) {
      //        [first] ... [last]
      //   <--|
      // Very first point is the leftmost point after position.
      return SimplePoint.startOf(first);
    }

    //
    // [first]  ...  [last]
    // |<-------------->|
    // Find the youngest sibling with an end after position. That sibling is
    // the best candidate of all siblings, so the answer must be in the subtree
    // of that sibling.
    //
    Block youngest;
    if (first == last) {
      youngest = first;
    } else if (toLast <= fromFirst) {
      // Search, backwards from last, for the first block that ends at, or
      // before, position, then backtrack one.
      youngest = last;
      Block previous = youngest.getPreviousSibling();
      while (previous != null && previous.getEnd() > position) {
        youngest = previous;
        previous = youngest.getPreviousSibling();
      }
    } else {
      // Search, forwards from first, for the first block that ends after
      // position. Given that last ends after position, then it is known
      // that this will not terminate with null.
      youngest = first;
      while (!(youngest.getEnd() > position)) {
        youngest = youngest.getNextSibling();
      }
    }
    // All three branches above maintain the invariant:
    assert youngest.getEnd() > position;

    // ...]   [youngest]   [...
    //    |<----->|
    // The answer is within the subtree of youngest
    if (youngest.getStart() > position) {
      return SimplePoint.startOf(youngest);
    } else {
      //   [youngest]
      //   |<-->|
      // (Note: this method is tail-recursive, so could be optimized into a loop
      // if desired).
      double childPosition = position - youngest.getChildrenOrigin();
      Point answer = locateEnd(youngest.getFirstChild(), youngest.getLastChild(), childPosition);
      return answer != null ? answer : SimplePoint.endOf(youngest);
    }
  }

  /** @return true if and only if {@code block} is a root block. */
  private static boolean isRoot(Block block) {
    return block.getParent() == null && block.getPreviousSibling() == null
        && block.getNextSibling() == null;
  }

  /**
   * Tests if a point is between two others. This method is not constant time,
   * and may be linear in the worst case.
   *
   * @return true if and only if {@code point} is strictly between {@code start}
   *         and {@code end}.
   */
  public static boolean isBetween(Point start, Point end, Point point) {
    return start.compareTo(point) < 0 && point.compareTo(end) < 0;
  }

  /**
   * @return true if and only if {@code a} is a descendant of {@code b}.
   */
  public static boolean isDescendant(Block b, Block a) {
    for (Block block = a; block != null; block = block.getParent()) {
      if (block == b) {
        return true;
      }
    }
    return false;
  }
}
