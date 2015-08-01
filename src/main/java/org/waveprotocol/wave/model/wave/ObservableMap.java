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

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.util.ReadableMap;

/**
 * An observable map.
 *
 */
public interface ObservableMap<K, V> extends ReadableMap<K, V>,
    SourcesEvents<ObservableMap.Listener<? super K, ? super V>> {

  /**
   * Observer of map changes.
   */
  interface Listener<K, V> {
    /**
     * Notifies this listener that a map entry has been added.
     */
    void onEntryAdded(K key, V value);

    /**
     * Notifies this listener that a map entry has been removed.
     */
    void onEntryRemoved(K key, V value);
  }
}
