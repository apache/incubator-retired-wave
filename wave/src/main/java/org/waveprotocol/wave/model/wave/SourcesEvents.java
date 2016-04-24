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

package org.waveprotocol.wave.model.wave;

/**
 * An object that emits events to registered listeners.
 *
 *
 */
public interface SourcesEvents<Listener> {
  /**
   * Adds a listener.  If the listener is already registered on this object,
   * this method has no effect.
   *
   * @param listener  listener to add
   */
  void addListener(Listener listener);

  /**
   * Removes a listener.  If the listener is not already registered on this
   * object, this method has no effect.
   *
   * @param listener  listener to remove
   */
  void removeListener(Listener listener);
}
