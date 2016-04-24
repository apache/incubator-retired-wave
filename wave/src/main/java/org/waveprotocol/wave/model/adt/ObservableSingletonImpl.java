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

import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * Simple implementation of a singleton.
 *
 * @author anorth@google.com (Alex North)
 */
public final class ObservableSingletonImpl<V, I> implements ObservableSingleton<V, I> {
  /**
   * Factory which creates value objects from abstract initial state.
   */
  interface Factory<V, I> {
    V create(I initialState);
  }

  private final Factory<V, I> factory;
  private final CopyOnWriteSet<Listener<? super V>> listeners = CopyOnWriteSet.create();

  private V value = null;

  public ObservableSingletonImpl(Factory<V, I> factory) {
    this.factory = factory;
  }

  @Override
  public boolean hasValue() {
    return value != null;
  }

  @Override
  public V get() {
    return value;
  }

  @Override
  public V set(I initialState) {
    V oldValue = value;
    value = factory.create(initialState);
    maybeTriggerOnValueChanged(oldValue, value);
    return value;
  }

  @Override
  public void clear() {
    V oldValue = value;
    value = null;
    maybeTriggerOnValueChanged(oldValue, value);
  }

  @Override
  public void addListener(Listener<? super V> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<? super V> listener) {
    listeners.remove(listener);
  }

  private void maybeTriggerOnValueChanged(V oldValue, V newValue) {
    if (ValueUtils.notEqual(oldValue, newValue)) {
      for (Listener<? super V> l : listeners) {
        l.onValueChanged(oldValue, newValue);
      }
    }
  }
}
