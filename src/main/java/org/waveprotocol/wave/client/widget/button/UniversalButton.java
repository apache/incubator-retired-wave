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

package org.waveprotocol.wave.client.widget.button;


/**
 * Interface for objects that receive UI events for a physical implementation of
 * a button and send out changes to a button display state. Used to implement
 * different behaviours for classes that implement the {@link ButtonDisplay}
 * interface.
 *
 *
 * @param <Controller> An interface for controlling the button.
 */
public interface UniversalButton<Controller> {
  /**
   * @return A listener that receives UI events and updates the buttons logical
   *         state.
   *
   * NOTE(user): Maybe it would be better to make this type generic as well?
   */
  MouseListener getUiEventListener();

  /**
   * @return An interface for controlling the button.
   */
  Controller getController();

  /**
   * Sets the {@link ButtonDisplay} that will manifest the visual state of this
   * button.
   *
   * @param display The {@link ButtonDisplay} that represents the button.
   */
  void setButtonDisplay(ButtonDisplay display);
}
