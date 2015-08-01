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

import com.google.gwt.core.client.JsArrayNumber;

import java.util.NoSuchElementException;

/**
 * An unbounded priority queue based on a priority heap. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/PriorityQueue.html">[Sun
 * docs]</a>
 *
 * Specialized version copied from  com/google/gwt/emul/java/util/PriorityQueue.java
 * so it doesn't require box/unbox.
 *
 */
public class NumberPriorityQueue implements org.waveprotocol.wave.model.util.NumberPriorityQueue {
  public interface Comparator {
    int compare(double a, double b);
  }

  /**
   * A heap held in an array. heap[0] is the root of the heap (the smallest
   * element), the subtrees of node i are 2*i+1 (left) and 2*i+2 (right). Node i
   * is a leaf node if 2*i>=n. Node i's parent, if i>0, is floor((i-1)/2).
   */
  private final JsArrayNumber heap;
  private final Comparator comparator;

  public NumberPriorityQueue() {
    this(new Comparator() {
      @Override
      public int compare(double a, double b) {
        return Double.compare(a, b);
      }
    });
  }

  public NumberPriorityQueue(Comparator comparator) {
    heap = (JsArrayNumber)JsArrayNumber.createArray();
    this.comparator = comparator;
  }

  @Override
  public boolean offer(double e) {
    int node = heap.length();
    heap.push(e);
    while (node > 0) {
      int childNode = node;
      node = (node - 1) / 2; // get parent of current node
      if (comparator.compare(heap.get(node), e) <= 0) {
        // parent is smaller, so we have a valid heap
        heap.set(childNode, e);
        return true;
      }
      // exchange with parent and try again
      heap.set(childNode, heap.get(node));
    }
    heap.set(node, e);
    return true;
  }

  @Override
  public double peek() {
    if (heap.length() == 0) {
      throw new NoSuchElementException("Empty queue");
    }
    return heap.get(0);
  }

  @Override
  public double poll() {
    if (heap.length() == 0) {
      throw new NoSuchElementException("Empty queue");
    }
    double value = heap.get(0);
    if (heap.length() > 1) {
      heap.set(0, pop(heap)); // move last element to root
      mergeHeaps(0); // make it back into a heap
    } else {
      pop(heap);
    }
    return value;
  }

  private static native double pop(JsArrayNumber arr) /*-{
    return arr.pop();
  }-*/;

  @Override
  public int size() {
    return heap.length();
  }

  /**
   * Merge two subheaps into a single heap. O(log n) time
   *
   * PRECONDITION: both children of <code>node</code> are heaps
   *
   * @param node the parent of the two subtrees to merge
   */
  protected void mergeHeaps(int node) {
    int n = heap.length();
    double value = heap.get(node);
    while (node * 2 + 1 < n) {
      int childNode = 2 * node + 1; // start with left child
      if ((childNode + 1 < n)
          && (comparator.compare(heap.get(childNode + 1), heap.get(childNode)) < 0)) {
        childNode++; // right child is smaller, go down that path
      }
      // if the current element is smaller than the children, stop
      if (comparator.compare(value, heap.get(childNode)) <= 0) {
        break;
      }
      heap.set(node, heap.get(childNode));
      node = childNode;
    }
    heap.set(node, value);
  }

}
