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

package org.waveprotocol.box.webclient.client;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.StageOne;
import org.waveprotocol.wave.client.StageTwo;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.AsyncHolder;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Set;

/**
 * Provides stage 2 of the staged loading of the wave panel
 *
 * @author zdwang@google.com (David Wang)
 */
public class StageTwoProvider extends StageTwo.DefaultProvider {

  private final WaveRef waveRef;
  private final RemoteViewServiceMultiplexer channel;
  private final boolean isNewWave;
  // TODO: Remove this after WebClientBackend is deleted.
  private final IdGenerator idGenerator;

  // shared across other client components
  private final ProfileManager profiles;

  /**
   * Continuation to progress to the next stage. This will disappear with the
   * new protocol.
   */
  private AsyncHolder.Accessor<StageTwo> whenReady;
  private final Set<ParticipantId> otherParticipants;

  /**
   * @param waveId the id of the wave to open, or null to create a new wave
   * @param channel communication channel
   * @param idGenerator
   * @param otherParticipants the participants to add to the newly created wave,
   *        in addition to the creator. {@code null} if only the creator
   *        should be added.
   * @param unsavedIndicatorElement
   */
  public StageTwoProvider(StageOne stageOne, WaveRef waveRef, RemoteViewServiceMultiplexer channel,
      boolean isNewWave, IdGenerator idGenerator, ProfileManager profiles,
      UnsavedDataListener unsavedDataListener, Set<ParticipantId> otherParticipants) {
    super(stageOne, unsavedDataListener);
    Preconditions.checkArgument(stageOne != null);
    Preconditions.checkArgument(waveRef != null);
    Preconditions.checkArgument(waveRef.getWaveId() != null);
    this.waveRef = waveRef;
    this.channel = channel;
    this.isNewWave = isNewWave;
    this.idGenerator = idGenerator;
    this.profiles = profiles;
    this.otherParticipants = otherParticipants;
  }

  @Override
  protected SchemaProvider createSchemas() {
    return new ConversationSchemas();
  }

  @Override
  public String createSessionId() {
    return Session.get().getIdSeed();
  }

  @Override
  protected IdGenerator createIdGenerator() {
    return idGenerator;
  }

  @Override
  protected ParticipantId createSignedInUser() {
    return ParticipantId.ofUnsafe(Session.get().getAddress());
  }

  @Override
  protected WaveViewService createWaveViewService() {
    return new RemoteWaveViewService(waveRef.getWaveId(), channel, getDocumentRegistry());
  }

  /**
   * Swaps order of open and render.
   */
  @Override
  protected void install() {
    if (isNewWave) {
      // For a new wave, initial state comes from local initialization.
      getConversations().createRoot().getRootThread().appendBlip();

      // Adding any initial participant to the new wave
      getConversations().getRoot().addParticipantIds(otherParticipants);
      super.install();
      whenReady.use(StageTwoProvider.this);
    } else {
      // For an existing wave, while we're still using the old protocol,
      // rendering must be delayed until the channel is opened, because the
      // initial state snapshots come from the channel.
      getConnector().connect(new Command() {
        @Override
        public void execute() {
          // This code must be kept in sync with the default install()
          // method, but excluding the connect() call.

          // Install diff control before rendering, because logical diff state
          // may
          // need to be adjusted due to arbitrary UI policies.
          getDiffController().install();

          // Ensure the wave is rendered.
          stageOne.getDomAsViewProvider().setRenderer(getRenderer());
          ensureRendered();

          // Install eager UI.
          installFeatures();

          // Rendering, and therefore the whole stage is now ready.
          whenReady.use(StageTwoProvider.this);
        }
      });
    }
  }

  @Override
  protected ProfileManager createProfileManager() {
    return profiles;
  }

  @Override
  protected void create(final AsyncHolder.Accessor<StageTwo> whenReady) {
    this.whenReady = whenReady;
    super.create(new AsyncHolder.Accessor<StageTwo>() {
      @Override
      public void use(StageTwo x) {
        // Delay progression until rendering is ready.
      }
    });
  }

  @Override
  protected void fetchWave(final AsyncHolder.Accessor<WaveViewData> whenReady) {
    whenReady.use(WaveViewDataImpl.create(waveRef.getWaveId()));
  }
}
