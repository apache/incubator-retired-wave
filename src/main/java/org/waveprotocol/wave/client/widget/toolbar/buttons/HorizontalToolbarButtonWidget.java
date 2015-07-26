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
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;
import org.waveprotocol.wave.client.widget.common.ImplPanel;

/**
 * Widget implementation of a {@link ToolbarButtonView} for displaying
 * in a horizontal toolbar.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class HorizontalToolbarButtonWidget extends Composite implements ToolbarButtonUi {

  interface Resources extends ClientBundle {
    @Source("HorizontalToolbarButtonWidget.css")
    Css css();

    @Source("button_down_large.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource buttonDown();

    @Source("toolbar_divider.png")
    ImageResource divider();

    @Source("arrow_horizontal.png")
    ImageResource dropdownArrow();
  }

  interface Css extends CssResource {
    String self();
    String wide();
    String compact();
    String visualElement();
    String textElement();
    String textElementWithVisualElement();
    String divider();
    String overlay();
    String enabled();
    String hidden();
    String down();
    String dropdownArrow();
  }

  interface Binder extends UiBinder<Widget, HorizontalToolbarButtonWidget> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true)
  static final Resources res = GWT.create(Resources.class);
  static {
    StyleInjector.inject(res.css().getText(), true);
  }

  @UiField ImplPanel self;
  @UiField Element visualElement;
  @UiField Element textElement;
  @UiField Element dropdownArrow;
  @UiField Element divider;
  @UiField Element overlay;

  private Listener listener;
  private Element currentVisualElement = null;
  private String debugText; // For toString() debgging.

  public HorizontalToolbarButtonWidget() {
    initWidget(BINDER.createAndBindUi(this));
    // By default, the button is displayed "compact" (e.g. less padding).  If
    // text is set on the button then the button will be displayed "wide".
    // If this is needed by more than just the overflow and edit-mode buttons
    // then perhaps a view method is warranted.
    self.getElement().addClassName(res.css().compact());
  }

  @UiHandler("self")
  void handleButtonClicked(ClickEvent e) {
    if (listener != null) {
      listener.onClick();
    }
  }

  @UiHandler("self")
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
    debugText = text;
    if (text.isEmpty()) {
      textElement.addClassName(res.css().hidden());
      self.getElement().replaceClassName(res.css().wide(), res.css().compact());
    } else {
      textElement.removeClassName(res.css().hidden());
      self.getElement().replaceClassName(res.css().compact(), res.css().wide());
    }
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
    currentVisualElement = element;
    visualElement.removeClassName(res.css().hidden());
    visualElement.appendChild(element);
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
  public void addDebugClass(String dc) {
    DebugClassHelper.addDebugClass(this, dc);
  }

  @Override
  public void removeDebugClass(String dc) {
    DebugClassHelper.removeDebugClass(this, dc);
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public Widget hackGetWidget() {
    return this;
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
  public String toString() {
    return debugText;
  }
}
