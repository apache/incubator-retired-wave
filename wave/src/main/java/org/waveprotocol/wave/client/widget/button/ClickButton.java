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

import org.waveprotocol.wave.client.widget.button.ButtonDisplay.ButtonState;

/**
 * Implements the logic for buttons that only have one associated action, that
 * is, buttons that immediately return to their original state when they are
 * clicked.
 *
 */
public class ClickButton implements UniversalButton<ClickButtonController> {
  /**
   * The visual representation of this button.
   */
  protected ButtonDisplay buttonDisplay = null;

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
   * Listener for click events (provided by users to make the button do
   * something).
   */
  protected ClickButtonListener clickListener;

  /**
   * Listener for click-when-disabled events.
   */
  protected ClickWhenDisabledListener clickWhenDisabledListener;

  /**
   * Implementation of the public interface for controlling this button.
   */
  protected ClickButtonController controller = new ClickButtonController() {
    @Override
    public void setClickButtonListener(ClickButtonListener listener) {
      clickListener = listener;
    }

    @Override
    public void setClickWhenDisabledListener(ClickWhenDisabledListener listener) {
      clickWhenDisabledListener = listener;
    }

    @Override
    public void setDisabled(boolean isDisabled) {
      if (ClickButton.this.isDisabled != isDisabled) {
        ClickButton.this.isDisabled = isDisabled;
        updateState();
      }
    }

    @Override
    public void setTooltip(String tooltip) {
      ClickButton.this.buttonDisplay.setTooltip(tooltip);
    }

    @Override
    public void setText(String text) {
      ClickButton.this.buttonDisplay.setText(text);
    }

  };

  /**
   * Listener for click events from a button.
   *
   */
  public interface ClickButtonListener {
    /**
     * Called when a click event occurs on a button.
     */
    void onClick();
  }

  /**
   * Listener for click events from a button when it's disabled.
   *
   * TODO(kalman): Put in ClickButtonListener, provide vacuous implementation.
   */
  public interface ClickWhenDisabledListener {
    void onClickWhenDisabled();
  }

  /**
   * Create the listener on object creation so it can be used later
   */
  protected MouseListener mouseListener = new MouseListener() {
      /** {@inheritDoc} */
      public void onMouseDown() {
        isMouseDown = true;
        updateState();
      }

      /** {@inheritDoc} */
      public void onMouseEnter() {
        isMouseOver = true;
        updateState();
      }

      /** {@inheritDoc} */
      public void onMouseLeave() {
        isMouseOver = false;
        isMouseDown = false;
        updateState();
      }

      /** {@inheritDoc} */
      public void onMouseUp() {
        isMouseDown = false;
        updateState();
      }

      /** {@inheritDoc} */
      public void onClick() {
        if (isDisabled) {
          if (clickWhenDisabledListener != null) {
            clickWhenDisabledListener.onClickWhenDisabled();
          }
          return;
        }
        if (clickListener != null) {
          clickListener.onClick();
        }
      }
  };

  public ClickButton(ButtonDisplay button) {
    setButtonDisplay(button);
    button.setUiListener(getUiEventListener());
  }

  public ClickButton() { }

  /** {@inheritDoc} */
  public MouseListener getUiEventListener() {
    return mouseListener;
  }

  /**
   * Updates the visual state of the button to reflect the current logical
   * state.
   */
   protected void updateState() {
     if (isDisabled) {
       setState(ButtonState.DISABLED);
       return;
     }
    if (isMouseOver) {
      setState(isMouseDown ? ButtonDisplay.ButtonState.DOWN : ButtonDisplay.ButtonState.HOVER);
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
    if (buttonDisplay != null) {
      buttonDisplay.setState(state);
    }
  }

  /** {@inheritDoc} */
  public void setButtonDisplay(ButtonDisplay controlledButton) {
    this.buttonDisplay = controlledButton;
    updateState();
  }

  /**
   * @param listener Callback to be fired when the button is clicked.
   */
  public void setClickButtonListener(ClickButtonListener listener) {
    this.clickListener = listener;
  }

  /** {@inheritDoc} */
  public ClickButtonController getController() {
    return controller;
  }

  public static void attachListenerTo(ButtonDisplay view,
      ClickButton.ClickButtonListener listener) {
    ClickButton buttonLogic = new ClickButton();
    buttonLogic.setClickButtonListener(listener);
    buttonLogic.setButtonDisplay(view);
    view.setUiListener(buttonLogic.getUiEventListener());
  }
}
