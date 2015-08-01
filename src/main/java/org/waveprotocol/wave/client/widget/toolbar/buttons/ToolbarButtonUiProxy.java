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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

/**
 * A {@link ToolbarButtonUi} which records its state to have delegate (proxy)
 * displays set. When set, the stored state is copied into the proxy and
 * delegated to from then on.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ToolbarButtonUiProxy implements ToolbarButtonUi {

  /** Delegate display, set in {@link #setDelegate}. */
  private ToolbarButtonUi delegate;

  // State specific to ToolbarButtonUi.
  private Listener listener;
  private Boolean isDown;

  // Delegate all other state to a ToolbarButtonViewProxy.
  private final ToolbarButtonViewProxy proxy = new ToolbarButtonViewProxy();

  public ToolbarButtonUiProxy(ToolbarButtonUi delegate) {
    setDelegate(delegate);
  }

  /**
   * Sets the delegate, copying in state.
   */
  public void setDelegate(ToolbarButtonUi delegate) {
    if (this.delegate != delegate) {
      this.delegate = delegate;
      proxy.setDelegate(delegate);
      copyUiStateInto(delegate);
    }
  }

  private void copyUiStateInto(ToolbarButtonUi display) {
    if (listener != null) {
      display.setListener(listener);
    }
    if (isDown != null) {
      display.setDown(isDown);
    }
  }

  /**
   * @return the display currently being delegated to
   */
  public ToolbarButtonUi getDelegate() {
    return delegate;
  }

  /**
   * @return the {@link ToolbarButtonView.State} of the toolbar, exposed for
   *         the overflow toolbar
   */
  public State hackGetState() {
    return proxy.hackGetState();
  }

  @Override
  public void setDown(boolean isDown) {
    this.isDown = isDown;
    if (delegate != null) {
      delegate.setDown(isDown);
    }
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener;
    if (delegate != null) {
      delegate.setListener(listener);
    }
  }

  //
  // Trivial delegation to a ToolbarButtonViewProxy.
  //

  @Override
  public void addDebugClass(String dc) {
    proxy.addDebugClass(dc);
  }

  @Override
  public Widget hackGetWidget() {
    return proxy.hackGetWidget();
  }

  @Override
  public void removeDebugClass(String dc) {
    proxy.removeDebugClass(dc);
  }

  @Override
  public void setShowDropdownArrow(boolean showDropdown) {
    proxy.setShowDropdownArrow(showDropdown);
  }

  @Override
  public void setShowDivider(boolean showDivider) {
    proxy.setShowDivider(showDivider);
  }

  @Override
  public void setState(State state) {
    proxy.setState(state);
  }

  @Override
  public void setText(String text) {
    proxy.setText(text);
  }

  @Override
  public void setTooltip(String tooltip) {
    proxy.setTooltip(tooltip);
  }

  @Override
  public void setVisualElement(Element element) {
    proxy.setVisualElement(element);
  }
}
