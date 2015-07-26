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
 * A {@link ToolbarButtonView} which behaves like a toggle button.
 *
 * Non-final for mocking.  Do not extend.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class ToolbarToggleButton extends AbstractToolbarButton {

  /**
   * Listener for click events.
   */
  public interface Listener {
    /**
     * Called when the button is toggled on via a click event.
     */
    void onToggledOn();

    /**
     * Called when the button is toggled off via a click event.
     */
    void onToggledOff();
  }

  private Listener listener;
  private boolean isToggledOn;

  public ToolbarToggleButton(ToolbarButtonUi button) {
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
      setToggledOn(!isToggledOn);
      triggerOnToggled();
      // NOTE: this has the effect of hiding the submenu (overflow or otherwise)
      // that the toggle button is in, whenever it's clicked.  It may be more
      // desirable to just toggle and force the user to close the submenu
      // manually (e.g. clicking in neural area), or, maybe not.  Both gmail and
      // eclipse have the toggle-closes-submenu behaviour.
      if (getParent() != null) {
        getParent().onActionPerformed();
      }
    }
  }

  private void triggerOnToggled() {
    if (listener != null) {
      if (isToggledOn) {
        listener.onToggledOn();
      } else {
        listener.onToggledOff();
      }
    }
  }

  /**
   * Sets the toggle state of the button. Does not fire event.
   *
   * @param toggledOn true if the button should be toggled on, false if it
   *        should be toggled off
   */
  public void setToggledOn(boolean toggledOn) {
    isToggledOn = toggledOn;
    getButton().setDown(isToggledOn);
  }
}
