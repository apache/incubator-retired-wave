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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import org.mockito.Mock;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.Map;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class MemoryPerUserWaveViewProviderTest extends PerUserWaveViewProviderTestBase {

  @Mock
  private WaveMap waveMap;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    LocalWaveletContainer.Factory localFactory = mock(LocalWaveletContainer.Factory.class);

    WaveletNotificationSubscriber notifiee = mock(WaveletNotificationSubscriber.class);

    SettableFuture<ImmutableSet<WaveletId>> lookedupWavelets = SettableFuture.create();
    lookedupWavelets.set(ImmutableSet.of(WAVELET_NAME.waveletId));

    Wave wave =
        new Wave(WAVELET_NAME.waveId, lookedupWavelets, notifiee, localFactory, null,
            WAVELET_NAME.waveId.getDomain());
    Map<WaveId, Wave> waves = Maps.newHashMap();
    waves.put(WAVELET_NAME.waveId, wave);
    when(waveMap.getWaves()).thenReturn(waves);
    ImmutableSet<WaveletId> wavelets = ImmutableSet.of(WAVELET_NAME.waveletId);
    when(waveMap.lookupWavelets(WAVELET_NAME.waveId)).thenReturn(wavelets);

    LocalWaveletContainer c = mock(LocalWaveletContainer.class);
    when(c.hasParticipant(PARTICIPANT)).thenReturn(true);
    when(waveMap.getLocalWavelet(WAVELET_NAME)).thenReturn(c);
  }

  @Override
  protected PerUserWaveViewHandler createPerUserWaveViewHandler() {
    return new MemoryPerUserWaveViewHandlerImpl(waveMap);
  }

  @Override
  protected void postUpdateHook() {
    // No op.
  }
}
