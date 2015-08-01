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


/**
 * Extension of a {@link BasicMap} that broadcasts events whenever
 * the map state changes.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface ObservableBasicMap<K, V> extends BasicMap<K, V> {
  /**
   * Interface for listening to BasicMap change events.
   */
  public interface Listener<K, V> {
    /**
     * Notifies this listener that either a new entry has been added, or the
     * value for an existing key has increased, or the key-value pair has been
     * removed, in which case newValue is null.
     *
     * @param key       key value
     * @param newValue  new entry value
     */
    void onEntrySet(K key, V oldValue, V newValue);
  }

  /**
   * Adds a listener to this map.
   *
   * @param l  listener to add
   */
  void addListener(Listener<? super K, ? super V> l);

  /**
   * Removes a listener from this map.
   *
   * @param l  listener to remove
   */
  void removeListener(Listener<? super K, ? super V> l);
}
