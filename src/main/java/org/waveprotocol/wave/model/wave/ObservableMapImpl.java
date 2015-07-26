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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.SimpleMap;

import java.util.Map;

/**
 * Observable collection of items.
 *
 */
public final class ObservableMapImpl<K, V> implements SimpleMap<K, V>, ObservableMap<K, V> {
  /** Listeners to notify of collection changes. */
  private final CopyOnWriteSet<Listener<? super K, ? super V>> listeners = CopyOnWriteSet.create();

  /** Items. */
  private final Map<K, V> items = CollectionUtils.newHashMap();

  /**
   * Creates an observable map.
   */
  private ObservableMapImpl() {
    // nothing to do
  }

  /**
   * Creates an observable map.
   */
  public static <K, V> ObservableMapImpl<K, V> create() {
    ObservableMapImpl<K, V> map = new ObservableMapImpl<K, V>();
    return map;
  }

  @Override
  public V put(K id, V item) {
    V old = items.get(id);
    if (old == item) {
      // Already there.
      return old;
    }
    items.put(id, item);
    if (old != null) {
      triggerOnItemRemoved(id, old);
    }
    triggerOnItemAdded(id, item);
    return old;
  }

  @Override
  public V remove(K id) {
    V old = items.remove(id);
    if (old != null) {
      triggerOnItemRemoved(id, old);
    }
    return old;
  }

  @Override
  public void clear() {
    items.clear();
  }

  @Override
  public V get(K key) {
    return items.get(key);
  }

  @Override
  public int size() {
    return items.size();
  }

  @Override
  public boolean isEmpty() {
    return items.isEmpty();
  }

  @Override
  public Iterable<K> copyKeys() {
    return CollectionUtils.newArrayList(items.keySet());
  }

  //
  // Extra methods that are useful, but not exposed by the map interfaces.
  //

  @Override
  public final boolean equals(Object o) {
    return (this == o)
        || (o instanceof ObservableMapImpl && items.equals(((ObservableMapImpl<?, ?>) o).items));
  }

  @Override
  public final int hashCode() {
    return items.hashCode();
  }

  //
  // Listeners.
  //

  @Override
  public void addListener(Listener<? super K, ? super V> l) {
    listeners.add(l);
  }

  @Override
  public void removeListener(Listener<? super K, ? super V> l) {
    listeners.remove(l);
  }

  private void triggerOnItemAdded(K key, V blip) {
    for (Listener<? super K, ? super V> l : listeners) {
      l.onEntryAdded(key, blip);
    }
  }

  private void triggerOnItemRemoved(K key, V blip) {
    for (Listener<? super K, ? super V> l : listeners) {
      l.onEntryRemoved(key, blip);
    }
  }
}
