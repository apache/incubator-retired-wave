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
 * Interface for objects that represent the visual state of a button.
 *
 */
public interface ButtonDisplay {
  /**
   * Represents the different visual states a button can be in.
   *
   */
  public enum ButtonState {
    NORMAL, HOVER, DOWN, DISABLED
  }

  /**
   * Sets the visual state of this display.
   *
   * @param state The visual state to adopt.
   */
  void setState(ButtonDisplay.ButtonState state);

  /**
   * Sets the controller of this display.
   *
   * @param mouseListener Listener for mouse events fired by this display.
   */
  void setUiListener(MouseListener mouseListener);

  /**
   * Updates the tooltip text.
   *
   * @param tooltip tooltip text.
   */
  void setTooltip(String tooltip);

  /**
   * Updates the text on the button.
   */
  void setText(String text);
}
