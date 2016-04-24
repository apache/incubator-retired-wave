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

package org.waveprotocol.wave.model.adt;

import java.util.Set;

/**
 * Provides a basic mapping of keys to values.  This map is intended to be used
 * in contexts where the access to the underlying state is not exclusive, and
 * thus clients of this interface and those inheriting from it should not expect
 * {@link #put(Object, Object)} to be the only means by which the map state
 * may change.
 *
 * Null values may not be supported by some implementations.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface BasicMap<K, V> {
  /**
   * Gets the value associated with a specified key.
   *
   * @param key  key value
   * @return the value for {@code key}.
   */
  V get(K key);

  /**
   * Puts a value in this map under a specified key. Implementations of this
   * class may choose to not accept the new value and return {@code false}
   * instead.
   *
   * @param key key value
   * @param value value to associate with {@code key}
   * @return true if the map changed as a result of this method.
   */
  boolean put(K key, V value);

  /**
   * @return unmodifiable set of the keys in this map.
   */
  Set<K> keySet();

  /** Remove the specified key and its associated value from the map. */
  void remove(K key);
}
