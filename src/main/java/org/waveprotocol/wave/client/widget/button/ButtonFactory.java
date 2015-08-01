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
import org.waveprotocol.wave.client.widget.button.ToggleButton.ToggleButtonListener;
import org.waveprotocol.wave.client.widget.button.icon.IconButtonTemplate;
import org.waveprotocol.wave.client.widget.button.icon.IconButtonTemplate.IconButtonStyle;
import org.waveprotocol.wave.client.widget.button.text.TextButton;
import org.waveprotocol.wave.client.widget.button.text.TextButton.TextButtonStyle;

/**
 * Collection of methods for creating various kinds of buttons.
 *
 * There are two axes for variation in terms of the buttons that can be created -
 * they can vary in terms of visual style and in terms of behaviour.
 *
 *
 * VISUAL STYLES
 *
 *  IconButton - IconButtons are buttons that are just an image.
 *
 *  TextButton - TextButtons have text associated with them as well as a set
 *               of images that surround the text.
 *
 *
 * BEHAVIOURS
 *
 *  ClickButton  - ClickButtons are buttons that have a single action
 *                 associated with it that is triggered whenever the button is
 *                 clicked.
 *
 *  ToggleButton - Buttons that can be either on and off, and fire different
 *                 actions depending on what state they're in when they are
 *                 clicked. Clicking a toggle button causes it to change state.
 *
 */
public class ButtonFactory {

  /**
   * Create a click button with an icon as its visual style.
   *
   * @param style The style of the icon for the button.
   * @param tooltip The tooltip for the button.
   * @param listener The listener to be notified when the user clicks the button.
   * @return The button.
   */
  public static ClickButtonWidget createIconClickButton(IconButtonStyle style,
      String tooltip, ClickButtonListener listener) {
    ClickButton logic = new ClickButton();
    logic.setClickButtonListener(listener);
    IconButtonTemplate template = new IconButtonTemplate(style, tooltip);
    template.setUiListener(logic.getUiEventListener());
    logic.setButtonDisplay(template);

    return new ClickButtonWidget(logic.getController(), template);
  }

  /**
   * Create a click button with text.
   *
   * @param text The text to put inside the button.
   * @param style The style of the frame and background for the button.
   * @param tooltip The tooltip for the button.
   * @param listener The listener to be notified when the user clicks the
   * button.
   * @return The button.
   * @see #createTextClickButton(String, TextButtonStyle, String,
   * ClickButtonListener, String)
   */
  public static ClickButtonWidget createTextClickButton(String text,
      TextButtonStyle style, String tooltip, ClickButtonListener listener) {
    return createTextClickButton(text, style, tooltip, listener, null);
  }

  /**
   * Create a click button with text, with a debugClass identifier.
   *
   * @param text The text to put inside the button.
   * @param style The style of the frame and background for the button.
   * @param tooltip The tooltip for the button.
   * @param listener The listener to be notified when the user clicks the
   * button.
   * @param debugClassId An optional debugClass identifier for Webdriver
   * testing, set it to <code>null</code> if unused.
   * @return The button.
   */
  public static ClickButtonWidget createTextClickButton(String text,
      TextButtonStyle style, String tooltip, ClickButtonListener listener, String debugClassId) {
    ClickButton logic = new ClickButton();
    logic.setClickButtonListener(listener);
    TextButton template = new TextButton(text, style, tooltip);
    if (null != debugClassId) {
//      DebugClassHelper.addDebugClass(template, debugClassId);
    }
    template.setStopPropagation(true);
    template.setUiListener(logic.getUiEventListener());
    logic.setButtonDisplay(template);

    return new ClickButtonWidget(logic.getController(), template);
  }

  /**
   * Create a toggle button with text.
   *
   * @param text The text to put inside the button.
   * @param style The style of the frame and background for the button.
   * @param tooltip The tooltip for the button.
   * @param listener The listener to be notified when the button is toggled.
   * @return The button.
   */
  public static ToggleButtonWidget createTextToggleButton(String text,
      TextButtonStyle style, String tooltip, ToggleButtonListener listener) {
    ToggleButton logic = new ToggleButton();
    logic.setToggleButtonListener(listener);
    TextButton template = new TextButton(text, style, tooltip);
    template.setStopPropagation(true);
    template.setUiListener(logic.getUiEventListener());
    logic.setButtonDisplay(template);

    return new ToggleButtonWidget(logic.getController(), template);
  }

  /**
   * Create a toggle button with icon styling.
   *
   * @param style The style of the icon for the button.
   * @param tooltip The tooltip for the button.
   * @param listener The listener to be notified when the button is toggled.
   * @return The button.
   */
  public static ToggleButtonWidget createIconToggleButton(IconButtonStyle style,
      String tooltip, ToggleButtonListener listener) {
    ToggleButton logic = new ToggleButton();
    logic.setToggleButtonListener(listener);
    IconButtonTemplate template = new IconButtonTemplate(style, tooltip);
    template.setUiListener(logic.getUiEventListener());
    logic.setButtonDisplay(template);

    return new ToggleButtonWidget(logic.getController(), template);
  }
}
