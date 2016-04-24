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


package org.waveprotocol.wave.client.wavepanel.event;

import org.waveprotocol.wave.client.common.util.KeyCombo;

import java.util.EnumMap;

/**
 * A by-key registry of key handlers. At most one handler is permitted for any
 * one key combination.
 *
 */
public final class KeySignalRouter implements KeySignalHandler {

  private final EnumMap<KeyCombo, KeySignalHandler> handlers =
      new EnumMap<KeyCombo, KeySignalHandler>(KeyCombo.class);

  /**
   * Creates a signal router.
   */
  static KeySignalRouter create() {
    return new KeySignalRouter();
  }

  /**
   * Registers a key handler for a set of keys.
   *
   * @param keys keys to handle
   * @param handler handler for {@code keys}
   * @throws IllegalArgumentException if there is already a handler registered
   *         for any of the keys in {@code keys}.
   */
  public void register(Iterable<KeyCombo> keys, KeySignalHandler handler) {
    for (KeyCombo key : keys) {
      if (handlers.containsKey(key)) {
        throw new IllegalArgumentException(
            "Feature conflict: multiple handlers registered for key " + key);
      }
      handlers.put(key, handler);
    }
  }

  @Override
  public boolean onKeySignal(KeyCombo key) {
    KeySignalHandler handler = handlers.get(key);
    return (handler != null) ? handler.onKeySignal(key) : false;
  }
}
