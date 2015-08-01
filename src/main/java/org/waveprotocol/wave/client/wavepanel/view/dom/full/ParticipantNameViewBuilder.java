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

import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.ParticipantsViewBuilder.Css;

/**
 * UiBuilder for a participant.
 *
 */
public final class ParticipantNameViewBuilder implements IntrinsicParticipantView, UiBuilder {

  private final Css css;
  private final String id;

  private String name;

  @VisibleForTesting
  ParticipantNameViewBuilder(String id, Css css) {
    this.id = id;
    this.css = css;
  }

  public static ParticipantNameViewBuilder create(String id) {
    return new ParticipantNameViewBuilder(id, WavePanelResourceLoader.getParticipants().css());
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setAvatar(String avatarUrl) {
    // No avatar in this view.
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    open(output, id, css.participant(), TypeCodes.kind(Type.PARTICIPANT));
    output.appendEscaped(name);
    close(output);
  }
}
