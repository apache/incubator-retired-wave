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

package org.waveprotocol.wave.client.widget.button.icon;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;
import org.waveprotocol.wave.client.widget.button.ButtonDisplay;
import org.waveprotocol.wave.client.widget.button.MouseListener;
import org.waveprotocol.wave.client.widget.button.StyleAxis;

/**
 * Template for a button that is implemented as a single image with no text.
 *
 */
public class IconButtonTemplate extends Composite implements ButtonDisplay, ClickHandler,
    MouseOverHandler, MouseOutHandler, MouseUpHandler, MouseDownHandler {
  /**
   * Separate out the down/hover styling, to allow buttons from outside
   *   this template to be used for icon buttons.
   */
  @Shared
  public interface ButtonStateCss extends CssResource {
    /* Normal state assumed to be an empty modifier */
    String down();
    String hover();
    String disabled();
  }

  /**
   * This is part 1 of the Resource bundle.  The bundle is split
   * into 2 parts to avoid crashing chrome.
   */
  public interface Resources extends ClientBundle {
    // Panel icons.
    @Source("panel-minimize.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMinimize();

    @Source("panel-minimize-down.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMinimizeDown();

    @Source("panel-minimize-hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMinimizeHover();


    @Source("panel-maximize.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMaximize();

    @Source("panel-maximize-down.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMaximizeDown();

    @Source("panel-maximize-hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelMaximizeHover();

    @Source("panel-restore.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelRestore();

    @Source("panel-restore-down.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelRestoreDown();

    @Source("panel-restore-hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelRestoreHover();

    @Source("panel-close.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelClose();

    @Source("panel-close-down.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelCloseDown();

    @Source("panel-close-hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource panelCloseHover();

    @Source("IconButtonTemplate.css")
    Css css();

    interface Css extends ButtonStateCss {
      String panelMinimize();
      String panelMaximize();
      String panelRestore();
      String panelClose();
    }
  }

  /**
   * This is part 2 of the Resource bundle.  The bundle is split
   * into 2 parts to avoid crashing chrome.
   */
  public interface Resources1 extends ClientBundle {
    @Source("blue-plus.png")
    @ImageOptions(flipRtl = true)
    ImageResource bluePlus();

    @Source("folder-closed.png")
    @ImageOptions(flipRtl = true)
    ImageResource folderClosed();

    @Source("folder-open.png")
    @ImageOptions(flipRtl = true)
    ImageResource folderOpen();


    @Source("alert_close_button.png")
    @ImageOptions(flipRtl = true)
    ImageResource alertClose();

    @Source("alert_close_button_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource alertCloseDown();

    @Source("alert_close_button_hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource alertCloseHover();

    @Source("popup-button.png")
    @ImageOptions(flipRtl = true)
    ImageResource popupButtonImage();

    @Source("view_switcher_feed.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherFeed();

    @Source("view_switcher_feed_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherFeedDown();

    @Source("view_switcher_single.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherSingle();

    @Source("view_switcher_single_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherSingleDown();

    @Source("view_switcher_avatar.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherAvatar();

    @Source("view_switcher_avatar_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource viewSwitcherAvatarDown();

    @Source("spelly-dropdown.png")
    @ImageOptions(flipRtl = true)
    ImageResource spellyDropdown();

    @Source("button_digest_next.png")
    @ImageOptions(flipRtl = true)
    ImageResource nextDigestPage();

    @Source("button_digest_next_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource nextDigestPageDown();

    @Source("button_digest_prev.png")
    @ImageOptions(flipRtl = true)
    ImageResource previousDigestPage();

    @Source("button_digest_prev_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource previousDigestPageDown();

    @Source("lightbulb.png")
    @ImageOptions(flipRtl = true)
    ImageResource lightbulb();

    @Source("add.png")
    @ImageOptions(flipRtl = true)
    ImageResource add();

    @Source("addDown.png")
    @ImageOptions(flipRtl = true)
    ImageResource addDown();

    @Source("add-big.png")
    @ImageOptions(flipRtl = true)
    ImageResource addBig();

    @Source("add-big-down.png")
    @ImageOptions(flipRtl = true)
    ImageResource addBigDown();

    @Source("add_small.png")
    @ImageOptions(flipRtl = true)
    ImageResource addSmall();

    @Source("add_small_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource addSmallDown();

    @Source("remove_tag_button.png")
    @ImageOptions(flipRtl = true)
    ImageResource removeTag();

    @Source("remove_tag_button_hover.png")
    @ImageOptions(flipRtl = true)
    ImageResource removeTagHover();

    @Source("split_button_dropdown.png")
    @ImageOptions(flipRtl = true)
    ImageResource splitButtonDropdown();

    @Source("split_button_dropdown_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource splitButtonDropdownDown();


    @Source("IconButtonTemplate1.css")
    Css1 css();

    interface Css1 extends ButtonStateCss {
      String bluePlus();
      String folderExpand();
      String alertClose();
      String viewSwitcherAvatar();
      String viewSwitcherSingle();
      String nextDigestPage();
      String previousDigestPage();
      String spellyDropdown();
      String lightbulb();
      String removeTag();

      String popupButton();
      String viewSwitcherFeed();
      String splitButtonDropdown();
      String add();
      String addBig();
      String addSmall();

      String cursorDefault();
      String cursorPointer();
    }
  }
  /**
   * The styles that this kind of button can have.
   *
   */
  public static enum IconButtonStyle {
    BLUE_PLUS, PANEL_MINIMIZE, PANEL_RESTORE, PANEL_MAXIMIZE, PANEL_CLOSE,
    PLUS_MINUS, POPUP_MENU, ALERT_CLOSE, VIEW_SWITCHER_AVATAR,
    VIEW_SWITCHER_FEED, VIEW_SWITCHER_SINGLE, NEXT_DIGEST_PAGE, PREVIOUS_DIGEST_PAGE,
    SPELLY_DROPDOWN, LIGHTBULB, ADD, ADD_BIG, ADD_SMALL, REMOVE_TAG,
    SPLIT_BUTTON_DROPDOWN
  }

  /** The singleton instance of our resources. */
  private static final Resources RESOURCES = GWT.create(Resources.class);
  /** The singleton instance of our resources. */
  private static final Resources1 RESOURCES1 = GWT.create(Resources1.class);

  /**
   * The controller of this UI widget.
   */
  private MouseListener mouseListener;

  /**
   * The CSS class that describes the current state of the button.
   */
  private StyleAxis buttonStateClassName;

  /**
   * The CSS class that describes the style of the icon.
   */
  private StyleAxis iconStyleClassName;

  /**
   * The last debug class name set relating to the state.
   */
  private String stateDebugClassName = "";

  /**
   * The CSS class that describes the cursor for this icon.
   */
  private StyleAxis cursorStyleClassName;

  @UiField HTMLPanel icon;

  static {
    StyleInjector.inject(RESOURCES.css().getText(), true);
    StyleInjector.inject(RESOURCES1.css().getText(), true);
  }

  /**
   * @param style Dictates what the button should look like.
   * @param tooltip The tooltip for this button.
   */
  public IconButtonTemplate(IconButtonStyle style, String tooltip) {
    this();
    init(style, tooltip);
  }

  @UiConstructor
  public IconButtonTemplate() {
    Label label = new Label();
    initWidget(label);
    label.addClickHandler(this);
    label.addMouseOverHandler(this);
    label.addMouseOutHandler(this);
    label.addMouseUpHandler(this);
    label.addMouseDownHandler(this);
  }

  /**
   * Initialize the state of this widget.
   *
   * @param style Dictates what the button should look like.
   * @param tooltip The tooltip for this button.
   */
  public void init(IconButtonStyle style, String tooltip) {
    buttonStateClassName = new StyleAxis(getElement());
    iconStyleClassName = new StyleAxis(getElement());
    cursorStyleClassName = new StyleAxis(getElement());
    cursorStyleClassName.setStyle(RESOURCES1.css().cursorPointer());
    setStyle(style);
    setTitle(tooltip);
  }

  /**
   * TODO(user) to make the style final once UiBinder is in.
   *
   * @param style The {@link IconButtonStyle} this button should adopt.
   */
  public void setStyle(IconButtonStyle style) {
    String styleName = null;
    switch (style) {
      case PANEL_CLOSE:
        styleName = RESOURCES.css().panelClose();
        DebugClassHelper.addDebugClass(this, "close");
        break;
      case PANEL_MAXIMIZE:
        styleName = RESOURCES.css().panelMaximize();
        DebugClassHelper.addDebugClass(this, "maximise");
        break;
      case PANEL_MINIMIZE:
        styleName = RESOURCES.css().panelMinimize();
        DebugClassHelper.addDebugClass(this, "minimise");
        break;
      case PANEL_RESTORE:
        styleName = RESOURCES.css().panelRestore();
        DebugClassHelper.addDebugClass(this, "restore");
        break;
      case PLUS_MINUS:
        styleName = RESOURCES1.css().folderExpand();
        break;
      case POPUP_MENU:
        styleName = RESOURCES1.css().popupButton();
        break;
      case ALERT_CLOSE:
        styleName = RESOURCES1.css().alertClose();
        break;
      case BLUE_PLUS:
        styleName = RESOURCES1.css().bluePlus();
        break;
      case VIEW_SWITCHER_AVATAR:
        styleName = RESOURCES1.css().viewSwitcherAvatar();
        break;
      case VIEW_SWITCHER_FEED:
        styleName = RESOURCES1.css().viewSwitcherFeed();
        break;
      case VIEW_SWITCHER_SINGLE:
        styleName = RESOURCES1.css().viewSwitcherSingle();
        break;
      case SPELLY_DROPDOWN:
        styleName = RESOURCES1.css().spellyDropdown();
        break;
      case NEXT_DIGEST_PAGE:
        styleName = RESOURCES1.css().nextDigestPage();
        break;
      case PREVIOUS_DIGEST_PAGE:
        styleName = RESOURCES1.css().previousDigestPage();
        break;
      case LIGHTBULB:
        styleName = RESOURCES1.css().lightbulb();
        break;
      case REMOVE_TAG:
        styleName = RESOURCES1.css().removeTag();
        break;
      case SPLIT_BUTTON_DROPDOWN:
        styleName = RESOURCES1.css().splitButtonDropdown();
        break;
      case ADD:
        styleName = RESOURCES1.css().add();
        break;
      case ADD_BIG:
        styleName = RESOURCES1.css().addBig();
        break;
      case ADD_SMALL:
        styleName = RESOURCES1.css().addSmall();
        break;
    }
    iconStyleClassName.setStyle(styleName);
  }

  /** {@inheritDoc} */
  public void setState(ButtonState state) {
    String styleName = null;
    String newStateDebugClassName = "";
    switch (state) {
      case DISABLED:
        newStateDebugClassName = "disabled";
        styleName = internalStateCss.disabled();
        cursorStyleClassName.setStyle(RESOURCES1.css().cursorDefault());
        break;
      case DOWN:
        newStateDebugClassName = "down";
        styleName = internalStateCss.down();
        break;
      case HOVER:
        newStateDebugClassName = "hover";
        styleName = internalStateCss.hover();
        break;
      case NORMAL:
        newStateDebugClassName = "normal";
        styleName = null;
        break;
    }
    DebugClassHelper.replaceDebugClass(getElement(),
        "ibts_" + stateDebugClassName, "ibts_" + newStateDebugClassName);
    stateDebugClassName = newStateDebugClassName;
    buttonStateClassName.setStyle(styleName);
  }

  /** {@inheritDoc} */
  public void setUiListener(MouseListener mouseListener) {
    this.mouseListener = mouseListener;
  }

  @Override
  public void setTooltip(String tooltip) {
    setTitle(tooltip);
  }

  @Override
  public void setText(String text) {
    ((Label) getWidget()).setText(text);
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    if (mouseListener != null) {
      mouseListener.onMouseLeave();
    }
  }

  /**
   * Opens up the ability to use a style (= icon) not within the control of this template
   * Requires using style sheet with .down and .hover styles
   */
  private ButtonStateCss internalStateCss = RESOURCES.css(); // by default, do the same as always

  /**
   * Creates a new {@link IconButtonTemplate} with the given style and logic,
   *   taking the style name directly
   *
   * @param styleName Dictates what the button should look like.
   * @param tooltip The tooltip for this button.
   * @return A new {@link IconButtonTemplate} that behaves and looks as
   *         described.
   */
  public static IconButtonTemplate createDirect(String styleName, String tooltip,
                                                ButtonStateCss stateCss) {
    IconButtonTemplate template = new IconButtonTemplate();
    template.initDirect(styleName, tooltip, stateCss);
    return template;
  }

  /**
   * Initialize the state of this widget, taking the style name directly
   *
   * @param styleName Dictates what the button should look like.
   * @param tooltip The tooltip for this button.
   */
  private void initDirect(String styleName, String tooltip, ButtonStateCss stateCss) {
    internalStateCss = stateCss;
    buttonStateClassName = new StyleAxis(getElement());
    iconStyleClassName = new StyleAxis(getElement());
    iconStyleClassName.setStyle(styleName);
    setTitle(tooltip);
  }

  @Override
  public void onClick(ClickEvent event) {
    mouseListener.onClick();
    event.stopPropagation();
  }

  @Override
  public void onMouseOver(MouseOverEvent event) {
    mouseListener.onMouseEnter();
  }

  @Override
  public void onMouseOut(MouseOutEvent event) {
    mouseListener.onMouseLeave();
  }

  @Override
  public void onMouseUp(MouseUpEvent event) {
    mouseListener.onMouseUp();
  }

  @Override
  public void onMouseDown(MouseDownEvent event) {
    mouseListener.onMouseDown();
  }
}
