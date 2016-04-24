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

import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;

/**
 * Utilities for wave server tests.
 *
 * @author anorth@google.com (Alex North)
 */
public final class WaveServerTestUtil {
  /**
   * Build an applied delta message from a POJO delta. The delta is not signed.
   */
  public static ByteStringMessage<ProtocolAppliedWaveletDelta> buildAppliedDelta(WaveletDelta delta,
      long applicationTimestamp) {
    ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);
    ByteStringMessage<ProtocolWaveletDelta> deltaBytes =
        ByteStringMessage.serializeMessage(protoDelta);
    ProtocolSignedDelta signedDelta =
        ProtocolSignedDelta.newBuilder().setDelta(deltaBytes.getByteString()).build();
    return AppliedDeltaUtil.buildAppliedDelta(signedDelta, delta.getTargetVersion(), delta.size(),
        applicationTimestamp);
  }

  /**
   * Applies a delta to a wavelet container.
   */
  public static void applyDeltaToWavelet(WaveletContainerImpl wavelet, WaveletDelta delta,
      long applicationTimestamp)
      throws InvalidProtocolBufferException, OperationException {
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        buildAppliedDelta(delta, applicationTimestamp);
    wavelet.applyDelta(appliedDelta, delta);
  }
}
