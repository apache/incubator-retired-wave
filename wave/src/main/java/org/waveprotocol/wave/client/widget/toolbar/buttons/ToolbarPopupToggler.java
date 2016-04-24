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

import org.waveprotocol.wave.client.widget.popup.AlignedPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * Sets up a {@link ToolbarToggleButton} to toggle a popup, and notify a
 * listener when a new popup is created.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ToolbarPopupToggler implements ToolbarToggleButton.Listener, PopupEventListener {

  /**
   * Listener for when a new popup is created.
   */
  public interface Listener {
    /**
     * Called when a new popup is created, and prior to showing the popup.
     *
     * @param popup the new popup, to which listeners can add their own popup
     *        event listeners if they wish
     */
    void onPopupCreated(UniversalPopup popup);

    /**
     * Called when the current popup is destroyed, after hiding the popup.
     *
     * @param popup the popup destroyed
     */
    void onPopupDestroyed(UniversalPopup popup);
  }

  private final ToolbarToggleButton toggleButton;
  private final Listener listener;
  private UniversalPopup activePopup = null;

  private ToolbarPopupToggler(ToolbarToggleButton toggleButton, Listener listener) {
    this.toggleButton = toggleButton;
    this.listener = listener;
  }

  /**
   * Sets up a {@link ToolbarToggleButton} to toggle a popup and notify a
   * listener when a new popup is created/destroyed.
   *
   * @param toggleButton the toggle button to set up
   * @param listener the listener to be notified when a new popup is created
   */
  public static void associate(ToolbarToggleButton toggleButton, Listener listener) {
    new ToolbarPopupToggler(toggleButton, listener).init();
  }

  private void init() {
    toggleButton.setListener(this);
  }

  @Override
  public void onToggledOff() {
    assert activePopup != null;
    activePopup.hide();
    assert activePopup == null;
    listener.onPopupDestroyed(activePopup);
  }

  @Override
  public void onToggledOn() {
    assert activePopup == null;
    activePopup =
        PopupFactory.createPopup(toggleButton.hackGetWidget().getElement(),
            AlignedPopupPositioner.BELOW_RIGHT, PopupChromeFactory.createPopupChrome(), true);
    activePopup.associateWidget(toggleButton.hackGetWidget());
    activePopup.addPopupEventListener(this);
    listener.onPopupCreated(activePopup);
    activePopup.show();
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    toggleButton.setToggledOn(false);
    activePopup.removePopupEventListener(this);
    activePopup = null;
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    // Called only from the activePopup.show() in onToggledOn().
  }
}
