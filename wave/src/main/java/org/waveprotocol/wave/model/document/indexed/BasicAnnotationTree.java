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

import org.waveprotocol.wave.model.util.CollectionFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree-based data structure for annotations.
 *
 * This has a simple random access interface.  AnnotationTree
 * implements a streaming interface and other support for
 * IndexedDocumentImpl on top of this simple interface.
 *
 * Note: This is not a brute-force implementation.
 *
 * @param <V> the value type
 *
 * @author ohler@google.com (Christian Ohler)
 */
// Package-private because only AnnotationTree needs access.
class BasicAnnotationTree<V> {

  // The basic ideas behind this data structure:
  //
  // The underlying document is a sequence of items that we don't know
  // much about (except for its length, since our length is the
  // same). We maintain a set of key-value pairs (annotations) for
  // every item, with the optimization that we often (but not always)
  // merge consecutive runs of items with the same annotations into an
  // interval.
  //
  // These intervals form the leaves of a red-black tree.  Each
  // interior node of the tree thus represents a sequence of two or
  // more intervals and stores its total length to allow quick
  // navigation by index. Key-value pairs that are common for the
  // entire range represented by an interior node are also stored in
  // that node, not in the nodes below it.  (Moving common key-value
  // pairs from siblings to their parent is what we call
  // "propagation".)  This, together with the fact that we always
  // force each item to have a value for every key, allows relatively
  // fast "where is the next change of the value of this key" lookups.
  //
  // Updates are somewhat complicated. Setting an annotation (or
  // deleting a range) can lead to up to two intervals splits and/or
  // an arbitrary number of propagations and interval merges. Since
  // the red-black tree may have to rotate when internal nodes are
  // added or removed, and rotations along the path back to the root
  // confuse our recursive update algorithms, we use two tricks: When
  // we notice that we have to split nodes, we update only part of the
  // intervals, allow the tree to rotate, and then update the
  // remainder. When we merge intervals, we defer any deletions (and
  // thus rotations) until we have completed the entire update.
  //
  // Newly inserted items will always inherit the annotations from their
  // left neighbors; insertions on the left border will have no annotations
  // initially.  To avoid special cases in the difficult parts of the
  // algorithms, we have one sentinel item on the left with no annotations.
  // This is where the +1/-1 operations when translating the API methods to
  // the internal tree operations come from.
  // TODO(ohler): Maybe it would be simpler to define that this item has the
  // index -1?

  private enum NodeType {
    // Leaves are always black.
    LEAF_BLACK, INTERNAL_RED, INTERNAL_BLACK;
  }

  private final CollectionFactory factory = CollectionUtils.getCollectionFactory();

  private final StringSet knownKeys;

  // I haven't checked recently whether the sentinel still simplifies things.
  // It might be redundant now.
  private Node sentinel;
  private final V oneValue;
  private final V anotherValue;
  private List<Node> leavesThatHaveBecomeEmpty = new ArrayList<Node>();

  private int nextId = 0;
  // We give every node a sequential number for debugging.
  private int createNodeId() {
    return nextId++;
  }

  private Node newLeaf(int subtreeLength) {
    return new Node(NodeType.LEAF_BLACK, subtreeLength, factory.<V>createStringMap());
  }

  private Node newInternalNode(int subtreeLength) {
    return new Node(NodeType.INTERNAL_BLACK, subtreeLength, factory.<V>createStringMap());
  }

  private Node newInternalNode(int subtreeLength, StringMap<V> localMap) {
    return new Node(NodeType.INTERNAL_BLACK, subtreeLength, localMap);
  }

  // Instances would be more compact (no pointer to outer class) if this were
  // static, but we use things like checkState and the sentinel from the outer
  // class, so it would be some work to make it static.
  private final class Node {
    NodeType type;
    protected int subtreeLength;
    Node parent;
    Node left;
    Node right;
    protected StringMap<V> localMap;
    int id;

    private Node(NodeType type, int subtreeLength, StringMap<V> localMap) {
      this.subtreeLength = subtreeLength;
      this.localMap = localMap;
      this.type = type;
      this.id = createNodeId();
    }

    // red/unred state
    boolean isRed() {
      return type == NodeType.INTERNAL_RED;
    }

    void setRed(boolean flag) {
      if (type == NodeType.LEAF_BLACK) {
        assert flag == false;
      } else {
        type = flag ? NodeType.INTERNAL_RED : NodeType.INTERNAL_BLACK;
      }
    }

    boolean isLeaf() {
      return type == NodeType.LEAF_BLACK;
    }

    boolean isRoot() {
      return parent == sentinel;
    }

    void eraseAnnotations(int nodeStart, String key) {
      if (localMap.containsKey(key)) {
        localMap.remove(key);
        return;
      }
      assert !isLeaf();
      left.eraseAnnotations(nodeStart, key);
      right.eraseAnnotations(nodeStart + left.subtreeLength, key);
      tryToMergeChildren();
    }

    boolean isLeftChild() {
      if (parent.left == this) {
        return true;
      } else {
        assert parent.right == this;
        return false;
      }
    }

    Node sibling() {
      // doesn't hold when rebalancing a child of the root
      //assert this != root();
      if (isLeftChild()) {
        return parent.right;
      } else {
        return parent.left;
      }
    }

    void replaceThisNodeWith(Node other) {
      assert other.parent == null;
      other.parent = parent;
      if (isLeftChild()) {
        parent.left = other;
      } else {
        parent.right = other;
      }
      // Just to be clean.
      parent = null;
    }

    // Should be used only for debugging utility functions, not on normal
    // execution paths.
    int absoluteFromRelative(int relativeIndex) {
      if (this == sentinel) {
        return relativeIndex;
      }
      if (isLeftChild()) {
        return parent.absoluteFromRelative(relativeIndex);
      } else {
        return parent.absoluteFromRelative(relativeIndex + parent.left.subtreeLength);
      }
    }

    @Override
    public String toString() {
      String rangeString;
      try {
        rangeString = "" + absoluteFromRelative(0)
            + "+" + subtreeLength
            + "=" + absoluteFromRelative(subtreeLength);
      } catch (RuntimeException e) {
        rangeString = "<RuntimeException computing range; length=" + subtreeLength + ">";
      }
      String typeString;
      switch (type) {
        case INTERNAL_BLACK:
          typeString = "internal, black";
          break;
        case INTERNAL_RED:
          typeString = "internal, red";
          break;
        case LEAF_BLACK:
          typeString = "leaf, black";
          break;
        default:
          typeString = "<error: invalid node type " + type + ">";
      }
      return pathString() + " Node (" + id() + ") "
          + rangeString + " " + typeString
          + (this == sentinel ? " (sentinel)" : this == root() ? " (root)" : "")
          + " " + mapToString(localMap);
    }

    String pathString() {
      if (isRoot()) {
        return "#";
      }
      if (sentinel == this) {
        return "S";
      }
      if (parent == null) {
        // Orphan.
        return "O";
      }
      if (this == parent.left) {
        return parent.pathString() + "l";
      }
      if (this == parent.right) {
        return parent.pathString() + "r";
      }
      return "<not a child of parent>";
    }

    final String id() {
      return id + ":" + Integer.toHexString(System.identityHashCode(this));
    }

    void rebalanceAfterRemoval() {
      if (isRoot()) {
        return;
      }
      Node s = sibling();
      if (s.isRed()) {
        parent.setRed(true);
        s.setRed(false);
        if (isLeftChild()) {
          parent.rotateL();
        } else {
          parent.rotateR();
        }
      }

      s = sibling();

      assert !s.isLeaf();
      if ((!parent.isRed())
          && (!s.isRed())
          && !s.left.isRed()
          && !s.right.isRed()) {
        s.setRed(true);
        parent.rebalanceAfterRemoval();
        return;
      }

      if ((parent.isRed())
          && (!s.isRed())
          && !s.left.isRed()
          && !s.right.isRed()) {
        s.setRed(true);
        parent.setRed(false);
        return;
      }

      if (isLeftChild()) {
        if (!s.isRed()
            && s.left.isRed()
            && !s.right.isRed()) {
          s.setRed(true);
          s.left.setRed(false);
          s.rotateR();

          s = sibling();
        }

        s.setRed(parent.isRed());
        parent.setRed(false);
        s.right.setRed(false);
        parent.rotateL();
      } else {
        if (!s.isRed()
            && !s.left.isRed()
            && s.right.isRed()) {
          s.setRed(true);
          s.right.setRed(false);
          s.rotateL();

          s = sibling();
        }

        s.setRed(parent.isRed());
        parent.setRed(false);
        s.left.setRed(false);
        parent.rotateR();
      }
    }

    /**
     * Throws an exception if the parent-child pointers or subtreeLength entries
     * are inconsistent in the subtree rooted at this node.
     */
    void checkTreeStructure() {
      if (isLeaf()) {
        // nothing to do
      } else {
        checkState(left.parent == this);
        checkState(right.parent == this);
        left.checkTreeStructure();
        right.checkTreeStructure();
        if (this != sentinel) {
          checkState(subtreeLength == left.subtreeLength + right.subtreeLength,
              "subtree lengths inconsistent", this);
        }
      }
    }

    /**
     * Throws an exception if the subtree rooted at this node is not balanced.
     */
    int checkBalancingAndReturnBlackHeight() {
      if (isLeaf()) {
        assert !isRed();
        return 0;
      } else {
        if (isRed()) {
          checkState(!left.isRed(), "left child of red node is red", this);
          checkState(!right.isRed(), "right child of red node is red", this);
        }
        int leftHeight = left.checkBalancingAndReturnBlackHeight();
        int rightHeight = right.checkBalancingAndReturnBlackHeight();
        checkState(leftHeight == rightHeight, "black height mismatch at " + this + ": "
            + leftHeight + " left, " + rightHeight + " right", this);
        return isRed() ? leftHeight : (leftHeight + 1);
      }
    }

    /**
     * Checks the following invariants in the subtree rooted at this node and
     * throws an exception if any of them is violated:
     *
     * - no leaf may have a subtreeLength of zero
     *
     * - siblings must not have the same annotation (it must be propagated
     * to the parent)
     *
     * - two intervals that are siblings must not have identical annotations
     * (they must be merged)
     *
     * - an internal node must not have a child with zero subtreeLength
     */
    void checkPropagationAndMerging() {
      if (isLeaf()) {
        checkState(subtreeLength > 0, "empty Node", this);
      } else {
        left.checkPropagationAndMerging();
        right.checkPropagationAndMerging();
        left.localMap.each(new StringMap.ProcV<V>() {
              @Override
                  public void apply(String key, V valueLeft) {
                if (right.localMap.containsKey(key)) {
                  V valueRight = right.localMap.getExisting(key);
                  if (ValueUtils.equal(valueLeft, valueRight)) {
                    checkState(false, "left and right have equal annotations "
                        + key + "=" + valueLeft, Node.this);
                  }
                }
              }
            });
        if (left.localMap.isEmpty() && right.localMap.isEmpty()) {
          checkState(!(left.isLeaf() && right.isLeaf()),
              "two leaves not merged", this);
          checkState(!(left.subtreeLength == 0), "left is empty", this);
          checkState(!(left.subtreeLength == 0), "right is empty", this);
        }
      }
    }

    /**
     * Throws an exception if any node in the subtree rooted at this node
     * has an annotation that is overridden anywhere further up in the tree.
     */
    void checkNoStaleKeys() {
      if (!isRoot()) {
        localMap.each(new StringMap.ProcV<V>() {
              @Override
                  public void apply(String key, V value) {
                // The key must not be set in any ancestor.
                for (Node ancestor = Node.this; ancestor != root();
                     ancestor = ancestor.parent) {
                  checkState(!ancestor.parent.localMap.containsKey(key), "stale key " + key,
                      Node.this);
                }
              }
            });
      }
      if (!isLeaf()) {
        left.checkNoStaleKeys();
        right.checkNoStaleKeys();
      }
    }

    /**
     * Throws an exception if any index in the range covered by the subtree
     * rooted at this node has no annotation for the given key.
     */
    void checkKeyCoverage(String key) {
      if (isLeaf()) {
        checkState(localMap.containsKey(key), "key " + key + " has no value", this);
      } else {
        if (localMap.containsKey(key)) {
          return;
        }
        left.checkKeyCoverage(key);
        right.checkKeyCoverage(key);
      }
    }

    // return value of -1 means continue
    // return value of >0 means restart from top at that absolute position
    // return value of 0 is invalid
    int setAnnotationForLeaf(int absoluteNodeStart, int start, int end,
        String key, V value) {
      assert isLeaf();
      start = Math.max(start, 0);
      end = Math.min(end, subtreeLength);
      if (start >= end) {
        return -1;
      }

      assert localMap.containsKey(key);
      if (ValueUtils.equal(localMap.getExisting(key), value)) {
        return -1;
      }

      // Entire node?
      if (start == 0 && end == subtreeLength) {
        eraseAnnotations(absoluteNodeStart, key);
        localMap.put(key, value);
        if (parent.tryToPropagateFromChildren(key)) {
          return absoluteNodeStart + end;
        } else {
          return -1;
        }
      }

      // Left part?
      if (start == 0) {
        Node newParent = splitNode(end);
        newParent.pushKeyIntoChildren(key);
        newParent.left.setAnnotationForLeaf(absoluteNodeStart, start, end, key, value);
        newParent.tryToPropagateFromChildren(key);
        newParent.rebalanceAfterInsertion();
        return absoluteNodeStart + end;
      }
      // Right part?
      if (end == subtreeLength) {
        int split = start;
        Node newParent = splitNode(split);
        newParent.pushKeyIntoChildren(key);
        newParent.right.setAnnotationForLeaf(absoluteNodeStart + split,
            start - split, end - split, key, value);
        newParent.tryToPropagateFromChildren(key);
        newParent.rebalanceAfterInsertion();
        return absoluteNodeStart + end;
      }

      // Somewhere in the middle.
      V previousValue = localMap.getExisting(key);
      int indexOnRight = absoluteNodeStart + subtreeLength;
      V valueOnRight = indexOnRight == root().subtreeLength ?
          // null is acceptable here because we merely need a value that
          // will inhibit upwards propagation of this key from the temporary
          // right interval and the interval to the right of it.  If we
          // are looking at the rightmost interval, there is no interval
          // to the right of it that would be a candidate for upwards propagation.
          null : getAnnotationRaw(indexOnRight, key);
      int split1 = start;
      int split2 = end;

      // newLeft will retain previous value, temporaryRight will be split in
      // two nodes: newMiddle which contains the new value, newRight which contains
      // the previous value.
      Node newParent1 = splitNode(start);
      newParent1.pushKeyIntoChildren(key);
      Node newLeft = newParent1.left;
      Node temporaryRight = newParent1.right;

      temporaryRight.localMap.put(key, differentValue(previousValue, valueOnRight));
      // assert that tryToPropagateFromChildren would do nothing
      //newParent1.checkPropagationAndMerging();
      newParent1.rebalanceAfterInsertion();
      temporaryRight.localMap.put(key, previousValue);

      Node newParent2 = temporaryRight.splitNode(split2 - split1);
      newParent2.pushKeyIntoChildren(key);
      Node newMiddle = newParent2.left;
      Node newRight = newParent2.right;
      assert start - split1 == 0;
      assert end - split2 == 0;
      assert end - split1 == newMiddle.subtreeLength;
      // Even though ranges covered by internal nodes may have shifted during
      // rotation, each leaf will still represent the same range, so we can
      // still use start, split and end with the same semantics.
      newMiddle.setAnnotationForLeaf(absoluteNodeStart + split1, start - split1, end - split1,
          key, value);
      newParent2.tryToPropagateFromChildren(key);
      newParent2.rebalanceAfterInsertion();
      return absoluteNodeStart + end;
    }

    // Replaces this node with a new internal node with two children.
    // This node will be disconnected from the tree to be discarded.
    Node splitNode(int splitIndex) {
      assert isLeaf();
      Node newLeft = newLeaf(splitIndex);
      Node newRight = newLeaf(subtreeLength - splitIndex);
      Node newParent = newInternalNode(subtreeLength, localMap);
      newLeft.parent = newParent;
      newRight.parent = newParent;
      newParent.left = newLeft;
      newParent.right = newRight;
      newParent.setRed(true);
      replaceThisNodeWith(newParent);
      // Cannot rebalance here; caller needs a chance to operate on children
      // in the state that it expects.
      return newParent;
    }

    void pushKeyIntoChildren(String key) {
      assert !isLeaf();
      assert localMap.containsKey(key);
      V value = localMap.getExisting(key);
      left.localMap.put(key, value);
      right.localMap.put(key, value);
      localMap.remove(key);
    }

    Node grandparent() {
      if (parent == sentinel || parent.parent == sentinel) {
        return null;
      }
      return parent.parent;
    }

    Node uncle() {
      if (parent == sentinel || parent.parent == sentinel) {
        return null;
      }
      if (parent.isLeftChild()) {
        return parent.parent.right;
      } else {
        return parent.parent.left;
      }
    }

    void rebalanceAfterInsertion() {
      assert !isLeaf();
      if (parent == sentinel) {
        setRed(false);
        return;
      }
      if (!parent.isRed()) {
        return;
      }
      if (parent.parent == sentinel) {
        parent.setRed(false);
        return;
      }
      Node g = parent.parent;
      assert !g.isRed();
      {
        Node u = parent.sibling();
        if (g != sentinel && u.isRed()) {
          parent.setRed(false);
          u.setRed(false);
          g.setRed(true);
          g.rebalanceAfterInsertion();
          return;
        }
      }
      if (!isLeftChild() && parent.isLeftChild()) {
        Node n = parent;
        parent.rotateL();
        g = this.parent;
        this.setRed(false);
        g.setRed(true);
        assert n.isLeftChild() && this.isLeftChild();
        g.rotateR();
        return;
      } else if (isLeftChild() && !parent.isLeftChild()) {
        Node n = parent;
        parent.rotateR();
        Node p = n.parent;
        g = this.parent;
        this.setRed(false);
        g.setRed(true);
        assert !n.isLeftChild() && !p.isLeftChild();
        g.rotateL();
        return;
      }
      parent.setRed(false);
      g.setRed(true);
      if (isLeftChild() && parent.isLeftChild()) {
        g.rotateR();
        return;
      } else {
        assert !isLeftChild();
        assert !parent.isLeftChild();
        g.rotateL();
        return;
      }
    }


    void rotateL() {
      assert !isLeaf();
      //     p            p
      //     |            |
      //     a            c
      //    / \    ->    / \
      //  (b)  c        a  (e)
      //      / \      / \
      //     d  (e)  (b)  d
      Node a = this;
      Node p = a.parent;
      Node b = a.left;
      // We would have no reason to rotate if c was not an internal node.
      assert !a.right.isLeaf();
      Node c = a.right;
      Node d = c.left;
      Node e = c.right;
      prepareMapsBeforeSingleRotation(a, b, c, d, e);
      c.parent = null;
      a.replaceThisNodeWith(c);
      a.right = d;
      c.left = a;
      a.parent = c;
      c.parent = p;
      d.parent = a;
      a.subtreeLength -= e.subtreeLength;
      c.subtreeLength += b.subtreeLength;
      fixupMapsAfterSingleRotation(a, b, c, d, e);
      a.tryToMergeChildren();
    }

    void rotateR() {
      assert !isLeaf();
      //      p          p
      //      |          |
      //      a          c
      //     / \   ->   / \
      //    c  (b)    (e)  a
      //   / \            / \
      // (e)  d          d  (b)
      Node a = this;
      Node p = a.parent;
      Node b = a.right;
      // We would have no reason to rotate if c was not an internal node.
      assert !a.left.isLeaf();
      Node c = a.left;
      Node d = c.right;
      Node e = c.left;
      prepareMapsBeforeSingleRotation(a, b, c, d, e);
      c.parent = null;
      a.replaceThisNodeWith(c);
      a.left = d;
      c.right = a;
      a.parent = c;
      c.parent = p;
      d.parent = a;
      a.subtreeLength -= e.subtreeLength;
      c.subtreeLength += b.subtreeLength;
      fixupMapsAfterSingleRotation(a, b, c, d, e);
      a.tryToMergeChildren();
    }

    // Rotate left child to the left, then rotate self to the right.
    void rotateLR() {
      left.rotateL();
      this.rotateR();
    }

    // Rotate right child to the right, then rotate self to the left.
    void rotateRL() {
      right.rotateR();
      this.rotateL();
    }

    void tryToMergeChildren() {
      assert !isLeaf();
      if (left.localMap.isEmpty() && right.localMap.isEmpty()
          && left.isLeaf() && right.isLeaf()) {
        int rightLength = right.subtreeLength;
        if (rightLength == 0) {
          return;
        }
        if (left.subtreeLength == 0) {
          return;
        }

        left.subtreeLength += rightLength;
        right.subtreeLength = 0;
        leavesThatHaveBecomeEmpty.add(right);
      }
    }

    boolean tryToPropagateFromChildren(String key) {
      assert !isLeaf();
      if (left.localMap.containsKey(key) && right.localMap.containsKey(key)) {
        V valueLeft = left.localMap.getExisting(key);
        if (ValueUtils.equal(valueLeft, right.localMap.getExisting(key))) {
          localMap.put(key, valueLeft);
          left.localMap.remove(key);
          right.localMap.remove(key);
          tryToMergeChildren();
          parent.tryToPropagateFromChildren(key);
          return true;
        }
      }
      return false;
    }

    void tryToPropagateFromChildren(ReadableStringMap<V> entries) {
      assert !isLeaf();
      entries.each(new StringMap.ProcV<V>() {
            @Override
                public void apply(String key, V value) {
              tryToPropagateFromChildren(key);
            }
          });
    }

    void replaceNodeWithSoleChild(Node child) {
      assert !isLeaf();
      assert this.left == child || this.right == child;
      assert child.parent == this;
      assert (child.isLeftChild() && this.right.subtreeLength == 0)
          || (!child.isLeftChild() && this.left.subtreeLength == 0);
      // Disconnect other child just to be clean.
      if (child.isLeftChild()) {
        right.parent = null;
      } else {
        left.parent = null;
      }
      child.parent = null;
      replaceThisNodeWith(child);
      assert child.parent != null;

      child.parent.tryToMergeChildren();

      if (isRed()) {
        // This node was red; black height hasn't changed, nothing to do.
        return;
      }
      if (child.isRed()) {
        child.setRed(false);
        return;
      }
      child.rebalanceAfterRemoval();

      child.parent.tryToMergeChildren();
    }

    void tryToCollapse() {
      assert !isLeaf();
      if (subtreeLength == 0) {
        Node originalParent = this.parent;
        replaceNodeWithSoleChild(right);
        // assert: all leaves in right subtree are in leavesThatHaveBecomeEmpty
        return;
      }
      if (left.subtreeLength == 0) {
        right.localMap.putAll(localMap);
        Node originalParent = this.parent;
        replaceNodeWithSoleChild(right);
        return;
      }
      if (right.subtreeLength == 0) {
        left.localMap.putAll(localMap);
        Node originalParent = this.parent;
        replaceNodeWithSoleChild(left);
        return;
      }
    }

    void printForDebugging(StringBuilder out) {
      if (isLeaf()) {
        out.append(toString() + "\n");
      } else {
        if (right == null) {
          out.append("null right?!\n");
        } else {
          right.printForDebugging(out);
        }
        out.append(toString() + "\n");
        if (left == null) {
          out.append("null left?!\n");
        } else {
          left.printForDebugging(out);
        }
      }
    }
  }

  private void prepareMapsBeforeSingleRotation(Node a, Node b, Node c,
      Node d, Node e) {
    // do nothing
  }

  /**
   * A' = (D union C) intersect B
   * B' = B minus (D union C)
   * C' = A
   * D' = (D union C) minus B
   * E' = E union C
   */
  private void fixupMapsAfterSingleRotation(Node a, Node b, Node c, Node d, Node e) {
    // e.g.
    //      p          p
    //      |          |
    //      a          c
    //     / \   ->   / \
    //    c  (b)    (e)  a
    //   / \            / \
    // (e)  d          d  (b)

    // within each of the following sets, the sets will be disjoint:
    // {A, B}, {A, C, D}, {A, C, E}
    //
    // C' = A
    // A' = (D union C) intersect B
    // B' = B minus (D union C)
    // D' = (D union C) minus B
    // E' = E union C
    StringMap<V> a0 = a.localMap;
    final StringMap<V> b0 = b.localMap;
    StringMap<V> c0 = c.localMap;
    final StringMap<V> d0 = d.localMap;
    final StringMap<V> e0 = e.localMap;
    // Strategy:
    // create new set for a
    // iterate over d
    //   if in b with same value,
    //     add to a
    //     remove from b
    //     remove from d
    // now we have: a = d intersect b, d = d minus b, b = b minus d
    // iterate over c
    //   if in b with same value,
    //     remove from b
    //     add to a
    //   else
    //     add to d
    //   add to e
    // now we have: a = (d intersect b) union (c intersect (b minus d));
    // d = (d minus b) union (c minus (b minus d)); e = e union c, b = (b minus d) minus c
    final StringMap<V> a1 = factory.createStringMap();
    d0.filter(new StringMap.EntryFilter<V>() {
          @Override
              public boolean apply(String key, V value) {
            if (b0.containsKey(key) && ValueUtils.equal(b0.getExisting(key), value)) {
              a1.put(key, value);
              b0.remove(key);
              return false;
            }
            return true;
          }
        });
    c0.each(new StringMap.ProcV<V>() {
          @Override
              public void apply(String key, V value) {
            if (b0.containsKey(key) && ValueUtils.equal(b0.getExisting(key), value)) {
              b0.remove(key);
              a1.put(key, value);
            } else {
              d0.put(key, value);
            }
            e0.put(key, value);
          }
        });
    a.localMap = a1;
    c.localMap = a0;
  }

  private Node root() {
    return sentinel.left;
  }

  // TODO(ohler): oneValue and anotherValue are an ugly hack.  Perhaps find a way
  // to fix the setAnnotation algorithm to not rely on them, or perhaps remove V and
  // always store Object so we can generate our own sentinels.
  public BasicAnnotationTree(V oneValue, V anotherValue) {
    this.knownKeys = factory.createStringSet();
    this.oneValue = oneValue;
    this.anotherValue = anotherValue;
    clear();
  }

  private void clear() {
    // One sentinel item at the start (insertion inherits from the left, so we
    // need something on our left to inherit from).
    Node root = newLeaf(1);
    sentinel = newInternalNode(-1);
    sentinel.left = root;
    root.parent = sentinel;
    sentinel.right = newLeaf(0);
    sentinel.right.parent = sentinel;
    knownKeys.clear();
  }

  public int length() {
    return root().subtreeLength - 1;
  }

  /**
   * Returns a read-only view of the known key set of this annotation
   * tree.
   */
  // This is a view because AnnotationTree.knownKeysLive() relies on
  // that.
  public ReadableStringSet knownKeys() {
    return knownKeys;
  }

  private V getAnnotationRaw(int index, String key) {
    Node node = root();

    while (true) {
      if (node.localMap.containsKey(key)) {
        return node.localMap.getExisting(key);
      }
      assert !node.isLeaf();
      int leftLength = node.left.subtreeLength;
      if (index < leftLength) {
        node = node.left;
      } else {
        index -= leftLength;
        node = node.right;
      }
    }
  }

  public V getAnnotation(int index, String key) {
    assert 0 <= index;
    assert index < length();
    if (!knownKeys.contains(key)) {
      return null;
    }
    return getAnnotationRaw(index + 1, key);
  }

  private void forEachAnnotationAtRaw(int index, ReadableStringMap.ProcV<V> callback) {
    Node node = root();
    int nodeStart = 0;
    while (true) {
      node.localMap.each(callback);
      if (node.isLeaf()) {
        break;
      }
      int leftLength = node.left.subtreeLength;
      if (index < nodeStart + leftLength) {
        node = node.left;
      } else {
        node = node.right;
        nodeStart += leftLength;
      }
    }
  }

  public void forEachAnnotationAt(int index, ReadableStringMap.ProcV<V> callback) {
    forEachAnnotationAtRaw(index + 1, callback);
  }

  private void collectAllAnnotationsAtRaw(int index, StringMap<V> accu) {
    Node node = root();
    int nodeStart = 0;
    while (true) {
      accu.putAll(node.localMap);
      if (node.isLeaf()) {
        break;
      }
      int leftLength = node.left.subtreeLength;
      if (index < nodeStart + leftLength) {
        node = node.left;
      } else {
        node = node.right;
        nodeStart += leftLength;
      }
    }
  }

  public void collectAllAnnotationsAt(int index, StringMap<V> accu) {
    collectAllAnnotationsAtRaw(index + 1, accu);
  }

  private void insertRaw(int firstShiftedIndex, int length) {
    Node node = root();
    int nodeStart = 0;
    while (true) {
      node.subtreeLength += length;
      if (node.isLeaf()) {
        return;
      }
      if (firstShiftedIndex <= nodeStart + node.left.subtreeLength) {
        node = node.left;
      } else {
        nodeStart += node.left.subtreeLength;
        node = node.right;
      }
    }
  }

  private void deleteRaw(int start, int end) {
    Node node = root();
    int nodeStart = 0;

    outer:
    while (true) {
      if (end <= nodeStart) {
        return;
      }
      int nodeEnd = nodeStart + node.subtreeLength;

      assert start < nodeEnd;
      int deletionStart = Math.max(nodeStart, start);
      int deletionEnd = Math.min(nodeEnd, end);
      int deletionLength = deletionEnd - deletionStart;
      assert deletionLength > 0;

      node.subtreeLength -= deletionLength;

      if (node.isLeaf()) {
        end -= deletionLength;
        if (node.subtreeLength == 0) {
          leavesThatHaveBecomeEmpty.add(node);
        }
      } else {
        if (start < nodeStart + node.left.subtreeLength) {
          node = node.left;
        } else {
          nodeStart += node.left.subtreeLength;
          node = node.right;
        }
        continue outer;
      }

      // next node
      while (true) {
        if (node.isLeftChild()) {
          nodeStart += node.subtreeLength;
          node = node.parent.right;
          continue outer;
        }
        nodeStart -= node.parent.left.subtreeLength;
        node = node.parent;
        if (node.isRoot()) {
          return;
        }
      }
    }
  }

  // TODO(ohler): Eliminate restarting and implement eraseAnnotationsRaw.
  private int setAnnotationRaw(int start, int end, String key, V value) {
    Node node = root();
    int nodeStart = 0;

    outer:
    while (true) {
      if (end <= nodeStart) {
        return -1;
      }
      if (!(node.localMap.containsKey(key)
              && ValueUtils.equal(node.localMap.getExisting(key), value))) {
        int nodeEnd = nodeStart + node.subtreeLength;

        // entire node?
        if (start <= nodeStart && end >= nodeEnd) {
          node.eraseAnnotations(nodeStart, key);
          node.localMap.put(key, value);
          if (node.parent.tryToPropagateFromChildren(key)) {
            return nodeStart + node.subtreeLength;
          } else {
            // go to next node;
          }
        } else {
          // partial node
          if (node.isLeaf()) {
            int leafResult = node.setAnnotationForLeaf(nodeStart, start - nodeStart,
                end - nodeStart, key, value);
            if (leafResult == -1) {
              // go to next node
            } else {
              assert leafResult > 0;
              return leafResult;
            }
          } else {
            if (node.localMap.containsKey(key)) {
              node.pushKeyIntoChildren(key);
            }

            if (start < nodeStart + node.left.subtreeLength) {
              node = node.left;
            } else {
              nodeStart += node.left.subtreeLength;
              node = node.right;
            }
            continue outer;
          }
        }
      }

      // next node
      while (true) {
        if (node.isLeftChild()) {
          nodeStart += node.subtreeLength;
          node = node.parent.right;
          continue outer;
        }
        nodeStart -= node.parent.left.subtreeLength; // law of demeter
        node = node.parent;
        if (node.isRoot()) {
          return -1;
        }
      }
    }
  }

  public void setAnnotation(int start, int end, String key, V value) {
    assert 0 <= start;
    assert start <= end;
    assert end <= length();
    if (start >= end) {
      return;
    }
    if (!knownKeys.contains(key)) {
      root().localMap.put(key, null);
      knownKeys.add(key);
    }

    int currentStart = start + 1;
    int end1 = end + 1;
    do {
      currentStart = setAnnotationRaw(currentStart, end1, key, value);
    } while (currentStart != -1);

    cleanupLeavesThatHaveBecomeEmpty();
    // TODO(ohler): change the adapter that implements the streaming
    // interface to not call setAnnotation while iterating over the
    // known key set.  Then we can reenable this.  For now, we have
    // a separate, less efficient method cleanupKnownKeys() below
    // that the adapter invokes explicitly when it's safe.
    if (false) {
      if (value == null
          && root().localMap.containsKey(key)
          && root().localMap.getExisting(key) == null) {
        // Make sure the data structure does not grow without bound even for an
        // unbounded key set as long as only a bounded number of keys is in use
        // at any time.
        root().localMap.remove(key);
        knownKeys.remove(key);
      }
    }
  }

  public void cleanupKnownKeys() {
    knownKeys.filter(new StringSet.StringPredicate() {
        @Override
        public boolean apply(String key) {
          if (root().localMap.containsKey(key)
              && root().localMap.getExisting(key) == null) {
            root().localMap.remove(key);
            return false;
          } else {
            return true;
          }
        }
      });
  }

  // TODO(ohler): Verify this code.  It is the bottleneck for these queries.
  // Perhaps it's not pruning as much as it should?
  private int firstAnnotationChangeRaw(int start, int end, String key, V fromValue) {
    Node node = root();
    int nodeStart = 0;

    outer:
    while (true) {
      if (nodeStart >= end) {
        return -1;
      }
      if (node.localMap.containsKey(key)) {
        V valueHere = node.localMap.getExisting(key);
        if (!ValueUtils.equal(valueHere, fromValue)) {
          return Math.max(nodeStart, start);
        }
        if (node.isRoot()) {
          return -1;
        }
        // InternalAnnotationsCursor seems to have similar but simpler code.
        while (true) {
          if (node.isLeftChild()) {
            nodeStart += node.subtreeLength;
            node = node.parent.right;
            continue outer;
          }
          nodeStart -= node.parent.left.subtreeLength;
          node = node.parent;
          if (node.isRoot()) {
            return -1;
          }
        }
      }
      assert !node.isLeaf();
      Node leftNode = node.left;
      if (leftNode == null) {
        // Log some additional information in the hope that this
        // will help track down bug 1816163.
        throw new NullPointerException("Unexpected null leftNode:\n"
            + toStringForDebugging());
      }
      if (start >= nodeStart + leftNode.subtreeLength) {
        nodeStart += leftNode.subtreeLength;
        node = node.right;
      } else {
        node = node.left;
      }
    }
  }

  private int lastAnnotationChangeRaw(int start, int end, String key, V fromValue) {
    Node node = root();
    int nodeEnd = node.subtreeLength;

    outer:
    while (true) {
      if (nodeEnd <= start) {
        return -1;
      }
      if (node.localMap.containsKey(key)) {
        V valueHere = node.localMap.getExisting(key);
        if (!ValueUtils.equal(valueHere, fromValue)) {
          return Math.min(nodeEnd, end);
        }
        if (node.isRoot()) {
          return -1;
        }
        while (true) {
          if (!node.isLeftChild()) {
            nodeEnd -= node.subtreeLength;
            node = node.parent.left;
            continue outer;
          }
          nodeEnd += node.parent.right.subtreeLength;
          node = node.parent;
          if (node.isRoot()) {
            return -1;
          }
        }
      }
      assert !node.isLeaf();
      Node rightNode = node.right;
      if (end <= nodeEnd - rightNode.subtreeLength) {
        nodeEnd -= rightNode.subtreeLength;
        node = node.left;
      } else {
        node = node.right;
      }
    }
  }

  public int firstAnnotationChange(int start, int end, String key, V fromValue) {
    assert 0 <= start;
    assert start <= end;
    assert end <= length();
    if (start >= end) {
      return -1;
    }
    if (!knownKeys.contains(key)) {
      if (fromValue == null) {
        return -1;
      } else {
        return start;
      }
    }
    int pos = firstAnnotationChangeRaw(start + 1, end + 1, key, fromValue);
    if (pos == -1) {
      return -1;
    }
    assert pos != 0;
    return pos - 1;
  }

  public int lastAnnotationChange(int start, int end, String key, V fromValue) {
    assert 0 <= start;
    assert start <= end;
    assert end <= length();
    if (start >= end) {
      return -1;
    }
    if (!knownKeys.contains(key)) {
      if (fromValue == null) {
        return -1;
      } else {
        return end;
      }
    }
    int pos = lastAnnotationChangeRaw(start + 1, end + 1, key, fromValue);
    if (pos == -1) {
      return -1;
    }
    assert pos != 0;
    return pos - 1;
  }

  private void tryToPropagateFromDyingSubtree(Node child) {
    Node parent = child.parent;
    while (parent.subtreeLength == 0) {
      child = parent;
      parent = child.parent;
    }

    Node sibling = child.sibling();

    ReadableStringMap<V> entries = sibling.localMap;
    parent.localMap.putAll(entries);
    parent.parent.tryToPropagateFromChildren(entries);
    // There is some inefficiency here.  We shouldn't have to clear any
    // map, one node is going to die anyway.
    sibling.localMap.clear();
  }

  // If this cleanup turns out to be expensive, we should be able to
  // defer the work and do it incrementally fairly easily.  Relaxing
  // the invariants accordingly (allowing empty leaves) should be no
  // problem.  We may even be able to recycle the empty leaves in some
  // cases.
  private void cleanupLeavesThatHaveBecomeEmpty() {
    while (!leavesThatHaveBecomeEmpty.isEmpty()) {
      List<Node> mine = leavesThatHaveBecomeEmpty;
      leavesThatHaveBecomeEmpty = new ArrayList<Node>();
      for (Node leaf : mine) {
        assert leaf.subtreeLength == 0;
        assert !leaf.isRoot();
        tryToPropagateFromDyingSubtree(leaf);
        Node originalParent = leaf.parent;
        originalParent.tryToCollapse();
      }
    }
  }

  public void delete(int start, int end) {
    assert 0 <= start;
    assert start <= end;
    assert end <= length();

    if (start >= end) {
      return;
    }

    deleteRaw(start + 1, end + 1);
    cleanupLeavesThatHaveBecomeEmpty();
  }

  public void insert(int firstShiftedIndex, int length) {
    assert firstShiftedIndex <= length();
    insertRaw(firstShiftedIndex + 1, length);
  }

  public String toStringForDebugging() {
    StringBuilder out = new StringBuilder();
    out.append("AnnotationTree, length " + length() + ", sentinel=" + sentinel + ":\n");
    root().printForDebugging(out);
    // Remove trailing newline so that it can be used nicely with
    // println() and does not lead to a redundant empty line when
    // used as an exception message.
    if (out.charAt(out.length() - 1) == '\n') {
      out.setLength(out.length() - 1);
    }
    return out.toString();
  }

  private V differentValue(V notThis, V notThat) {
    if (!oneValue.equals(notThis) && !oneValue.equals(notThat)) {
      return oneValue;
    }
    if (!anotherValue.equals(notThis) && !anotherValue.equals(notThat)) {
      return anotherValue;
    }
    assert notThis != null && notThat != null;
    return null;
  }

  private void checkState(boolean condition, String description, Node node) {
    if (!condition) {
      String message = "Tree invariant check failed at node " + node + ": " + description
          + "\n" + toStringForDebugging();
      System.err.println("*** " + message);
      throw new RuntimeException(message);
    }
  }

  private void checkState(boolean condition) {
    if (!condition) {
      String message = "Tree invariant check failed\n" + toStringForDebugging();
      System.err.println("*** " + message);
      throw new RuntimeException(message);
    }
  }

  public void checkSomeInvariants() {
    checkState(leavesThatHaveBecomeEmpty.isEmpty());
    checkSentinels();
    checkTreeStructure();
    checkBalancing();
    checkPropagationAndMerging();
    checkStaleness();
    checkKnownKeysSetEverywhere();
  }

  private void checkTreeStructure() {
    checkState(root().parent == sentinel);
    root().checkTreeStructure();
  }

  private void checkBalancing() {
    checkState(!root().isRed());
    root().checkBalancingAndReturnBlackHeight();
  }

  private void checkPropagationAndMerging() {
    root().checkPropagationAndMerging();
  }

  private void checkStaleness() {
    root().checkNoStaleKeys();
  }

  private void checkSentinels() {
    checkState(!sentinel.isRed());
    checkState(sentinel.right.isLeaf());
    checkState(sentinel.right.subtreeLength == 0);
    checkState(sentinel.localMap.isEmpty());
    checkState(sentinel.subtreeLength == -1);
    // Check that position 0 exists and has only annotations with the value null.
    checkState(root().subtreeLength >= 1);
    knownKeys.each(new StringSet.Proc() {
        @Override
        public void apply(String key) {
          checkState(ValueUtils.equal(getAnnotationRaw(0, key), null));
        }
      });
  }

  private void checkKnownKeysSetEverywhere() {
    knownKeys.each(new StringSet.Proc() {
        @Override
        public void apply(String key) {
          root().checkKeyCoverage(key);
        }
      });
  }

  private String mapToString(StringMap<V> map) {
    final StringBuilder buf = new StringBuilder("{");
    final boolean first[] = new boolean[] { true };
    map.each(new StringMap.ProcV<V>() {
      @Override
      public void apply(String key, V value) {
        if (first[0]) {
          first[0] = false;
        } else {
          buf.append(", ");
        }
        buf.append(key + "=" + value);
      }
    });
    buf.append("}");
    return buf.toString();
  }

}
