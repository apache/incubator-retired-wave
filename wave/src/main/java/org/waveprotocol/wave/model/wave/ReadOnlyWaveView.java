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

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Collections;
import java.util.Map;

/**
 * A read-only ObservableWaveView
 *
 */
public class ReadOnlyWaveView implements ObservableWaveView {
  private final WaveId waveId;
  private final Map<WaveletId, ObservableWavelet> wavelets = CollectionUtils.newHashMap();

  public ReadOnlyWaveView(WaveId waveId) {
    this.waveId = waveId;
  }

  @Override
  public WaveId getWaveId() {
    return waveId;
  }

  /**
   * Returns the conversational root wavelet, if such a wavelet is in view.
   */
  @Override
  public ObservableWavelet getRoot() {
    for (ObservableWavelet w : wavelets.values()) {
      if (IdUtil.isConversationRootWaveletId(w.getId())) {
        return w;
      }
    }
    return null;
  }

  @Override
  public ObservableWavelet createRoot() {
    throw new UnsupportedOperationException("Read only wave views are read-only");
  }

  @Override
  public Iterable<? extends ObservableWavelet> getWavelets() {
    return Collections.unmodifiableCollection(wavelets.values());
  }

  @Override
  public ObservableWavelet getWavelet(WaveletId waveletId) {
    return wavelets.get(waveletId);
  }

  @Override
  public ObservableWavelet createWavelet() {
    throw new UnsupportedOperationException("Read only wave views are read-only");
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public ObservableWavelet getUserData() {
    throw new UnsupportedOperationException("Read only wave views don't support"
        + " user data wavelets");
  }

  /**
   * @throws UnsupportedOperationException always
   */
  @Override
  public ObservableWavelet createUserData() {
    throw new UnsupportedOperationException("Read only wave views don't support"
        + " user data wavelets");
  }

  /**
   * Listeners are ignored in this read-only view.
   */
  @Override
  public void addListener(WaveViewListener listener) {
    // Listeners are ignored.
  }

  /**
   * Listeners are ignored in this read-only view.
   */
  @Override
  public void removeListener(WaveViewListener listener) {
    // Listeners are ignored.
  }

  /**
   * Adds a wavelet to this view.
   *
   * @param wavelet the wavelet to add
   * @throws IllegalArgumentException if a wavelet with the same id is already
   *         in the view or the wavelet is from a different wave
   */
  public void addWavelet(ObservableWavelet wavelet) {
    Preconditions.checkArgument(wavelet.getWaveId().equals(waveId),
        "Attempted to add wavelet to wrong view");
    Preconditions.checkArgument(!wavelets.containsKey(wavelet.getId()),
        "Attempted to add duplicate wavelet to view");
    wavelets.put(wavelet.getId(), wavelet);
  }
}
