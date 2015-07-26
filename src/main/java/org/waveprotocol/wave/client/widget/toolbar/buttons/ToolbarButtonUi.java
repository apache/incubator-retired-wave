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

package org.waveprotocol.wave.client.widget.toolbar.buttons;

/**
 * A variant of {@link ToolbarButtonView} with additional methods necessary
 * for {@link ToolbarClickButton} and {@link ToolbarToggleButton}.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface ToolbarButtonUi extends ToolbarButtonView {

  /**
   * Listener for all click events (regardless of disabled state).
   */
  interface Listener {
    /**
     * Called when the button is clicked, regardless of disabled state.
     */
    void onClick();
  }

  /**
   * Sets whether the toolbar button should always display in the "active"
   * state.
   */
  void setDown(boolean isDown);

  /**
   * Sets the listener for UI events.
   */
  void setListener(Listener listener);
}
