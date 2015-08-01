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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

/**
 * A wave view whose wavelets are {@link CcBasedWavelet}s, and that can be
 * opened and closed.
 *
 */
public interface CcBasedWaveView extends ObservableWaveView {

  /**
   * Listener to the view becoming opened.
   *
   * @see OperationChannelMultiplexer.Listener
   */
  interface OpenListener {
    /**
     * Called when the view becomes fully opened.
     */
    void onOpenFinished();
  }

  //
  // Channel lifecycle.
  //

  void open(OpenListener openListener);

  void close();

  //
  // Accessibility.
  //

  /**
   * Returns whether a wavelet is in a "terminal" state, referring to where the
   * server knows a participant does not have access to a wavelet but the client
   * may not. For example, if group membership changes such that the participant
   * is no longer a member.
   */
  boolean isTerminal(Wavelet wavelet);

  //
  // WaveView implementation, covariant in wavelet type.
  // TODO(user): delete this copy&paste when WaveView properly becomes
  // generic.
  //

  @Override
  CcBasedWavelet createRoot();

  @Override
  CcBasedWavelet createWavelet();

  @Override
  CcBasedWavelet createUserData();

  @Override
  CcBasedWavelet getWavelet(WaveletId waveletId);

  @Override
  CcBasedWavelet getRoot();

  @Override
  CcBasedWavelet getUserData();

  @Override
  Iterable<? extends CcBasedWavelet> getWavelets();
}
