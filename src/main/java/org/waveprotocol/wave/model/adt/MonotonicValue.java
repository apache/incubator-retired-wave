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
 * Encapsulates a value that can be set and can be read, but whose value only
 * ever increases, as defined by {@link Comparable#compareTo(Object)}.
 *
 * The value may never be set to null, but may be null initially.
 *
 */
public interface MonotonicValue<C extends Comparable<C>> {

  /**
   * @return current value.
   */
  C get();

  /**
   * Sets this value.  If the current value is equal or greater (as defined by
   * {@link Comparable#compareTo(Object)}), this method has no effect.
   *
   * @param value  new value, must not be null
   */
  void set(C value);
}
