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
 * Describes a basic set. This is a subset of the JDK set functionality.
 *
 * Null values may not be supported by some implementations.
 *
 */
public interface BasicSet<T> {
  /**
   * @return the values in this set.
   */
  Iterable<T> getValues();

  /**
   * Tests if this set contains a specific value.
   *
   * @param value  value to test for
   * @return true if {@code value} is in this set.
   */
  boolean contains(T value);

  /**
   * Adds a value to this set.
   *
   * @param value  value to add
   */
  void add(T value);

  /**
   * Removes a value from this set.
   *
   * @param value  value to remove
   */
  void remove(T value);

  /**
   * Removes all values from this set.
   */
  void clear();
}
