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

package org.waveprotocol.box.server.frontend;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.comms.WaveClientRpc;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Implements {@link ClientFrontend}.
 *
 * When a wavelet is added and it's not at version 0, buffer updates until a
 * request for the wavelet's history has completed.
 */
public class ClientFrontendImpl implements ClientFrontend, WaveBus.Subscriber {
  private static final Log LOG = Log.get(ClientFrontendImpl.class);

  private final static AtomicInteger channel_counter = new AtomicInteger(0);

  private final WaveletProvider waveletProvider;
  private final WaveletInfo waveletInfo;

  /**
   * Creates a client frontend and subscribes it to the wave bus.
   *
   * @throws WaveServerException if the server fails during initialization.
   */
  public static ClientFrontendImpl create(WaveletProvider waveletProvider, WaveBus wavebus,
      WaveletInfo waveletInfo) throws WaveServerException {

    ClientFrontendImpl impl =
        new ClientFrontendImpl(waveletProvider, waveletInfo);

    wavebus.subscribe(impl);
    return impl;
  }

  /**
   * Constructor.
   *
   * @param waveletProvider
   * @param waveDomain the server wave domain. It is assumed that the wave domain is valid.
   */
  @VisibleForTesting
  ClientFrontendImpl(
      WaveletProvider waveletProvider, WaveletInfo waveletInfo) {
    this.waveletProvider = waveletProvider;
    this.waveletInfo = waveletInfo;
  }

  @Override
  public void openRequest(ParticipantId loggedInUser, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    LOG.info("received openRequest from " + loggedInUser + " for " + waveId + ", filter "
        + waveletIdFilter + ", known wavelets: " + knownWavelets);

    // TODO(josephg): Make it possible for this to succeed & return public
    // waves.
    if (loggedInUser == null) {
      openListener.onFailure("Not logged in");
      return;
    }

    if (!knownWavelets.isEmpty()) {
      openListener.onFailure("Known wavelets not supported");
      return;
    }

    try {
      waveletInfo.initialiseWave(waveId);
    } catch (WaveServerException e) {
      LOG.severe("Wave server failed lookup for " + waveId, e);
      openListener.onFailure("Wave server failed to look up wave");
      return;
    }

    String channelId = generateChannelID();
    UserManager userManager = waveletInfo.getUserManager(loggedInUser);
    WaveViewSubscription subscription =
        userManager.subscribe(waveId, waveletIdFilter, channelId, openListener);
    LOG.info("Subscribed " + loggedInUser + " to " + waveId + " channel " + channelId);

    Set<WaveletId> waveletIds;
    try {
      waveletIds = waveletInfo.visibleWaveletsFor(subscription, loggedInUser);
    } catch (WaveServerException e1) {
      waveletIds = Sets.newHashSet();
      LOG.warning("Failed to retrieve visible wavelets for " + loggedInUser, e1);
    }
    for (WaveletId waveletId : waveletIds) {
      WaveletName waveletName = WaveletName.of(waveId, waveletId);
      // Ensure that implicit participants will also receive updates.
      // TODO (Yuri Z.) If authorizing participant was removed from the wave
      // (the shared domain participant), then all implicit participant that
      // were authorized should be unsubsrcibed.
      waveletInfo.notifyAddedImplcitParticipant(waveletName, loggedInUser);
      // The WaveletName by which the waveletProvider knows the relevant deltas

      // TODO(anorth): if the client provides known wavelets, calculate
      // where to start sending deltas from.

      CommittedWaveletSnapshot snapshotToSend;

      // Send a snapshot of the current state.
      // TODO(anorth): calculate resync point if the client already knows
      // a snapshot.
      try {
        snapshotToSend = waveletProvider.getSnapshot(waveletName);
      } catch (WaveServerException e) {
        LOG.warning("Failed to retrieve snapshot for wavelet " + waveletName, e);
        openListener.onFailure("Wave server failure retrieving wavelet");
        return;
      }

      LOG.info("snapshot in response is: " + (snapshotToSend != null));
      if (snapshotToSend == null) {
        // Send deltas.
        openListener.onUpdate(waveletName, snapshotToSend, DeltaSequence.empty(), null, null,
            channelId);
      } else {
        // Send the snapshot.
        openListener.onUpdate(waveletName, snapshotToSend, DeltaSequence.empty(),
            snapshotToSend.committedVersion, null, channelId);
      }
    }

    WaveletName dummyWaveletName = createDummyWaveletName(waveId);
    if (waveletIds.size() == 0) {
      // Send message with just the channel id.
      LOG.info("sending just a channel id for " + dummyWaveletName);
      openListener.onUpdate(dummyWaveletName, null, DeltaSequence.empty(), null, null, channelId);
    }
    LOG.info("sending marker for " + dummyWaveletName);
    openListener.onUpdate(dummyWaveletName, null, DeltaSequence.empty(), null, true, null);
  }

  private String generateChannelID() {
    return "ch" + channel_counter.addAndGet(1);
  }

  @Override
  public void submitRequest(ParticipantId loggedInUser, final WaveletName waveletName,
      final ProtocolWaveletDelta delta, final String channelId,
      final SubmitRequestListener listener) {
    final ParticipantId author = new ParticipantId(delta.getAuthor());

    if (!author.equals(loggedInUser)) {
      listener.onFailure("Author field on delta must match logged in user");
      return;
    }

    waveletInfo.getUserManager(author).submitRequest(channelId, waveletName);
    waveletProvider.submitRequest(waveletName, delta, new SubmitRequestListener() {
      @Override
      public void onSuccess(int operationsApplied,
          HashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
        listener.onSuccess(operationsApplied, hashedVersionAfterApplication,
            applicationTimestamp);
        waveletInfo.getUserManager(author).submitResponse(channelId, waveletName,
            hashedVersionAfterApplication);
      }

      @Override
      public void onFailure(String error) {
        listener.onFailure(error);
        waveletInfo.getUserManager(author).submitResponse(channelId, waveletName, null);
      }
    });
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    for (ParticipantId participant : waveletInfo.getWaveletParticipants(waveletName)) {
      waveletInfo.getUserManager(participant).onCommit(waveletName, version);
    }
  }

  /**
   * Sends new deltas to a particular user on a particular wavelet.
   * Updates the participants of the specified wavelet if the participant was added or removed.
   *
   * @param waveletName the waveletName which the deltas belong to.
   * @param participant on the wavelet.
   * @param newDeltas newly arrived deltas of relevance for participant. Must
   *        not be empty.
   * @param add whether the participant is added by the first delta.
   * @param remove whether the participant is removed by the last delta.
   */
  private void participantUpdate(WaveletName waveletName, ParticipantId participant,
      DeltaSequence newDeltas, boolean add, boolean remove) {
    if(LOG.isFineLoggable()) {
      LOG.fine("Notifying " + participant + " for " + waveletName);
    }
    if (add) {
      waveletInfo.notifyAddedExplicitWaveletParticipant(waveletName, participant);
    }
    waveletInfo.getUserManager(participant).onUpdate(waveletName, newDeltas);
    if (remove) {
      waveletInfo.notifyRemovedExplicitWaveletParticipant(waveletName, participant);
    }
  }

  /**
   * Tracks wavelet versions and ensures that the deltas are contiguous. Updates
   * wavelet subscribers with new new deltas.
   */
  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence newDeltas) {
    if (newDeltas.isEmpty()) {
      return;
    }

    WaveletName waveletName = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());

    if(waveletInfo.getCurrentWaveletVersion(waveletName).getVersion() == 0 && LOG.isWarningLoggable()) {
      LOG.warning("Wavelet does not appear to have been initialized by client. Continuing anyway.");
    }

    waveletInfo.syncWaveletVersion(waveletName, newDeltas);

    Set<ParticipantId> remainingparticipants =
        Sets.newHashSet(waveletInfo.getWaveletParticipants(waveletName));
    // Participants added during the course of newDeltas.
    Set<ParticipantId> newParticipants = Sets.newHashSet();
    for (int i = 0; i < newDeltas.size(); i++) {
      TransformedWaveletDelta delta = newDeltas.get(i);
      // Participants added or removed in this delta get the whole delta.
      for (WaveletOperation op : delta) {
        if (op instanceof AddParticipant) {
          ParticipantId p = ((AddParticipant) op).getParticipantId();
          remainingparticipants.add(p);
          newParticipants.add(p);
        }
        if (op instanceof RemoveParticipant) {
          ParticipantId p = ((RemoveParticipant) op).getParticipantId();
          remainingparticipants.remove(p);
          participantUpdate(waveletName, p, newDeltas.subList(0, i + 1), newParticipants.remove(p),
              true);
        }
      }
    }

    // Send out deltas to those who end up being participants at the end
    // (either because they already were, or because they were added).
    for (ParticipantId p : remainingparticipants) {
      boolean isNew = newParticipants.contains(p);
      participantUpdate(waveletName, p, newDeltas, isNew, false);
    }
  }

  @VisibleForTesting
  static WaveletName createDummyWaveletName(WaveId waveId) {
    final WaveletName dummyWaveletName =
      WaveletName.of(waveId, WaveletId.of(waveId.getDomain(), "dummy+root"));
    return dummyWaveletName;
  }
}
