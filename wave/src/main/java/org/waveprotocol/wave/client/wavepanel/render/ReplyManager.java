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

package org.waveprotocol.wave.client.wavepanel.render;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.wavepanel.render.InlineAnchorLiveRenderer.ReplyDoodad;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;

public class ReplyManager implements InlineAnchorLiveRenderer.AnchorHandler {


  private final ModelAsViewProvider views;

  public ReplyManager(ModelAsViewProvider views) {
    this.views = views;
  }

  @Override
  public void onAnchorAddedBefore(ConversationBlip blip, ReplyDoodad ref, ReplyDoodad dood) {
    BlipView blipUi = views.getBlipView(blip);
    BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;
    if (metaUi != null) {
      AnchorView inlineUi = dood.getAnchor();
      metaUi.insertInlineAnchorBefore(ref != null ? ref.getAnchor() : null, inlineUi);

      // Move reply if it exists.
      ConversationThread reply = blip.getReplyThread(dood.getId());
      AnchorView defaultUi = reply != null ? views.getDefaultAnchor(reply) : null;
      InlineThreadView replyUi = defaultUi != null ? defaultUi.getThread() : null;
      if (replyUi != null) {
        defaultUi.detach(replyUi);
        inlineUi.attach(replyUi);
      }
    }
  }

  @Override
  public void onAnchorRemoved(ConversationBlip blip, ReplyDoodad dood) {
    BlipView blipUi = views.getBlipView(blip);
    BlipMetaView metaUi = blip != null ? blipUi.getMeta() : null;
    if (metaUi != null) {
      AnchorView inlineUi = dood.getAnchor();

      // Move reply if there is one.
      ConversationThread reply = blip.getReplyThread(dood.getId());
      AnchorView defaultUi = reply != null ? views.getDefaultAnchor(reply) : null;
      InlineThreadView replyUi = inlineUi.getThread();
      if (replyUi != null) {
        inlineUi.detach(replyUi);
        defaultUi.attach(replyUi);
      }

      dood.getAnchor().remove();
    }
  }

  public InlineThreadView presentAfter(BlipView blipUi, ConversationThread ref,
      ConversationThread thread) {
    AnchorView refDefault = ref != null ? views.getDefaultAnchor(ref) : null;
    Preconditions.checkArgument(ref == null || refDefault != null, "ref is not rendered");

    // Render thread in default anchor (and move to inline if there is one).
    AnchorView defaultUi = blipUi.insertDefaultAnchorAfter(refDefault, thread);
    InlineThreadView threadUi = defaultUi.getThread();
    AnchorView inlineUi = views.getInlineAnchor(thread);
    if (inlineUi != null) {
      defaultUi.detach(threadUi);
      inlineUi.attach(threadUi);
    }
    return threadUi;
  }
}
