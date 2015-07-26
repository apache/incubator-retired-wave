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

package org.waveprotocol.wave.client.wavepanel;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalRouter;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * A UI component for interacting with a wave.
 * <p>
 * This interface does not expose features; rather, it exposes mechanisms
 * through which features can be implemented.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface WavePanel extends SourcesEvents<WavePanel.LifecycleListener> {

  /**
   * Observer of events affecting this panel's lifecycle.
   */
  public interface LifecycleListener {
    /**
     * Notifies this listener that the wave panel is now showing a wave.
     */
    void onInit();

    /**
     * Notifies this listener that the wave panel it no longer showing a wave
     * and has been discarded.
     */
    void onReset();
  }

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the provider of views of dom elements.
   */
  DomAsViewProvider getViewProvider();

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the registry against which event handlers are registered.
   */
  EventHandlerRegistry getHandlers();

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the registry of key handlers for when focus is on the wave panel.
   */
  KeySignalRouter getKeyRouter();

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the panel for connecting GWT widgets.
   */
  LogicalPanel getGwtPanel();

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return true if this panel has contents. This will be true between, and
   *         only between, {@link LifecycleListener#onInit} and
   *         {LifecycleListener#onReset} events.
   */
  boolean hasContents();

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the UI of the main conversation in this panel. Never returns null.
   * @throws IllegalStateException if this panel has no contents. It is safe to
   *         call this method between {@link LifecycleListener#onInit} and
   *         {@link LifecycleListener#onReset}. If unsure, calls to this method
   *         should be guarded with {@link #hasContents()}.
   */
  TopConversationView getContents();
}
