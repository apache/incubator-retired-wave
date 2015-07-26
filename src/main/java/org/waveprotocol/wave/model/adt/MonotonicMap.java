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
 * Provides a map of keys to monotonically increasing values. This interface
 * only guarantees that values seen by {@link #get(Object)} will increase
 * monotonically, and will be at least that last set by a
 * {@link #put(Object, Object)}.
 *
 * Values may never be set to null, but may be null initially.
 *
 * When {@link #put(Object, Object)}ing values, if there is already a value
 * associated with the same key that is equal or greater (as defined by
 * {@link Comparable#compareTo(Object)}), the put call has no effect.
 *
 * @param <K> key type
 * @param <C> value type (must be comparable)
 */
public interface MonotonicMap<K, C extends Comparable<C>> extends BasicMap<K, C> { }
