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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.widget.popup.AlignedPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * The Class ColorPopup shows a popup with a extensible color picker.
 *
 * @author vjrj@ourproject.org (Vicente J. Ruiz Jurado)
 */
public class ColorPopup {

  /** The extensible color picker. */
  private ComplexColorPicker colorPicker;

  /** The popup. */
  private UniversalPopup popup;

  /**
   * Instantiates a new color popup.
   *
   * @param relative the relative
   * @param allowNone the allow none
   */
  public ColorPopup(Element relative, boolean allowNone) {
    colorPicker = ComplexColorPicker.getInstance();
    colorPicker.setAllowNone(allowNone);
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    popup = PopupFactory.createPopup(relative, AlignedPopupPositioner.ABOVE_RIGHT, chrome, true);
    popup.add(colorPicker);
  }

  /**
   * Show the popup with the color picker.
   *
   * @param listener the listener
   */
  public void show(OnColorChooseListener listener) {
    colorPicker.setListener(listener);
    colorPicker.show();
    popup.show();
  }

  /**
   * Hide the popup with the color picker.
   */
  public void hide() {
    popup.hide();
  }
}
