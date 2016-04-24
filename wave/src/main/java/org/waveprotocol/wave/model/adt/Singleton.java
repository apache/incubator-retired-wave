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
 * Encapsulates a value of which there is either zero or one canonical instance.
 *
 * @author anorth@google.com (Alex North)
 * @param <V> type of the value
 * @param <I> type of a value initializer
 */
public interface Singleton<V, I> {
  /**
   * Checks whether the singleton has a value.
   */
  boolean hasValue();

  /**
   * Gets the singleton value, or null if it has no value.
   */
  V get();

  /**
   * Sets the singleton value.
   *
   * It is implementation-dependent whether null is a valid initial state.
   */
  V set(I initialState);

  /**
   * Clears the singleton value.
   */
  void clear();
}
