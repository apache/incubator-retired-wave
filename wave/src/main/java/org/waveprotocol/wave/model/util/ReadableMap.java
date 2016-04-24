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

/**
 * A simplification of the {@link java.util.Map} interface. This interface is
 * used so that Map implementations may be simpler (e.g., no values or entries
 * collections), but still present a familiar API.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 * @param <V>
 */
public interface ReadableMap<K, V> {

  /** @see java.util.Map#isEmpty() */
  boolean isEmpty();

  /** @see java.util.Map#get(Object) */
  V get(K key);

  /** @see java.util.Map#size() */
  int size();

  /**
   * @return a copy of the current keys. The returned object is a snapshot of
   *         the current key collection, unlike {@link java.util.Map#keySet()},
   *         is not updated when this map changes, nor do changes to this copy
   *         propagate into the map. It is safe to modify the map while
   *         iterating through the returned snapshot object.
   */
  Iterable<K> copyKeys();
}
