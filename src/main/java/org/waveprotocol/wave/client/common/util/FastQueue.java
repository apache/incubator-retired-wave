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

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An efficent implementation of queue for use in GWT.
 * Its efficiency is due to using JSO maps.
 *
 * Against the advice of AbstractQueue, this class accepts null.
 */
public class FastQueue<T> extends AbstractQueue<T>{
  private final IntMapJsoView<T> contents = IntMapJsoView.create();

  private int currentGetIndex = 0;
  private int currentPutIndex = 0;
  private int numEntry = 0;

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      int index = currentGetIndex;
      int lastEntry = -1;

      @Override
      public boolean hasNext() {
        index = forwardGetIndex(index);
        return index < currentPutIndex;
      }

      @Override
      public T next() {
        index = forwardGetIndex(index);
        lastEntry = index;
        return contents.get(index++);
      }

      @Override
      public void remove() {
        if (lastEntry >= 0) {
          removeEntry(lastEntry);
        }
      }
    };
  }
  @Override
  public int size() {
    return numEntry;
  }

  @Override
  public boolean offer(T e) {
    addEntry(e);
    return true;
  }

  @Override
  public T peek() {
    currentGetIndex = forwardGetIndex(currentGetIndex);
    return currentGetIndex < currentPutIndex ? contents.get(currentGetIndex) : null;
  }

  // This implementation of queue cannot use the inherited remove() method because a
  // null element will be misinterpreted as the queue being empty.
  @Override
  public T remove() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return poll();
  }

  /**
   * Move the index forward until it hit the next non empty index or the end.
   */
  private int forwardGetIndex(int index) {
    // the first possible index is the current get index.
    index = Math.max(currentGetIndex, index);
    while (index < currentPutIndex && !contents.has(index)) {
      index++;
    }
    return index;
  }

  @Override
  public T poll() {
    T ret = peek();
    if (!isEmpty()) {
      removeEntry(currentGetIndex);
      currentGetIndex++;
    }
    return ret;
  }

  /**
   * Remove the entry with the given index
   * @param index
   */
  protected T removeEntry(int index) {
    T ret = null;
    if (contents.has(index)) {
      numEntry--;
      ret = contents.get(index);
      contents.remove(index);
    }
    assert numEntry < currentPutIndex - currentGetIndex;
    return ret;
  }

  /**
   * Add an entry to the queue.
   *
   * @param e
   *
   * @return the index of the added entry.
   */
  protected int addEntry(T e) {
    numEntry++;
    int putIndex = currentPutIndex++;
    contents.put(putIndex, e);
    assert numEntry <= currentPutIndex - currentGetIndex;
    return putIndex;
  }

  @Override
  public void clear() {
    currentPutIndex = 0;
    currentGetIndex = 0;
    numEntry = 0;
    contents.clear();
  }
}
