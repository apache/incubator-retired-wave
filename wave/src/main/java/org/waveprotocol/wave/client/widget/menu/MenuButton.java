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

import org.waveprotocol.wave.client.widget.common.ButtonSwitch;
import org.waveprotocol.wave.client.widget.popup.AlignedPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;


/**
 *
 * Combine a {@link ButtonSwitch} and a {@link PopupMenu}
 *
 * TODO(user): Change code that uses this class to use normal buttons and
 * instantiate the popups explicitly so we aren't tied to the implementations
 * used here.
 */
public final class MenuButton implements ButtonSwitch.Listener, Menu {

  /**
   * The switch button that will trigger the menu
   */
  private final ButtonSwitch button;

  /**
   * The menu that will be displayed
   */
  private final PopupMenu menu;

  /**
   * Constructs unlabeled button and menu in 'off' state (i.e., menu is not showing)
   *
   * @param styleName Stylename for both button and menu
   * @param title Title for button (when menu is not shown)
   */
  public MenuButton(String styleName, String title) {
    this(styleName, title, null);
  }

  /**
   * Constructs labeled button and menu in 'off' state (i.e., menu is not showing)
   *
   * @param styleName Stylename for the button
   * @param title Title for button (when menu is not shown)
   * @param label Label for button
   */
  public MenuButton(String styleName, String title, String label) {
    this(styleName, title, label, null);
  }

  /**
   * Constructs labeled button and menu in 'off' state (i.e., menu is not
   * showing)
   *
   * @param styleName Stylename for the button
   * @param title Title for button (when menu is not shown)
   * @param label Label for button
   * @param userPositioner Optional positioner to use instead of the default
   */
  public MenuButton(String styleName, String title, String label,
      RelativePopupPositioner userPositioner) {
    if (label == null) {
      button = new ButtonSwitch(styleName, "Hide menu", title, this);
    } else {
      button = new ButtonSwitch(styleName, "Hide menu", title, this, label);
    }
    button.addStyleName("menu");
    button.setState(false);

    if (userPositioner == null) {
      userPositioner = AlignedPopupPositioner.BELOW_RIGHT;
    }

    menu = new PopupMenu(button.getElement(), userPositioner);
    menu.associateWidget(button);
    menu.addPopupEventListener(new PopupEventListener() {
      public void onHide(PopupEventSourcer source) {
        button.setState(false);
      }

      public void onShow(PopupEventSourcer source) {
        button.setState(true);
      }
    });
  }

  public interface Builder {
    Builder setStyleName(String style);
    Builder setTooltip(String title);
    Builder setLabel(String label);
    Builder setPositioner(RelativePopupPositioner positioner);
    Builder addItem(String text, Command action);
    MenuButton create();
  }

  public static final Builder BUILDER = new Builder() {
    private final Command NO_OP = new Command() {
      public void execute() {}
    };
    private final StringMap<Command> actions = CollectionUtils.createStringMap();
    private final ProcV<Command> actionAdder = new ProcV<Command>() {
      public void apply(String item, Command action) {
        button.addItem(item, action, action != NO_OP);
      }
    };

    private MenuButton button;
    private String label;
    private String style;
    private String tooltip;
    private RelativePopupPositioner positioner;

    @Override
    public Builder addItem(String text, Command action) {
      actions.put(text, action != null ? action : NO_OP);
      return this;
    }

    @Override
    public Builder setLabel(String label) {
      this.label = label;
      return this;
    }

    @Override
    public Builder setStyleName(String style) {
      this.style = style;
      return this;
    }

    @Override
    public Builder setTooltip(String title) {
      this.tooltip = title;
      return this;
    }

    @Override
    public Builder setPositioner(RelativePopupPositioner positioner) {
      this.positioner = positioner;
      return this;
    }

    @Override
    public MenuButton create() {
      button = new MenuButton(style, tooltip, label, positioner);
      actions.each(actionAdder);
      actions.clear();
      style = null;
      tooltip = null;
      label = null;
      positioner = null;
      MenuButton toReturn = button;
      button = null;
      return toReturn;
    }
  };

  /**
   * @return The switch button
   */
  public ButtonSwitch getButton() {
    return button;
  }

  /**
   * {@inheritDoc}
   */
  public void onOff(ButtonSwitch sender) {
    menu.hide();
  }

  /**
   * {@inheritDoc}
   */
  public void onOn(ButtonSwitch sender) {
    menu.show();
  }

  @Override
  public MenuItem addItem(String label, final Command cmd, boolean enabled) {
    return menu.addItem(label, cmd, enabled);
  }

  @Override
  public Pair<Menu, MenuItem> createSubMenu(String title, boolean enabled) {
    return menu.createSubMenu(title, enabled);
  }

  @Override
  public void clearItems() {
    menu.clearItems();
  }

  @Override
  public void addDivider() {
    menu.addDivider();
  }

  @Override
  public void show() {
    menu.show();
  }

  @Override
  public void hide() {
    menu.hide();
  }

  /**
   * Add a PopupEventListener to the menu.
   * @param listener The listener to add.
   */
  public void addPopupEventListener(PopupEventListener listener) {
    menu.addPopupEventListener(listener);
  }

  /**
   * Remove a PopupEventListener to the menu.
   * @param listener The listener to remove.
   */
  public void removePopupEventListener(PopupEventListener listener) {
    menu.removePopupEventListener(listener);
  }
}
