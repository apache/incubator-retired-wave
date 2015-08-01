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

package org.waveprotocol.wave.client.widget.button;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.button.ClickButton.ClickButtonListener;
import org.waveprotocol.wave.client.widget.button.ClickButton.ClickWhenDisabledListener;

/**
 * Convenience widget that combines the abstract notion of a
 * {@link ClickButtonController} with some physical widget so that we don't have
 * to return both in a pair from the ButtonFactory.
 *
 */
public class ClickButtonWidget extends Composite implements ClickButtonController {
  private final ClickButtonController controller;

  /**
   * @param controller Controls a button represented by w.
   * @param w The physical representation of a button.
   */
  public ClickButtonWidget(ClickButtonController controller, Widget w) {
    this.controller = controller;
    initWidget(w);
  }

  @Override
  public void setClickButtonListener(ClickButtonListener listener) {
    controller.setClickButtonListener(listener);
  }

  @Override
  public void setClickWhenDisabledListener(ClickWhenDisabledListener listener) {
    controller.setClickWhenDisabledListener(listener);
  }

  @Override
  public void setDisabled(boolean isDisabled) {
    controller.setDisabled(isDisabled);
  }

  @Override
  public void setTooltip(String tooltip) {
    controller.setTooltip(tooltip);
  }

  @Override
  public void setText(String text) {
    controller.setText(text);
  }
}
