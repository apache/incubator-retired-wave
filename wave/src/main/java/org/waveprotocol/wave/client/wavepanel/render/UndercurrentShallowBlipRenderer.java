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

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.util.DateUtils;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.supplement.ReadableSupplementedWave;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;
import java.util.Date;

/**
 * Defines the shallow blip rendering for the Undercurrent UI.
 *
 */
public final class UndercurrentShallowBlipRenderer implements ShallowBlipRenderer {
  private static final int MAX_CONTRIBUTORS = 3;

  /** Provides names and avatars of participants. */
  private final ProfileManager manager;

  /** Provides read state of blips. */
  private final ReadableSupplementedWave supplement;

  /** Provides direct access to a DateUtils instance */
  private final DateUtils dateUtils;

  /**
   * Defines the rendering function for the contents of a blip.
   */
  public interface DocumentRenderer {
    SafeHtml render(String blipId, Document doc);
  }

  public UndercurrentShallowBlipRenderer(
      ProfileManager manager, ReadableSupplementedWave supplement, DateUtils dateUtils) {
    this.manager = manager;
    this.supplement = supplement;
    this.dateUtils = dateUtils;
  }

  @Override
  public void render(ConversationBlip blip, IntrinsicBlipMetaView view) {
    renderContributors(blip, view);
    renderTime(blip, view);
    renderRead(blip, view);
  }

  @Override
  public void renderContributors(ConversationBlip blip, IntrinsicBlipMetaView meta) {
    Set<ParticipantId> contributors = blip.getContributorIds();
    if (!contributors.isEmpty()) {
      meta.setAvatar(avatarOf(contributors.iterator().next()));
      meta.setMetaline(buildNames(contributors));
    } else {
      // Blips are never meant to have no contributors.  The wave state is broken.
      meta.setAvatar("");
      meta.setMetaline("anon");
    }
  }

  @Override
  public void renderTime(ConversationBlip blip, IntrinsicBlipMetaView meta) {
    if (blip.getLastModifiedTime() == 0) {
      //Blip sent using c/s protocol, which has no timestamp attached (WAVE-181)
      //Using received time as an estimate of the sent time
      meta.setTime(dateUtils.formatPastDate(new Date().getTime()));
    }
    else {
      meta.setTime(dateUtils.formatPastDate(blip.getLastModifiedTime()));
    }
  }

  @Override
  public void renderRead(ConversationBlip blip, IntrinsicBlipMetaView blipUi) {
    blipUi.setRead(!supplement.isUnread(blip));
  }

  /**
   * @return the rich text for the contributors in a blip.
   */
  private String buildNames(Set<ParticipantId> contributors) {
    StringBuilder names = new StringBuilder();
    int i = 0;
    for (ParticipantId contributor : contributors) {
      if (i >= MAX_CONTRIBUTORS) {
        break;
      } else if (manager.shouldIgnore(contributor)) {
        continue;
      }

      if (i > 0) {
        names.append(", ");
      }
      names.append(nameOf(contributor));
      i++;
    }
    return names.toString();
  }

  private String nameOf(ParticipantId contributor) {
    return manager.getProfile(contributor).getFirstName();
  }

  private String avatarOf(ParticipantId contributor) {
    return manager.getProfile(contributor).getImageUrl();
  }
}
