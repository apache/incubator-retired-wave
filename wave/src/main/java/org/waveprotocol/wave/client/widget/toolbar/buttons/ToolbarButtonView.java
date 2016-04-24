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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * View interface for a toolbar button.
 *
 * TODO(kalman): Rename to ToolbarButtonView when/if the old way of specifying
 * toolbars is replaced.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface ToolbarButtonView {

  /**
   * The possible display states of a toolbar button.
   */
  enum State {
    ENABLED,
    DISABLED,
    INVISIBLE
  }

  /**
   * Sets the display state,
   */
  void setState(State state);

  /**
   * Sets the text of the button.
   */
  void setText(String text);

  /**
   * Sets the "visual element" of a button, for example an icon or unread count.
   */
  void setVisualElement(Element element);

  /**
   * Sets the hovertext tooltip message for the button.
   */
  void setTooltip(String tooltip);

  /**
   * Sets whether the button should show the dropdown arrow.
   */
  void setShowDropdownArrow(boolean showDropdown);

  /**
   * Sets whether the button should show a divider (placed before the button).
   */
  void setShowDivider(boolean showDivider);

  /**
   * Adds a debug class for the button.
   */
  void addDebugClass(String dc);

  /**
   * Removes a debug class for the button.
   */
  void removeDebugClass(String dc);

  /**
   * Gets the toplevel widget for this toolbar button.
   */
  Widget hackGetWidget();

  /**
   * Implementation which does not set any state and returns a null Widget.
   */
  public static final ToolbarButtonView EMPTY = new ToolbarButtonView() {
    @Override
    public void setState(State state) {
    }

    @Override
    public void setText(String text) {
    }

    @Override
    public void setTooltip(String hovertext) {
    }

    @Override
    public void setVisualElement(Element element) {
    }

    @Override
    public void setShowDropdownArrow(boolean showDropdown) {
    }

    @Override
    public void setShowDivider(boolean showDivider) {
    }

    @Override
    public void addDebugClass(String dc) {
    }

    @Override
    public void removeDebugClass(String dc) {
    }

    @Override
    public Widget hackGetWidget() {
      return null;
    }
  };
}
