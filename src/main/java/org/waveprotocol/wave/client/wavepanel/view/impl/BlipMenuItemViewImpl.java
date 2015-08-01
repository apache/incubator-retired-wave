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

package org.waveprotocol.wave.client.wavepanel.view.impl;

import org.waveprotocol.wave.client.wavepanel.view.BlipMenuItemView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMenuItemView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView.MenuOption;

/**
 * Implements a blip menu-item view by delegating primitive state matters to a
 * view object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic menu-item implementation
 */
public final class BlipMenuItemViewImpl<I extends IntrinsicBlipMenuItemView> // \u2620
    extends AbstractStructuredView<BlipMenuItemViewImpl.Helper<? super I>, I> // \u2620
    implements BlipMenuItemView {

  /**
   * Handles structural queries on menu-item views.
   *
   * @param <I> intrinsic menu-item implementation
   */
  public interface Helper<I> {

    //
    // Structure
    //

    void remove(I impl);

    BlipMetaView getParent(I impl);
  }

  private BlipMenuItemViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  public static <I extends IntrinsicBlipMenuItemView> BlipMenuItemViewImpl<I> create(
      Helper<? super I> helper, I impl) {
    return new BlipMenuItemViewImpl<I>(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.MENU_ITEM;
  }

  //
  // Structural delegation.
  //

  @Override
  public BlipMetaView getParent() {
    return helper.getParent(impl);
  }

  @Override
  public void remove() {
    helper.remove(impl);
  }

  //
  // Intrinsic delegation.
  //

  @Override
  public void select() {
    impl.select();
  }

  @Override
  public void deselect() {
    impl.deselect();
  }

  @Override
  public MenuOption getOption() {
    return impl.getOption();
  }

  @Override
  public void setOption(MenuOption option) {
    impl.setOption(option);
  }

  @Override
  public boolean isSelected() {
    return impl.isSelected();
  }
}
