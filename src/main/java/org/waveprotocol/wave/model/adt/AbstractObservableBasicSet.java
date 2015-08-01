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

/**
 * Base class for {@linkplain ObservableBasicSet} implementations. Handles the
 * Observable aspect.
 *
 */
public abstract class AbstractObservableBasicSet<T> implements ObservableBasicSet<T> {

  /** All the listeners for changes to this map. */
  private final CopyOnWriteSet<Listener<? super T>> listeners = CopyOnWriteSet.create();

  /**
   * Initializes a new map with no listeners.
   */
  public AbstractObservableBasicSet() {
  }

  @Override
  public void addListener(Listener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener<T> listener) {
    listeners.remove(listener);
  }

  /** Fires the onValueAdded method for all listeners. */
  protected void fireOnValueAdded(T value) {
    for (Listener<? super T> listener : listeners) {
      listener.onValueAdded(value);
    }
  }

  /** Fires the onValueRemoved method for all listeners. */
  protected void fireOnValueRemoved(T value) {
    for (Listener<? super T> listener : listeners) {
      listener.onValueRemoved(value);
    }
  }
}
