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

package org.waveprotocol.box.server.frontend;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import junit.framework.TestCase;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.frontend.testing.FakeClientFrontend;
import org.waveprotocol.box.server.rpc.testing.FakeServerRpcController;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Tests for the {@link WaveClientRpcImpl}.
 */
public class WaveClientRpcImplTest extends TestCase implements TestingConstants {

  private static final String FAIL_MESSAGE = "Failed";

  private static final HashedVersion BEGIN_VERSION = HashedVersion.unsigned(101L);
  private static final HashedVersion END_VERSION = HashedVersion.unsigned(102L);

  private static final ProtocolWaveletDelta DELTA = ProtocolWaveletDelta.newBuilder()
    .setAuthor(USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(BEGIN_VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setNoOp(true).build()).build();

  private static final ImmutableList<ProtocolWaveletDelta> DELTAS = ImmutableList.of(DELTA);
  private static final DeltaSequence POJO_DELTAS =
      DeltaSequence.of(CoreWaveletOperationSerializer.deserialize(DELTA, END_VERSION, 0L));

  private RpcController controller;

  private int counter = 0;

  private FakeClientFrontend frontend;

  private WaveClientRpcImpl rpcImpl;

  private WaveletName getWaveletName(String waveletName) {
    try {
      return ModernIdSerialiser.INSTANCE.deserialiseWaveletName(waveletName);
    } catch (InvalidIdException e) {
      throw new IllegalStateException(e);
    }
  }

  private String getWaveletUri(WaveletName waveletName) {
    return ModernIdSerialiser.INSTANCE.serialiseWaveletName(waveletName);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    counter = 0;
    controller = new FakeServerRpcController();
    frontend = new FakeClientFrontend();
    rpcImpl = WaveClientRpcImpl.create(frontend, false);
  }

  // TODO(arb): test with channelIds.

  /**
   * Tests that an open results in a proper wavelet commit update.
   */
  public void testOpenCommit() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER)
        .setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(WAVE_ID)).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
        assertEquals(WAVELET_NAME, getWaveletName(update.getWaveletName()));
        assertTrue(update.hasCommitNotice());
        assertEquals(BEGIN_VERSION,
            CoreWaveletOperationSerializer.deserialize(update.getCommitNotice()));
      }
    });
    frontend.waveletCommitted(WAVELET_NAME, BEGIN_VERSION);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that an open failure results in a proper wavelet failure update.
   */
  public void testOpenFailure() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER)
        .setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(WAVE_ID)).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
      }
    });
    frontend.doUpdateFailure(WAVE_ID, FAIL_MESSAGE);
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertEquals(FAIL_MESSAGE, controller.errorText());
  }

  /**
   * Tests that an open results in a proper wavelet update.
   */
  public void testOpenUpdate() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER)
        .setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(WAVE_ID)).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
        assertEquals(WAVELET_NAME, getWaveletName(update.getWaveletName()));
        assertEquals(DELTAS.size(), update.getAppliedDeltaCount());
        for (int i = 0; i < update.getAppliedDeltaCount(); ++i) {
          assertEquals(DELTAS.get(i), update.getAppliedDelta(i));
        }
        assertFalse(update.hasCommitNotice());
      }
    });
    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT,
        BEGIN_VERSION, dummyCreationTime);
    frontend.waveletUpdate(wavelet, POJO_DELTAS);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a failed submit results in the proper submit failure response.
   */
  public void testSubmitFailed() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName(getWaveletUri(WAVELET_NAME)).build();
    counter = 0;
    rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
      @Override
      public void run(ProtocolSubmitResponse response) {
        ++counter;
        assertEquals(0, response.getOperationsApplied());
        assertEquals(FAIL_MESSAGE, response.getErrorMessage());
      }
    });
    frontend.doSubmitFailed(WAVELET_NAME, FAIL_MESSAGE);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a successful submit results in the proper submit response.
   */
  public void testSubmitSuccess() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName(getWaveletUri(WAVELET_NAME)).build();
    counter = 0;
    rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
      @Override
      public void run(ProtocolSubmitResponse response) {
        ++counter;
        assertEquals(1, response.getOperationsApplied());
        assertFalse(response.hasErrorMessage());
      }
    });
    frontend.doSubmitSuccess(WAVELET_NAME);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a bad wave id request is gracefully handled.
   */
  public void testOpenEncodingError() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER)
        .setWaveId("badwaveid").build();
    counter = 0;
    try {
      rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
        @Override
        public void run(ProtocolWaveletUpdate update) {
          ++counter;
          fail("Unexpected callback");
        }
      });
    } catch (IllegalArgumentException e) {
      controller.setFailed(FAIL_MESSAGE);
    }
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertFalse(controller.errorText().isEmpty());
  }

  /**
   * Tests that a bad wavelet name submit is gracefully handled.
   */
  public void testSubmitEncodingError() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName("badwaveletname").build();
    counter = 0;
    try {
      rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
        @Override
        public void run(ProtocolSubmitResponse response) {
          ++counter;
          fail("Unexpected callback");
        }
      });
    } catch (IllegalArgumentException e) {
      controller.setFailed(FAIL_MESSAGE);
    }
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertFalse(controller.errorText().isEmpty());
  }
}
