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

/**
 * Handles a key signal.
 *
 * Unlike other event handlers (e.g., {@link WaveClickHandler}), there is no
 * context for a key signal, because they are not directed at any particular DOM
 * element.
 *
 */
public interface KeySignalHandler {

  /**
   * @return true if this handler intends other handlers not to see the event.
   *         This is typically because this handler has taken an action that
   *         should be mutually exclusive with other actions that other handlers
   *         may perform.
   */
  boolean onKeySignal(KeyCombo key);
}
