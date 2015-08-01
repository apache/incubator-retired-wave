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
package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.util.ReadableStringMap;

/**
 * Supplement-like interface for a single gadget state map.
 *
 */
interface GadgetState {
  /**
   * Sets or updates a key-value pair in the gadget state. If value is null
   * remove the key from the state.
   *
   * @param key The key.
   * @param value The value for the key. If null, the method removes the key.
   */
  void setState(String key, String value);

  /**
   * @returns the gadget state as a String-to-String map. The map is a snapshot
   *          of the current state. Future changes in the wave will not affect
   *          this map.
   */
  ReadableStringMap<String> getStateMap();

  /**
   * Removes the gadget state.
   */
  void remove();
}
