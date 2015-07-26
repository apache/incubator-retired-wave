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
import com.google.gson.Gson;

import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.impl.GsonFactory;

import org.waveprotocol.box.common.comms.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.common.comms.proto.WaveletSnapshotProtoImpl;

import java.util.Map;

/**
 * {@link OperationService} for the "exportSnapshot" operation.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ExportSnapshotService implements OperationService {
  private static final Log LOG = Log.get(ExportSnapshotService.class);

  private static final Gson gson = new GsonFactory().create();

  private ExportSnapshotService() {
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    WaveId waveId;
    WaveletId waveletId;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(
        OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVE_ID));
      waveletId = ApiIdSerializer.instance().deserialiseWaveletId(
        OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVELET_ID));
    } catch (InvalidIdException ex) {
      throw new InvalidRequestException("Invalid id", operation, ex);
    }
    WaveletName waveletName = WaveletName.of(waveId, waveletId);
    CommittedWaveletSnapshot snapshot = context.getWaveletSnapshot(waveletName, participant);
    WaveletSnapshot protoSnapshot = SnapshotSerializer.serializeWavelet(snapshot.snapshot, snapshot.snapshot.getHashedVersion());
    WaveletSnapshotProtoImpl protoSnapshotImpl = new WaveletSnapshotProtoImpl(protoSnapshot);
    String jsonSnapshot = gson.toJson(protoSnapshotImpl.toGson(null, gson));
    Map<ParamsProperty, Object> data =
      ImmutableMap.<ParamsProperty, Object> of(ParamsProperty.RAW_SNAPSHOT, jsonSnapshot);
    context.constructResponse(operation, data);
  }

  public static ExportSnapshotService create() {
    return new ExportSnapshotService();
  }
}
