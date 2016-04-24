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

import org.waveprotocol.wave.model.util.NumberMap;
import org.waveprotocol.wave.model.util.ReadableNumberMap;

import java.util.Map;

/**
 * An implementation of NumberMap<V> based on JavaScript objects.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> type of values in the map
 */
public class JsoNumberMap<V> implements NumberMap<V> {

  final org.waveprotocol.wave.client.common.util.NumberMapJsoView<V> backend =
      org.waveprotocol.wave.client.common.util.NumberMapJsoView.create();

  private JsoNumberMap() {}

  public static <V> JsoNumberMap<V> create() {
    return new JsoNumberMap<V>();
  }

  @Override
  public void clear() {
    backend.clear();
  }

  @Override
  public boolean containsKey(double key) {
    return backend.has(key);
  }

  @Override
  public V getExisting(double key) {
    return backend.get(key);
  }

  @Override
  public V get(double key, V defaultValue) {
    if (backend.has(key)) {
      return backend.get(key);
    } else {
      return defaultValue;
    }
  }

  @Override
  public V get(double key) {
    return backend.get(key);
  }

  @Override
  public void put(double key, V value) {
    backend.put(key, value);
  }

  @Override
  public void putAll(ReadableNumberMap<V> pairsToAdd) {
    // TODO(ohler): check instanceof here and implement a fallback.
    ((JsoNumberMap<V>) pairsToAdd).backend.addToMap(this.backend);
  }

  @Override
  public void putAll(Map<Double, V> pairsToAdd) {
    for (Map.Entry<Double, V> e : pairsToAdd.entrySet()) {
      backend.put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void remove(double key) {
    backend.remove(key);
  }

  @Override
  public void each(final ProcV<V> callback) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(double key, V item) {
        callback.apply(key, item);
      }
    });
  }

  @Override
  public void filter(final EntryFilter<V> filter) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(double key, V item) {
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
