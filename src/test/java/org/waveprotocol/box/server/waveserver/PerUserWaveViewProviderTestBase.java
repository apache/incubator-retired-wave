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

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;

import junit.framework.TestCase;

import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public abstract class PerUserWaveViewProviderTestBase extends TestCase implements TestingConstants {

  private PerUserWaveViewHandler provider;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    provider = createPerUserWaveViewHandler();
  }

  public void testOnParticipantAdded() throws InterruptedException, ExecutionException {
    addWaveToParticipantView(provider, WAVELET_NAME, PARTICIPANT);
    postUpdateHook();
    Multimap<WaveId, WaveletId> wavesView = provider.retrievePerUserWaveView(PARTICIPANT);
    assertNotNull(wavesView);
    assertEquals(1, wavesView.size());
    Entry<WaveId, WaveletId> entry = wavesView.entries().iterator().next();

    assertEquals(WAVELET_NAME.waveletId, entry.getValue());
    assertEquals(WAVELET_NAME.waveId, entry.getKey());
  }

  public void testOnParticipantRemoved() throws InterruptedException, ExecutionException {
    addWaveToParticipantView(provider, WAVELET_NAME, PARTICIPANT);
    postUpdateHook();
    Multimap<WaveId, WaveletId> wavesView = provider.retrievePerUserWaveView(PARTICIPANT);
    assertNotNull(wavesView);
    assertEquals(1, wavesView.size());
    ListenableFuture<Void> future = provider.onParticipantRemoved(WAVELET_NAME, PARTICIPANT);
    future.get();
    postUpdateHook();
    wavesView = provider.retrievePerUserWaveView(PARTICIPANT);
    assertNotNull(wavesView);
    assertEquals(0, wavesView.size());
  }

  private static void addWaveToParticipantView(PerUserWaveViewHandler provider,
      WaveletName waveletName, ParticipantId participant) throws InterruptedException,
      ExecutionException {
    // The first call to getPerUserWaveView() method causes the user's wavelets
    // to be tracked.
    provider.retrievePerUserWaveView(participant);
    ListenableFuture<Void> future = provider.onParticipantAdded(waveletName, participant);
    future.get();
  }

  /**
   * Factory method that creates the instance of {@link PerUserWaveViewHandler} to test.
   */
  abstract protected PerUserWaveViewHandler createPerUserWaveViewHandler();

  /**
   * Post update hook method that enables the subclasses to perform post update logic.
   */
  abstract protected void postUpdateHook();
}