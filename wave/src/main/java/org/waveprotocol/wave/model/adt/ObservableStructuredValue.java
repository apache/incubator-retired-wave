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

import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.StructuredValue;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Map;

/**
 * An observable structured value.
 *
 * @author anorth@google.com (Alex North)
 * @param <K> enumerated type of the field names
 * @param <V> field value type
 */
public interface ObservableStructuredValue<K extends Enum<K>, V> extends StructuredValue<K, V>,
    SourcesEvents<ObservableStructuredValue.Listener<K, ? super V>> {
  public interface Listener<K, V> extends DeletionListener {
    /**
     * Called when one or more fields change value. Each map will have the same
     * set of keys, being the names of the changed fields. A newly set value will
     * be null in {@code oldValues}. A cleared value will be null in {@code
     * newValues}.
     *
     * @param oldValues the old values of changed fields
     * @param newValues the new values of changed fields
     */
    void onValuesChanged(Map<K, ? extends V> oldValues, Map<K, ? extends V> newValues);
  }
}
