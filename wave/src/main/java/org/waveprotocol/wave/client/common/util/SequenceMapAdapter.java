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

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Presents a sequence that is backed by a {@link SequenceMap}.
 *
 * In particular, this class removes the inconvenient parts of the
 * {@link SequenceMap} API (sequence elements are exposed, circularity,
 * map-ness), presenting a nice, simple, sequence.
 *
 */
public final class SequenceMapAdapter<T extends VolatileComparable<T>> implements
    Sequence<T> {

  /** Underlying doodad map. */
  private final SequenceMap<T, T> map;

  private SequenceMapAdapter(SequenceMap<T, T> map) {
    this.map = map;
  }

  public static <T extends VolatileComparable<T>> SequenceMapAdapter<T> create(
      SequenceMap<T, T> maps) {
    return new SequenceMapAdapter<T>(maps);
  }

  @Override
  public T getFirst() {
    SequenceElement<T> first = map.getFirst();
    return first != null ? first.value() : null;
  }

  @Override
  public T getLast() {
    SequenceElement<T> last = map.getLast();
    return last != null ? last.value() : null;
  }

  @Override
  public T getNext(T item) {
    SequenceElement<T> node = map.getElement(item);
    Preconditions.checkArgument(node != null, "item not in this sequence");
    return node != map.getLast() ? node.getNext().value() : null;
  }

  @Override
  public T getPrevious(T item) {
    SequenceElement<T> node = map.getElement(item);
    Preconditions.checkArgument(node != null, "item not in this sequence");
    return node != map.getFirst() ? node.getPrev().value() : null;
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean contains(T x) {
    return x != null && map.get(x) != null;
  }

  public void add(T x) {
    map.put(x, x);
  }

  public void remove(T x) {
    map.remove(x);
  }
}
