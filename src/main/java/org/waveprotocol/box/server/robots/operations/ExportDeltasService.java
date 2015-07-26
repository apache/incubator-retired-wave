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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.impl.RawDeltasListener;

import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.common.Receiver;

import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link OperationService} for the "exportDeltas" operation.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ExportDeltasService implements OperationService {
  private static final Log LOG = Log.get(ExportDeltasService.class);

  private static final int GET_HISTORY_BYTES_LENGTH_LIMIT = 1000000;

  private ExportDeltasService() {
  }

  public static ExportDeltasService create() {
    return new ExportDeltasService();
  }

  @Override
  public void execute(final OperationRequest operation, final OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    WaveId waveId;
    WaveletId waveletId;
    HashedVersion startVersion;
    HashedVersion endVersion;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(
        OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVE_ID));
      waveletId = ApiIdSerializer.instance().deserialiseWaveletId(
        OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVELET_ID));
    } catch (InvalidIdException ex) {
      throw new InvalidRequestException("Invalid id", operation, ex);
    }
    startVersion = getVersionParameter(operation, ParamsProperty.FROM_VERSION);
    endVersion = getVersionParameter(operation, ParamsProperty.TO_VERSION);
    WaveletName waveletName = WaveletName.of(waveId, waveletId);
    getDeltas(context, waveletName, participant, startVersion, endVersion, new RawDeltasListener() {

          @Override
          public void onSuccess(List<byte[]> rawDeltas, byte[] rawTargetVersion) {
            Map<ParamsProperty, Object> data = ImmutableMap.<ParamsProperty, Object> of(
              ParamsProperty.RAW_DELTAS,  rawDeltas,
              ParamsProperty.TARGET_VERSION, rawTargetVersion);
            context.constructResponse(operation, data);
          }

          @Override
          public void onFailire(String message) {
            context.constructErrorResponse(operation, message);
          }
        });
  }

  private void getDeltas(OperationContext context, WaveletName waveletName,
      ParticipantId participant, HashedVersion fromVersion, HashedVersion toVersion,
      RawDeltasListener listener) throws InvalidRequestException {
    final List<byte[]> deltaBytes = new LinkedList<byte[]>();
    final AtomicReference<HashedVersion> version = new AtomicReference<HashedVersion>();
    final AtomicInteger length = new AtomicInteger(0);
    context.getDeltas(waveletName, participant, fromVersion, toVersion, new Receiver<TransformedWaveletDelta>() {

          @Override
          public boolean put(TransformedWaveletDelta delta) {
            ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);
            byte[] bytes = protoDelta.toByteArray();
            deltaBytes.add(bytes);
            version.set(delta.getResultingVersion());
            return length.addAndGet(bytes.length) < GET_HISTORY_BYTES_LENGTH_LIMIT;
          }
        });
    listener.onSuccess(deltaBytes, CoreWaveletOperationSerializer.serialize(version.get()).toByteArray());
  }

  private HashedVersion getVersionParameter(OperationRequest operation, ParamsProperty parameter)
      throws InvalidRequestException {
    byte[] bytes = OperationUtil.<byte[]>getRequiredParameter(
          operation, parameter);
    ProtocolHashedVersion protoVersion;
    try {
      protoVersion = ProtocolHashedVersion.parseFrom(bytes);
    } catch (InvalidProtocolBufferException ex) {
      throw new InvalidRequestException("Invalid version " + parameter.key(), operation, ex);
    }
    return CoreWaveletOperationSerializer.deserialize(protoVersion);
  }
}