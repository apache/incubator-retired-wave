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

import static org.mockito.Mockito.verify;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

public class PerUserWaveViewDistpatcherTest extends TestCase implements TestingConstants {

  private static final HashedVersion BEGIN_VERSION = HashedVersion.unsigned(101L);
  private static final HashedVersion END_VERSION = HashedVersion.unsigned(102L);

  private static final ProtocolWaveletDelta ADD_DELTA = ProtocolWaveletDelta.newBuilder()
      .setAuthor(USER).setHashedVersion(CoreWaveletOperationSerializer.serialize(BEGIN_VERSION))
      .addOperation(ProtocolWaveletOperation.newBuilder().setAddParticipant(USER).build()).build();

  private static final ProtocolWaveletDelta REMOVE_DELTA = ProtocolWaveletDelta.newBuilder()
      .setAuthor(USER).setHashedVersion(CoreWaveletOperationSerializer.serialize(BEGIN_VERSION))
      .addOperation(ProtocolWaveletOperation.newBuilder().setRemoveParticipant(USER).build()).build();

  private PerUserWaveViewDistpatcher dispatcher;

  @Mock
  private PerUserWaveViewBus.Listener listener;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    dispatcher = new PerUserWaveViewDistpatcher();
    dispatcher.addListener(listener);
  }

  public void testOnAddParticipantEvent() {
    DeltaSequence POJO_DELTAS = DeltaSequence.of(CoreWaveletOperationSerializer
        .deserialize(ADD_DELTA, END_VERSION, 0L));
    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet =
        WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT, BEGIN_VERSION,
            dummyCreationTime);

    dispatcher.waveletUpdate(wavelet, POJO_DELTAS);
    verify(listener).onParticipantAdded(WAVELET_NAME, PARTICIPANT);
  }

  public void testOnRemoveParticipantEvent() {
    DeltaSequence POJO_DELTAS = DeltaSequence.of(CoreWaveletOperationSerializer
        .deserialize(REMOVE_DELTA, END_VERSION, 0L));
    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet =
        WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT, BEGIN_VERSION,
            dummyCreationTime);

    dispatcher.waveletUpdate(wavelet, POJO_DELTAS);
    verify(listener).onParticipantRemoved(WAVELET_NAME, PARTICIPANT);
  }
}
