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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.scroll.ScrollPanel;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Fake, pojo implementation of a conversation view.
 *
 */
public final class FakeTopConversationView extends FakeConversationView
    implements TopConversationView {

  FakeTopConversationView(FakeRootThreadView thread) {
    super(thread);
  }

  @Override
  public ScrollPanel<? super View> getScroller() {
    throw new UnsupportedOperationException("not yet faked");
  }

  @Override
  public Type getType() {
    return Type.ROOT_CONVERSATION;
  }

  @Override
  public void remove() {
    throw new RuntimeException("Can not remove a top conversation");
  }

  @Override
  void remove(FakeRootThreadView thread) {
    throw new RuntimeException("Can not remove a thread from its conversation");
  }

  @Override
  public void setToolbar(Element toolbar) {
    throw new UnsupportedOperationException("setToolbar not yet faked");
  }

  @Override
  public String toString() {
    return "TopConversation [ " + thread.toString() + " ]";
  }
}
