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

import org.waveprotocol.wave.model.util.IntMap;
import org.waveprotocol.wave.model.util.ReadableIntMap;

import java.util.Map;

/**
 * An implementation of IntMap<V> based on JavaScript objects.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> type of values in the map
 */
public class JsoIntMap<V> implements IntMap<V> {

  final org.waveprotocol.wave.client.common.util.IntMapJsoView<V> backend =
      org.waveprotocol.wave.client.common.util.IntMapJsoView.create();

  private JsoIntMap() {}

  public static <V> JsoIntMap<V> create() {
    return new JsoIntMap<V>();
  }

  @Override
  public void clear() {
    backend.clear();
  }

  @Override
  public boolean containsKey(int key) {
    return backend.has(key);
  }

  @Override
  public V getExisting(int key) {
    return backend.get(key);
  }

  @Override
  public V get(int key, V defaultValue) {
    if (backend.has(key)) {
      return backend.get(key);
    } else {
      return defaultValue;
    }
  }

  @Override
  public V get(int key) {
    return backend.get(key);
  }

  @Override
  public void put(int key, V value) {
    backend.put(key, value);
  }

  @Override
  public void putAll(ReadableIntMap<V> pairsToAdd) {
    // TODO(ohler): check instanceof here and implement a fallback.
    ((JsoIntMap<V>) pairsToAdd).backend.addToMap(this.backend);
  }

  @Override
  public void putAll(Map<Integer, V> pairsToAdd) {
    for (Map.Entry<Integer, V> e : pairsToAdd.entrySet()) {
      backend.put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void remove(int key) {
    backend.remove(key);
  }

  @Override
  public void each(final ProcV<V> callback) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(int key, V item) {
        callback.apply(key, item);
      }
    });
  }

  @Override
  public void filter(final EntryFilter<V> filter) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(int key, V item) {
        if (filter.apply(key, item)) {
          // entry stays
        } else {
          backend.remove(key);
        }
      }
    });
  }

  @Override
  public boolean isEmpty() {
    return backend.isEmpty();
  }

  @Override
  public int countEntries() {
    return backend.countEntries();
  }

  @Override
  public String toString() {
    return JsoMapStringBuilder.toString(backend);
  }
}
