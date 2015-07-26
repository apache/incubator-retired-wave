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

import org.waveprotocol.wave.client.widget.menu.MenuNode.MenuCommand;
import org.waveprotocol.wave.model.util.Pair;

import java.util.List;

/**
 * Utility class for populating a Menu from the contents of a MenuBranch.
 *
 */
public class MenuBuilder {
  /**
   * Wraps a MenuCommand so that it can be used as a Command.
   */
  private static class MenuCommandAdapter implements Command {
    private final MenuCommand command;

    public MenuCommandAdapter(MenuCommand command) {
      this.command = command;
    }

    @Override
    public void execute() {
      command.execute();
    }
  }

  /**
   * Renders vertical sub-menus. This does not handle recursive menus.
   *
   * @param popupMenu is the destination UI component to be rendered.
   * @param menu is the sub-menu to be rendered into <code>popupMenu</code>.
   */
  public static void loadMenu(final Menu popupMenu, MenuBranch menu) {
    List<MenuNode> allNodes = menu.getAllNodes();

    for (MenuNode node : allNodes) {
      if (node.isSeparator()) {
        popupMenu.addDivider();
      } else if (node.getSubMenu() != null) {
        Pair<Menu, MenuItem> subMenu = popupMenu.createSubMenu(node.getText(), true);
        Menu mm = subMenu.getFirst();
        if (mm instanceof PopupMenu) {
          PopupMenu pp = (PopupMenu) mm;
          pp.setDebugClass(node.getPopupDebugClassName());
        }
        String itemId = node.getId();
        if (null != itemId && itemId.length() > 0) {
          MenuItem mi = subMenu.getSecond();
          mi.setDebugClassTODORename(itemId);
        }
        loadMenu(subMenu.getFirst(), node.getSubMenu());
      } else {
        popupMenu.addItem(node.getText(), new MenuCommandAdapter(node.getCommand()), true);
      }
    }
  }
}
