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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implements a PruningSequenceMap using a doubly-linked list.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class LinkedPruningSequenceMap<K extends VolatileComparable<? super K>, V>
    implements PruningSequenceMap<K, V> {
  private final Comparator<K> cmp;
  private Node first = null;
  private Node last = null;
  private Node recent = null;
  private int size = 0;

  private class Node implements SequenceElement<V> {
    Node next;
    Node prev;
    K key;
    V value;

    private Node(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public Node getNext() {
      while (next != null && !next.key.isComparable()) {
        remove(next);
      }
      return next;
    }

    @Override
    public Node getPrev() {
      while (prev != null && !prev.key.isComparable()) {
        remove(prev);
      }
      return prev;
    }

    @Override
    public V value() {
      return value;
    }
  }

  /**
   * Factory method for when the key type is Comparable
   *
   * @return instance with comparator automatically supplied
   */
  public static <K extends VolatileComparable<K>, V> LinkedPruningSequenceMap<K, V> create() {
    return new LinkedPruningSequenceMap<K, V>(new Comparator<K>() {
      public int compare(K o1, K o2) {
        return o1.compareTo(o2);
      }
    });
  }

  @Override
  public void clear() {
    // Use getFirst() to ensure pruning of invalid nodes
    while (getFirst() != null) {
      remove(first.key);
    }
  }

  /**
   * Constructor
   *
   * @param cmp
   *            Comparator to use
   */
  public LinkedPruningSequenceMap(Comparator<K> cmp) {
    this.cmp = cmp;
  }

  @Override
  public V get(K key) {
    SequenceElement<V> node = getElement(key);
    return node == null ? null : node.value();
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public V put(K key, V value) {
    V old = null;
    if (size == 0) {
      emptyPut(key, value);
    } else {
      Node prev = findBefore(key);
      // findBefore might have emptied the map, so check again
      if (size == 0) {
        emptyPut(key, value);
      } else {
        if (prev == null || cmp.compare(key, prev.key) != 0) {
          Node node = new Node(key, value);

          // goes after last
          if (prev == last) {
            last = node;
          }

          // goes before first
          if (prev == null) {
            prev = last;
            first = node;
          }

          insertAfter(prev, node);
          size++;
        } else {
          old = prev.value;
          prev.value = value;
        }
      }
    }
    return old;
  }

  @Override
  public V remove(K key) {
    V val = null;
    if (!key.isComparable()) {
      // Key is already gone. If it is still in the node structure, it will get
      // cleaned up eventually and transparently. Logically, the entry is gone
      // the instant the key becomes non-comparable.
      return null;
    }
    Node node = findBefore(key);
    if (node != null && cmp.compare(key, node.key) == 0) {
      val = node.value;

      remove(node);
    }
    return val;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public SequenceElement<V> getElement(K key) {
    Node prev = findBefore(key);
    if (prev == null) {
      return first;
    }
    return cmp.compare(prev.key, key) == 0 ? prev : null;
  }

  @Override
  public boolean isFirst(SequenceElement<V> elt) {
    return elt == first;
  }

  @Override
  public boolean isLast(SequenceElement<V> elt) {
    return elt == last;
  }

  @Override
  public Node getFirst() {
    return getUsable(first);
  }

  @Override
  public Node getLast() {
    return getUsable(last);
  }

  @Override
  public Iterable<K> copyKeys() {
    Node first = getFirst();
    if (first != null) {
      List<K> keys = CollectionUtils.newArrayList();
      Node node = first;
      do {
        keys.add(node.key);
        node = node.getNext();
      } while (node != first);
      return keys;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public Node findBefore(K key) {
    if (key == null) {
      throw new IllegalArgumentException("key is null");
    }
    Node search = getUsable(recent);
    if (search == null) {
      return null;
    }
    while (true) {
      int cmpPrev = cmp.compare(search.key, key);
      if (cmpPrev <= 0) {
        if (last == search || cmpPrev == 0) {
          return search;
        }
        Node next = search.getNext();
        int cmpNext = cmp.compare(next.key, key);
        if (cmpNext > 0) {
          return search;
        }
        search = search.getNext();
      } else {
        if (first == search) {
          return null;
        }
        search = search.getPrev();
      }
    }
  }

  /**
   * put, when the map is empty.
   */
  private void emptyPut(K key, V value) {
    Node node = new Node(key, value);
    first = last = recent = node;
    recent.next = recent;
    recent.prev = recent;
    size++;
  }

  /**
   * Place node in the doubly linked list after prev
   *
   * @param prev
   * @param node
   */
  private void insertAfter(Node prev, Node node) {
    Node next = prev.getNext();
    node.prev = prev;
    node.next = next;
    prev.next = node;
    next.prev = node;
  }

  private void remove(Node node) {
    Node prev = node.prev, next = node.next;
    prev.next = next;
    next.prev = prev;

    node.next = node.prev = null; // cleanup node, in case of external use
    if (recent == node) {
      recent = prev;
    }
    if (first == node && last == node) {
      first = null;
      last = null;
      recent = null;
    } else {
      if (first == node) {
        first = next;
      }
      if (last == node) {
        last = prev;
      }
    }
    size--;
  }

  /**
   * @param node A node that may or may not be currently comparable
   * @return The input if usable, otherwise a nearby usable node, or null if none
   */
  private Node getUsable(Node node) {
    if (node != null && !node.key.isComparable()) {
      if (isLast(node)) {
        node = node.getNext();
        if (node != null) {
          node = node.getPrev();
        }
      } else {
        node = node.getPrev();
        if (node != null) {
          node = node.getNext();
        }
      }
    }
    return node;
  }
}
