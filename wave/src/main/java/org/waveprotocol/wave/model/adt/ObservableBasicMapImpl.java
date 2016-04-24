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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A simple implementation of an {@linkplain ObservableBasicMap}.
 *
 */
public class ObservableBasicMapImpl<K, V> implements ObservableBasicMap<K, V> {
  /** Backing store for the collection's data. */
  private final Map<K, V> data = CollectionUtils.newHashMap();

  /** All the listeners for changes to this map. */
  private final CopyOnWriteSet<Listener<? super K, ? super V>> listeners = CopyOnWriteSet.create();

  /**
   * Initializes a new, empty map.
   */
  public ObservableBasicMapImpl() {
  }

  @Override
  public void addListener(ObservableBasicMap.Listener<? super K, ? super V> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ObservableBasicMap.Listener<? super K, ? super V> listener) {
    listeners.remove(listener);
  }

  @Override
  public V get(K key) {
    return data.get(key);
  }

  @Override
  public boolean put(K key, V newValue) {
    V oldValue = data.put(key, newValue);

    if (ValueUtils.equal(oldValue, newValue)) {
      return false;
    }

    fireOnEntrySet(key, oldValue, newValue);
    return true;
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(data.keySet());
  }

  @Override
  public void remove(K key) {
    V oldValue = data.remove(key);
    if (oldValue != null) {
      fireOnEntrySet(key, oldValue, null);
    }
  }

  /** Fires the changed event. */
  private void fireOnEntrySet(K key, V oldValue, V newValue) {
    for (ObservableBasicMap.Listener<? super K, ? super V> listener : listeners) {
      listener.onEntrySet(key, oldValue, newValue);
    }
  }
}
