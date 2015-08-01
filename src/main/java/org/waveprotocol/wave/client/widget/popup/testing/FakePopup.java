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

package org.waveprotocol.wave.client.widget.popup.testing;

import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.TitleBar;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake popup for testing purposes.
 *
 */
public class FakePopup implements UniversalPopup {
  private final List<PopupEventListener> listeners = new ArrayList<PopupEventListener>();
  private boolean isShowing = false;

  @Override
  public void add(Widget w) {
    // Do nothing.
  }

  @Override
  public void clear() {
    // Do nothing.
  }

  @Override
  public TitleBar getTitleBar() {
    // Do nothing.
    return null;
  }

  @Override
  public void move() {
    // Do nothing.
  }

  @Override
  public boolean remove(Widget w) {
    // Do nothing.
    return false;
  }

  @Override
  public void show() {
    isShowing = true;
    for (PopupEventListener listener : listeners) {
      listener.onShow(null);
    }
  }

  @Override
  public void addPopupEventListener(PopupEventListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removePopupEventListener(PopupEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void hide() {
    isShowing = false;
    for (PopupEventListener listener : listeners) {
      listener.onHide(null);
    }
  }

  @Override
  public boolean isShowing() {
    return isShowing;
  }

  @Override
  public void associateWidget(Widget w) {
    // Do nothing
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
