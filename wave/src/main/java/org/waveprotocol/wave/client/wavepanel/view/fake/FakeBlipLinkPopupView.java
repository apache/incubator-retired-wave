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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;

/**
 * Fake, pojo implementation of a blip-link popup.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class FakeBlipLinkPopupView implements BlipLinkPopupView {

  /** Optional listener for view events. */
  private Listener listener;

  private String blipLinkInfo;

  FakeBlipLinkPopupView(FakeBlipView fakeBlipView) {
  }

  @Override
  public void init(Listener listener) {
    Preconditions.checkState(this.listener == null);
    Preconditions.checkArgument(listener != null);
    this.listener = listener;
  }

  @Override
  public void reset() {

  }

  @Override
  public void setLinkInfo(String url) {
    blipLinkInfo = url;
  }

  @Override
  public void show() {

  }

  @Override
  public void hide() {

  }

  @Override
  public String toString() {
    return "BlipLinkPopupView [blipLinkInfo=" + blipLinkInfo + "]";
  }

}
