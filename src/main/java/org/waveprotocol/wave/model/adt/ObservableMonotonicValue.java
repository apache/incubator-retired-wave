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
 * Extension of a {@link MonotonicValue} that broadcasts events whenever
 * the value changes.
 *
 */
public interface ObservableMonotonicValue<C extends Comparable<C>> extends MonotonicValue<C> {
  public interface Listener<C> {
    /**
     * Notifies this listener that the value has increased, or been removed
     * (in which case the new value is null). In the latter case, the value
     * is no longer usable.
     *
     * @param newValue  new value
     */
    void onSet(C oldValue, C newValue);
  }

  /**
   * Adds a listener.
   *
   * @param l  listener to add
   */
  void addListener(Listener<? super C> l);

  /**
   * Removes a listener.
   *
   * @param l  listener to remove
   */
  void removeListener(Listener<? super C> l);
}
