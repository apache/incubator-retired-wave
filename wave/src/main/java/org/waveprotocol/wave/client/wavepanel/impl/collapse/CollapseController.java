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


package org.waveprotocol.wave.client.wavepanel.impl.collapse;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;

import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.WaveMouseDownHandler;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.TypeCodes;

/**
 * Interpets user gestures as collapse/expand actions.
 *
 */
public final class CollapseController implements WaveMouseDownHandler {
  private final CollapsePresenter collapser;
  private final DomAsViewProvider panel;

  /**
   * Creates a focus controller.
   */
  private CollapseController(CollapsePresenter focus, DomAsViewProvider panel) {
    this.collapser = focus;
    this.panel = panel;
  }

  /**
   * Installs the collapse/expand feature in a wave panel.
   */
  public static void install(CollapsePresenter focus, WavePanel panel) {
    new CollapseController(focus, panel.getViewProvider()).install(panel.getHandlers());
  }

  private void install(EventHandlerRegistry handlers) {
    handlers.registerMouseDownHandler(TypeCodes.kind(Type.TOGGLE), this);
  }

  @Override
  public boolean onMouseDown(MouseDownEvent event, Element source) {
    if (event.getNativeButton() != NativeEvent.BUTTON_LEFT) {
      return false;
    }
    handleClick(panel.fromToggle(source));
    return false;
  }

  /**
   * Handles a click on a toggle.
   */
  private void handleClick(InlineThreadView thread) {
    collapser.toggle(thread);
  }
}
