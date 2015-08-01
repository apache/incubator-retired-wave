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
 * A list of elements. This offers a subset of JDK list functionality.
 *
 * The list is a factory for new elements, created with {@link #add(Object)}.
 * Element initialisation occurs within the {@code add()} method, rather than
 * initialising an element before adding it.
 *
 *
 * @param <T> type of the elements
 * @param <I> type of initialisation data for new elements
 *
 */
public interface ElementList<T, I> {
  /**
   * @return the values in this list
   */
  Iterable<T> getValues();

  /**
   * Removes a value from this set.
   *
   * @param element value to remove
   */
  boolean remove(T element);

  /**
   * Adds an element as the last element of this list and returns it.
   *
   * @param initialState describes initial state for the created element
   * @return the created and added element
   */
  T add(I initialState);

  /**
   * Adds an element at the specified location. The location must be between 0
   * and {@link #size()}.
   *
   * @param index location where the element is added
   * @param initialState describes initial state for the created element
   * @return the newly added element
   * @throws IndexOutOfBoundsException if the location is out of bounds
   */
  T add(int index, I initialState);

  /**
   * @param element the element to be located in this list
   * @return the location of the element on this list, or -1 if not present
   */
  int indexOf(T element);

  /**
   * @param index the location of the desired element
   * @return the element at the given index
   */
  T get(int index);

  /**
   * @return the number of elements in this list
   */
  int size();

  /**
   * Removes all elements from this list.
   */
  void clear();
}
