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

/**
 * A sequence maintains an ordering of objects defined via successor and
 * predecessor relationships.
 *
 * This interface is intended to provide read-only access. It is up to each
 * implementation to define whether it can reflect live changes to the
 * underlying state.
 *
 * @param <T> type of item in this sequence.
 */
public interface Sequence<T> {

  /**
   * @return true iff this sequence is empty.
   */
  boolean isEmpty();

  /** @return first object in this sequence. */
  T getFirst();

  /** @return the last object in this sequence. */
  T getLast();

  /**
   * @return the object after {@code x}, or {@code null} if {@code x} is the
   *         last item.
   */
  T getNext(T x);

  /**
   * @return the object before {@code x}, or {@code null} if {@code x} is the
   *         first item.
   */
  T getPrevious(T x);

  /**
   * @return true if and only if {@code x} is in this sequence.
   */
  boolean contains(T x);
}
