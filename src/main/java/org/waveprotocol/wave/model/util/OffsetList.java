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
 * A list which indexes objects by offsets. The objects being indexed are stored
 * in containers in this list. Each container has a size, and the offset of each
 * container is the sum of the sizes of all containers before it.
 *
 *
 * @param <T> The type of data contained in the data structure.
 */
public interface OffsetList<T> extends Iterable<T> {

  /**
   * A container for holding data.
   *
   * @param <T> The type of data contained in the container.
   */
  public interface Container<T> {

    /**
     * Gets the container preceding this container.
     *
     * @return The container preceding this container.
     */
    Container<T> getPreviousContainer();

    /**
     * Gets the container following this container.
     *
     * @return The container following this container.
     */
    Container<T> getNextContainer();

    /**
     * Gets the value contained in this container.
     *
     * @return The value contained in this container.
     */
    T getValue();

    /**
     * Sets the value contained in this container.
     *
     * @param value The value to be contained in this container.
     */
    void setValue(T value);

    /**
     * Returns the offset of this container in the <code>OffsetList</code> to
     * which it belongs.
     *
     * @return The offset of this container in the <code>OffsetList</code> to
     *         which it belongs.
     */
    int offset();

    /**
     * Gets the size of this container.
     *
     * @return The size of this container.
     */
    int size();

    /**
     * Inserts a new container with the given value and size before this
     * container.
     *
     * @param newValue The new value to insert.
     * @param valueSize The size of the new container to insert.
     * @return The new container.
     */
    Container<T> insertBefore(T newValue, int valueSize);

    /**
     * Removes this container from its <code>OffsetList</code>.
     *
     * The container will no longer be usable (i.e. all references from this
     * container to other containers will be null).
     */
    void remove();

    /**
     * Splits this container. The sum of the sizes of the two resulting
     * containers will be the same as the size of the container being split.
     * This container will become the first of the two resulting containers, and
     * the second of the two resulting containers will be returned.
     *
     * @param offset The offset at which to perform the split. The size of this
     *        container will be set to the value of this offset, and the size of
     *        the new container will be the difference between the old size of
     *        this container and the value of this offset.
     * @param newValue The value that is to be contained in the new container,
     *        which will be the second of the two containers in the result of
     *        the split.
     * @return The new container.
     */
    Container<T> split(int offset, T newValue);

    /**
     * Increases the size of this container by the given amount.
     *
     * @param sizeDelta The amount by which to increase this container's size.
     *        This may be negative.
     */
    void increaseSize(int sizeDelta);

  }

  /**
   * An action that can be performed at a location in an
   * <code>OffsetList</code>.
   *
   * @param <T> The type of the values stored in this <code>OffsetList</code>.
   * @param <R> The type of the return value from performing this action.
   */
  public interface LocationAction<T, R> {

    /**
     * Performs an action on a given location.
     *
     * @param container The container that contains the location.
     * @param offset The offset of the location within the container.
     * @return The return value of this action.
     */
    R performAction(Container<T> container, int offset);

  }

  /**
   * Gets the first container in this offset list.
   *
   * @return The first container in this offset list.
   */
  Container<T> firstContainer();

  /**
   * Gets the sentinel container of this offset list. The sentinel is not
   * considered to be part of the offset list's data and its contents will not
   * be returned by any iterators over this offset list. The sentinel is capable
   * of containing a value, however, if a sentinel value is ever useful. Calling
   * <code>getPreviousContainer()</code> on the first container in this offset
   * list or <code>getNextContainer()</code> on the last container in this
   * offset list will return the sentinel container. The offset of the sentinel
   * container is the offset directly following the last container in this
   * offset list.
   *
   * @return The sentinel container.
   */
  Container<T> sentinel();

  /**
   * @return The total size of the list (i.e., the sum of the sizes of all its
   *         non-sentinel containers).
   */
  int size();

  /**
   * Performs an action at a given offset.
   *
   * @param <R> The return type of the action.
   * @param offset The offset at which to perform the action.
   * @param locationAction The action to perform.
   * @return The value returned from performing the action.
   */
  <R> R performActionAt(int offset, LocationAction<T, R> locationAction);

}
