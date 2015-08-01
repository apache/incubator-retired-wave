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
package org.waveprotocol.wave.client.wavepanel.impl.focus;

import org.waveprotocol.wave.client.scroll.SmartScroller;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.FocusFrameView;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.FocusFrame;

/**
 * Builds and installs the focus frame feature.
 *
 */
public final class FocusFrameBuilder {
  private FocusFrameBuilder() {
  }

  /**
   * Builds and installs the focus frame feature.
   *
   * @param panel wave panel to hold the feature
   * @return the feature.
   */
  public static FocusFramePresenter createAndInstallIn(
      WavePanel panel, SmartScroller<? super BlipView> scroller) {
    FocusFrameView view = new FocusFrame();
    ViewTraverser traverser = new ViewTraverser();
    traverser.skipCollapsedContent();
    FocusFramePresenter focus = new FocusFramePresenter(view, scroller, traverser);
    FocusFrameController.install(focus, panel);
    if (panel.hasContents()) {
      focus.onInit();
    }
    panel.addListener(focus);
    return focus;
  }
}
