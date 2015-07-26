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
import java.util.NoSuchElementException;

/**
 * A list implementation that permits modification while iterating. Iteration
 * order is LIFO, compared to the order of {@link #add(Object) add} calls.
 *
 * Elements added while iterating will not be exposed by the iterator, and
 * elements removed while iterating will also not be exposed by future
 * {@link Iterator#next()} calls of existing iterators (except for the rare
 * case of the removal an element x, which was preceded by a
 * {@link Iterator#hasNext()} call that succeeded because of the existence of
 * that element x; in that case, {@link Iterator#next()} will return the
 * removed element).
 *
 * This implementation has no extra runtime cost over a linked list, and is
 * suitable for collections that fire events frequently, and/or change
 * frequently during iteration.  However, it does incur the space cost of a
 * list node per list item, and the reasoning cost of a non-trivial removal
 * policy.
 *
 */
public final class ConcurrentList<T> implements Iterable<T> {

  /**
   * A simple doubly-linked node that can be deleted.
   */
  private final static class Node<T> {
    private boolean isDeleted;
    private Node<T> prev;
    private Node<T> next;
    private final T data;

    Node(Node<T> prev, Node<T> next, T data) {
      this.isDeleted = false;
      this.prev = prev;
      this.next = next;
      this.data = data;
    }

    Node<T> prev() {
      return prev;
    }

    Node<T> next() {
      return next;
    }

    T data() {
      return data;
    }

    boolean isDeleted() {
      return isDeleted;
    }

    /**
     * Routes surrounding nodes around this one, but does _not_ change this
     * node's links (so it can still be iterated over).
     */
    void remove() {
      if (prev != null) {
        prev.next = next;
      }
      if (next != null) {
        next.prev = prev;
      }
      isDeleted = true;
    }
  }

  /**
   * Iterator that skips deleted nodes.  The iterator simply maintains a
   * reference to a node in the list.  This node is never in a deleted state
   * at the time when it is set.  It is only updated as follows:
   * <ul>
   *   <li>hasNext() updates the node by skipping newly deleted nodes;</li>
   *   <li>next() moves the node to the next non-deleted node;</li>
   *   <li>remove() does not move the node.</li>
   * </ul>
   */
  private final class NodeIterator implements Iterator<T> {
    /** Node which defines our current iteration point. */
    private Node<T> node;

    /** Node at most recent return of next().  We keep this to make remove() trivial. */
    private Node<T> lastReturnedNode;

    /**
     * Creates an iterator pointing to the start of the list.
     */
    private NodeIterator() {
      node = start;
    }

    /**
     * Finds the next non-deleted node after a reference one.
     *
     * @param node  reference node
     * @return next non-deleted node, or {@code null} if there are none.
     */
    private Node<T> nextNonDeletedNode(Node<T> node) {
      while (node != null && node.isDeleted()) {
        node = node.next();
      }
      return node;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
      // skip nodes deleted since node was last set
      node = nextNonDeletedNode(node);
      return node != null;
    }

    /**
     * {@inheritDoc}
     */
    public T next() {
      if (node == null) {
        throw new NoSuchElementException();
      }
      // Do not skip deleted nodes here.  We want the nullity of this method to match the
      // truth of a prior call to hasNext(), regardless of mutations inbetween.
      lastReturnedNode = node;
      node = nextNonDeletedNode(node.next());
      return lastReturnedNode.data();
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
      if (lastReturnedNode != null && !lastReturnedNode.isDeleted()) {
        ConcurrentList.this.remove(node);
      }
    }
  }

  /**
   * First node of the list, or null for an empty list.
   * A class invariant is that ((start == null) || (!start.isDeleted)).
   */
  private Node<T> start;

  /**
   * Creates a concurrent list.
   *
   * @return a new concurrent list.
   */
  public static <T> ConcurrentList<T> create() {
    return new ConcurrentList<T>();
  }

  /**
   * Adds an item to the front of this list.
   *
   * @param item  item to add
   */
  public void add(T item) {
    if (start == null) {
      start = new Node<T>(null, null, item);
    } else {
      start = new Node<T>(null, start, item);
      start.next.prev = start;
    }
  }

  /**
   * Removes the first occurrence of an item from this list.
   *
   * @param item  item to remove
   */
  public void remove(T item) {
    Node<T> node = start;

    while (node != null) {
      if (node.data().equals(item)) {
        break;
      } else {
        node = node.next();
      }
    }

    if (node != null) {
      remove(node);
    }
  }

  /**
   * Removes a node, updating terminal references.
   *
   * @param node  node to remove
   */
  private void remove(Node<T> node) {
    if (node == start) {
      start = start.next();
    }
    node.remove();
  }

  /**
   * Returns true if this collection contains no elements.
   *
   * @return true if this collection contains no elements
   */
  public boolean isEmpty() {
    // This test is valid, despite the concept of deleted nodes, due to the
    // invariant condition on start.
    return start == null;
  }

  /**
   * {@inheritDoc}
   */
  public Iterator<T> iterator() {
    return new NodeIterator();
  }
}
