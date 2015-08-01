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

import java.util.List;

/**
 * A base class for in-memory element lists. Extending classes should at minimum provide
 * the implementation of the {@link #createInitialized(Object)} method, which is to
 * return a fully initialized element to be stored in the list.
 *
 * <T> The type of the element stored in the list.
 * <I> The type of the state used to initialize newly inserted elements.
 *
 */
public abstract class InMemoryElementList<T, I> implements ElementList<T, I> {
  private final List<T> delegate = CollectionUtils.newArrayList();

  /**
   * Create and return a new element to be inserted in the list. The element is to be
   * initialized with the provided {@code initialState}.
   *
   * @param initialState The state to initialize the new element with.
   * @return A new element, initialized with the provided {@code initialState}.
   */
  protected abstract T createInitialized(I initialState);

  @Override
  public T add(I initialState) {
    return add(delegate.size(), initialState);
  }

  @Override
  public T add(int index, I initialState) {
    T element = createInitialized(initialState);
    delegate.add(index, element);
    return element;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public T get(int index) {
    return delegate.get(index);
  }

  @Override
  public Iterable<T> getValues() {
    return delegate;
  }

  @Override
  public int indexOf(T element) {
    return delegate.indexOf(element);
  }

  @Override
  public boolean remove(T element) {
    return delegate.remove(element);
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
