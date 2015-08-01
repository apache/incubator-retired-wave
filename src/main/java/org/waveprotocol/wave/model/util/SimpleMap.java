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
 * A subset of the {@link java.util.Map} interface.  This interface is used so
 * that Map implementations may be simpler (e.g., no iteration), but still
 * present a familiar API.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 * @param <V>
 */
public interface SimpleMap<K, V> extends ReadableMap<K, V> {

  /** @see java.util.Map#put(Object,Object) */
  V put(K key, V value);

  /** @see java.util.Map#remove(Object) */
  V remove(K key);

  /** @see java.util.Map#clear() */
  void clear();
}
