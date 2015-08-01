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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;

/**
 * The Class SampleCustomColorPicker is a sample of how the ColorPicker can
 * extended with other color pickers.
 *
 * @author vjrj@ourproject.org (Vicente J. Ruiz Jurado)
 */
public class SampleCustomColorPicker extends AbstractColorPicker {

  /** The textbox used in this sample to set hex colors. */
  private TextBox textbox;

  /**
   * Instantiates a new sample custom color picker.
   *
   * @param colorPicker the color picker
   */
  public SampleCustomColorPicker(final ComplexColorPicker colorPicker) {
    super(colorPicker);
    textbox = new TextBox();
    textbox.setVisibleLength(8);
    textbox.setValue("#000000");
    textbox.addStyleName(ComplexColorPicker.style.margins());

    PushButton custom = new PushButton("Custom...");
    custom.addStyleName(ComplexColorPicker.style.buttonsMargins());
    custom.setStylePrimaryName(ComplexColorPicker.style.customColorPushbutton());
    custom.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // ComplexColorPicker is a DeckPanel, so we show our widget
        colorPicker.showWidget(1);
        textbox.setFocus(true);
        textbox.selectAll();
      }
    });

    textbox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        chooseColor();
      }
    });

    textbox.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(final KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          if (textbox.getText().length() > 0) {
            chooseColor();
          }
        }
      }
    });

    initWidget(textbox);
    setWidth("50px");

    // We add the button and the panel to the ComplexColorPicker (the button
    // opens the panel)
    colorPicker.addToBottom(custom);
    colorPicker.addColorPicker(this);

  }

  /**
   * Choose the color of the textbox.
   */
  private void chooseColor() {
    onColorChoose(textbox.getValue());
  }

}
