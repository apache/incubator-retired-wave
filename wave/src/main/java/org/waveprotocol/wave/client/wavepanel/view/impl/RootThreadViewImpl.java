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

import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicThreadView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;

/**
 * Implements a thread view by delegating primitive state matters to a view
 * object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic thread implementation
 */
public final class RootThreadViewImpl<I extends IntrinsicThreadView> // \u2620
    extends ThreadViewImpl<I, RootThreadViewImpl.Helper<? super I>> implements RootThreadView {

  /**
   * Handles structural queries on thread views.
   *
   * @param <I> intrinsic thread implementation
   */
  public interface Helper<I> extends ThreadViewImpl.Helper<I> {
    ConversationView getThreadParent(I thread);
    ReplyBoxView getIndicator(I thread);
  }

  public RootThreadViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.ROOT_THREAD;
  }

  @Override
  public ConversationView getParent() {
    return helper.getThreadParent(impl);
  }

  public ReplyBoxView getReplyIndicator() {
    return helper.getIndicator(impl);
  }

  @Override
  public void setTotalBlipCount(int totalBlipCount) {
    impl.setTotalBlipCount(totalBlipCount);
  }

  @Override
  public void setUnreadBlipCount(int unreadBlipCount) {
    impl.setUnreadBlipCount(unreadBlipCount);
  }
}
