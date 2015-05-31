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

package org.waveprotocol.wave.model.testing;


import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;

/**
 * Exposes any {@link ObservableWaveletData.Factory} as a {@link Factory}, by
 * injecting suitable dependencies for testing.
 *
 */
public final class WaveletDataFactory<T extends WaveletData> implements Factory<T> {
  private final static WaveId WAVE_ID;
  private final static WaveletId WAVELET_ID;
  private static final ParticipantId PARTICIPANT_ID = new ParticipantId("fake@example.com");

  static {
    IdGenerator gen = FakeIdGenerator.create();
    WAVE_ID = gen.newWaveId();
    WAVELET_ID = gen.newConversationWaveletId();
  }

  private final WaveletData.Factory<T> factory;

  private WaveletDataFactory(WaveletData.Factory<T> factory) {
    this.factory = factory;
  }

  public static <T extends WaveletData> Factory<T> of(WaveletData.Factory<T> factory) {
    return new WaveletDataFactory<T>(factory);
  }

  @Override
  public T create() {
    return factory.create(new EmptyWaveletSnapshot(WAVE_ID, WAVELET_ID, PARTICIPANT_ID,
        HashedVersion.unsigned(0), 0));
  }
}
