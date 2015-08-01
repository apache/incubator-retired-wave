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

import org.waveprotocol.wave.client.widget.button.ClickButton.ClickButtonListener;
import org.waveprotocol.wave.client.widget.button.ClickButton.ClickWhenDisabledListener;

/**
 * Interface for controlling a click button.
 *
 *
 * NOTE(user): This interface is currently empty, but future public methods for
 * click buttons should go here.
 *
 * TODO(user): Add helpful methods here (such as setVisibility()).
 */
public interface ClickButtonController extends Disableable {
  /**
   * Sets the receiver of events fired by this button.
   *
   * @param listener The receiver of events fired by this button.
   */
  void setClickButtonListener(ClickButtonListener listener);

  /**
   * Sets the reciever of events fired by this button when disabled.
   *
   * TODO(kalman): Put in ClickButtonListener, provide vacuous implementation.
   */
  void setClickWhenDisabledListener(ClickWhenDisabledListener listener);

  /**
   * Updates the tooltip text.
   *
   * @param tooltip tooltip text.
   */
  void setTooltip(String tooltip);

  /**
   * Updates the text of the button.
   */
  void setText(String text);
}
