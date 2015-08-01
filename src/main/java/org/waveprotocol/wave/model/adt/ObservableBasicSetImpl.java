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

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Set;

/**
 * A simple implementation of an {@linkplain ObservableBasicMap}.
 *
 */
public class ObservableBasicSetImpl<T> extends AbstractObservableBasicSet<T> {
  /** Backing store for the collection's data. */
  private final Set<T> data = CollectionUtils.newHashSet();

  /**
   * Initializes a new, empty map.
   */
  public ObservableBasicSetImpl() {
  }

  @Override
  public Iterable<T> getValues() {
    return data;
  }

  @Override
  public boolean contains(T value) {
    return data.contains(value);
  }

  @Override
  public void add(T value) {
    boolean added = data.add(value);
    if (added) {
      fireOnValueAdded(value);
    }
  }

  @Override
  public void remove(T value) {
    boolean removed = data.remove(value);
    if (removed) {
      fireOnValueRemoved(value);
    }
  }

  @Override
  public void clear() {
    while (!data.isEmpty()) {
      T value = data.iterator().next();
      remove(value);
    }
  }
}
