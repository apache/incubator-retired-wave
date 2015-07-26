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
 * A read-write interface to a set of elements.
 *
 * This is used in favor of using a standard Java collections interface so that
 * Javascript-optimized implementations can be used in GWT code.
 *
 * Consistent with {@link ReadableIdentitySet}, null is not permitted as a
 * value. All methods will reject null values.
 *
 */
public interface IdentitySet<T> extends ReadableIdentitySet<T> {

  /**
   * Adds that an element to this set it is it not already present. Otherwise,
   * does nothing.
   *
   * @param s element to add
   */
  void add(T s);

  /**
   * Removes an element from this set if it is present. Otherwise, does nothing.
   *
   * @param s element to remove
   */
  void remove(T s);

  /**
   * Removes all elements from this set.
   */
  void clear();
}
