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

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalHandler;

import java.util.EnumSet;

/**
 * Translates UI gestures to scroll actions.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class ScrollHandler implements KeySignalHandler {
  private final ScrollController controller;

  /**
   * Creates a scroll controller.
   */
  private ScrollHandler(ScrollController controller) {
    this.controller = controller;
  }

  public static ScrollHandler install(WavePanel panel, ScrollPanel<?> scroller) {
    ScrollHandler c = new ScrollHandler(new ScrollController(scroller));
    panel.getKeyRouter().register(
        EnumSet.of(KeyCombo.PAGE_UP, KeyCombo.PAGE_DOWN, KeyCombo.HOME, KeyCombo.END), c);
    return c;
  }

  @Override
  public boolean onKeySignal(KeyCombo key) {
    switch (key) {
      case PAGE_UP:
        controller.pageUp();
        return true;
      case PAGE_DOWN:
        controller.pageDown();
        return true;
      case HOME:
        controller.home();
        return true;
      case END:
        controller.end();
        return true;
      default:
        throw new RuntimeException();
    }
  }
}
