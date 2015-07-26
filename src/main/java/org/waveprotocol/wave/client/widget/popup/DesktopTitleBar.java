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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * TitleBar implementation for the desktop client.
 *
 *
 * This class is package private.
 */
class DesktopTitleBar extends FlowPanel implements TitleBar {
  /** Resources used by DesktopTitleBar. */
  interface Resources extends ClientBundle {
    /** CSS class names used by DesktopTitleBar */
    interface Css extends CssResource {
      String titleBar();
      String buttons();
      String title();
    }

    @Source("DesktopTitleBar.css")
    Css css();
  }

  /** The singleton instance of our CSS resources. */
  private static final Resources RESOURCES = GWT.create(Resources.class);

  /**
   * Inject the stylesheet only once, and only when the class is used.
   */
  static {
    StyleInjector.inject(RESOURCES.css().getText());
  }

  /** Label containing the title */
  private Label title = new Label();

  /** Panel to hold the buttons */
  private FlowPanel buttons = new FlowPanel();

  /** Create a new DesktopTitleBar */
  public DesktopTitleBar() {
    add(title);
    add(buttons);
    setStyleName(RESOURCES.css().titleBar());
    buttons.setStyleName(RESOURCES.css().buttons());
    title.setStyleName(RESOURCES.css().title());
  }

  /**
   * {@inheritDoc}
   */
  public void addButton(Widget button) {
    buttons.add(button);
  }

  /**
   * {@inheritDoc}
   */
  public boolean removeButton(Widget button) {
    return buttons.remove(button);
  }

  /**
   * {@inheritDoc}
   */
  public void clearButtons() {
    buttons.clear();
  }

  /**
   * {@inheritDoc}
   */
  public void setTitleText(String text) {
    title.setText(text);
  }
}
