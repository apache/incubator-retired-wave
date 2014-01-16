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

package org.waveprotocol.box.server.waveserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
@Singleton
public class MemoryPerUserWaveViewHandlerImpl implements PerUserWaveViewHandler {

  private static final Log LOG = Log.get(MemoryPerUserWaveViewHandlerImpl.class);

  /**
   * The period of time in minutes the per user waves view should be actively
   * kept up to date after last access.
   */
  private static final int PER_USER_WAVES_VIEW_CACHE_MINUTES = 5;

  /** The loading cache that holds wave viev per each online user.*/
  public LoadingCache<ParticipantId, Multimap<WaveId, WaveletId>> explicitPerUserWaveViews;

  @Inject
  public MemoryPerUserWaveViewHandlerImpl(final WaveMap waveMap) {
    // Let the view expire if it not accessed for some time.
    explicitPerUserWaveViews =
        CacheBuilder.newBuilder().expireAfterAccess(PER_USER_WAVES_VIEW_CACHE_MINUTES, TimeUnit.MINUTES)
            .<ParticipantId, Multimap<WaveId, WaveletId>>build(new CacheLoader<ParticipantId, Multimap<WaveId, WaveletId>>() {

              @Override
              public Multimap<WaveId, WaveletId> load(final ParticipantId user) {
                Multimap<WaveId, WaveletId> userView = HashMultimap.create();

                // Create initial per user waves view by looping over all waves
                // in the waves store.
                Map<WaveId, Wave> waves = waveMap.getWaves();
                for (Map.Entry<WaveId, Wave> entry : waves.entrySet()) {
                  Wave wave = entry.getValue();
                  for (WaveletContainer c : wave) {
                    WaveletId waveletId = c.getWaveletName().waveletId;
                    try {
                      if (!c.hasParticipant(user)) {
                        continue;
                      }
                      // Add this wave to the user view.
                      userView.put(entry.getKey(), waveletId);
                    } catch (WaveletStateException e) {
                      LOG.warning("Failed to access wavelet " + c.getWaveletName(), e);
                    }
                  }
                }
                LOG.info("Initalized waves view for user: " + user.getAddress()
                    + ", number of waves in view: " + userView.size());
                return userView;
              }
            });
  }

  @Override
  public ListenableFuture<Void> onParticipantAdded(WaveletName waveletName, ParticipantId user) {
    Multimap<WaveId, WaveletId> perUserView = explicitPerUserWaveViews.getIfPresent(user);
    if (perUserView != null) {
      if (!perUserView.containsEntry(waveletName.waveId, waveletName.waveletId)) {
        perUserView.put(waveletName.waveId, waveletName.waveletId);
        if(LOG.isFineLoggable()) {
          LOG.fine("Added wavelet: " + waveletName + " to the view of user: " + user.getAddress());
          LOG.fine("View size is now: " + perUserView.size());
        }
      }
    }
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }

  @Override
  public ListenableFuture<Void> onParticipantRemoved(WaveletName waveletName, ParticipantId user) {
    Multimap<WaveId, WaveletId> perUserView = explicitPerUserWaveViews.getIfPresent(user);
    if (perUserView != null) {
      if (perUserView.containsEntry(waveletName.waveId, waveletName.waveletId)) {
        perUserView.remove(waveletName.waveId, waveletName.waveletId);
        LOG.fine("Removed wavelet: " + waveletName
            + " from the view of user: " + user.getAddress());
      }
    }
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }

  @Override
  public Multimap<WaveId, WaveletId> retrievePerUserWaveView(ParticipantId user) {
    try {
      return explicitPerUserWaveViews.get(user);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public ListenableFuture<Void> onWaveInit(WaveletName waveletName) {
    // No op.
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }
}
