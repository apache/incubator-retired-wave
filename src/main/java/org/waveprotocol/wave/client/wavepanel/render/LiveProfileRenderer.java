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

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.scheduler.QueueProcessor;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ParticipantView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Listens to profiles update and update the avatar images for blip contributors
 * and conversation participants.
 *
 */
final class LiveProfileRenderer implements ProfileListener {
  private final ProfileManager profiles;
  private final ModelAsViewProvider views;

  /**
   * A map of participant ids to the blips in which that participant is a
   * contributor. This renderer re-renders the entire contributor area of each
   * such blip when profile updates for those participants occur.
   */
  private final StringMap<IdentitySet<ConversationBlip>> contributions =
      CollectionUtils.createStringMap();

  /**
   * A map of participant ids to the conversations on which they are a
   * participant. This renderer re-renders the participant part of those
   * conversations when profile updates for those participants occur.
   */
  private final StringMap<IdentitySet<Conversation>> participations =
      CollectionUtils.createStringMap();

  /**
   * Task that re-renders the contributors of blips.
   */
  // Since profile updates are expected to happen in large batches, and the
  // number of blips affected by a profile update may be large, re-rendering the
  // contributors is done as an incremental task.
  private final QueueProcessor<ConversationBlip> contributorUpdater;

  LiveProfileRenderer(ProfileManager profiles, ModelAsViewProvider views,
      QueueProcessor<ConversationBlip> contributorUpdater) {
    this.profiles = profiles;
    this.views = views;
    this.contributorUpdater = contributorUpdater;
  }

  public static LiveProfileRenderer create(TimerService timer, ProfileManager profiles,
      final ModelAsViewProvider views, final ShallowBlipRenderer blipRenderer) {
    QueueProcessor<ConversationBlip> contributorUpdater =
        new QueueProcessor<ConversationBlip>(timer) {
          @Override
          public void process(ConversationBlip blip) {
            BlipView blipUi = blip != null ? views.getBlipView(blip) : null;
            BlipMetaView metaUi = blipUi != null ? blipUi.getMeta() : null;
            if (metaUi != null) {
              blipRenderer.renderContributors(blip, metaUi);
            }
          }
        };
    return new LiveProfileRenderer(profiles, views, contributorUpdater);
  }

  /**
   * Initializes this live renderer.
   */
  void init() {
    profiles.addListener(this);
  }

  /**
   * Destroys this renderer, releasing its resources.
   */
  void destroy() {
    profiles.removeListener(this);
    contributorUpdater.cancel();
  }

  /**
   * Starts propagating updates of participant {@code p}'s profile to the
   * participant rendering of the conversation {@code c}.
   */
  public void monitorParticipation(Conversation c, ParticipantId p) {
    IdentitySet<Conversation> conversations = participations.get(p.getAddress());
    if (conversations == null) {
      conversations = CollectionUtils.createIdentitySet();
      participations.put(p.getAddress(), conversations);
    }
    conversations.add(c);
  }

  /**
   * Stops propagating updates of participant {@code p}'s profile to the
   * participant rendering in conversation {@code c}.
   */
  public void unmonitorParticipation(Conversation c, ParticipantId p) {
    IdentitySet<Conversation> conversations = participations.get(p.getAddress());
    if (conversations != null) {
      conversations.remove(c);
      if (conversations.isEmpty()) {
        participations.remove(p.getAddress());
      }
    }
  }

  /**
   * Starts propagating updates of contributor {@code c}'s profile to the
   * contributor rendering of blip {@code b}.
   */
  public void monitorContribution(ConversationBlip b, ParticipantId c) {
    IdentitySet<ConversationBlip> blips = contributions.get(c.getAddress());
    if (blips == null) {
      blips = CollectionUtils.createIdentitySet();
      contributions.put(c.getAddress(), blips);
    }
    blips.add(b);
  }

  /**
   * Stops propagating updates of contributor {@code c}'s profile to the
   * contributor rendering of blip {@code b}.
   */
  public void unmonitorContribution(ConversationBlip b, ParticipantId c) {
    IdentitySet<ConversationBlip> blips = contributions.get(c.getAddress());
    if (blips != null) {
      blips.remove(b);
      if (blips.isEmpty()) {
        contributions.remove(c.getAddress());
      }
    }
  }

  @Override
  public void onProfileUpdated(final Profile profile) {
    // Update contributors later.
    IdentitySet<ConversationBlip> blips = contributions.get(profile.getAddress());
    if (blips != null) {
      blips.each(new Proc<ConversationBlip>() {
        @Override
        public void apply(ConversationBlip blip) {
          // It's not worth worrying about duplicates. This will only happen on
          // multi-contributor blips when there are profile updates of multiple
          // contributors in rapid succession. We're already re-rendering the
          // entire contributor area of every blip per individual contributor
          // update anyway, so filtering duplicate blips here is not a worthy
          // optimization.
          contributorUpdater.add(blip);
        }
      });
    }

    // Update participants now.
    IdentitySet<Conversation> conversations = participations.get(profile.getAddress());
    if (conversations != null) {
      conversations.each(new Proc<Conversation>() {
        @Override
        public void apply(Conversation conversation) {
          ParticipantView participantUi =
              views.getParticipantView(conversation, profile.getParticipantId());
          if (participantUi != null) {
            participantUi.setAvatar(profile.getImageUrl());
            participantUi.setName(profile.getFullName());
          }
        }
      });
    }
  }
}
