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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 * @author soren@google.com (Soren Lassen)
 */
public class WaveMapTest extends TestCase {

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = WaveId.of(DOMAIN, "abc123");
  private static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "conv+root");
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);


  @Mock private WaveletNotificationDispatcher notifiee;
  @Mock private RemoteWaveletContainer.Factory remoteWaveletContainerFactory;

  private DeltaAndSnapshotStore waveletStore;
  private WaveMap waveMap;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    final DeltaStore deltaStore = new MemoryDeltaStore();
    final Executor persistExecutor = MoreExecutors.sameThreadExecutor();
    final Executor storageContinuationExecutor = MoreExecutors.sameThreadExecutor();
    LocalWaveletContainer.Factory localWaveletContainerFactory =
        new LocalWaveletContainer.Factory() {
          @Override
          public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
              WaveletName waveletName, String domain) {
            WaveletState waveletState;
            try {
              waveletState = DeltaStoreBasedWaveletState.create(deltaStore.open(waveletName),
                  persistExecutor);
            } catch (PersistenceException e) {
              throw new RuntimeException(e);
            }
            return new LocalWaveletContainerImpl(waveletName, notifiee,
                Futures.immediateFuture(waveletState), DOMAIN, storageContinuationExecutor);
          }
        };

    waveletStore = mock(DeltaAndSnapshotStore.class);
    waveMap =
        new WaveMap(waveletStore, notifiee, localWaveletContainerFactory,
            remoteWaveletContainerFactory, "example.com", storageContinuationExecutor);
  }

  public void testWaveMapStartsEmpty() throws WaveServerException {
    assertFalse(waveMap.getWaveIds().hasNext());
  }

  public void testWavesStartWithNoWavelets() throws WaveletStateException, PersistenceException {
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of());
    assertNull(waveMap.getLocalWavelet(WAVELET_NAME));
    assertNull(waveMap.getRemoteWavelet(WAVELET_NAME));
  }

  public void testWaveAvailableAfterLoad() throws PersistenceException, WaveServerException {
    when(waveletStore.getWaveIdIterator()).thenReturn(eitr(WAVE_ID));
    waveMap.loadAllWavelets();

    ExceptionalIterator<WaveId, WaveServerException> waves = waveMap.getWaveIds();
    assertTrue(waves.hasNext());
    assertEquals(WAVE_ID, waves.next());
  }

  public void testWaveletAvailableAfterLoad() throws WaveletStateException, PersistenceException {
    when(waveletStore.getWaveIdIterator()).thenReturn(eitr(WAVE_ID));
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of(WAVELET_ID));
    waveMap.loadAllWavelets();

    assertNotNull(waveMap.getLocalWavelet(WAVELET_NAME));
  }

  public void testGetOrCreateCreatesWavelets() throws WaveletStateException, PersistenceException {
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of());
    LocalWaveletContainer wavelet = waveMap.getOrCreateLocalWavelet(WAVELET_NAME);
    assertSame(wavelet, waveMap.getLocalWavelet(WAVELET_NAME));
  }

  private ExceptionalIterator<WaveId, PersistenceException> eitr(WaveId... waves) {
    return ExceptionalIterator.FromIterator.<WaveId, PersistenceException>create(
        Arrays.asList(waves).iterator());
  }
}
