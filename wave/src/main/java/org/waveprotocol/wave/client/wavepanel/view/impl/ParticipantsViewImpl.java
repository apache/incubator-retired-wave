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

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantsView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantsView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Implements a participants-collection view by delegating primitive state
 * matters to a view object, and structural state matters to a helper. The
 * intent is that the helper is a flyweight handler.
 *
 * @param <I> intrinsic participants implementation
 */
public final class ParticipantsViewImpl<I extends IntrinsicParticipantsView> // \u2620
    extends AbstractStructuredView<ParticipantsViewImpl.Helper<? super I>, I> // \u2620
    implements ParticipantsView {

  /**
   * Handles structural queries on participants views.
   *
   * @param <I> intrinsic participants implementation
   */
  public interface Helper<I> {
    ParticipantView append(I impl, Conversation conv, ParticipantId participant);
    void remove(I impl);
  }

  public ParticipantsViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.PARTICIPANTS;
  }

  @Override
  public ParticipantView appendParticipant(Conversation conv, ParticipantId participant) {
    return helper.append(impl, conv, participant);
  }

  @Override
  public void remove() {
    helper.remove(impl);
  }

  @Override
  public String getId() {
    return impl.getId();
  }
}
