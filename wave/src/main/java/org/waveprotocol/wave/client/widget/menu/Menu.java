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

package org.waveprotocol.wave.client.widget.menu;

import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.model.util.Pair;

/**
 * Simple abstract interface to a menu.  Unlike
 * {@link org.waveprotocol.oldwave.client.editor.sugg.Menu}, this interface does not depend
 * on GWT {@code MenuItem}s
 *
 */
public interface Menu {

  /**
   * Add an item to the menu.
   *
   * @param title Title of the item
   * @param callback Code to execute when the item is selected
   * @param enabled Whether to initialise the item as enabled or disabled
   * @return A MenuItem representing the added item
   */
  MenuItem addItem(String title, Command callback, boolean enabled);

  /**
   * Create a submenu for this menu.
   * @param title Title of the submenu.
   * @return A a new Menu and its parent MenuItem.
   */
  Pair<Menu, MenuItem> createSubMenu(String title, boolean enabled);

  /**
   * Remove all items from the menu.
   */
  void clearItems();

  /**
   * Add a divider to the menu.
   */
  void addDivider();

  /**
   * Show the menu.
   */
  void show();

  /**
   * Hide the menu.
   */
  void hide();
}
