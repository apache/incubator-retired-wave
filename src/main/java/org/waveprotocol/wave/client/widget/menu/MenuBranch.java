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


import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a branch in a menu.
 * This representation is totally independent from its rendering.
 * <p>
 * The root menu is itself modeled as a MenuBranch.
 *
 */

public class MenuBranch {
  /**
   * A <code>MenuBranch</code> is simply a collection of {@link MenuNode}s
   */

  /**
   * List of all menu items.
   */
  private final List<MenuNode> allItems = new ArrayList<MenuNode>();

  /**
   * @return the collection of menu items.
   */
  public List<MenuNode> getAllNodes() {
    return allItems;
  }

  /**
   * Appends a menu item.
   * @param node menu item to append.
   */
  public void append(MenuNode node) {
    allItems.add(node);
  }

  /**
   * Adds a menu item to a specific location.
   * @param index the location to insert the item starting from the left.
   * @param node menu item to add.
   */
  public void add(int index, MenuNode node) {
    allItems.add(index, node);
  }

  /**
   * Appends a sub menu.
   * @param branch sub menu to be appended.
   * @param text name of this sub menu to be rendered.
   */
  public void append(MenuBranch branch, String text) {
    append(branch, text, null, null);
  }

  /**
   * Appends a sub menu.
   * @param branch sub menu to be appended.
   * @param text name of this sub menu to be rendered.
   */
  public void append(MenuBranch branch, String text, String id, String popupDebugClassName) {
    MenuNode node = new MenuNode(text, branch, id, popupDebugClassName);
    allItems.add(node);
  }

  /**
   * Appends a separator symbol.
   */
  public void addDivider() {
    MenuNode node = MenuNode.createDivider();
    allItems.add(node);
  }
}
