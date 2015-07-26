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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;
import org.waveprotocol.wave.client.widget.common.ImplPanel;

/**
 * Widget implementation of a {@link ToolbarButtonView} for displaying
 * in a vertical toolbar.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class VerticalToolbarButtonWidget extends Composite implements ToolbarButtonUi {

  interface Resources extends ClientBundle {
    @Source("VerticalToolbarButtonWidget.css")
    Css css();

    @Source("button_down_large.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource buttonDown();

    @Source("arrow_vertical.png")
    ImageResource dropdownArrow();
  }

  interface Css extends CssResource {
    String self();
    String content();
    String visualElement();
    String textElement();
    String textElementWithVisualElement();
    String enabled();
    String hidden();
    String down();
    String dropdownArrow();
    String divider();
  }

  interface Binder extends UiBinder<Widget, VerticalToolbarButtonWidget> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  static final Resources res = GWT.create(Resources.class);
  static {
    StyleInjector.inject(res.css().getText(), true);
  }

  @UiField HTMLPanel self;
  @UiField ImplPanel content;
  @UiField Element visualElement;
  @UiField Element textElement;
  @UiField Element dropdownArrow;
  @UiField Element divider;

  private Listener listener = null;
  private Element currentVisualElement = null;

  public VerticalToolbarButtonWidget() {
    initWidget(BINDER.createAndBindUi(this));
  }

  @UiHandler("content")
  void handleButtonClicked(ClickEvent e) {
    if (listener != null) {
      listener.onClick();
    }
  }

  @UiHandler("content")
  void handleMouseDown(MouseDownEvent e) {
    // Prevent the editor from losing selection focus.
    e.preventDefault();
    e.stopPropagation();
  }

  @Override
  public void setState(State state) {
    switch (state) {
      case ENABLED:
        self.getElement().removeClassName(res.css().hidden());
        self.getElement().addClassName(res.css().enabled());
        removeDebugClass("disabled");
        break;

      case DISABLED:
        self.getElement().removeClassName(res.css().hidden());
        self.getElement().removeClassName(res.css().enabled());
        addDebugClass("disabled");
        break;

      case INVISIBLE:
        self.getElement().addClassName(res.css().hidden());
        break;
    }
  }

  @Override
  public void setText(String text) {
    textElement.setInnerText(text);
  }

  @Override
  public void setTooltip(String tooltip) {
    setTitle(tooltip);
  }

  @Override
  public void setVisualElement(Element element) {
    if (currentVisualElement != null) {
      currentVisualElement.removeFromParent();
    }
    visualElement.appendChild(element);
    currentVisualElement = element;
    textElement.addClassName(res.css().textElementWithVisualElement());
  }

  @Override
  public void setDown(boolean isDown) {
    if (isDown) {
      self.addStyleName(res.css().down());
    } else {
      self.removeStyleName(res.css().down());
    }
  }

  @Override
  public void setShowDropdownArrow(boolean showDropdownArrow) {
    if (showDropdownArrow) {
      dropdownArrow.removeClassName(res.css().hidden());
    } else {
      dropdownArrow.addClassName(res.css().hidden());
    }
  }

  @Override
  public void setShowDivider(boolean showDivider) {
    if (showDivider) {
      divider.removeClassName(res.css().hidden());
    } else {
      divider.addClassName(res.css().hidden());
    }
  }

  @Override
  public void addDebugClass(String dc) {
    DebugClassHelper.addDebugClass(content, dc);
  }

  @Override
  public void removeDebugClass(String dc) {
    DebugClassHelper.removeDebugClass(content, dc);
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public Widget hackGetWidget() {
    return this;
  }
}
