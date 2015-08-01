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


package org.waveprotocol.wave.client.scroll;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.WavePanel.LifecycleListener;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Proxies scroll control to the scroller in the conversation view. This
 * provides a scroll panel object that has the same lifetime as a wave panel,
 * rather than the lifetime of its contents.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class ProxyScrollPanel implements ScrollPanel<View>, LifecycleListener {
  /** Panel whose contents have a scroller. */
  private final WavePanel panel;
  /** Cached reference to the scroller of the panel contents. */
  private ScrollPanel<? super View> scroller;

  private ProxyScrollPanel(WavePanel panel) {
    this.panel = panel;
  }

  /**
   * Creates a proxy scroll panel.
   */
  public static ProxyScrollPanel create(WavePanel panel) {
    ProxyScrollPanel proxy = new ProxyScrollPanel(panel);
    panel.addListener(proxy);
    proxy.onInit();
    return proxy;
  }

  /** @return the current scroll panel, failing if there is none. */
  private ScrollPanel<? super View> getScroller() {
    Preconditions.checkState(scroller != null, "No contents");
    return scroller;
  }

  @Override
  public void onInit() {
    scroller =
        panel.hasContents() ? AnimatedScrollPanel.create(panel.getContents().getScroller()) : null;
  }

  @Override
  public void onReset() {
    scroller = null;
  }

  // Forward API to scroller.
  @Override
  public Extent extentOf(View measurable) {
    return getScroller().extentOf(measurable);
  }

  @Override
  public Extent getContent() {
    return getScroller().getContent();
  }

  @Override
  public Extent getViewport() {
    return getScroller().getViewport();
  }

  @Override
  public void moveTo(double location) {
    getScroller().moveTo(location);
  }
}
