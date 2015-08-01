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
 * A {@link ToolbarButtonView} which behaves like a click button.
 *
 * Non-final for mocking.  Do not extend.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class ToolbarClickButton extends AbstractToolbarButton {

  /**
   * Listener for click events.
   */
  public interface Listener {
    /**
     * Called when the button is clicked.  This event will only fire if the
     * button is enabled.
     */
    void onClicked();
  }

  private Listener listener;

  public ToolbarClickButton(ToolbarButtonUi button) {
    super(button, true);
  }

  /**
   * Sets the listener to click events.
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public void onClick() {
    if (!isDisabled()) {
      if (listener != null) {
        listener.onClicked();
      }
      if (getParent() != null) {
        getParent().onActionPerformed();
      }
    }
  }
}
