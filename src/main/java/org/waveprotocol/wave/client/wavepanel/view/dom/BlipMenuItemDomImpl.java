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

package org.waveprotocol.wave.client.wavepanel.view.dom;

import static org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipMetaViewBuilder.OPTION_ID_ATTRIBUTE;
import static org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipMetaViewBuilder.OPTION_SELECTED_ATTRIBUTE;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMenuItemView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView.MenuOption;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipMetaViewBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipViewBuilder;

/**
 * A blip menu item.
 *
 */
public final class BlipMenuItemDomImpl implements DomView, IntrinsicBlipMenuItemView {
  /** The DOM element of this view. */
  private final Element self;

  /** The CSS classes used to manipulate style based on state changes. */
  private final BlipViewBuilder.Css css;
  
  BlipMenuItemDomImpl(Element self, BlipViewBuilder.Css css) {
    this.self = self;
    this.css = css;
  }

  public static BlipMenuItemDomImpl of(Element e, BlipViewBuilder.Css css) {
    return new BlipMenuItemDomImpl(e, css);
  }

  @Override
  public Element getElement() {
    return self;
  }

  @Override
  public String getId() {
    return self.getId();
  }

  public void remove() {
    self.removeFromParent();
  }

  @Override
  public void select() {
    self.setAttribute(OPTION_SELECTED_ATTRIBUTE, "s");
    self.setClassName(css.menuOption() + " " + css.menuOptionSelected());
  }

  @Override
  public void deselect() {
    self.removeAttribute(OPTION_SELECTED_ATTRIBUTE);
    self.setClassName(css.menuOption());
  }

  @Override
  public MenuOption getOption() {
    return BlipMetaViewBuilder.getMenuOption(self.getAttribute(OPTION_ID_ATTRIBUTE));
  }

  @Override
  public void setOption(MenuOption option) {
    self.setAttribute(OPTION_ID_ATTRIBUTE, BlipMetaViewBuilder.getMenuOptionId(option).asString());
  }

  @Override
  public boolean isSelected() {
    return self.hasAttribute(OPTION_SELECTED_ATTRIBUTE);
  }

  @Override
  public boolean equals(Object obj) {
    return DomViewHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return DomViewHelper.hashCode(this);
  }
}
