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

package org.waveprotocol.wave.client.common.util;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Iterator;

/**
 * An implementation of {@link Sequence} that uses linked nodes to provide
 * constant-time implementations of all queries.
 *
 * @param <T> type of block in this sequence.
 */
public final class LinkedSequence<T> implements Sequence<T>, Iterable<T> {

  /**
   * Node in the linked structure.
   */
  private final static class Node<T> {
    private final T target;
    private Node<T> next;
    private Node<T> prev;

    Node(T target) {
      this.target = target;
    }

    @Override
    public String toString() {
      return target.toString();
    }
  }

  /** Maps objects to their sequence nodes. */
  private final IdentityMap<T, Node<T>> items = CollectionUtils.createIdentityMap();

  /** First item in this sequence. */
  private Node<T> first;

  /** Last item in this sequence. */
  private Node<T> last;

  private LinkedSequence() {
  }

  /**
   * Creates a linked sequence.
   */
  public static <T> LinkedSequence<T> create() {
    return new LinkedSequence<T>();
  }

  /**
   * Creates a linked sequence, initialized by a given order.
   */
  public static <T> LinkedSequence<T> create(Iterable<T> source) {
    LinkedSequence<T> sequence = new LinkedSequence<T>();
    for (T x : source) {
      sequence.append(x);
    }
    return sequence;
  }

  /**
   * Appends an object to the end of this sequence.
   *
   * @param x object to append
   */
  public void append(T x) {
    Preconditions.checkArgument(x != null, "Item is null");
    Node<T> item = new Node<T>(x);
    if (first == null) {
      first = last = item;
    } else {
      last.next = item;
      item.prev = last;
      last = item;
    }
    items.put(x, item);
  }

  /**
   * Prepends an object to the start of this sequence.
   *
   * @param x object to prepend
   */
  public void prepend(T x) {
    Preconditions.checkArgument(x != null, "Item is null");
    Node<T> item = new Node<T>(x);
    if (first == null) {
      first = last = item;
    } else {
      first.prev = item;
      item.next = first;
      first = item;
    }
    items.put(x, item);
  }

  /**
   * Inserts an object before a reference x in this sequence.
   *
   * @param reference object before which {@code x} is to be inserted (or
   *        {@code null} to append)
   * @param x object to insert
   */
  public void insertBefore(T reference, T x) {
    if (reference == null) {
      append(x);
      return;
    }
    Preconditions.checkArgument(x != null, "Item is null");
    Node<T> refNode = items.get(reference);
    Preconditions.checkArgument(refNode != null, "Reference not in this sequence");
    Node<T> item = new Node<T>(x);

    if (first == refNode) {
      first = item;
    } else {
      refNode.prev.next = item;
      item.prev = refNode.prev;
    }
    item.next = refNode;
    refNode.prev = item;
    items.put(x, item);
  }

  /**
   * Inserts an object after a reference x in this sequence.
   *
   * @param reference object after which {@code x} is to be inserted (or {@code
   *        null} to prepend)
   * @param x object to insert
   */
  public void insertAfter(T reference, T x) {
    if (reference == null) {
      prepend(x);
      return;
    }
    Preconditions.checkArgument(x != null, "Item is null");
    Node<T> refNode = items.get(reference);
    Preconditions.checkArgument(refNode != null, "Reference not in this sequence");
    Node<T> item = new Node<T>(x);

    if (last == refNode) {
      last = item;
    } else {
      refNode.next.prev = item;
      item.next = refNode.next;
    }
    item.prev = refNode;
    refNode.next = item;
    items.put(x, item);
  }

  /**
   * Removes an object from this sequence.
   *
   * @param x object to remove
   * @throws IllegalArgumentException if {@code x} is not in this sequence.
   */
  public void remove(T x) {
    Preconditions.checkArgument(x != null, "Item is null");
    Node<T> item = items.removeAndReturn(x);
    Preconditions.checkArgument(item != null, "Reference not in this sequence");

    if (first == item) {
      first = item.next;
    } else {
      item.prev.next = item.next;
    }

    if (last == item) {
      last = item.prev;
    } else {
      item.next.prev = item.prev;
    }
  }

  /**
   * Clears this sequence.
   */
  public void clear() {
    first = last = null;
    items.clear();
  }

  @Override
  public boolean isEmpty() {
    return items.isEmpty();
  }

  @Override
  public boolean contains(T x) {
    return x != null && items.has(x);
  }

  @Override
  public T getFirst() {
    return targetOf(first);
  }

  @Override
  public T getLast() {
    return targetOf(last);
  }

  @Override
  public T getNext(T block) {
    return targetOf(nextOf(nodeOf(block)));
  }

  @Override
  public T getPrevious(T block) {
    return targetOf(prevOf(nodeOf(block)));
  }

  @Override
  public Iterator<T> iterator() {
    return SequenceIterator.create(this);
  }

  private T targetOf(Node<T> node) {
    return node != null ? node.target : null;
  }

  private Node<T> nodeOf(T node) {
    return items.get(node);
  }

  private Node<T> nextOf(Node<T> node) {
    return node != null ? node.next : first;
  }

  private Node<T> prevOf(Node<T> node) {
    return node != null ? node.prev : last;
  }

  @Override
  public String toString() {
    return CollectionUtils.newArrayList(this).toString();
//    StringBuilder s = new StringBuilder();
//    s.append("[");
//    Node<T> item = first;
//    while (item != null) {
//      s.append(item.target);
//      if (item != last) {
//        s.append(", ");
//      }
//      item = item.next;
//    }
//    s.append("]");
//    return s.toString();
  }
}
