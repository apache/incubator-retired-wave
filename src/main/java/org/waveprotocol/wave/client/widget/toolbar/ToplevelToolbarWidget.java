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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.overflowpanel.OverflowPanelUpdater;
import org.waveprotocol.wave.client.widget.overflowpanel.OverflowPanelUpdater.OverflowPanel;
import org.waveprotocol.wave.client.widget.toolbar.buttons.AbstractToolbarButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.HorizontalToolbarButtonWidget;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonUiProxy;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView.State;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.VerticalToolbarButtonWidget;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.List;

/**
 * A {@link ToolbarView} to be used as a top-level widget.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ToplevelToolbarWidget extends Composite
    implements
    GroupingToolbar.View,
    OverflowPanel,
    SubmenuItem.Parent {

  interface Resources extends ClientBundle {
    @Source("ToplevelToolbarWidget.css")
    Css css();

    @Source("button_fill.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource fillImage();

    @Source("toolbar_more_button.png")
    ImageResource overflowButtonIcon();
  }

  interface Css extends CssResource {
    String toolbar();
    String overflowButton();
    String overflowButtonIcon();
  }

  /**
   * An item in the toolbar.
   */
  private static final class Item {
    /** The item's widget in the toplevel toolbar. */
    public final HorizontalToolbarButtonWidget onToplevel;

    /** The item's widget in the overflow toolbar. */
    public final VerticalToolbarButtonWidget onOverflow;

    /**
     * The proxy of the item, always delegates to either {@link #onToplevel} or
     * {@link #onOverflow} depending on the placement of the item.
     */
    public final ToolbarButtonUiProxy proxy;

    /**
     * The item as an {@link AbstractToolbarButton}, the component of the item
     * returned to callers that add buttons and submenus. Maintained in order to
     * change parents as the item overflows (and un-overflows), and to remove
     * the item.
     */
    public AbstractToolbarButton asAbstractButton = null;

    public Item(HorizontalToolbarButtonWidget onToplevel, VerticalToolbarButtonWidget onOverflow,
        ToolbarButtonUiProxy proxy) {
      this.onToplevel = onToplevel;
      this.onOverflow = onOverflow;
      this.proxy = proxy;
    }
  }

  interface Binder extends UiBinder<Widget, ToplevelToolbarWidget> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  static final Resources res = GWT.create(Resources.class);
  static {
    StyleInjector.inject(res.css().getText(), true);
  }

  @UiField FlowPanel self;
  @UiField SimplePanel overflowButton;

  /** The overflow submenu. */
  private final SubmenuToolbarWidget overflowSubmenu =
      new SubmenuToolbarWidget(new ToolbarToggleButton(new HorizontalToolbarButtonWidget()));

  /** The logic for controlling which items show in the overflow submenu. */
  private final OverflowPanelUpdater overflowLogic;

  /** Items in the menu. */
  private final List<Item> items = CollectionUtils.newArrayList();

  public ToplevelToolbarWidget() {
    initWidget(BINDER.createAndBindUi(this));
    overflowButton.setWidget(overflowSubmenu.hackGetWidget());
    overflowSubmenu.addDebugClass("more");
    // Build the "..." icon.
    Element icon = DOM.createDiv();
    icon.setClassName(res.css().overflowButtonIcon());
    overflowSubmenu.setVisualElement(icon);
    overflowSubmenu.setShowDivider(true);
    // Attach overflow logic.
    overflowLogic = new OverflowPanelUpdater(this);
  }

  //
  // For vanilla ToolbarView
  //

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
  public SubmenuToolbarView addSubmenu() {
    SubmenuToolbarView submenu = insertSubmenu(items.size());
    showDividerIfNotFirst(submenu);
    return submenu;
  }

  private void showDividerIfNotFirst(ToolbarButtonView button) {
    if (button.hackGetWidget() != items.get(0).onToplevel) {
      button.setShowDivider(true);
    }
  }

  @Override
  public ToolbarView addGroup() {
    return new GroupingToolbar(this, addFakeItem());
  }

  /**
   * Adds a fake item that isn't rendered but still has an entry in items
   * (so that it plays correctly with overflowing, etc).
   */
  private ToolbarButtonView addFakeItem() {
    // NOTE: simplest way to add a fake item is to add an invisible button.
    ToolbarButtonView fakeButton = addClickButton();
    fakeButton.setState(State.INVISIBLE);
    return fakeButton;
  }

  @Override
  public void clearItems() {
    self.clear();
    items.clear();
  }

  //
  // For GroupingToolbar.View
  //

  @Override
  public ToolbarClickButton insertClickButton(int beforeIndex) {
    Item item = insertItem(beforeIndex);
    ToolbarClickButton button = new ToolbarClickButton(item.proxy);
    item.asAbstractButton = button;
    return button;
  }

  @Override
  public ToolbarToggleButton insertToggleButton(int beforeIndex) {
    Item item = insertItem(beforeIndex);
    ToolbarToggleButton button = new ToolbarToggleButton(item.proxy);
    item.asAbstractButton = button;
    return button;
  }

  @Override
  public SubmenuToolbarView insertSubmenu(int beforeIndex) {
    Item item = insertItem(beforeIndex);
    SubmenuToolbarWidget submenu = new SubmenuToolbarWidget(new ToolbarToggleButton(item.proxy));
    submenu.setShowDropdownArrow(true);
    item.asAbstractButton = submenu;
    return submenu;
  }

  @Override
  public int indexOf(ToolbarButtonView button) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).asAbstractButton == button) {
        return i;
      }
    }
    throw new IllegalArgumentException("button is not in this toolbar");
  }

  /**
   * Adds a new item to the toolbar, handling its placement in both the toplevel
   * and overflow toolbars.
   */
  private Item insertItem(int beforeIndex) {
    // The widget for the toplevel toolbar.
    HorizontalToolbarButtonWidget toplevelButton = new HorizontalToolbarButtonWidget();
    self.insert(toplevelButton, beforeIndex);
    overflowLogic.updateStateEventually();

    // The widget for the overflow toolbar.  Construct manually and use
    // hackAddWidget so that the ToolbarButtonViewProxy can manage the state
    // of the submenus correctly.
    VerticalToolbarButtonWidget overflowButton = new VerticalToolbarButtonWidget();
    overflowSubmenu.hackInsertWidget(overflowButton, beforeIndex);

    // Return the item, initially proxying the toplevel button.
    Item item = new Item(toplevelButton, overflowButton, new ToolbarButtonUiProxy(toplevelButton));
    items.add(beforeIndex, item);
    return item;
  }

  //
  // Resizing
  //

  public void onResizeDone() {
    overflowLogic.updateStateEventually();
  }

  //
  // OverflowPanel
  //

  @Override
  public boolean hasOverflowed(int index) {
    return items.get(index).onToplevel.getElement().getOffsetTop() > 0;
  }

  @Override
  public boolean isVisible(int index) {
    return items.get(index).proxy.hackGetState() != State.INVISIBLE;
  }

  @Override
  public void moveToOverflowBucket(int index) {
    Item item = items.get(index);
    // The item is in the overflow menu now, so parent is the overflow submenu.
    item.asAbstractButton.setParent(overflowSubmenu);
    // ... and so is the proxy.
    item.proxy.setDelegate(item.onOverflow);
    // Even though there is overflow: hidden, explicitly hide the toplevel
    // buttons so that fast resize events don't look strange (e.g. wrong icon
    // placement).
    item.onToplevel.setState(State.INVISIBLE);
    // Force a state change event to possibly enable the overflow submenu.
    overflowSubmenu.onChildStateChanged(item.asAbstractButton, item.proxy.hackGetState());
  }

  @Override
  public void onBeginOverflowLayout() {
    // Reset all the items to the toplevel and hide the overflow submenu;
    // very difficult to calculate overflow without a consistent state.
    for (Item item : items) {
      // The item is in the toplevel now, so the toplevel is the parent.
      item.asAbstractButton.setParent(this);
      // ... and the proxy is on the toplevel.
      item.proxy.setDelegate(item.onToplevel);
      // Item now invisible on the overflow menu.  This is done to all
      // items of course, so by the end the overflow submenu will think that
      // all buttons are disabled.
      item.onOverflow.setState(State.INVISIBLE);
    }
    overflowSubmenu.setState(State.INVISIBLE);
  }

  @Override
  public void onEndOverflowLayout() {
    // Hide the divider of the first overflowed button, if any.  Since this is
    // set on the button itself and not the proxy, the divider state will be
    // reset to the correct value next time there is an overflow.
    for (Item item : items) {
      if (item.proxy.getDelegate() == item.onOverflow) {
        item.onOverflow.setShowDivider(false);
        // TODO(kalman): Don't hide all the dividers.
        //break;
      }
    }
  }

  @Override
  public void showMoreButton() {
    // This is called before moving any items to the overflow panel; doing so
    // will result in the submenu enabling itself so long as the button states
    // are kept up to date.  So, for now, just make it visible and disabled.
    overflowSubmenu.setState(State.DISABLED);
  }

  @Override
  public int getWidgetCount() {
    return items.size();
  }

  //
  // SubmenuItem.Parent
  //

  @Override
  public void onChildStateChanged(SubmenuItem item, State newState) {
    overflowLogic.updateStateEventually();
  }

  @Override
  public void onActionPerformed() {
  }
}
