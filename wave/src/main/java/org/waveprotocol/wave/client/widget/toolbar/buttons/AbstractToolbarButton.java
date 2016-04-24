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

import org.waveprotocol.wave.client.widget.toolbar.SubmenuItem;

/**
 * An abstract implementation of a toolbar button for the logic shared between
 * {@link ToolbarClickButton}, {@link ToolbarToggleButton}, and
 * {@link org.waveprotocol.wave.client.widget.toolbar.SubmenuToolbarWidget}.
 *   - Necessary interface implementation,
 *   - Delegation to a {@link ToolbarButtonUi} where needed, and
 *   - Propagation of state-setting to parents in a hierarchy.
 *
 * 3 helper methods are provided to subclasses:
 *   - {@link #isDisabled()},
 *   - {@link #getParent()}, and
 *   - {@link #getButton()} (necessary to have public access).
 *
 * Subclasses must explicitly ask to listen to button clicks (and then should
 * also override {@link #onClick()}.  The expectation is that click and toggle
 * buttons will do this, while the submenu (which delegates its button behaviour
 * to a toggle button) will not.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public abstract class AbstractToolbarButton
    implements
    ToolbarButtonView,
    SubmenuItem,
    ToolbarButtonUi.Listener {

  private final ToolbarButtonUi button;
  private SubmenuItem.Parent parent;
  private State state;

  /**
   * Initialises the abstract toolbar button.
   *
   * @param button button to delegate to
   * @param listenToButton whether to listener to events on this button; this
   *        should be true for click and toggle buttons, and false for submenus
   */
  protected AbstractToolbarButton(ToolbarButtonUi button, boolean listenToButton) {
    this.button = button;
    if (listenToButton) {
      button.setListener(this);
    }
  }

  @Override
  public void setParent(SubmenuItem.Parent parent) {
    this.parent = parent;
  }

  @Override
  public void clearParent() {
    parent = null;
  }

  /**
   * @return whether this button is disabled
   */
  protected boolean isDisabled() {
    // If state has never been set then assume !disabled.
    return (state != null) && (state != State.ENABLED);
  }

  /**
   * @return the current action listener, or null if none has been set
   */
  protected SubmenuItem.Parent getParent() {
    return parent;
  }

  /**
   * @return the underlying button implementation
   */
  public ToolbarButtonUi getButton() {
    return button;
  }

  /**
   * {@inheritDoc}
   *
   * Propagates our state change to the parent in addition to the delegate.
   */
  @Override
  public void setState(State state) {
    if (state != this.state) {
      this.state = state;
      button.setState(state);
      if (parent != null) {
        parent.onChildStateChanged(this, state);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * Subclasses must explicitly ask to be added as listeners to the underlying
   * button, and then they should override this method.
   */
  @Override
  public void onClick() {
  }

  //
  // Trivial delegation of ToolbarButtonView from here on.
  //

  @Override
  public void addDebugClass(String dc) {
    button.addDebugClass(dc);
  }

  @Override
  public Widget hackGetWidget() {
    return button.hackGetWidget();
  }

  @Override
  public void removeDebugClass(String dc) {
    button.removeDebugClass(dc);
  }

  @Override
  public void setShowDropdownArrow(boolean showDropdown) {
    button.setShowDropdownArrow(showDropdown);
  }

  @Override
  public void setShowDivider(boolean showDivider) {
    button.setShowDivider(showDivider);
  }

  @Override
  public void setText(String text) {
    button.setText(text);
  }

  @Override
  public void setTooltip(String tooltip) {
    button.setTooltip(tooltip);
  }

  @Override
  public void setVisualElement(Element element) {
    button.setVisualElement(element);
  }
}
