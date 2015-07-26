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


package org.waveprotocol.wave.client.render;

import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * A Composite renderer helper that call deal out the rendering to multiple RendererHelper.
 *
 */
public class CompositeRendererHelper implements ResultProducingRenderHelper<SafeHtml> {

  private final ResultProducingRenderHelper<? extends UiBuilder>[] helpers;

  @SuppressWarnings("unchecked")
  public CompositeRendererHelper(ResultProducingRenderHelper<? extends UiBuilder> ... helpers){
    this.helpers = helpers;
  }

  @Override
  public void startView(ConversationView view) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.startView(view);
    }
  }

  @Override
  public void startConversation(Conversation conv) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.startConversation(conv);
    }
  }

  @Override
  public void startThread(ConversationThread thread) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.startThread(thread);
    }
  }

  @Override
  public void startInlineThread(ConversationThread thread) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.startInlineThread(thread);
    }
  }

  @Override
  public void startBlip(ConversationBlip blip) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.startBlip(blip);
    }
  }

  @Override
  public void endView(ConversationView view) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.endView(view);
    }
  }

  @Override
  public void endConversation(Conversation conv) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.endConversation(conv);
    }
  }

  @Override
  public void endThread(ConversationThread thread) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.endThread(thread);
    }
  }

  @Override
  public void endInlineThread(ConversationThread thread) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.endInlineThread(thread);
    }
  }

  @Override
  public void endBlip(ConversationBlip blip) {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.endBlip(blip);
    }
  }

  @Override
  public void begin() {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.begin();
    }
  }

  @Override
  public SafeHtml getResult() {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    for (ResultProducingRenderHelper<? extends UiBuilder> h : helpers) {
      h.getResult().outputHtml(builder);
    }
    return builder.toSafeHtml();
  }

  @Override
  public void end() {
    for (ResultProducingRenderHelper<?> h : helpers) {
      h.end();
    }
  }
}
