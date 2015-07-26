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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;

/**
 * Fake, pojo implementation of a thread view.
 */
public final class FakeInlineConversationView extends FakeConversationView
    implements InlineConversationView {

  private FakeBlipView container;
  private boolean collapsed;

  FakeInlineConversationView(FakeRootThreadView thread) {
    super(thread);
  }

  void setContainer(FakeBlipView container) {
    this.container = container;
  }

  @Override
  public Type getType() {
    return Type.INLINE_CONVERSATION;
  }

  @Override
  public FakeBlipView getParent() {
    return container;
  }

  @Override
  public void remove() {
    container.remove();
  }

  // Uninteresting below.

  @Override
  public void setCollapsed(boolean collapsed) {
    this.collapsed = collapsed;
  }

  @Override
  public boolean isCollapsed() {
    return collapsed;
  }

  @Override
  public String toString() {
    return "InlineConversation [" // \u2620
        + " participants: none" // \u2620
        + ", thread: " + thread // \u2620
        + "]";
  }
}
