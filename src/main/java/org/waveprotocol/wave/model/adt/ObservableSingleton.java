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

import org.waveprotocol.wave.model.wave.SourcesEvents;


/**
 * Extends {@link Singleton} to provide events whenever the value changes.
 *
 * @author anorth@google.com (Alex North)
 * @param <V> type of the value
 * @param <I> type of a value initializer
 */
public interface ObservableSingleton<V, I> extends Singleton<V, I>,
    SourcesEvents<ObservableSingleton.Listener<? super V>> {
  interface Listener<V> {
    /**
     * Called when the value changes.
     *
     * @param oldValue the previous value (may be null)
     * @param newValue the new value (may be null)
     */
    void onValueChanged(V oldValue, V newValue);
  }
}
