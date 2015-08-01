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

package org.waveprotocol.wave.model.util;

import java.util.Iterator;

/**
 * An offset list that has the capability of evaluating to a value.
 *
 * TODO(user): Consider reducing recomputation during evaluation to the
 * minimum possible.
 *
 *
 * @param <T> The type of data contained in the data structure.
 * @param <V> The result of evaluating this data structure.
 */
public final class EvaluableOffsetList<T, V> implements OffsetList<T> {

  /**
   * An associative operator.
   *
   * @param <T> The type of the base data.
   * @param <V> The type of data handled by the associative operator.
   */
  public interface AssociativeOperator<T, V> {

    /**
     * Extracts a <code>V</code> from a <code>T</code>.
     *
     * @param value The <code>T</code> from which the <code>V</code> is to be
     *        extracted.
     * @return The extracted value.
     */
    V extract(T value);

    /**
     * Applies the operator to the two given operands.
     *
     * @param operand1 The first operand.
     * @param operand2 The second operand.
     * @return The result of applying the operator on the two given operands.
     */
    V operate(V operand1, V operand2);

  }

  /**
   * A container for holding data.
   *
   * @param <T> The type of data contained in the container.
   */
  public interface Container<T> extends OffsetList.Container<T> {

    @Override
    Container<T> getPreviousContainer();

    @Override
    Container<T> getNextContainer();

    @Override
    Container<T> insertBefore(T newValue, int valueSize);

    @Override
    Container<T> split(int offset, T newValue);

    /**
     * Invalidate the cached evaluation of this container, to indicate that the
     * evaluation of this container should be recomputed the next time its
     * offset list is evaluated.
     */
    void invalidate();

  }

  /**
   * A container, implemented as a node in a tree.
   *
   * @param <T> The type of data contained in the container.
   */
  private static final class Node<T, V> implements Container<T> {

    /**
     * The value contained in this container.
     */
    private T value;

    /**
     * The cached evaluation of this node.
     */
    private V subtreeComputation;

    /**
     * The parent of this node
     */
    private Node<T, V> parent;

    /**
     * The left child of this node.
     */
    private Node<T, V> leftChild;

    /**
     * The right child of this node.
     */
    private Node<T, V> rightChild;

    /**
     * The container before this container.
     */
    private Node<T, V> previousContainer;

    /**
     * The container after this container.
     */
    private Node<T, V> nextContainer;

    /**
     * The offset of this node in its subtree.
     */
    private int offset;

    /**
     * The size of this node.
     */
    private int size;

    /**
     * The height of this node.
     */
    private int height;

    private Node(T value, int size) {
      assert size >= 0;
      this.value = value;
      this.size = size;
    }

    @Override
    public Container<T> getPreviousContainer() {
      return previousContainer;
    }

    @Override
    public Container<T> getNextContainer() {
      return nextContainer;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public void setValue(T value) {
      this.value = value;
    }

    @Override
    public int offset() {
      int totalOffset = offset;
      for (Node<T, V> currentNode = this, currentParent = parent; currentParent != null;
          currentParent = currentNode.parent) {
        if (currentParent.rightChild == currentNode) {
          totalOffset += currentParent.offset + currentParent.size;
        }
        currentNode = currentParent;
      }
      return totalOffset;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public Container<T> insertBefore(T newValue, int valueSize) {
      Node<T, V> newContainer = new Node<T, V>(newValue, valueSize);
      newContainer.previousContainer = previousContainer;
      newContainer.nextContainer = this;
      previousContainer.nextContainer = newContainer;
      previousContainer = newContainer;
      if (leftChild == null) {
        setLeftChild(newContainer);
      } else {
        leftChild.getLastInSubtree().setRightChild(newContainer);
      }
      offset += newContainer.size;
      correctOffsets(newContainer.size);
      newContainer.parent.rebalance();
      return newContainer;
    }

    @Override
    public void remove() {
      // Assert that the node to remove is not the sentinel node.
      assert parent != null;
      nextContainer.previousContainer = previousContainer;
      previousContainer.nextContainer = nextContainer;
      correctOffsets(-size);
      if (rightChild == null) {
        replaceWith(leftChild);
        parent.rebalance();
      } else {
        Node<T, V> replacementNode = rightChild.getFirstInSubtree();
        Node<T, V> parentOfReplacement = replacementNode.parent;
        int removalSize = replacementNode.size;
        for (Node<T, V> node = parentOfReplacement; node != this; node = node.parent) {
          node.offset -= removalSize;
        }
        replacementNode.replaceWith(replacementNode.rightChild);
        replaceWith(replacementNode);
        // Ensure that the replacement node assimilates the left child, right
        // child, offset, and height of the node it is replacing.
        replacementNode.setLeftChildSafely(leftChild);
        replacementNode.setRightChildSafely(rightChild);
        replacementNode.offset = offset;
        replacementNode.height = height;
        parent.invalidate();
        ((parentOfReplacement != this) ? parentOfReplacement : replacementNode).rebalance();
      }

      // Help accidental reuse fail quickly
      nextContainer = null;
      previousContainer = null;
      parent = null;
      leftChild = null;
      rightChild = null;
    }

    @Override
    public Container<T> split(int offset, T newValue) {
      assert offset >= 0;
      assert offset <= size;
      int secondSize = size - offset;
      Node<T, V> newContainer = new Node<T, V>(newValue, secondSize);
      newContainer.nextContainer = nextContainer;
      newContainer.previousContainer = this;
      nextContainer.previousContainer = newContainer;
      nextContainer = newContainer;
      size = offset;
      if (rightChild == null) {
        setRightChild(newContainer);
      } else {
        Node<T, V> node = rightChild;
        while (node.leftChild != null) {
          node.offset += secondSize;
          node = node.leftChild;
        }
        node.offset += secondSize;
        node.setLeftChild(newContainer);
      }
      newContainer.parent.rebalance();
      return newContainer;
    }

    @Override
    public void increaseSize(int sizeDelta) {
      assert size >= -sizeDelta;
      size += sizeDelta;
      correctOffsets(sizeDelta);
    }

    @Override
    public void invalidate() {
      if (subtreeComputation != null) {
        subtreeComputation = null;
        if (parent != null) {
          parent.invalidate();
        }
      }
    }

    /**
     * Rebalances the tree along the path from this node up to the root. This
     * will also clear the cached evaluations of all nodes along the path to the
     * root.
     */
    private void rebalance() {
      invalidate();
      for (Node<T, V> node = this; node.parent != null; node = node.parent) {
        int oldHeight = node.height;
        int leftHeight = getHeight(node.leftChild);
        int rightHeight = getHeight(node.rightChild);
        if (rightHeight == leftHeight + 2) {
          if (getHeight(node.rightChild.rightChild) != rightHeight - 1) {
            node.rightChild.rotateRight();
          }
          node = node.rotateLeft();
        } else if (leftHeight == rightHeight + 2) {
          if (getHeight(node.leftChild.leftChild) != leftHeight - 1) {
            node.leftChild.rotateLeft();
          }
          node = node.rotateRight();
        } else {
          node.height = Math.max(leftHeight, rightHeight) + 1;
        }
        if (node.height == oldHeight) {
          break;
        }
      }
    }

    /**
     * Performs a left rotation around this node.
     *
     * @return The new root of the subtree involved in the rotation.
     */
    private Node<T, V> rotateLeft() {
      Node<T, V> newRoot = rightChild;
      Node<T, V> subtree1 = newRoot.leftChild;
      Node<T, V> subtree2 = leftChild;
      replaceWith(newRoot);
      setRightChildSafely(subtree1);
      newRoot.setLeftChild(this);
      height = Math.max(getHeight(subtree1), getHeight(subtree2)) + 1;
      if (height + 1 > newRoot.height) {
        newRoot.height = height + 1;
      }
      newRoot.offset += offset + size;
      subtreeComputation = null;
      newRoot.subtreeComputation = null;
      return newRoot;
    }

    /**
     * Performs a right rotation around this node.
     *
     * @return The new root of the subtree involved in the rotation.
     */
    private Node<T, V> rotateRight() {
      Node<T, V> newRoot = leftChild;
      Node<T, V> subtree1 = newRoot.rightChild;
      Node<T, V> subtree2 = rightChild;
      replaceWith(newRoot);
      setLeftChildSafely(subtree1);
      newRoot.setRightChild(this);
      height = Math.max(getHeight(subtree1), getHeight(subtree2)) + 1;
      if (height + 1 > newRoot.height) {
        newRoot.height = height + 1;
      }
      offset -= newRoot.offset + newRoot.size;
      subtreeComputation = null;
      newRoot.subtreeComputation = null;
      return newRoot;
    }

    /**
     * Alters the offsets along the path from this node to the root.
     *
     * @param sizeDelta The size of the correction.
     */
    private void correctOffsets(int sizeDelta) {
      for (Node<T, V> node = this, parentNode = parent; parentNode != null;
          node = parentNode, parentNode = node.parent) {
        if (parentNode.leftChild == node) {
          parentNode.offset += sizeDelta;
        }
      }
    }

    /**
     * Gets the first node in the subtree rooted at this node.
     *
     * @return The first node in the subtree rooted at this node.
     */
    private Node<T, V> getFirstInSubtree() {
      Node<T, V> node = this;
      for (Node<T, V> child = leftChild; child != null; node = child, child = node.leftChild) {}
      return node;
    }

    /**
     * Gets the last node in the subtree rooted at this node.
     *
     * @return The last node in the subtree rooted at this node.
     */
    private Node<T, V> getLastInSubtree() {
      Node<T, V> node = this;
      for (Node<T, V> child = rightChild; child != null; node = child, child = node.rightChild) {}
      return node;
    }

    /**
     * Sets the left child of this node, ensuring that the parent-child
     * relationship is correctly registered in both the parent and the child.
     * This assumes that the input argument is not null.
     *
     * @param leftChild The node which should become the left child of this
     *        node. This should not be null.
     */
    private void setLeftChild(Node<T, V> leftChild) {
      assert leftChild != null;
      this.leftChild = leftChild;
      leftChild.parent = this;
    }

    /**
     * Sets the right child of this node, ensuring that the parent-child
     * relationship is correctly registered in both the parent and the child.
     * This assumes that the input argument is not null.
     *
     * @param rightChild The node which should become the right child of this
     *        node. This should not be null.
     */
    private void setRightChild(Node<T, V> rightChild) {
      assert rightChild != null;
      this.rightChild = rightChild;
      rightChild.parent = this;
    }

    /**
     * Sets the left child of this node, ensuring that the parent-child
     * relationship is correctly registered in both the parent and the child.
     *
     * @param leftChild The node which should become the right child of this
     *        node. This may be null to indicate that this node should have no
     *        left child.
     */
    private void setLeftChildSafely(Node<T, V> leftChild) {
      this.leftChild = leftChild;
      if (leftChild != null) {
        leftChild.parent = this;
      }
    }

    /**
     * Sets the right child of this node, ensuring that the parent-child
     * relationship is correctly registered in both the parent and the child.
     *
     * @param rightChild The node which should become the right child of this
     *        node. This may be null to indicate that this node should have no
     *        right child.
     */
    private void setRightChildSafely(Node<T, V> rightChild) {
      this.rightChild = rightChild;
      if (rightChild != null) {
        rightChild.parent = this;
      }
    }

    /**
     * Replaces the subtree rooted at this node with another subtree.
     *
     * @param replacement The root of the replacement subtree.
     */
    private void replaceWith(Node<T, V> replacement) {
      assert parent != null;
      if (replacement != null) {
        replacement.parent = parent;
      }
      if (parent.leftChild == this) {
        parent.leftChild = replacement;
      } else {
        parent.rightChild = replacement;
      }
    }

    @Override
    public String toString() {
      return offset() + "," + size() + ":" + getValue();
    }

  }

  private final Node<T, V> root;

  private final AssociativeOperator<? super T, V> operator;

  /**
   * Constructs a new offset list.
   */
  public EvaluableOffsetList() {
    this(null);
  }

  /**
   * Constructs a new offset list.
   */
  public EvaluableOffsetList(AssociativeOperator<? super T, V> operator) {
    root = new Node<T, V>(null, 1);
    root.previousContainer = root;
    root.nextContainer = root;
    this.operator = operator;
  }

  @Override
  public Container<T> firstContainer() {
    return root.nextContainer;
  }

  @Override
  public Container<T> sentinel() {
    return root;
  }

  @Override
  public <R> R performActionAt(int offset, LocationAction<T, R> locationAction) {
    Node<T, V> node = root;
    while (node != null) {
      if (offset < node.offset) {
        node = node.leftChild;
      } else {
        offset -= node.offset;
        if (offset < node.size) {
          return locationAction.performAction(node, offset);
        }
        offset -= node.size;
        node = node.rightChild;
      }
    }
    throw new IndexOutOfBoundsException("Invalid offest: " + offset + ", size: " + size());
  }

  @Override
  public int size() {
    return root.offset;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      private Node<T, V> nextContainer = root.nextContainer;

      public boolean hasNext() {
        return nextContainer != root;
      }

      public T next() {
        T returnValue = nextContainer.value;
        nextContainer = nextContainer.nextContainer;
        return returnValue;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  /**
   * Evaluate the offset list using the associative operator provided when it
   * was constructed.
   *
   * @return The result of the evaluation.
   */
  public V evaluate() {
    if (operator == null) {
      throw new IllegalStateException("No associative operator was provided.");
    }
    if (root.leftChild == null) {
      return null;
    }
    return evaluate(root.leftChild);
  }

  private V evaluate(Node<T, V> node) {
    if (node.subtreeComputation != null) {
      return node.subtreeComputation;
    }
    V value = operator.extract(node.value);
    if (node.leftChild != null) {
      value = operator.operate(evaluate(node.leftChild), value);
    }
    if (node.rightChild != null) {
      value = operator.operate(value, evaluate(node.rightChild));
    }
    node.subtreeComputation = value;
    return value;
  }

  /**
   * Gets the height of the given subtree.
   *
   * @param node The root of the subtree.
   * @return The height of the subtree. The leaf node is a height of 0.
   */
  private static <T, V> int getHeight(Node<T, V> node) {
    return (node == null) ? -1 : node.height;
  }

}
