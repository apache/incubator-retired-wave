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

import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadReplyIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * Implements a thread view by delegating primitive state matters to a view
 * object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic thread implementation
 * @param <H> flyweight handler / helper type
 */
abstract class ThreadViewImpl<
    I extends IntrinsicThreadView, H extends ThreadViewImpl.Helper<? super I>>
    extends AbstractStructuredView<H, I> implements ThreadView {

  /**
   * Handles structural queries on thread views.
   *
   * @param <I> intrinsic thread implementation
   */
  public interface Helper<I> {

    BlipView insertBlipBefore(I thread, View ref, ConversationBlip blip);

    BlipView insertBlipAfter(I thread, View ref, ConversationBlip blip);

    BlipView getBlipBefore(I thread, View ref);

    BlipView getBlipAfter(I thread, View ref);

    View getThreadParent(I thread);

    void removeThread(I thread);

    ThreadReplyIndicatorView getIndicator( I thread );
  }

  public ThreadViewImpl(H helper, I impl) {
    super(helper, impl);
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  @Override
  public BlipView getBlipBefore(View ref) {
    return helper.getBlipBefore(impl, ref);
  }

  @Override
  public BlipView getBlipAfter(View ref) {
    return helper.getBlipAfter(impl, ref);
  }

  @Override
  public BlipView insertBlipBefore(View ref, ConversationBlip blip) {
    return helper.insertBlipBefore(impl, ref, blip);
  }

  @Override
  public BlipView insertBlipAfter(View ref, ConversationBlip blip) {
    return helper.insertBlipAfter(impl, ref, blip);
  }

  @Override
  public View getParent() {
    return helper.getThreadParent(impl);
  }

  @Override
  public void remove() {
    helper.removeThread(impl);
  }
}
