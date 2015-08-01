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

package org.waveprotocol.wave.client.wavepanel.impl;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.EventDispatcherPanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.Focusable;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalRouter;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

/**
 * A wave panel, which is just a view and an event system.
 *
 */
public final class WavePanelImpl implements WavePanel, Focusable {

  /** Event system on which features install their controllers. */
  private final EventDispatcherPanel panel;

  /** Key handlers for key events that are routed to this panel. */
  private final KeySignalRouter keys = new KeySignalRouter();

  /** Views through which features manipulate the UI. */
  private final DomAsViewProvider views;

  private final CopyOnWriteSet<LifecycleListener> listeners = CopyOnWriteSet.create();

  //
  // Fields referencing the wave rendering are dynamically inserted and removed.
  //

  /** True between {@link #init} and {@link #reset}. */
  private boolean initialized;

  /** Main conversation shown in this panel. */
  private TopConversationView main;


  private WavePanelImpl(
      DomAsViewProvider views, EventDispatcherPanel panel) {
    this.views = views;
    this.panel = panel;
  }

  /**
   * Creates a wave panel.
   *
   * @param views view bundle
   * @param panelDom element in the DOM on which to build the wave panel
   * @param container panel to adopt the wave panel's widget, or {@code null}
   *        for the wave panel to be a root widget
   */
  public static WavePanelImpl create(
      DomAsViewProvider views, Element panelDom, LogicalPanel container) {
    Preconditions.checkArgument(panelDom != null);
    EventDispatcherPanel events =
        (container != null) ? EventDispatcherPanel.inGwtContext(panelDom, container)
            : EventDispatcherPanel.of(panelDom);
    WavePanelImpl panel = new WavePanelImpl(views, events);

    // Existing content?
    Element frameDom = panelDom.getFirstChildElement();
    if (frameDom != null) {
      panel.init(frameDom);
    }
    return panel;
  }

  /**
   * Destroys this wave panel, releasing its resources.
   */
  public void destroy() {
    panel.removeFromParent();
  }

  @Override
  public DomAsViewProvider getViewProvider() {
    return views;
  }

  @Override
  public EventHandlerRegistry getHandlers() {
    return panel;
  }

  @Override
  public LogicalPanel getGwtPanel() {
    return panel;
  }

  @Override
  public KeySignalRouter getKeyRouter() {
    return keys;
  }

  @Override
  public boolean hasContents() {
    return main != null;
  }

  @Override
  public TopConversationView getContents() {
    Preconditions.checkState(main != null);
    return main;
  }

  //
  // Key plumbing.
  //

  @Override
  public boolean onKeySignal(KeyCombo key) {
    return keys.onKeySignal(key);
  }

  @Override
  public void onFocus() {
  }

  @Override
  public void onBlur() {
  }

  //
  // Lifecycle.
  //

  public void init(Element main) {
    Preconditions.checkState(!initialized);

    boolean fireEvent;  // true if onInit should be fired before exiting.
    if (main != null) {
      panel.getElement().appendChild(main);
      this.main = views.asTopConversation(main);
      fireEvent = true;
    } else {
      // Render empty message.
      panel.getElement().setInnerHTML("No conversations in this wave.");
      fireEvent = false;
    }
    initialized = true;

    if (fireEvent) {
      fireOnInit();
    }
  }

  public void reset() {
    Preconditions.checkState(initialized);

    boolean fireEvent;  // true if onInit should be fired before exiting.
    initialized = false;
    if (main != null) {
      main.remove();
      main = null;
      fireEvent = true;
    } else {
      panel.getElement().setInnerHTML("");
      fireEvent = false;
    }

    if (fireEvent) {
      fireOnReset();
    }
  }

  @Override
  public void addListener(LifecycleListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(LifecycleListener listener) {
    listeners.remove(listener);
  }

  private void fireOnInit() {
    for (LifecycleListener listener : listeners) {
      listener.onInit();
    }
  }

  private void fireOnReset() {
    for (LifecycleListener listener : listeners) {
      listener.onReset();
    }
  }
}
