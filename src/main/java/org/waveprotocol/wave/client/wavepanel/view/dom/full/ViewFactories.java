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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import org.waveprotocol.wave.client.uibuilder.UiBuilder;

/**
 * A utility class that contains the ViewFactory for a default client and the
 * mobile client.
 *
 */
public final class ViewFactories {

  private ViewFactories() {
  }

  private static abstract class BaseFactory implements ViewFactory {
    @Override
    public final InlineConversationViewBuilder createInlineConversationView(
        String id, UiBuilder threadUi, UiBuilder participantsUi) {
      return InlineConversationViewBuilder.create(id, participantsUi, threadUi);
    }
  }

  /**
   * A ViewFactory that creates views suitable for embedding in normal flow,
   * with no fixed height.
   */
  public static final ViewFactory FLOW = new BaseFactory() {

    @Override
    public TopConversationViewBuilder createTopConversationView(
        String id, UiBuilder threadUi, UiBuilder participantsUi) {
      return FlowConversationViewBuilder.createRoot(id, threadUi, participantsUi);
    }
  };

  /**
   * A ViewFactory that creates views suitable for embedding in a fixed-height
   * context.
   */
  public static final ViewFactory FIXED = new BaseFactory() {

    @Override
    public TopConversationViewBuilder createTopConversationView(
        String id, UiBuilder threadUi, UiBuilder participantsUi) {
      return FixedConversationViewBuilder.createRoot(id, threadUi, participantsUi);
    }
  };
}
