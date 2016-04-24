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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A skeleton implementation of {@link WaveViewData}.
 *
 */
public class WaveViewDataImpl implements WaveViewData {

  /** The wave id */
  private final WaveId id;

  /** Wavelets in this wave. */
  private final Map<WaveletId, ObservableWaveletData> wavelets = CollectionUtils.newHashMap();

  private WaveViewDataImpl(WaveId id) {
    this.id = id;
  }

  /**
   * Creates an empty wave.
   *
   * @param id wave id
   */
  public static WaveViewDataImpl create(WaveId id) {
    return new WaveViewDataImpl(id);
  }

  /**
   * Creates a wave with some initial state.
   *
   * @param id wave id
   * @param wavelets initial wavelet data
   */
  public static WaveViewDataImpl create(
      WaveId id, Collection<? extends ObservableWaveletData> wavelets) {
    WaveViewDataImpl wave = new WaveViewDataImpl(id);
    for (ObservableWaveletData wavelet : wavelets) {
      wave.addWavelet(wavelet);
    }
    return wave;
  }

  @Override
  public WaveId getWaveId() {
    return id;
  }

  @Override
  public Iterable<? extends ObservableWaveletData> getWavelets() {
    return Collections.unmodifiableCollection(wavelets.values());
  }

  @Override
  public ObservableWaveletData getWavelet(WaveletId waveletId) {
    return wavelets.get(waveletId);
  }

  @Override
  public void addWavelet(ObservableWaveletData wavelet) {
    WaveletId waveletId = wavelet.getWaveletId();
    Preconditions.checkArgument(
        !wavelets.containsKey(waveletId), "Duplicate wavelet id: %s", waveletId);
    wavelets.put(waveletId, wavelet);
  }

  @Override
  public void removeWavelet(WaveletId waveletId) {
    if (wavelets.remove(waveletId) == null) {
      throw new IllegalArgumentException(waveletId + " is not present");
    }
  }
}
