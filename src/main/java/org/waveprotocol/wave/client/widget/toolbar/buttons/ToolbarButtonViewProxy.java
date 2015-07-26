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

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * A {@link ToolbarButtonView} which records its state to have delegate
 * (proxy) displays set. When set, the stored state is copied into the proxy and
 * delegated to from then on.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class ToolbarButtonViewProxy implements ToolbarButtonView {

  /** Delegate display, set in {@link #setDelegate}. */
  private ToolbarButtonView delegate;

  // Recorded state...
  private State state;
  private String text;
  private String tooltip;
  private Element element;
  private Boolean showDropdownArrow;
  private Boolean showDivider;
  private final StringSet dcs = CollectionUtils.createStringSet();

  public ToolbarButtonViewProxy() {
  }

  public ToolbarButtonViewProxy(ToolbarButtonView delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setState(State state) {
    this.state = state;
    if (delegate != null) {
      delegate.setState(state);
    }
  }

  @Override
  public void setText(String text) {
    this.text = text;
    if (delegate != null) {
      delegate.setText(text);
    }
  }

  @Override
  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
    if (delegate != null) {
      delegate.setTooltip(tooltip);
    }
  }

  @Override
  public void setVisualElement(Element element) {
    this.element = element;
    if (delegate != null) {
      delegate.setVisualElement(element);
    }
  }

  @Override
  public void setShowDropdownArrow(boolean showDropdownArrow) {
    this.showDropdownArrow = Boolean.valueOf(showDropdownArrow);
    if (delegate != null) {
      delegate.setShowDropdownArrow(showDropdownArrow);
    }
  }

  @Override
  public void setShowDivider(boolean showDivider) {
    this.showDivider = Boolean.valueOf(showDivider);
    if (delegate != null) {
      delegate.setShowDivider(showDivider);
    }
  }

  @Override
  public void addDebugClass(String dc) {
    dcs.add(dc);
    if (delegate != null) {
      delegate.addDebugClass(dc);
    }
  }

  @Override
  public void removeDebugClass(String dc) {
    dcs.remove(dc);
    if (delegate != null) {
      delegate.removeDebugClass(dc);
    }
  }

  @Override
  public Widget hackGetWidget() {
    return (delegate != null) ? delegate.hackGetWidget() : null;
  }

  /**
   * Sets the display to delegate to.  If the delegate changes, copy the state
   * of this stub into the new delegate.
   *
   * @param delegate The display to start delegating to.
   */
  public void setDelegate(ToolbarButtonView delegate) {
    Preconditions.checkState(delegate != null, "Cannot set a null delegate");
    if (this.delegate != delegate) {
      this.delegate = delegate;
      copyInto(delegate);
    }
  }

  /**
   * @return the display currently being delegated to
   */
  public ToolbarButtonView getDelegate() {
    return delegate;
  }

  private void copyInto(final ToolbarButtonView display) {
    if (state != null) {
      display.setState(state);
    }
    if (text != null) {
      display.setText(text);
    }
    if (tooltip != null) {
      display.setTooltip(tooltip);
    }
    if (element != null) {
      display.setVisualElement(element);
    }
    if (showDropdownArrow != null) {
      display.setShowDropdownArrow(showDropdownArrow);
    }
    if (showDivider != null) {
      display.setShowDivider(showDivider);
    }
    dcs.each(new StringSet.Proc() {
      @Override
      public void apply(String dc) {
        display.addDebugClass(dc);
      }
    });
  }

  /**
   * @return the {@link ToolbarButtonView.State} of the toolbar, exposed for
   *         {@link ToolbarButtonUiProxy}
   */
  State hackGetState() {
    return state;
  }
}
