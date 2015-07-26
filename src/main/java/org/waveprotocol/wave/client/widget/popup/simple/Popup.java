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

package org.waveprotocol.wave.client.widget.popup.simple;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupProvider;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.TitleBar;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight popup implementation for use within the editor test harness.
 *
 */
public class Popup implements UniversalPopup {
  /** Convenience provider that returns these lightweight popups. */
  public static final PopupProvider LIGHTWEIGHT_POPUP_PROVIDER = new PopupProvider() {
    public UniversalPopup createPopup(Element reference, RelativePopupPositioner positioner,
      PopupChrome chrome, boolean autoHide) {
      return new Popup(reference, positioner);
    }
    public void setRootPanel(Panel rootPanel) {}
  };


  private final PopupPanel popupPanel = new PopupPanel(true);
  private final List<PopupEventListener> listeners = new ArrayList<PopupEventListener>();
  private final RelativePopupPositioner positioner;
  private final Element reference;
  private boolean showing = false;

  /**
   * Creates a popup.
   *
   * @param reference   reference element for the positioner
   * @param positioner  strategy for positioning the popup
   */
  public Popup(Element reference, RelativePopupPositioner positioner) {
    this.reference = reference;
    this.positioner = positioner;
    popupPanel.addStyleName("editor-popup");
    popupPanel.setAnimationEnabled(true);
    popupPanel.addCloseHandler(new CloseHandler<PopupPanel>() {
      @Override
      public void onClose(CloseEvent<PopupPanel> event) {
        Popup.this.hide();
      }
    });
  }

  /**
   * Positions and shows this popup.
   */
  private void position() {
    positioner.setPopupPositionAndMakeVisible(reference, popupPanel.getElement());
  }

  /** {@inheritDoc} */
  public void add(Widget w) {
    popupPanel.add(w);
  }

  /** {@inheritDoc} */
  public boolean remove(Widget w) {
    return popupPanel.remove(w);
  }

  /** {@inheritDoc} */
  public void show() {
    popupPanel.show();
    showing = true;
    position();
    for (PopupEventListener l : listeners) {
      l.onShow(this);
    }
  }

  /** {@inheritDoc} */
  public void hide() {
    popupPanel.hide();
    showing = false;
    for (PopupEventListener l : listeners) {
      l.onHide(this);
    }
  }

  /** {@inheritDoc} */
  public void move() {
    position();
  }

  /** {@inheritDoc} */
  public void addPopupEventListener(PopupEventListener listener) {
    listeners.add(listener);
  }

  /** {@inheritDoc} */
  public void removePopupEventListener(PopupEventListener listener) {
    listeners.remove(listener);
  }

  /** {@inheritDoc} */
  public TitleBar getTitleBar() {
    // Not used
    return null;
  }

  /** {@inheritDoc} */
  public boolean isShowing() {
    return showing;
  }

  /** {@inheritDoc} */
  public void clear() {
    popupPanel.clear();
  }

  @Override
  public void associateWidget(Widget w) {
    // forget about it
  }

  @Override
  public void setMaskEnabled(boolean isMaskEnabled) {
    // Do nothing.
  }

  @Override
  public void setDebugClass(String dcName) {
    // Do nothing.
  }

}
