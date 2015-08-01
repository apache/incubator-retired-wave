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

package org.waveprotocol.wave.model.wave.opbased;

import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveView;
import org.waveprotocol.wave.model.wave.WaveViewListener;

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Do NOT extend this interface any further, choose to extend the top level interfaces if you want
 * to mix and match another set of features.
 *
 * This interface is simply a convenience interface that bounds the top level interfaces together
 * so that you don't have to pass 2 interfaces around all the time.
 *
 * @author zdwang@google.com (David Wang)
 * @author hearnden@google.com (David Hearnden)
 */
public interface ObservableWaveView extends WaveView, SourcesEvents<WaveViewListener> {

  //
  // Covariant specialization of Observable capabilities
  //

  @Override
  public ObservableWavelet getRoot();

  @Override
  public ObservableWavelet getUserData();

  @Override
  public Iterable<? extends ObservableWavelet> getWavelets();

  @Override
  public ObservableWavelet getWavelet(WaveletId waveletId);

  @Override
  public ObservableWavelet createWavelet();

  @Override
  public ObservableWavelet createRoot();

  @Override
  public ObservableWavelet createUserData();
}
