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

package org.waveprotocol.wave.client.widget.toolbar;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;

/**
 * Builds {@link ToolbarButtonView}s.  The interface is not identical, but
 * rather has the intention of initialising a button with useful defaults.
 * Notably, setVisualElement is replace by the simpler setIcon.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ToolbarButtonViewBuilder {

  private String text = null;
  private String tooltip = null;
  private String iconCss = null;
  private ToolbarButtonView.State initialState = null;
  private String dc = null;

  /**
   * Sets the text.
   *
   * @return this builder.
   */
  public ToolbarButtonViewBuilder setText(String text) {
    this.text = text;
    return this;
  }

  /**
   * Sets the tooltip.
   *
   * @return this builder.
   */
  public ToolbarButtonViewBuilder setTooltip(String tooltip) {
    this.tooltip = tooltip;
    return this;
  }

  /**
   * Sets the icon from a CSS class.
   *
   * @return this builder.
   */
  public ToolbarButtonViewBuilder setIcon(String iconCss) {
    this.iconCss = iconCss;
    return this;
  }

  /**
   * Sets the initial state of the button.
   *
   * @return this builder.
   */
  public ToolbarButtonViewBuilder setInitialState(ToolbarButtonView.State initialState) {
    this.initialState = initialState;
    return this;
  }

  /**
   * Sets the debug class.
   *
   * @return this builder.
   */
  public ToolbarButtonViewBuilder setDebugClass(String dc) {
    this.dc = dc;
    return this;
  }

  /**
   * Applies the builder state to a click button.
   *
   * @return the configured click button
   */
  public ToolbarClickButton applyTo(
      ToolbarClickButton button, ToolbarClickButton.Listener listener) {
    applyToDisplay(button);
    if (listener != null) {
      button.setListener(listener);
    }
    return button;
  }

  /**
   * Applies the builder state to a toggle button.
   *
   * @return the configured toggle button
   */
  public ToolbarToggleButton applyTo(
      ToolbarToggleButton button, ToolbarToggleButton.Listener listener) {
    applyToDisplay(button);
    if (listener != null) {
      button.setListener(listener);
    }
    return button;
  }

  /**
   * Applies the builder state to a submenu.
   *
   * @return the configured submenu
   */
  public SubmenuToolbarView applyTo(
      SubmenuToolbarView submenu, SubmenuToolbarView.Listener listener) {
    applyToDisplay(submenu);
    if (listener != null) {
      submenu.setListener(listener);
    }
    return submenu;
  }

  /**
   * Applies the builder state to a toolbar button display.
   *
   * @return the configured button
   */
  private ToolbarButtonView applyToDisplay(ToolbarButtonView button) {
    if (text != null) {
      button.setText(text);
    }
    if (tooltip != null) {
      button.setTooltip(tooltip);
    }
    if (iconCss != null) {
      button.setVisualElement(createIcon(iconCss));
    }
    if (initialState != null) {
      button.setState(initialState);
    }
    if (dc != null) {
      button.addDebugClass(dc);
    }
    return button;
  }

  /**
   * Creates an icon Element from the CSS of the icon.
   */
  private Element createIcon(String css) {
    Element sprite = DOM.createDiv();
    sprite.setClassName(css);
    return sprite;
  }
}
