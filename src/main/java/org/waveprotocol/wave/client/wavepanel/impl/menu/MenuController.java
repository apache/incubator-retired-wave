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

package org.waveprotocol.wave.client.wavepanel.impl.menu;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.WaveMouseDownHandler;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions;
import org.waveprotocol.wave.client.wavepanel.impl.menu.i18n.MenuMessages;
import org.waveprotocol.wave.client.wavepanel.view.BlipMenuItemView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;

/**
 * Interprets user gestures on blip menu items.
 *
 */
public final class MenuController implements WaveMouseDownHandler {
  private final DomAsViewProvider panel;
  private final Actions actions;
  private final MenuMessages messages;

  /**
   * Creates a manu handler.
   *
   * @param actions
   * @param panel
   */
  private MenuController(Actions actions, DomAsViewProvider panel, MenuMessages messages) {
    this.actions = actions;
    this.panel = panel;
    this.messages = messages;
  }

  /**
   * Installs the focus-frame feature in a wave panel.
   */
  public static void install(Actions handler, WavePanel panel, MenuMessages messages) {
    MenuController controller = new MenuController(handler, panel.getViewProvider(), messages);
    panel.getHandlers().registerMouseDownHandler(TypeCodes.kind(Type.MENU_ITEM), controller);
  }

  @Override
  public boolean onMouseDown(MouseDownEvent event, Element context) {
    if (event.getNativeButton() != NativeEvent.BUTTON_LEFT) {
      return false;
    }
    BlipMenuItemView item = panel.asBlipMenuItem(context);
    switch (item.getOption()) {
      case EDIT:
        actions.startEditing(item.getParent().getParent());
        break;
      case EDIT_DONE:
        actions.stopEditing();
        break;
      case REPLY:
        actions.reply(item.getParent().getParent());
        break;
      case DELETE:
        // We delete the blip without confirmation if shift key is pressed
        if (event.getNativeEvent().getShiftKey() || Window.confirm(messages.confirmDeletion())) {
          actions.delete(item.getParent().getParent());
        }
        break;
      case LINK:
        actions.popupLink(item.getParent().getParent());
        break;
      default:
        throw new AssertionError();
    }
    event.preventDefault();
    return true;
  }
}
