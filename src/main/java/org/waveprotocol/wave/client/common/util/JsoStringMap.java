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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Map;

/**
 * An implementation of StringMap<V> based on JavaScript objects.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> type of values in the map
 */
public class JsoStringMap<V> implements StringMap<V> {

  @VisibleForTesting public final JsoView backend;

  private JsoStringMap(JsoView backend) {
    this.backend = backend;
  }

  @SuppressWarnings("unchecked")
  public static <V> JsoStringMap<V> create() {
    return new JsoStringMap<V>(createBackend());
  }

  /** Construct an empty StringMap */
  static JsoView createBackend() {
    if (!QuirksConstants.DOES_NOT_SUPPORT_JSO_PROTO_FIELD) {
      return createProtoless();
    } else {
      return JsoView.create();
    }
  }

  /** Construct an empty StringMap with null for the hidden __proto__ field */
  private static native JsoView createProtoless() /*-{
    return {__proto__:null};
  }-*/;

  @Override
  public void clear() {
    backend.clear();
  }

  @Override
  public boolean containsKey(String key) {
    return backend.containsKey(escape(key));
  }

  @Override
  public V getExisting(String key) {
    key = escape(key);
    if (!backend.containsKey(key)) {
      // Not using Preconditions.checkState to avoid unecessary string concatenation
      throw new IllegalStateException("getExisting: Key '" + key + "' is not in map");
    }
    return backend.<V>getObjectUnsafe(key);
  }

  @Override
  public V get(String key, V defaultValue) {
    key = escape(key);
    if (backend.containsKey(key)) {
      return backend.<V>getObjectUnsafe(key);
    } else {
      return defaultValue;
    }
  }

  @Override
  public V get(String key) {
    return backend.<V>getObjectUnsafe(escape(key));
  }

  @Override
  public void put(String key, V value) {
    backend.setObject(escape(key), value);
  }

  @Override
  public String someKey() {
    return backend.firstKey();
  }

  /**
   * The purpose of these methods is to prevent the __proto__ key from ever
   * being set on the underlying JSO.
   */
  static String escape(String key) {
    Preconditions.checkNotNull(key, "StringMap/StringSet cannot contain null keys");
    return QuirksConstants.DOES_NOT_SUPPORT_JSO_PROTO_FIELD || key.startsWith("__") ?
        '_' + key : key;
  }

  static String unescape(String key) {
    return QuirksConstants.DOES_NOT_SUPPORT_JSO_PROTO_FIELD || key.startsWith("__") ?
        key.substring(1) : key;
  }

  @Override
  public void putAll(ReadableStringMap<V> pairsToAdd) {
    // This cast should not fail as we should not have other
    // implementations of ReadableStringMap in the client.
    backend.putAll(((JsoStringMap<V>) pairsToAdd).backend);
  }

  @Override
  public void putAll(Map<String, V> pairsToAdd) {
    for (Map.Entry<String, V> e : pairsToAdd.entrySet()) {
      backend.setObject(escape(e.getKey()), e.getValue());
    }
  }

  @Override
  public void remove(String key) {
    backend.remove(escape(key));
  }

  @Override
  public void each(final ProcV<? super V> callback) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(String key, V item) {
        callback.apply(unescape(key), item);
      }
    });
  }

  @Override
  public void filter(final EntryFilter<? super V> filter) {
    backend.each(new ProcV<V>() {
      @Override
      public void apply(String key, V item) {
        if (filter.apply(unescape(key), item)) {
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

  @SuppressWarnings("unchecked") // Safe because we return a read-only string set
  @Override
  public ReadableStringSet keySet() {
    return new JsoStringSet(backend);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder("{");
    each(new ProcV<V>() {
      @Override
      public void apply(String key, V item) {
        if (b.length() > 1) {
          b.append(",");
        }
        b.append(key + ":" + item);
      }
    });
    b.append("}");
    return b.toString();
  }
}
