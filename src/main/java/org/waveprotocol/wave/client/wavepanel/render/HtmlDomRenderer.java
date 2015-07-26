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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.render.WaveRenderer;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.DomRenderer;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Renders waves into HTML DOM, given a renderer that renders waves as HTML
 * closures.
 */
public final class HtmlDomRenderer implements DomRenderer {

  private final WaveRenderer<UiBuilder> driver;

  public HtmlDomRenderer(WaveRenderer<UiBuilder> driver) {
    this.driver = driver;
  }

  //
  // Temporary invokers. Anti-parser API will remove these methods.
  //

  @Override
  public Element render(ConversationView wave) {
    return parseHtml(driver.render(wave));
  }

  @Override
  public Element render(Conversation conversation) {
    return parseHtml(driver.render(conversation));
  }

  @Override
  public Element render(ConversationThread thread) {
    return parseHtml(driver.render(thread));
  }

  @Override
  public Element render(ConversationBlip blip) {
    return parseHtml(driver.render(blip));
  }

  @Override
  public Element render(Conversation conversation, ParticipantId participant) {
    return parseHtml(driver.render(conversation, participant));
  }

  /** Turns a UiBuilder rendering into a DOM element. */
  private Element parseHtml(UiBuilder ui) {
    if (ui == null) {
      return null;
    }
    SafeHtmlBuilder html = new SafeHtmlBuilder();
    ui.outputHtml(html);
    Element div = com.google.gwt.dom.client.Document.get().createDivElement();
    div.setInnerHTML(html.toSafeHtml().asString());
    Element ret = div.getFirstChildElement();
    // Detach, in order that this element looks free-floating (required by some
    // preconditions).
    ret.removeFromParent();
    return ret;
  }
}
