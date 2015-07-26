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

import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * An extension of the element list that broadcasts events whenever the state of
 * the list changes.
 *
 */
public interface ObservableElementList<T, I> extends ElementList<T, I>,
    SourcesEvents<ObservableElementList.Listener<T>> {
  /**
   * Defines the methods of the listener to element list events.
   */
  public interface Listener<T> {
    /**
     * @param entry A new value that was added to this list.
     */
    void onValueAdded(T entry);

    /**
     * @param entry An existing value that was removed from the list.
     */
    void onValueRemoved(T entry);
  }
}
