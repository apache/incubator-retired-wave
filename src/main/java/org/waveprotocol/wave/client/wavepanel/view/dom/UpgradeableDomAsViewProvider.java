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

import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.DomRenderer;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * A view provider that providers views with reduced capabilities, until the
 * provider is {@link #setRenderer(DomRenderer) upgraded}. After being
 * upgraded, all provided views (including those provided prior to upgrade) have
 * full capabilities.
 * <p>
 * The reduction of capabilities simply means that views are not capable of
 * synthesizing new views through their rendering functions (e.g., {@link
 * org.waveprotocol.wave.client.wavepanel.view.ThreadView#insertBlipBefore(View,
 * ConversationBlip)}); however, they still support all structure and direct
 * mutators.
 *
 */
public interface UpgradeableDomAsViewProvider extends DomAsViewProvider {
  /**
   * Upgrades the views managed by this handler with the capability to add new
   * views by rendering model objects. The renderer can only be set once.
   *
   * @param renderer renderer to use for rendering model objects
   */
  void setRenderer(DomRenderer renderer);
}
