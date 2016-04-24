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

import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.button.ToggleButton.ToggleButtonListener;

/**
 * Convenience widget that combines the abstract notion of a
 * {@link ToggleButtonController} with some physical widget so that we don't
 * have to return both in a pair from the ButtonFactory.
 *
 */
public class ToggleButtonWidget extends Composite implements ToggleButtonController,
    HasMouseOverHandlers, HasMouseOutHandlers {
  /**
   * The controller for the wrapped widget.
   */
  private final ToggleButtonController controller;

  /**
   * @param controller Controls a button represented by w.
   * @param w The physical representation of a button.
   */
  public ToggleButtonWidget(ToggleButtonController controller, Widget w) {
    initWidget(w);
    this.controller = controller;
  }

  /** {@inheritDoc} */
  public void setOn(boolean isOn) {
    controller.setOn(isOn);
  }

  /** {@inheritDoc} */
  public void setToggleListener(ToggleButtonListener listener) {
    controller.setToggleListener(listener);
  }

  @Override
  public void setDisabled(boolean isDisabled) {
    controller.setDisabled(isDisabled);
  }

  /** {@inheritDoc} */
  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  /** {@inheritDoc} */
  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }
}
