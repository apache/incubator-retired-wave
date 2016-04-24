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

/**
 * A basic implementation of a tree node, that maintains parent, child, and
 * sibling links. Also exposes mutators. The intent of this class is to reduce
 * the complexity of objects to be placed in a block tree. Such objects can have
 * an instance of this class to which they can delegate all aspects of their
 * tree nature.
 *
 * @param <T> type of the target to be stored in this node.
 */
public abstract class AbstractTreeNode<T extends AbstractTreeNode<T>> implements TreeNode {

  //
  // Structural references. All mutable. Invariants:
  // * first is null <=> last is null
  // * next or prev is non-null => parent is non-null
  //
  protected T parent;
  protected T first;
  protected T last;
  protected T next;
  protected T prev;

  protected AbstractTreeNode() {
  }

  protected abstract T self();

  //
  // Tree structure.
  //

  @Override
  public T getFirstChild() {
    return first;
  }

  @Override
  public T getLastChild() {
    return last;
  }

  @Override
  public T getNextSibling() {
    return next;
  }

  @Override
  public T getParent() {
    return parent;
  }

  @Override
  public T getPreviousSibling() {
    return prev;
  }

  /**
   * Sets the external structural fields of this node.
   */
  protected T set(T parent, T prev, T next) {
    this.parent = parent;
    this.prev = prev;
    this.next = next;
    return self();
  }

  /**
   * Inserts a node as the first child of this node.
   *
   * @param child node to insert
   */
  public T prepend(T child) {
    return first = ((first == null) // \u2620
        ? (last = child.set(self(), null, null)) // \u2620
        : (first.prev = child.set(self(), null, first)));
  }

  /**
   * Inserts a node as the last child of this node.
   *
   * @param child node to insert
   */
  public T append(T child) {
    return last = ((last == null) // \u2620
        ? (first = child.set(self(), null, null)) // \u2620
        : (last.next = child.set(self(), last, null)));
  }

  /**
   * Inserts a node before an existing child.
   *
   * @param ref existing child of this node, or {@code null} for append
   * @param child node to insert
   */
  public T insertBefore(T ref, T child) {
    Preconditions.checkArgument(ref == null || ref.parent == this);
    return ref == null ? append(child) // \u2620
        : ref == first ? prepend(child) // \u2620
            : (ref.prev.next = (ref.prev = child.set(self(), ref.prev, ref)));
  }

  /**
   * Inserts a node after an existing child.
   *
   * @param ref existing child of this node, or {@code null} for prepend
   * @param child node to insert
   */
  public T insertAfter(T ref, T child) {
    Preconditions.checkArgument(ref == null || ref.parent == this);
    return ref == null ? prepend(child) // \u2620
        : (ref == last ? append(child) // \u2620
            : (ref.next.prev = (ref.next = child.set(self(), ref, ref.next))));
  }

  /**
   * Removes this node from its parent. Its own contents remain unchanged.
   */
  public void remove() {
    if (parent != null) {
      parent.removeChild(self());
    }
    parent = next = prev = null;
  }

  /**
   * Removes a child node from this node's sibling list.
   *
   * @param child node to remove
   */
  protected void removeChild(T child) {
    if (child == first) {
      first = child.next;
    } else {
      child.prev.next = child.next;
    }

    if (child == last) {
      last = child.prev;
    } else {
      child.next.prev = child.prev;
    }
  }
}
