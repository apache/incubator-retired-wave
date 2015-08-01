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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.client.widget.toolbar.buttons.AbstractToolbarButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarPopupToggler;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.VerticalToolbarButtonWidget;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;

import java.util.List;

/**
 * A toolbar within a submenu.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class SubmenuToolbarWidget extends AbstractToolbarButton
    implements
    SubmenuToolbarView,
    GroupingToolbar.View,
    SubmenuItem.Parent,
    ToolbarPopupToggler.Listener {

  interface Resources extends ClientBundle {
    @Source("SubmenuToolbarWidget.css")
    Css css();
  }

  interface Css extends CssResource {
    String toolbar();
  }

  static final Resources res = GWT.create(Resources.class);
  static {
    StyleInjector.inject(res.css().getText(), true);
  }

  /** All items in the toolbar. */
  private final List<ToolbarButtonView> items = CollectionUtils.newArrayList();

  /** The set of child items which are enabled. */
  private final IdentitySet<SubmenuItem> enabledChildren = CollectionUtils.createIdentitySet();

  /** The contents of the popup. */
  private final FlowPanel panel = new FlowPanel();

  /** The active popup, maintained so that button actions can hide it. */
  private UniversalPopup activePopup = null;

  /** Listener for the submenu being shown/hidden. */
  private Listener listener = null;

  SubmenuToolbarWidget(ToolbarToggleButton button) {
    // NOTE: don't let AbstractToolbarButton add a click listener for the
    // button, this would override the existing click listener added for the
    // toggle button.
    super(button.getButton(), false);
    ToolbarPopupToggler.associate(button, this);
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public ToolbarClickButton addClickButton() {
    ToolbarClickButton button = insertClickButton(items.size());
    showDividerIfNotFirst(button);
    return button;
  }

  @Override
  public ToolbarToggleButton addToggleButton() {
    ToolbarToggleButton button = insertToggleButton(items.size());
    showDividerIfNotFirst(button);
    return button;
  }

  @Override
  public SubmenuToolbarWidget addSubmenu() {
    SubmenuToolbarWidget submenu = insertSubmenu(items.size());
    showDividerIfNotFirst(submenu);
    return submenu;
  }

  private void showDividerIfNotFirst(ToolbarButtonView button) {
    if (button != items.get(0)) {
      button.setShowDivider(true);
    }
  }

  @Override
  public ToolbarView addGroup() {
    return new GroupingToolbar(this, addFakeItem());
  }

  /**
   * Adds a fake item that isn't rendered but still has an entry in items.
   */
  private ToolbarButtonView addFakeItem() {
    // NOTE: simplest way to add a fake item is to add an invisible button.
    ToolbarButtonView fakeButton = addClickButton();
    fakeButton.setState(State.INVISIBLE);
    return fakeButton;
  }

  @Override
  public void clearItems() {
    panel.clear();
    enabledChildren.clear();
    items.clear();
    setState(State.DISABLED);
  }

  //
  // For GroupingToolbar.View
  //

  @Override
  public ToolbarClickButton insertClickButton(int beforeIndex) {
    ToolbarClickButton button = new ToolbarClickButton(new VerticalToolbarButtonWidget());
    insertAbstractButton(button, beforeIndex);
    return button;
  }

  @Override
  public ToolbarToggleButton insertToggleButton(int beforeIndex) {
    ToolbarToggleButton button = new ToolbarToggleButton(new VerticalToolbarButtonWidget());
    insertAbstractButton(button, beforeIndex);
    return button;
  }

  private void insertAbstractButton(AbstractToolbarButton button, int beforeIndex) {
    button.setParent(this);
    panel.insert(button.hackGetWidget(), beforeIndex);
    items.add(beforeIndex, button);
    enabledChildren.add(button);
    setState(State.ENABLED);
  }

  @Override
  public SubmenuToolbarWidget insertSubmenu(int beforeIndex) {
    ToolbarToggleButton submenuButton = new ToolbarToggleButton(new VerticalToolbarButtonWidget());
    submenuButton.setShowDropdownArrow(true);
    panel.insert(submenuButton.hackGetWidget(), beforeIndex);
    SubmenuToolbarWidget submenu = new SubmenuToolbarWidget(submenuButton);
    // NOTE: don't set action listener or parent on the submenu button, set on
    // the submenu itself.
    submenu.setParent(this);
    items.add(beforeIndex, submenu);
    return submenu;
  }

  @Override
  public int indexOf(ToolbarButtonView button) {
    return items.indexOf(button);
  }

  /**
   * Directly inserts a widget. Package-private for the overflow submenu.
   */
  void hackInsertWidget(Widget w, int beforeIndex) {
    panel.insert(w, beforeIndex);
  }

  //
  // Popup related
  //

  @Override
  public void onPopupCreated(UniversalPopup popup) {
    popup.add(panel);
    activePopup = popup;
    if (listener != null) {
      listener.onSubmenuShown();
    }
  }

  @Override
  public void onPopupDestroyed(UniversalPopup popup) {
    activePopup = null;
    if (listener != null) {
      listener.onSubmenuHidden();
    }
  }

  //
  // SubmenuItem.Parent
  //

  @Override
  public void onActionPerformed() {
    if (activePopup != null) {
      activePopup.hide();
      if (getParent() != null) {
        getParent().onActionPerformed();
      }
    }
  }

  @Override
  public void onChildStateChanged(SubmenuItem item, State newState) {
    if (newState == State.ENABLED) {
      enabledChildren.add(item);
      setState(State.ENABLED);
    } else {
      enabledChildren.remove(item);
      setState(enabledChildren.isEmpty() ? State.DISABLED : State.ENABLED);
    }
  }
}
