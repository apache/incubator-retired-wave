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

import java.util.Map;

/**
 * Encapsulates a structured value with named fields which can be set and read.
 * It is intended that implementations function as records, with a fixed set of
 * valid keys.
 *
 * @author anorth@google.com (Alex North)
 * @param <K> enumerated type of the field names
 * @param <V> field value type
 */
public interface StructuredValue<K extends Enum<K>, V> {
  /**
   * Sets the value for a field.
   *
   * @param name field name to set
   * @param value new value
   */
  void set(K name, V value);

  /**
   * Atomically sets values for multiple fields.
   *
   * @param values field names and values to set
   */
  void set(Map<K,  V> values);

  /**
   * @return the current value of a field.
   */
  V get(K name);
}
