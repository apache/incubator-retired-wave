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

import com.google.common.base.Preconditions;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;

/**
 * Decorates a toolbar with grouping functionality.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class GroupingToolbar implements ToolbarView {

  /**
   * The view that toolbars must implement to be groupable.
   */
  public interface View extends ToolbarView {
    /**
     * Inserts a click button before a given index.
     */
    public ToolbarClickButton insertClickButton(int beforeIndex);

    /**
     * Inserts a toggle button before a given index.
     */
    public ToolbarToggleButton insertToggleButton(int beforeIndex);

    /**
     * Inserts a submenu before a given index.
     */
    public SubmenuToolbarView insertSubmenu(int beforeIndex);

    /**
     * Gets the index of a given toolbar item.
     */
    public int indexOf(ToolbarButtonView button);
  }

  /** The decorated toolbar. */
  private final View toolbar;

  /** The stub item, used as a placeholder for the start of the group. */
  private final ToolbarButtonView stubItem;

  /** The current size of the group, not including the stub. */
  private int size = 0;

  /**
   * Creates a new group of buttons for a toolbar.
   *
   * @param toolbar the toolbar this group is in
   * @param stubItem used as a placeholder for the start of the group
   */
  public GroupingToolbar(View toolbar, ToolbarButtonView stubItem) {
    Preconditions.checkNotNull(stubItem, "stub item cannot be null");
    this.toolbar = toolbar;
    this.stubItem = stubItem;
  }

  @Override
  public ToolbarClickButton addClickButton() {
    ToolbarClickButton button = toolbar.insertClickButton(getAndIncrementNextIndex());
    showDividerIfSingleton(button);
    return button;
  }

  @Override
  public ToolbarToggleButton addToggleButton() {
    ToolbarToggleButton button = toolbar.insertToggleButton(getAndIncrementNextIndex());
    showDividerIfSingleton(button);
    return button;
  }

  @Override
  public SubmenuToolbarView addSubmenu() {
    SubmenuToolbarView submenu = toolbar.insertSubmenu(getAndIncrementNextIndex());
    showDividerIfSingleton(submenu);
    return submenu;
  }

  private int getAndIncrementNextIndex() {
    int nextIndex = toolbar.indexOf(stubItem) + size + 1;
    size++;
    return nextIndex;
  }

  /**
   * Shows the divider if this is the first button added to the toolbar, and
   * if (aesthetic tweak) it isn't the first item in the toolbar.
   */
  private void showDividerIfSingleton(ToolbarButtonView button) {
    // NOTE: use > 1 (rather than > 0) because there will always be a stub at
    // the start of the group.
    if (size == 1 && toolbar.indexOf(button) > 1) {
      button.setShowDivider(true);
    }
  }

  @Override
  public ToolbarView addGroup() {
    throw new UnsupportedOperationException("Cannot add a group within a group");
  }

  @Override
  public void clearItems() {
    throw new UnsupportedOperationException("Cannot clear items from a group");
  }
}
