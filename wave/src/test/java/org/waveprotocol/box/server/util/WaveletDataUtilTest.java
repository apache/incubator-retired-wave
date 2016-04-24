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

package org.waveprotocol.box.server.util;

import junit.framework.TestCase;

import com.google.common.collect.ImmutableSet;

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Arrays;

/**
 * @author soren@google.com (Soren Lassen)
 */
public class WaveletDataUtilTest extends TestCase {
  public static final ParticipantId CREATOR = new ParticipantId("creator@example.com");
  public static final ParticipantId JOE = new ParticipantId("joe@example.com");
  public static final WaveletName WAVELET_NAME =
      WaveletName.of(WaveId.of("example.com", "w+wave"), WaveletId.of("example.com", "wavelet"));

  private WaveletOperationContext opContext(long timestamp, HashedVersion version) {
    return new WaveletOperationContext(CREATOR, timestamp, 1L, version);
  }

  private WaveletOperation addParticipant(ParticipantId user, long time, HashedVersion version) {
    return new AddParticipant(opContext(time, version), user);
  }

  private WaveletOperation addBlip(String id, long time, HashedVersion version) {
    return new WaveletBlipOperation(id,
        new BlipContentOperation(opContext(time, version), new DocOpBuilder().build()));
  }

  private TransformedWaveletDelta delta(WaveletOperation... ops) {
    WaveletOperation last = ops[ops.length - 1];
    WaveletOperationContext ctx = last.getContext();
    return new TransformedWaveletDelta(
        ctx.getCreator(), ctx.getHashedVersion(), ctx.getTimestamp(), Arrays.asList(ops));
  }

  private ObservableWaveletData build(TransformedWaveletDelta... deltas) throws OperationException {
    return WaveletDataUtil.buildWaveletFromDeltas(WAVELET_NAME, Arrays.asList(deltas).iterator());
  }

  public void testBuildWaveletFromOneDelta() throws Exception {
    WaveletData wavelet = build(
        delta(addParticipant(CREATOR, 1093L, HashedVersion.unsigned(1)))
    );
    assertEquals(WAVELET_NAME, WaveletDataUtil.waveletNameOf(wavelet));
    assertEquals(CREATOR, wavelet.getCreator());
    assertEquals(1093L, wavelet.getCreationTime());
    assertEquals(1093L, wavelet.getLastModifiedTime());
    assertEquals(HashedVersion.unsigned(1), wavelet.getHashedVersion());
    assertEquals(ImmutableSet.of(), wavelet.getDocumentIds());
    assertEquals(ImmutableSet.of(CREATOR), wavelet.getParticipants());
  }

  public void testBuildWaveletFromThreeDeltas() throws Exception {
    WaveletData wavelet = build(
        delta(addParticipant(CREATOR, 1093L, HashedVersion.unsigned(1))),
        delta(addParticipant(JOE, 1492L, HashedVersion.unsigned(2))),
        delta(addBlip("blipid", 2010L, HashedVersion.unsigned(3)))
    );
    assertEquals(WAVELET_NAME, WaveletDataUtil.waveletNameOf(wavelet));
    assertEquals(CREATOR, wavelet.getCreator());
    assertEquals(1093L, wavelet.getCreationTime());
    assertEquals(2010L, wavelet.getLastModifiedTime());
    assertEquals(HashedVersion.unsigned(3), wavelet.getHashedVersion());
    assertEquals(ImmutableSet.of("blipid"), wavelet.getDocumentIds());
    assertEquals(ImmutableSet.of(CREATOR, JOE), wavelet.getParticipants());
  }
}
