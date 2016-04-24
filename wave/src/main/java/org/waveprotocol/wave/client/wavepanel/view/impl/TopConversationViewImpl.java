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

package org.waveprotocol.wave.client.wavepanel.view.impl;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.scroll.ScrollPanel;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Implements a conversation view by delegating primitive state matters to a
 * view object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic implementation
 */
public final class TopConversationViewImpl<I> // \u2620
    extends AbstractConversationViewImpl<I, TopConversationViewImpl.Helper<? super I>> // \u2620
    implements TopConversationView {

  /**
   * Handles structural queries on thread views.
   *
   * @param <I> intrinsic thread implementation
   */
  public interface Helper<I> extends AbstractConversationViewImpl.Helper<I> {
    ScrollPanel<? super View> getScroller(I impl);
    void setToolbar(I impl, Element e);
  }

  public TopConversationViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.ROOT_CONVERSATION;
  }

  @Override
  public ScrollPanel<? super View> getScroller() {
    return helper.getScroller(impl);
  }

  @Override
  public void setToolbar(Element toolbar) {
    helper.setToolbar(impl, toolbar);
  }
}
