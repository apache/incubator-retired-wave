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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalHandler;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalRouter;
import org.waveprotocol.wave.client.wavepanel.event.WaveMouseDownHandler;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;

import java.util.EnumSet;

/**
 * Interpets user gestures as focus-frame actions.
 *
 */
public final class FocusFrameController implements WaveMouseDownHandler, KeySignalHandler {
  private final FocusFramePresenter focus;
  private final DomAsViewProvider panel;

  /**
   * Creates a focus controller.
   */
  private FocusFrameController(FocusFramePresenter focus, DomAsViewProvider panel) {
    this.focus = focus;
    this.panel = panel;
  }

  /**
   * Installs the focus-frame feature in a wave panel.
   */
  public static void install(FocusFramePresenter focus, WavePanel panel) {
    new FocusFrameController(focus, panel.getViewProvider()).install(
        panel.getHandlers(), panel.getKeyRouter());
  }

  private void install(EventHandlerRegistry handlers, KeySignalRouter keys) {
    handlers.registerMouseDownHandler(TypeCodes.kind(Type.BLIP), this);
    keys.register(
        EnumSet.of(KeyCombo.UP, KeyCombo.DOWN, KeyCombo.SPACE, KeyCombo.SHIFT_SPACE), this);
  }

  @Override
  public boolean onMouseDown(MouseDownEvent event, Element source) {
    if (event.getNativeButton() != NativeEvent.BUTTON_LEFT) {
      return false;
    }
    focus.focusWithoutScroll(panel.asBlip(source));
    // Cancel bubbling, so that other blips do not grab focus.
    return true;
  }

  @Override
  public boolean onKeySignal(KeyCombo key) {
    switch (key) {
      case UP:
        focus.moveUp();
        return true;
      case DOWN:
        focus.moveDown();
        return true;
      case SPACE:
        focus.focusNext();
        return true;
      case SHIFT_SPACE:
        focus.focusPrevious();
        return true;
      default:
        return false;
    }
  }
}
