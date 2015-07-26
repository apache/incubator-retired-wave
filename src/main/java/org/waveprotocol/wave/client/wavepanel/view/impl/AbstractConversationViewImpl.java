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

import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;

/**
 * Implements a conversation view by delegating primitive state matters to a
 * view object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic implementation
 */
public abstract class AbstractConversationViewImpl<I, H extends AbstractConversationViewImpl.Helper<? super I>> // \u2620
    extends AbstractStructuredView<H, I> // \u2620
    implements ConversationView {

  /**
   * Handles structural queries on conversation views.
   *
   * @param <I> intrinsic view implementation
   */
  public interface Helper<I> {
    RootThreadView getRootThread(I impl);

    ParticipantsView getParticipants(I impl);

    void remove(I impl);
  }

  protected AbstractConversationViewImpl(H helper, I impl) {
    super(helper, impl);
  }

  //
  // Structural delegation.
  //

  @Override
  public ParticipantsView getParticipants() {
    return helper.getParticipants(impl);
  }

  @Override
  public RootThreadView getRootThread() {
    return helper.getRootThread(impl);
  }

  @Override
  public void remove() {
    helper.remove(impl);
  }
}
