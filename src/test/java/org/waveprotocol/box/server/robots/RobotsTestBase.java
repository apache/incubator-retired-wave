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

package org.waveprotocol.box.server.robots;

import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.OperationRequest.Parameter;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Common constants and methods for operation service tests.
 *
 * @author anorth@google.com (Alex North)
 */
public class RobotsTestBase extends TestCase {

  protected static final WaveId WAVE_ID = WaveId.of("example.com", "waveid");
  protected static final WaveletId WAVELET_ID = WaveletId.of("example.com", "conv+root");
  protected static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  protected static final String OPERATION_ID = "op1";
  protected static final String OPERATION2_ID = "op2";

  protected static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  protected static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  protected static final ParticipantId ROBOT = ParticipantId.ofUnsafe("robot@example.com");

  protected static OperationRequest operationRequest(OperationType opType, String operationId,
      String rootBlipId, Parameter... params) {
    return new OperationRequest(opType.method(), operationId, s(WAVE_ID), s(WAVELET_ID),
        rootBlipId, params);
  }

  /** Creates an operation request. */
  protected static OperationRequest operationRequest(OperationType opType, String rootBlipId,
      Parameter... params) {
    return new OperationRequest(opType.method(), OPERATION_ID, s(WAVE_ID), s(WAVELET_ID),
        rootBlipId, params);
  }

  /** Creates an operation request. */
  protected static OperationRequest operationRequest(OperationType opType, Parameter... params) {
    return new OperationRequest(opType.method(), OPERATION_ID, s(WAVE_ID), s(WAVELET_ID), params);
  }

  protected static String s(WaveId waveId) {
    return ApiIdSerializer.instance().serialiseWaveId(waveId);
  }

  protected static String s(WaveletId waveletId) {
    return ApiIdSerializer.instance().serialiseWaveletId(waveletId);
  }
}
