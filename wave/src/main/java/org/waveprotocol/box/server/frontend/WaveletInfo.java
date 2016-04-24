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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Provides services to manage and track wavelet participants and wavelet
 * subscriptions.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 * @see ClientFrontendImpl
 */
public class WaveletInfo {
  private static final Log LOG = Log.get(WaveletInfo.class);

  /** Information we hold in memory for each wavelet. */
  private static class PerWavelet {
    private final HashedVersion version0;
    private final Set<ParticipantId> explicitParticipants;
    private final Set<ParticipantId> implicitParticipants;
    private HashedVersion currentVersion;

    PerWavelet(WaveletName waveletName, HashedVersion hashedVersionZero) {
      this.explicitParticipants = Sets.newHashSet();
      this.implicitParticipants = Sets.newHashSet();
      this.version0 = hashedVersionZero;
      this.currentVersion = version0;
    }

    synchronized HashedVersion getCurrentVersion() {
      return currentVersion;
    }

    synchronized void setCurrentVersion(HashedVersion version) {
      this.currentVersion = version;
    }
  }

  private final LoadingCache<ParticipantId, UserManager> perUser;
  private final LoadingCache<WaveId, LoadingCache<WaveletId, PerWavelet>> perWavelet;
  private final WaveletProvider waveletProvider;

  /**
   * Creates new instance of {@link WaveletInfo}.
   *
   * @param hashFactory the factory for hashed versions.
   * @param provider the {@link WaveletProvider}.
   * @return new {@link WaveletInfo} instance.
   */
  public static WaveletInfo create(HashedVersionFactory hashFactory, WaveletProvider provider) {
    return new WaveletInfo(hashFactory, provider);
  }

  WaveletInfo(final HashedVersionFactory hashedVersionFactory, WaveletProvider waveletProvider) {
    this.waveletProvider = waveletProvider;
    perWavelet =
        CacheBuilder.newBuilder().build(new CacheLoader<WaveId, LoadingCache<WaveletId, PerWavelet>>() {
      @Override
      public LoadingCache<WaveletId, PerWavelet> load(final WaveId waveId) {
        return CacheBuilder.newBuilder().build(new CacheLoader<WaveletId, PerWavelet>() {
          @Override
          public PerWavelet load(WaveletId waveletId) {
            WaveletName waveletName = WaveletName.of(waveId, waveletId);
            return new PerWavelet(waveletName, hashedVersionFactory
                .createVersionZero(waveletName));
          }
        });
      }
    });

    perUser = CacheBuilder.newBuilder().build(new CacheLoader<ParticipantId, UserManager>() {
      @Override
      public UserManager load(ParticipantId from) {
        return new UserManager();
      }
    });
  }

  /**
   * Returns all visible wavelets in the wave specified by subscription which
   * are also comply with the subscription filter.
   */
  public Set<WaveletId> visibleWaveletsFor(WaveViewSubscription subscription,
      ParticipantId loggedInUser) throws WaveServerException {
    Set<WaveletId> visible = Sets.newHashSet();
    Set<Entry<WaveletId, PerWavelet>> entrySet;
    try {
      entrySet = perWavelet.get(subscription.getWaveId()).asMap().entrySet();
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
    for (Entry<WaveletId, PerWavelet> entry : entrySet) {
      WaveletName waveletName = WaveletName.of(subscription.getWaveId(), entry.getKey());
      if (subscription.includes(entry.getKey())
          && waveletProvider.checkAccessPermission(waveletName, loggedInUser)) {
        visible.add(entry.getKey());
      }
    }
    return visible;
  }

  /**
   * Initializes front-end information from the wave store, if necessary.
   */
  public void initialiseWave(WaveId waveId) throws WaveServerException {
    if(LOG.isFineLoggable()) {
      LOG.fine("frontend initialiseWave(" + waveId +")");
    }

    try {
      if (perWavelet.getIfPresent(waveId) == null) {
        LoadingCache<WaveletId, PerWavelet> wavelets = perWavelet.get(waveId);
        for (WaveletId waveletId : waveletProvider.getWaveletIds(waveId)) {
          ReadableWaveletData wavelet =
              waveletProvider.getSnapshot(WaveletName.of(waveId, waveletId)).snapshot;
          // Wavelets is a computing map, so get() initializes the entry.
          PerWavelet waveletInfo = wavelets.get(waveletId);
          synchronized (waveletInfo) {
            waveletInfo.currentVersion = wavelet.getHashedVersion();
            if(LOG.isFineLoggable()) {
              LOG.fine("frontend wavelet " + waveletId + " @" + wavelet.getHashedVersion().getVersion());
            }
            waveletInfo.explicitParticipants.addAll(wavelet.getParticipants());
          }
        }
      }
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Synchronizes the wavelet version and ensures that the deltas are
   * contiguous.
   *
   * @param waveletName the wavelet name.
   * @param newDeltas the new deltas.
   */
  public void syncWaveletVersion(WaveletName waveletName, DeltaSequence newDeltas) {
    HashedVersion expectedVersion;
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      expectedVersion = waveletInfo.getCurrentVersion();
      Preconditions.checkState(expectedVersion.getVersion() == newDeltas.getStartVersion(),
          "Expected deltas starting at version %s, got %s", expectedVersion,
          newDeltas.getStartVersion());
      waveletInfo.setCurrentVersion(newDeltas.getEndVersion());
    }
  }

  /**
   * Returns {@link UserManager} for the participant.
   */
  public UserManager getUserManager(ParticipantId participantId) {
    try {
      return perUser.get(participantId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Returns the current wavelet version.
   */
  public HashedVersion getCurrentWaveletVersion(WaveletName waveletName) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      return waveletInfo.getCurrentVersion();
    }
  }

  /**
   * @param waveletName the waveletName.
   * @return the wavelet participants.
   */
  public Set<ParticipantId> getWaveletParticipants(WaveletName waveletName) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      return ImmutableSet.copyOf(waveletInfo.explicitParticipants);
    }
  }

  /**
   * @param waveletName the waveletName.
   * @return the implicit wavelet participants. An implicit participant is not a
   *         "strict" participant on the wavelet, but rather only opened the
   *         wave and listens on updates. For example, anyone can open a shared
   *         wave without becoming explicit participant.
   */
  public Set<ParticipantId> getImplicitWaveletParticipants(WaveletName waveletName) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      return ImmutableSet.copyOf(waveletInfo.explicitParticipants);
    }
  }

  /**
   * Notifies that the participant was added from the wavelet.
   *
   * @param waveletName the wavelet name.
   * @param participant the participant.
   */
  public void notifyAddedExplicitWaveletParticipant(WaveletName waveletName,
      ParticipantId participant) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      waveletInfo.explicitParticipants.add(participant);
    }
  }

  /**
   * Notifies that the participant was removed from the wavelet.
   *
   * @param waveletName the wavelet name.
   * @param participant the participant.
   */
  public void notifyRemovedExplicitWaveletParticipant(WaveletName waveletName,
      ParticipantId participant) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      waveletInfo.explicitParticipants.remove(participant);
    }
  }

  /**
   * Notifies that an implicit participant opened the wave.
   *
   * @param waveletName the wavelet name.
   * @param participant the participant.
   */
  public void notifyAddedImplcitParticipant(WaveletName waveletName, ParticipantId participant) {
    PerWavelet waveletInfo = getWavelet(waveletName);
    synchronized (waveletInfo) {
      if (!waveletInfo.explicitParticipants.contains(participant)) {
        waveletInfo.implicitParticipants.add(participant);
      }
    }
  }

  private PerWavelet getWavelet(WaveletName name) {
    try {
      return perWavelet.get(name.waveId).get(name.waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }
}
