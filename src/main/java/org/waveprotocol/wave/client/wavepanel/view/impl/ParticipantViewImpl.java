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

import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.widget.profile.ProfilePopupView;

/**
 * Implements a participant view by delegating primitive state matters to a view
 * object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic participant implementation
 */
public final class ParticipantViewImpl<I extends IntrinsicParticipantView> // \u2620
    extends AbstractStructuredView<ParticipantViewImpl.Helper<? super I>, I> // \u2620
    implements ParticipantView {

  /**
   * Handles structural queries on participant views.
   *
   * @param <I> intrinsic participant implementation
   */
  public interface Helper<I> {
    void remove(I impl);

    ProfilePopupView showParticipation(I impl);
  }

  public ParticipantViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.PARTICIPANT;
  }

  @Override
  public void remove() {
    helper.remove(impl);
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  @Override
  public void setAvatar(String url) {
    impl.setAvatar(url);
  }

  @Override
  public void setName(String name) {
    impl.setName(name);
  }

  @Override
  public ProfilePopupView showParticipation() {
    return helper.showParticipation(impl);
  }
}
