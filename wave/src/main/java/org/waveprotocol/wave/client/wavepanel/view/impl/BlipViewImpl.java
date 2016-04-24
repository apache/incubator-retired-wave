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

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationThread;

/**
 * Implements a blip view by delegating primitive state matters to a view
 * object, and structural state matters to a helper. The intent is that the
 * helper is a flyweight handler.
 *
 * @param <I> intrinsic blip implementation
 */
public final class BlipViewImpl<I extends IntrinsicBlipView> // \u2620
    extends AbstractStructuredView<BlipViewImpl.Helper<? super I>, I> // \u2620
    implements BlipView {

  /**
   * Handles structural queries on blip views.
   *
   * @param <I> intrinsic blip implementation
   */
  public interface Helper<I> {

    ThreadView getBlipParent(I impl);

    BlipMetaView getMeta(I impl);

    void removeBlip(I impl);

    //
    // Anchors
    //

    AnchorView getDefaultAnchorBefore(I impl, AnchorView ref);

    AnchorView getDefaultAnchorAfter(I impl, AnchorView ref);

    AnchorView insertDefaultAnchorBefore(I impl, AnchorView ref, ConversationThread t);
    AnchorView insertDefaultAnchorAfter(I impl, AnchorView ref, ConversationThread t);

    InlineConversationView getConversationBefore(I impl, InlineConversationView ref);

    InlineConversationView getConversationAfter(I impl, InlineConversationView ref);

    InlineConversationView insertConversationBefore(I impl, InlineConversationView ref,
        Conversation c);

    BlipLinkPopupView createLinkPopup(I impl);
  }

  public BlipViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.BLIP;
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  //
  // Structural delegation.
  //

  @Override
  public BlipMetaView getMeta() {
    return helper.getMeta(impl);
  }

  @Override
  public AnchorView getDefaultAnchorAfter(AnchorView ref) {
    return helper.getDefaultAnchorAfter(impl, ref);
  }

  @Override
  public AnchorView getDefaultAnchorBefore(AnchorView ref) {
    return helper.getDefaultAnchorBefore(impl, ref);
  }

  @Override
  public AnchorView insertDefaultAnchorBefore(AnchorView ref, ConversationThread t) {
    return helper.insertDefaultAnchorBefore(impl, ref, t);
  }

  @Override
  public AnchorView insertDefaultAnchorAfter(AnchorView ref, ConversationThread t) {
    return helper.insertDefaultAnchorAfter(impl, ref, t);
  }

  @Override
  public InlineConversationView getConversationBefore(InlineConversationView ref) {
    return helper.getConversationBefore(impl, ref);
  }

  @Override
  public InlineConversationView getConversationAfter(InlineConversationView ref) {
    return helper.getConversationAfter(impl, ref);
  }

  @Override
  public InlineConversationView insertConversationBefore(InlineConversationView ref, Conversation c) {
    return helper.insertConversationBefore(impl, ref, c);
  }

  @Override
  public ThreadView getParent() {
    return helper.getBlipParent(impl);
  }

  @Override
  public void remove() {
    helper.removeBlip(impl);
  }

  @Override
  public BlipLinkPopupView createLinkPopup() {
    return helper.createLinkPopup(impl);
  }
}
