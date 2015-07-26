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

import java.util.Iterator;

/**
 * An iterator over a {@link Sequence}.
 *
 */
public final class SequenceIterator<T> implements Iterator<T> {
  private final Sequence<T> sequence;
  private T next;

  private SequenceIterator(Sequence<T> sequence, T next) {
    this.sequence = sequence;
    this.next = next;
  }

  public static <T> SequenceIterator<T> create(Sequence<T> sequence) {
    return new SequenceIterator<T>(sequence, sequence.getFirst());
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public T next() {
    T toReturn = next;
    next = sequence.getNext(next);
    return toReturn;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
