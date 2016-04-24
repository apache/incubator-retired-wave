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

import com.google.gwt.user.client.Event;

/**
 * Implements the logic for buttons that have two logical states and that toggle
 * between those states when they are clicked.
 *
 */
public class ToggleButton implements UniversalButton<ToggleButtonController> {
  /**
   * Is this button currently on? A {@link ToggleButton} is initially off, and
   * toggles between on and off whenever it is clicked.
   */
  protected boolean isOn = false;

  /**
   * Is the mouse currently over the button?
   */
  protected boolean isMouseOver = false;

  /**
   * Is the mouse button currently down?
   */
  protected boolean isMouseDown = false;

  /**
   * Is this button disabled?
   */
  protected boolean isDisabled = false;

  /**
   * The visual representation of this button.
   */
  protected ButtonDisplay display;

  /**
   * User-provided callback to be fired when the button is toggled.
   */
  protected ToggleButtonListener toggleListener;

  /**
   * Contains actions to be performed when a toggle button is toggled.
   *
   */
  public interface ToggleButtonListener {
    /**
     * Called when the button is toggled to the 'on' state.
     */
    void onOn();

    /**
     * Called when the button is toggled to the 'off' state.
     */
    void onOff();
  }

  protected MouseListener mouseListener = new MouseListener() {
    @Override
    public void onClick() {
      if (isDisabled) {
        return;
      }
      isOn = !isOn;
      if (isOn) {
        toggleListener.onOn();
      } else {
        toggleListener.onOff();
      }
      updateState();
    }

    @Override
    public void onMouseDown() {
      Event.getCurrentEvent().stopPropagation();
      Event.getCurrentEvent().preventDefault();
      isMouseDown = true;
      updateState();
    }

    @Override
    public void onMouseEnter() {
      isMouseOver = true;
      updateState();
    }

    @Override
    public void onMouseLeave() {
      isMouseDown = false;
      isMouseOver = false;
      updateState();
    }

    @Override
    public void onMouseUp() {
      isMouseDown = false;
      updateState();
    }
  };

  @Override
  public MouseListener getUiEventListener() {
    return mouseListener;
  }

  /**

  /**
   * Sets whether or not this toggle button is on or off.
   *
   * @param isOn {@code true} to turn this button on, {@code false} to turn it
   *        off.
   */
  public void setIsOn(boolean isOn) {
    this.isOn = isOn;
    updateState();
  }

  /**
   * @return {@code true} if this button is on.
   */
  public boolean isOn() {
    return isOn;
  }

  /**
   * @return {@code true} if this button is off.
   */
  public boolean isOff() {
    return !isOn;
  }

  /**
   * Updates the visual state of the button to reflect the current logical
   * state.
   */
  protected void updateState() {
    if (isDisabled) {
      setState(ButtonDisplay.ButtonState.DISABLED);
      return;
    }
    if (isOn) {
      setState(ButtonDisplay.ButtonState.DOWN);
      return;
    }
    if (isMouseOver) {
      setState(isMouseDown ? ButtonDisplay.ButtonState.DOWN
          : ButtonDisplay.ButtonState.HOVER);
    } else {
      setState(ButtonDisplay.ButtonState.NORMAL);
    }
  }

  /**
   * Sets a new state for this button and notifies the display.
   *
   * @param state The new state for this button.
   */
  private void setState(ButtonDisplay.ButtonState state) {
    // NOTE(patcoleman): Does not check here to see whether the new state is the same
    //   as the current state - possible optimisation can be added to return on NOOP? i.e.
    // if(newState.equals(state)) return;
    if (display != null) {
      display.setState(state);
    }
  }

  @Override
  public void setButtonDisplay(ButtonDisplay display) {
    this.display = display;
    updateState();
  }

  /**
   * @param toggleListener Listener for toggle events.
   */
  public void setToggleButtonListener(ToggleButtonListener toggleListener) {
    this.toggleListener = toggleListener;
  }

  /**
   * The controller that external users can use to control this toggle button.
   */
  protected final ToggleButtonController controller = new ToggleButtonController() {
    @Override
    public void setOn(boolean isOn) {
      ToggleButton.this.isOn = isOn;
      updateState();
    }

    @Override
    public void setToggleListener(ToggleButtonListener listener) {
      setToggleButtonListener(listener);
    }

    @Override
    public void setDisabled(boolean isDisabled) {
      if (ToggleButton.this.isDisabled != isDisabled) {
        ToggleButton.this.isDisabled = isDisabled;
        updateState();
      }
    }
  };

  @Override
  public ToggleButtonController getController() {
    return controller;
  }
}
