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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import static org.waveprotocol.wave.client.uibuilder.OutputHelper.image;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ParticipantsViewBuilder.Css;

/**
 * UiBuilder for a participant.
 *
 */
public final class ParticipantAvatarViewBuilder implements IntrinsicParticipantView, UiBuilder {

  private final Css css;
  private final String id;

  private String avatarUrl;
  private String name;

  @VisibleForTesting
  ParticipantAvatarViewBuilder(String id, Css css) {
    this.id = id;
    this.css = css;
  }

  public static ParticipantAvatarViewBuilder create(String id) {
    return new ParticipantAvatarViewBuilder(id, WavePanelResourceLoader.getParticipants().css());
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setAvatar(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    image(output,
        id,
        css.participant(),
        EscapeUtils.fromString(avatarUrl),
        EscapeUtils.fromString(name),
        TypeCodes.kind(Type.PARTICIPANT));
  }
}
