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
 * Extension of a {@link BasicSet} that broadcasts events whenever the set state
 * changes.
 *
 */
public interface ObservableBasicSet<T> extends BasicSet<T>,
    SourcesEvents<ObservableBasicSet.Listener<T>> {
  public interface Listener<T> {
    /**
     * Notifies this listener that a value has been added to the set.
     */
    void onValueAdded(T newValue);

    /**
     * Notifies this listener that a value has been removed from the set.
     */
    void onValueRemoved(T oldValue);
  }
}
