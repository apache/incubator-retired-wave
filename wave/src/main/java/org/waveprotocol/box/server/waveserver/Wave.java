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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.ListenableFuture;

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

/**
 * The wavelets in a wave.
 *
 * @author soren@google.com (Soren Lassen)
 */
final class Wave implements Iterable<WaveletContainer> {
  private static final Log LOG = Log.get(Wave.class);

  private class WaveletCreator<T extends WaveletContainer> extends CacheLoader<WaveletId, T> {
    private final WaveletContainer.Factory<T> factory;

    private final String waveDomain;

    public WaveletCreator(WaveletContainer.Factory<T> factory, String waveDomain) {
      this.factory = factory;
      this.waveDomain = waveDomain;
    }

    @Override
    public T load(WaveletId waveletId) {
      return factory.create(notifiee, WaveletName.of(waveId, waveletId), waveDomain);
    }
  }

  private final WaveId waveId;
  /** Future providing already-existing wavelets in storage. */
  private final ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets;
  private final LoadingCache<WaveletId, LocalWaveletContainer> localWavelets;
  private final LoadingCache<WaveletId, RemoteWaveletContainer> remoteWavelets;
  private final WaveletNotificationSubscriber notifiee;

  /**
   * Creates a wave. The {@code lookupWavelets} future is examined only when a
   * query is first made.
   */
  public Wave(WaveId waveId,
      ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets,
      WaveletNotificationSubscriber notifiee, LocalWaveletContainer.Factory localFactory,
      RemoteWaveletContainer.Factory remoteFactory,
      String waveDomain) {
    this.waveId = waveId;
    this.lookedupWavelets = lookedupWavelets;
    this.notifiee = notifiee;

    this.localWavelets = CacheBuilder.newBuilder().build(
        new WaveletCreator<LocalWaveletContainer>(localFactory, waveDomain));
    this.remoteWavelets = CacheBuilder.newBuilder().build(
        new WaveletCreator<RemoteWaveletContainer>(remoteFactory, waveDomain));
  }

  @Override
  public Iterator<WaveletContainer> iterator() {
    return Iterators.unmodifiableIterator(
        Iterables.concat(localWavelets.asMap().values(), remoteWavelets.asMap().values()).iterator());
  }

  LocalWaveletContainer getLocalWavelet(WaveletId waveletId)
      throws WaveletStateException {
    return getWavelet(waveletId, localWavelets);
  }

  RemoteWaveletContainer getRemoteWavelet(WaveletId waveletId)
      throws WaveletStateException {
    return getWavelet(waveletId, remoteWavelets);
  }

  LocalWaveletContainer getOrCreateLocalWavelet(WaveletId waveletId) {
    try {
      return localWavelets.get(waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletId waveletId) {
    try {
      return remoteWavelets.get(waveletId);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  private <T extends WaveletContainer> T getWavelet(WaveletId waveletId,
      LoadingCache<WaveletId, T> waveletsMap) throws WaveletStateException {
    ImmutableSet<WaveletId> storedWavelets;
    try {
      storedWavelets =
          FutureUtil.getResultOrPropagateException(lookedupWavelets, PersistenceException.class);
    } catch (PersistenceException e) {
      throw new WaveletStateException(
          "Failed to lookup wavelet " + WaveletName.of(waveId, waveletId), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WaveletStateException(
          "Interrupted looking up wavelet " + WaveletName.of(waveId, waveletId), e);
    }

    if(LOG.isFineLoggable()) {
      if(storedWavelets != null) {
        if(storedWavelets.contains(waveletId)) {
          LOG.fine("Wavelet is in storedWavelets");
        }
        if(waveletsMap.getIfPresent(waveletId) != null) {
          LOG.fine("Wavelet is in wavletsMap");
        }
      }
    }

    // Since waveletsMap is a computing map, we must call getIfPresent(waveletId)
    // to tell if waveletId is mapped, we cannot test if get(waveletId) returns null.
    if (storedWavelets != null && !storedWavelets.contains(waveletId)
        && waveletsMap.getIfPresent(waveletId) == null) {
      return null;
    } else {
      try {
        T wavelet = waveletsMap.get(waveletId);
        return wavelet;
      } catch (CacheLoader.InvalidCacheLoadException ex) {
        return null;
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  ListenableFuture<ImmutableSet<WaveletId>> getLookedupWavelets() {
    return lookedupWavelets;
  }
}
