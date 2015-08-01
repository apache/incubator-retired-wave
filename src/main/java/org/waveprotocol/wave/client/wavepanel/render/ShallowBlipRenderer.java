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

package org.waveprotocol.wave.client.wavepanel.render;

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * A shallow renderer which pushes data from a ConversationBlip into a BlipView.
 *
 */
public interface ShallowBlipRenderer {
  /**
   * Render the blip's content into the view.
   *
   * @param blip
   * @param view
   */
  void render(ConversationBlip blip, IntrinsicBlipMetaView view);

  /**
   * Render the contributors information into the view.
   *
   * @param blip
   * @param view
   */
  void renderContributors(ConversationBlip blip, IntrinsicBlipMetaView view);

  /**
   * Render the renderTime into the view.
   *
   * @param blip
   * @param view
   */
  void renderTime(ConversationBlip blip, IntrinsicBlipMetaView view);

  /**
   * Renders the read state into the view.
   *
   * @param blip
   * @param view
   */
  void renderRead(ConversationBlip blip, IntrinsicBlipMetaView view);
}
