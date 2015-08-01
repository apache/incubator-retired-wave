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

package org.waveprotocol.box.expimp;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.box.server.waveserver.WaveletStateException;

import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import junit.framework.TestCase;

/**
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class DomainConverterTest extends TestCase implements TestingConstants {

  private static final HashedVersion VERSION = HashedVersion.unsigned(111L);

  private static final String FROM_DOMAIN = "other.com";
  private static final String FROM_USER = USER_NAME + "@" + FROM_DOMAIN;
  private static final WaveId FROM_WAVE_ID = WaveId.of(FROM_DOMAIN, WAVE_ID.getId());
  private static final WaveletId FROM_WAVELET_ID = WaveletId.of(FROM_DOMAIN, WAVELET_ID.getId());
  private static final WaveletId FROM_USERDATA_WAVELET_ID = WaveletId.of(FROM_DOMAIN,
      IdUtil.join(IdUtil.USER_DATA_WAVELET_PREFIX, FROM_USER));

  private static final ProtocolWaveletDelta DELTA_ADD_USER = ProtocolWaveletDelta.newBuilder()
    .setAuthor(FROM_USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setAddParticipant(FROM_USER)).build();

  CommittedWaveletSnapshot snapshot = new CommittedWaveletSnapshot(
      new EmptyWaveletSnapshot(WAVE_ID, WAVELET_ID, PARTICIPANT, VERSION, -1), VERSION);

  @Override
  protected void setUp() throws Exception {
  }

  public void testConvertOfWaveId() {
    WaveId waveId = DomainConverter.convertWaveId(FROM_WAVE_ID, DOMAIN);
    assertEquals(WAVE_ID, waveId);
  }

  public void testConvertOfWaveletId() throws InvalidParticipantAddress {
    WaveletId waveletId = DomainConverter.convertWaveletId(FROM_WAVELET_ID, DOMAIN);
    assertEquals(WAVELET_ID, waveletId);
  }

  public void testConvertOfUserdataWaveletId() throws InvalidParticipantAddress {
    WaveletId waveletId = DomainConverter.convertWaveletId(FROM_USERDATA_WAVELET_ID, DOMAIN);
    assertEquals(waveletId.getDomain(), DOMAIN);
    assertEquals(IdUtil.split(waveletId.getId())[1], USER);
  }

  public void testConvertOfDelta() throws WaveletStateException, InvalidParticipantAddress {
    ProtocolWaveletDelta delta = DomainConverter.convertDelta(DELTA_ADD_USER, DOMAIN);
    assertEquals(USER, delta.getAuthor());
    assertEquals(USER, delta.getOperation(0).getAddParticipant());
  }
}
