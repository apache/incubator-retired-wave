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

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */

public class WaveViewDataImplTest extends TestCase {
  private static final WaveId WAVE_ID = WaveId.of("example.com", "wave");
  private static final WaveletId WAVELET_IDS[] = new WaveletId[] {
    WaveletId.of("example.com", "wavelet"),
    WaveletId.of("example.com", "wavelet1"),
    WaveletId.of("example.com", "wavelet2"),
  };
  private static final long CREATE_TIMESTAMP = 123456789;
  private static final ParticipantId CREATOR = new ParticipantId("nobody@example.com");
  private static final WaveletDataImpl.Factory EMPTY_DATA_FACTORY =
      WaveletDataImpl.Factory.create(StubDocumentFactory.INSTANCE);

  public void testEmptyViewHasNoWavelet() throws Exception {
    WaveViewDataImpl impl = WaveViewDataImpl.create(WAVE_ID);

    for (WaveletId id : WAVELET_IDS) {
      assertNull(impl.getWavelet(id));
    }

    assertEquals(0, getWavelets(impl).size());
  }

  public void testGetWaveletReturnsAllWavelets() throws Exception {
    List<ObservableWaveletData> list = new ArrayList<ObservableWaveletData>();
    for (WaveletId id : WAVELET_IDS) {
      ObservableWaveletData n =
          EMPTY_DATA_FACTORY.create(new EmptyWaveletSnapshot(WAVE_ID, id, CREATOR,
              HashedVersion.unsigned(0), CREATE_TIMESTAMP));
      list.add(n);
    }

    WaveViewDataImpl impl = WaveViewDataImpl.create(WAVE_ID, list);

    for (ObservableWaveletData data : impl.getWavelets()) {
      if (!list.remove(data)) {
        fail("Seeded data not available");
      }
    }
    assertEquals(0, list.size());
  }

  public void testAddedWaveletsAreAccessible() throws Exception {
    final Map<WaveletId, ObservableWaveletData> createdWavelet =
        new HashMap<WaveletId, ObservableWaveletData>();
    WaveViewDataImpl impl = WaveViewDataImpl.create(WAVE_ID);
    ObservableWaveletData.Factory<ObservableWaveletData> dataFactory =
        new ObservableWaveletData.Factory<ObservableWaveletData>() {
          @Override
          public ObservableWaveletData create(ReadableWaveletData data) {
            ObservableWaveletData n = EMPTY_DATA_FACTORY.create(data);
            createdWavelet.put(data.getWaveletId(), n);
            return n;
          }
        };

    for (WaveletId id : WAVELET_IDS) {
      impl.addWavelet(dataFactory.create(
          new EmptyWaveletSnapshot(WAVE_ID, id, CREATOR, HashedVersion.unsigned(0),
              CREATE_TIMESTAMP)));
    }

    // check to see if added wavelet can get get backed.
    for (WaveletId id : WAVELET_IDS) {
      assertEquals(createdWavelet.get(id), impl.getWavelet(id));
    }

    // test to make sure remove works correctly.
    int numItemsLeft = WAVELET_IDS.length;
    for (WaveletId id : WAVELET_IDS) {
      assertEquals(numItemsLeft, getWavelets(impl).size());
      impl.removeWavelet(id);
      assertEquals(--numItemsLeft, getWavelets(impl).size());
    }
  }

  public void testCannotCreateDuplicatedWavelet() throws Exception {
    WaveViewDataImpl impl = WaveViewDataImpl.create(WAVE_ID);
    ObservableWaveletData.Factory<ObservableWaveletData> dataFactory =
        new ObservableWaveletData.Factory<ObservableWaveletData>() {
          @Override
          public ObservableWaveletData create(ReadableWaveletData data) {
            ObservableWaveletData n = EMPTY_DATA_FACTORY.create(data);
            return n;
          }
        };


    impl.addWavelet(dataFactory.create(
        new EmptyWaveletSnapshot(WAVE_ID, WAVELET_IDS[0], CREATOR, HashedVersion.unsigned(0),
            CREATE_TIMESTAMP)));

    try {
      impl.addWavelet(dataFactory.create(
          new EmptyWaveletSnapshot(WAVE_ID, WAVELET_IDS[0], CREATOR, HashedVersion.unsigned(0),
              CREATE_TIMESTAMP)));
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      // expected an exception
    }
  }

  public void testCannotRemoveNonExistentWavelet() throws Exception {
    WaveViewDataImpl impl = WaveViewDataImpl.create(WAVE_ID);

    try {
      impl.removeWavelet(WAVELET_IDS[0]);
      fail("Exception expected");
    } catch (IllegalArgumentException e) {
      // expected an exception
    }
  }

  private Collection<ObservableWaveletData> getWavelets(WaveViewDataImpl view) {
    List<ObservableWaveletData> list = new ArrayList<ObservableWaveletData>();
    for (ObservableWaveletData w : view.getWavelets()) {
      list.add(w);
    }
    return list;
  }
}
