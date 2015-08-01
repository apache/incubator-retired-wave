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

package org.waveprotocol.wave.common.logging;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A circular buffer implementation backed by a LinkedList.
 *
 * This is basically a linked list with a limit on the number of entries.
 *
 *
 * @param <T>
 */
public class CircularBuffer<T> implements Iterable<T> {
  private final LinkedList<T> data;
  private final int maxSize;

  /**
   * Initializes a circular buffer with max size.
   * @param maxSize
   */
  public CircularBuffer(int maxSize) {
    data = new LinkedList<T>();
    this.maxSize = maxSize;
  }

  /**
   * Returns the number of entries in the buffer.
   */
  public int size() {
    return data.size();
  }

  /**
   * Adds an entry to the circular buffer. If the number of entries exceeds
   * buffer capacity, the first added entry is replaced.
   *
   * @param e
   */
  public void add(T e) {
    data.addLast(e);
    if (data.size() > maxSize) {
      data.removeFirst();
    }
  }

  /**
   * {@inheritDoc}
   */
  public Iterator<T> iterator() {
    return data.iterator();
  }

  /**
   * Clears the circular buffer.
   */
  public void clear() {
    data.clear();
  }
}
