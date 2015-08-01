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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.autohide.Hideable;

/**
 * Universal popups are framed, and can contain a title bar if requested.
 *
 */
public interface UniversalPopup extends PopupEventSourcer, Hideable {
  /**
   * Debug class used by WebDriver testing.
   */
  public static final String DEBUG_CLASS = "universal-popup";

  /**
   * Make this popup visible.
   */
  void show();

  /**
   * Set the debugClassName for this popup window so that webdriver can
   * identify it.
   * @param dcName debugClassName to be set.
   */
  void setDebugClass(String dcName);

  /**
   * The popup needs to be moved. Re-trigger PopupPositioner.
   */
  void move();

  /**
   * @return the TitleBar for this popup. A new TitleBar will be created if none exists.
   */
  TitleBar getTitleBar();

  /**
   * Adds a new child widget to the popup.
   *
   * @param w the widget to be added.
   */
  void add(Widget w);

  /**
   * Remove a child widget from the popup.
   *
   * @param w the widget to be removed.
   * @return true if the w was a child of this popup.
   */
  boolean remove(Widget w);

  /**
   * Remove all child widgets from the popup.
   */
  void clear();

  /**
   * Associate a widget to this popup - used to avoid hiding when clicking on associated widgets
   */
  void associateWidget(Widget w);

  /**
   * @param isMaskEnabled Whether or not to put a mask over the screen when this
   *        popup is shown which effectively makes the popup modal.
   */
  void setMaskEnabled(boolean isMaskEnabled);
}
