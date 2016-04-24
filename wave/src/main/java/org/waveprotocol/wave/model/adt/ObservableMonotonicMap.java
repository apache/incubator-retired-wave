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
 * Extension of a {@link MonotonicMap} that broadcasts events whenever
 * the map state changes.
 *
 * The semantics of an {@link ObservableBasicMap} are refined by the map being
 * monotonic. For the
 * {@link ObservableBasicMap.Listener#onEntrySet(Object, Object, Object)}
 * method, the new value will always be greater than or equal to the old value
 * for a key, unless the entire map is being deleted - in which case the new
 * value will be null.
 *
 */
public interface ObservableMonotonicMap<K, C extends Comparable<C>>
    extends MonotonicMap<K, C>, ObservableBasicMap<K, C> { }
