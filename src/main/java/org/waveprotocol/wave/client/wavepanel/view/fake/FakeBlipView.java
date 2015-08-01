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

import org.waveprotocol.wave.client.common.util.LinkedSequence;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationThread;

/**
 * Fake, pojo implementation of a blip view.
 *
 */
public final class FakeBlipView implements BlipView {

  private final FakeRenderer renderer;
  private final LinkedSequence<FakeAnchor> anchors;
  private final LinkedSequence<FakeInlineConversationView> convos;
  private final FakeBlipMetaView meta;

  private FakeThreadView container;

  FakeBlipView(FakeRenderer renderer, LinkedSequence<FakeAnchor> anchors,
      LinkedSequence<FakeInlineConversationView> convos) {
    this.meta = new FakeBlipMetaView(renderer, this);
    this.renderer = renderer;
    this.anchors = anchors;
    this.convos = convos;

    for (FakeAnchor anchor : anchors) {
      anchor.setContainer(this);
    }
    for (FakeInlineConversationView convo : convos) {
      convo.setContainer(this);
    }
  }

  void setContainer(FakeThreadView container) {
    this.container = container;
  }

  @Override
  public Type getType() {
    return Type.BLIP;
  }

  @Override
  public String getId() {
    return "fakeId";
  }

  @Override
  public ThreadView getParent() {
    return container;
  }

  @Override
  public FakeBlipMetaView getMeta() {
    return meta;
  }

  @Override
  public void remove() {
    container.removeChild(this);
  }

  @Override
  public FakeAnchor getDefaultAnchorAfter(AnchorView ref) {
    return anchors.getNext(asAnchorUi(ref));
  }

  @Override
  public FakeAnchor getDefaultAnchorBefore(AnchorView ref) {
    return anchors.getPrevious(asAnchorUi(ref));
  }

  @Override
  public FakeAnchor insertDefaultAnchorBefore(AnchorView ref, ConversationThread t) {
    FakeAnchor anchor = (FakeAnchor) renderer.render(t);
    anchor.setContainer(this);
    anchors.insertBefore(asAnchorUi(ref), anchor);
    return anchor;
  }

  @Override
  public FakeAnchor insertDefaultAnchorAfter(AnchorView ref, ConversationThread t) {
    FakeAnchor anchor = (FakeAnchor) renderer.render(t);
    anchor.setContainer(this);
    anchors.insertAfter(asAnchorUi(ref), anchor);
    return anchor;
  }

  @Override
  public InlineConversationView getConversationBefore(InlineConversationView ref) {
    return convos.getPrevious(asConvUi(ref));
  }

  @Override
  public InlineConversationView getConversationAfter(InlineConversationView ref) {
    return convos.getNext(asConvUi(ref));
  }

  @Override
  public FakeInlineConversationView insertConversationBefore(
      InlineConversationView ref, Conversation conv) {
    FakeInlineConversationView convUi = (FakeInlineConversationView) renderer.render(conv);
    convUi.setContainer(this);
    convos.insertBefore(asConvUi(ref), convUi);
    return convUi;
  }

  @Override
  public BlipLinkPopupView createLinkPopup() {
    return new FakeBlipLinkPopupView(this);
  }

  void removeChild(FakeAnchor x) {
    anchors.remove(x);
  }

  void removeChild(BlipMetaView x) {
    throw new UnsupportedOperationException("Fakes do not support dynamic metas");
  }

  private FakeAnchor asAnchorUi(View ref) {
    return (FakeAnchor) ref;
  }

  private FakeInlineConversationView asConvUi(View ref) {
    return (FakeInlineConversationView) ref;
  }

  @Override
  public String toString() {
    return "Blip [" + //
      "meta: " + meta + //
      (anchors.isEmpty() ? "" : ", default-anchors: " + anchors.toString()) + //
      "]";
  }
}
