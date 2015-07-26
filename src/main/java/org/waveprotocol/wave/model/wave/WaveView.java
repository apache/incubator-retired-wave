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

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * A Wave View is a collection of wavelets within a wave.
 *
 */
public interface WaveView {
  /**
   * Gets the unique identifier of the wave in view.
   *
   * @return the unique identifier of the wave.
   */
  WaveId getWaveId();

  /**
   * Gets the wavelets in this wave view. The order of iteration is unspecified.
   *
   * @return wavelets in this wave view.
   */
  Iterable<? extends Wavelet> getWavelets();

  /**
   * Gets a wavelet from the view by id.
   *
   * @return the requested wavelet, or null if it is not in view.
   */
  Wavelet getWavelet(WaveletId waveletId);

  /**
   * Creates a new wavelet in the wave.
   *
   * @return a new wavelet.
   */
  Wavelet createWavelet();

  /**
   * Gets some wavelet nominated to be a "root".
   *
   * @return the root wavelet, if it exists in the view.
   */
  Wavelet getRoot();

  /**
   * Creates a "root" wavelet.  Implementations of this interface are free to
   * choose what the root wavelet is, as long as it is the same wavelet later
   * returned by {@link #getRoot()}.
   *
   * @return the new wavelet.
   */
  Wavelet createRoot();

  /**
   * Gets the user-data wavelet in this view.
   *
   * @return the root wavelet, if it exists in the view.
   */
  Wavelet getUserData();

  /**
   * Creates the user-data wavelet.
   *
   * @return the new wavelet.
   * @throws IllegalStateException if the user-data wavelet already exists.
   */
  Wavelet createUserData();

}
