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

package org.waveprotocol.wave.client.widget.common;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

/**
 *
 * A skinable switch button that inherits from {@link Button}
 */
public class ButtonSwitch extends Button implements ClickHandler {

  /**
   *
   * Interface for listening to click events
   */
  public interface Listener {

    /**
     * The Switch has been turned on
     *
     * @param sender
     */
    void onOn(ButtonSwitch sender);

    /**
     * The Switch has been turned off
     *
     * @param sender
     */
    void onOff(ButtonSwitch sender);
  }

  /**
   * For now the switch can only have a single listener
   */
  private Listener listener = null;

  /**
   * Titles for on state
   */
  private final String onTitle;

  /**
   * Titles for off state
   */
  private final String offTitle;

  /**
   * Current state
   */
  private boolean on = false;

  /**
   * Constructs switch button (in 'on' state)
   *
   * @param styleName
   * @param onTitle Title for 'on' state
   * @param offTitle Title for 'of' state
   * @param listener
   */
  public ButtonSwitch(
          String styleName, String onTitle,
          String offTitle, Listener listener) {
    super(styleName, onTitle, null);
    addClickHandler(this);
    this.listener = listener;
    this.onTitle = onTitle;
    this.offTitle = offTitle;
    setState(true);
  }

  /**
   * Constructs labeled switch button (in 'on' state)
   *
   * @param styleName
   * @param onTitle Title for 'on' state
   * @param offTitle Title for 'of' state
   * @param listener
   * @param label Label for switch
   */
  public ButtonSwitch(
          String styleName, String onTitle,
          String offTitle, Listener listener,
          String label) {
    super(styleName, onTitle, null, label);
    addClickHandler(this);
    this.listener = listener;
    this.onTitle = onTitle;
    this.offTitle = offTitle;
    setState(true);
  }

  /**
   * @return current state
   */
  public boolean getState() {
    return on;
  }

  /**
   * Set new current state (w/o firing event)
   *
   * @param on
   */
  public void setState(boolean on) {
    if (on) {
      me.replaceClassName(ButtonStyle.off, ButtonStyle.on);
    } else {
      me.replaceClassName(ButtonStyle.on, ButtonStyle.off);
    }
    me.setTitle(on ? onTitle : offTitle);
    this.on = on;
  }

  public void onClick(ClickEvent e) {
    setState(!on);
    if (on) {
      listener.onOn(this);
    } else {
      listener.onOff(this);
    }

    // NOTE(user): This is used in the nav panel to make opening a popup for a
    // folder not select that folder.
    e.stopPropagation();
  }
}
