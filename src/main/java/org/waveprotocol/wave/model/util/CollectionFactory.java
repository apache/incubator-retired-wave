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

import java.util.Queue;

/**
 * A factory interface for creating the types of collections that we
 * have optimized JavaScript implementations for.
 */
public interface CollectionFactory {
  /**
   * Returns a new, empty StringMap.
   */
  <V> StringMap<V> createStringMap();

  /**
   * Returns a new, empty NumberMap.
   */
  <V> NumberMap<V> createNumberMap();

  /**
   * Returns a new, empty IntMap.
   */
  <V> IntMap<V> createIntMap();

  /**
   * Returns a new, empty StringSet.
   */
  <V> StringSet createStringSet();

  /**
   * Returns a new, empty IdentitySet.
   */
  <T> IdentitySet<T> createIdentitySet();

  /**
   * Returns a queue.
   */
  <E> Queue<E> createQueue();

  /**
   * Returns a priority queue.
   */
  NumberPriorityQueue createPriorityQueue();

  /**
   * Returns an identity map.
   */
  <K, V> IdentityMap<K, V> createIdentityMap();
}
