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

/**
 * Defines a registry of event handlers. Handlers are registered for particular
 * element kinds.
 *
 */
public interface EventHandlerRegistry {

  /**
   * Registers a click handler for an element kind.
   */
  void registerClickHandler(String kind, WaveClickHandler handler);

  /**
   * Registers a click handler for an element kind.
   */
  void registerMouseDownHandler(String kind, WaveMouseDownHandler handler);

  /**
   * Registers a click handler for an element kind.
   */
  void registerDoubleClickHandler(String kind, WaveDoubleClickHandler handler);

  /**
   * Registers a change handler for an element kind.
   */
  void registerChangeHandler(String kind, WaveChangeHandler handler);
}
