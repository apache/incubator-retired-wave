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
package org.waveprotocol.wave.client.wavepanel.impl.toolbar.color;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.waveprotocol.wave.client.wavepanel.impl.toolbar.color.i18n.ColorPickerMessages;

/**
 * The Class ComplexColorPicker is used to show a simple color picker but can be
 * extensible with other color pickers. See {@link SampleCustomColorPicker}.
 *
 * @author vjrj@ourproject.org (Vicente J. Ruiz Jurado)
 */
public class ComplexColorPicker extends DeckPanel {

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {

    @Source("ComplexColorPicker.css")
    Style style();
  }

  /** The Constant messages. */
  public static final ColorPickerMessages messages = GWT.create(ColorPickerMessages.class);

  /**
   * The Interface Style.
   */
  public interface Style extends CssResource {

    String fl();

    String fr();

    String margins();

    String buttonsMargins();

    String toolbar();

    String customColorPushbutton();

  }

  /** The instance. */
  private static ComplexColorPicker instance;

  /**
   * Gets the single instance of ComplexColorPicker (singleton in the
   * WebClient).
   *
   * @return single instance of ComplexColorPicker
   */
  public static ComplexColorPicker getInstance() {
    if (instance != null) {
      return instance;
    }
    instance = new ComplexColorPicker();
    return instance;
  }

  /** The CSS style. */
  final public static Style style = GWT.<Resources> create(Resources.class).style();

  private OnColorChooseListener listener;
  private SimpleColorPicker simplePicker;
  private VerticalPanel vp;
  private PushButton noneBtn;

  public ComplexColorPicker() {
    style.ensureInjected();

    // The background color can be set to "none"
    noneBtn = new PushButton(messages.none());
    noneBtn.addStyleName(ComplexColorPicker.style.buttonsMargins());
    noneBtn.setStylePrimaryName(ComplexColorPicker.style.customColorPushbutton());
    noneBtn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        listener.onNoneColorChoose();
      }
    });

    vp = new VerticalPanel();

    // We use a simple color picker by default
    simplePicker = new SimpleColorPicker(this);
    vp.add(simplePicker);
    vp.add(noneBtn);
    super.add(vp);
  }

  /**
   * Sets the listener.
   *
   * @param listener the new listener
   */
  public void setListener(OnColorChooseListener listener) {
    this.listener = listener;
  }

  /**
   * On color choose.
   *
   * @param color the color
   */
  public void onColorChoose(String color) {
    listener.onColorChoose(color);
  }

  /**
   * Show.
   */
  public void show() {
    this.showWidget(0);
  }

  /**
   * Adds an additional color pickers (to the DeckPanel).
   *
   * @param colorPicker the widget
   */
  public void addColorPicker(AbstractColorPicker colorPicker) {
    super.add(colorPicker);
  }

  /**
   * Adds the widget to bottom (useful to add buttons).
   *
   * @param widget the widget
   */
  public void addToBottom(IsWidget widget) {
    vp.add(widget);
  }

  /**
   * Sets the allow none (if the none button is visible or not).
   *
   * @param allowNone the new allow none
   */
  public void setAllowNone(boolean allowNone) {
    noneBtn.setVisible(allowNone);
  }

}
