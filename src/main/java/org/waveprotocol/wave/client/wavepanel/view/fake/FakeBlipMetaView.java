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
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.FocusFrameView;
import org.waveprotocol.wave.model.conversation.ConversationThread;

import java.util.Set;

/**
 * Fake, pojo implementation of a blip meta view.
 *
 */
public final class FakeBlipMetaView implements BlipMetaView {

  private final FakeRenderer renderer;
  private final FakeBlipView container;
  private final LinkedSequence<AnchorView> anchors = LinkedSequence.create();
  private FakeDocumentView content;

  FakeBlipMetaView(FakeRenderer renderer, FakeBlipView container) {
    this.renderer = renderer;
    this.container = container;
  }

  @Override
  public Type getType() {
    return Type.META;
  }

  @Override
  public BlipView getParent() {
    return container;
  }

  @Override
  public AnchorView getInlineAnchorBefore(AnchorView ref) {
    return anchors.getPrevious(ref);
  }

  @Override
  public AnchorView getInlineAnchorAfter(AnchorView ref) {
    return anchors.getNext(ref);
  }

  @Override
  public void insertInlineAnchorBefore(AnchorView ref, AnchorView x) {
    anchors.insertBefore(ref, x);
  }

  @Override
  public void remove() {
    container.removeChild(this);
  }

  public FakeAnchor createInlineAnchorBefore(AnchorView ref, ConversationThread thread) {
    FakeAnchor anchor = renderer.createInlineAnchor(thread);
    anchor.setContainer(this);
    insertInlineAnchorBefore(ref, anchor);
    return anchor;
  }

  void removeChild(FakeInlineThreadView thread) {
    // Ignore
  }

  public void setContent(FakeDocumentView content) {
    this.content = content;
  }

  //
  // Uninteresting.
  //

  @Override
  public void placeFocusFrame(FocusFrameView frame) {
  }

  @Override
  public void removeFocusChrome(FocusFrameView frame) {
  }

  @Override
  public void setAvatar(String avatar) {
  }

  @Override
  public void setTime(String time) {
  }

  @Override
  public void setMetaline(String metaline) {
  }

  @Override
  public void setRead(boolean read) {
  }

  @Override
  public void enable(Set<MenuOption> options) {
  }

  @Override
  public void disable(Set<MenuOption> options) {
  }

  @Override
  public void select(MenuOption option) {
  }

  @Override
  public void deselect(MenuOption option) {
  }

  @Override
  public String toString() {
    return "Meta [" //
        + "content: " + content //
        + (anchors.isEmpty() ? "" : ", anchors: " + anchors.toString()) //
        + "]";
  }
}
