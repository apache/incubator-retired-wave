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

import com.google.common.base.Preconditions;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.rpc.ServerRpcController;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * RPC interface implementation for the wave server. Adapts incoming and
 * outgoing RPCs to the client frontend interface.
 *
 *
 */
public class WaveClientRpcImpl implements ProtocolWaveClientRpc.Interface {

  private static final Log LOG = Log.get(WaveClientRpcImpl.class);

  private final ClientFrontend frontend;
  private final boolean handleAuthentication;

  /**
   * Creates a new RPC interface to the front-end.
   *
   * @param frontend front-end to which to forward requests
   * @param handleAuthentication whether to handle authentication; it's
   *        otherwise expected to be handled before this class
   */
  public static WaveClientRpcImpl create(ClientFrontend frontend,
      boolean handleAuthentication) {
    return new WaveClientRpcImpl(frontend, handleAuthentication);
  }

  private WaveClientRpcImpl(ClientFrontend frontend, boolean handleAuthentication) {
    this.frontend = frontend;
    this.handleAuthentication = handleAuthentication;
  }

  @Override
  public void open(final RpcController controller, ProtocolOpenRequest request,
      final RpcCallback<ProtocolWaveletUpdate> done) {
    WaveId waveId;
    try {
      waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(request.getWaveId());
    } catch (InvalidIdException e) {
      LOG.warning("Invalid id in open", e);
      controller.setFailed(e.getMessage());
      return;
    }
    IdFilter waveletIdFilter =
        IdFilter.of(Collections.<WaveletId>emptySet(), request.getWaveletIdPrefixList());

    ParticipantId loggedInUser = asBoxController(controller).getLoggedInUser();
    frontend.openRequest(loggedInUser, waveId, waveletIdFilter, request.getKnownWaveletList(),
        new ClientFrontend.OpenListener() {
          @Override
          public void onFailure(String errorMessage) {
            LOG.warning("openRequest failure: " + errorMessage);
            controller.setFailed(errorMessage);
          }

          @Override
          public void onUpdate(WaveletName waveletName,
              @Nullable CommittedWaveletSnapshot snapshot, List<TransformedWaveletDelta> deltas,
              @Nullable HashedVersion committedVersion, Boolean hasMarker, String channel_id) {
            ProtocolWaveletUpdate.Builder builder = ProtocolWaveletUpdate.newBuilder();
            if (hasMarker != null) {
              builder.setMarker(hasMarker.booleanValue());
            }
            if (channel_id != null) {
              builder.setChannelId(channel_id);
            }
            builder.setWaveletName(ModernIdSerialiser.INSTANCE.serialiseWaveletName(waveletName));
            for (TransformedWaveletDelta d : deltas) {
              // TODO(anorth): Add delta application metadata to the result
              // when the c/s protocol supports it.
              builder.addAppliedDelta(CoreWaveletOperationSerializer.serialize(d));
            }
            if (!deltas.isEmpty()) {
              builder.setResultingVersion(CoreWaveletOperationSerializer.serialize(
                  deltas.get((deltas.size() - 1)).getResultingVersion()));
            }
            if (snapshot != null) {
              Preconditions.checkState(committedVersion.equals(snapshot.committedVersion),
                  "Mismatched commit versions, snapshot: " + snapshot.committedVersion
                      + " expected: " + committedVersion);
              builder.setSnapshot(SnapshotSerializer.serializeWavelet(snapshot.snapshot,
                  snapshot.committedVersion));
              builder.setResultingVersion(CoreWaveletOperationSerializer.serialize(
                  snapshot.snapshot.getHashedVersion()));
              builder.setCommitNotice(CoreWaveletOperationSerializer.serialize(
                  snapshot.committedVersion));
            } else {
              if (committedVersion != null) {
                builder.setCommitNotice(
                    CoreWaveletOperationSerializer.serialize(committedVersion));
              }
            }
            done.run(builder.build());
          }
        });
  }

  @Override
  public void submit(RpcController controller, ProtocolSubmitRequest request,
      final RpcCallback<ProtocolSubmitResponse> done) {
    WaveletName waveletName = null;
    try {
      waveletName = ModernIdSerialiser.INSTANCE.deserialiseWaveletName(request.getWaveletName());
    } catch (InvalidIdException e) {
      LOG.warning("Invalid id in submit", e);
      controller.setFailed(e.getMessage());
      return;
    }
    String channelId;
    if (request.hasChannelId()) {
      channelId = request.getChannelId();
    } else {
      channelId = null;
    }
    ParticipantId loggedInUser = asBoxController(controller).getLoggedInUser();
    frontend.submitRequest(loggedInUser, waveletName, request.getDelta(), channelId,
        new SubmitRequestListener() {
          @Override
          public void onFailure(String error) {
            done.run(ProtocolSubmitResponse.newBuilder()
                .setOperationsApplied(0).setErrorMessage(error).build());
          }

          @Override
          public void onSuccess(int operationsApplied,
              HashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
            done.run(ProtocolSubmitResponse.newBuilder()
                .setOperationsApplied(operationsApplied)
                .setHashedVersionAfterApplication(
                    CoreWaveletOperationSerializer.serialize(hashedVersionAfterApplication))
                .build());
            // TODO(arb): applicationTimestamp??
          }
        });
  }

  @Override
  public void authenticate(RpcController controller, ProtocolAuthenticate request,
      RpcCallback<ProtocolAuthenticationResult> done) {
    Preconditions.checkState(handleAuthentication,
        "ProtocolAuthenticate should be handled in ServerRpcProvider");
    done.run(ProtocolAuthenticationResult.getDefaultInstance());
  }

  ServerRpcController asBoxController(RpcController controller) {
    // This cast is safe (because of how the WaveClientRpcImpl is instantiated). We need to do this
    // because ServerRpcController implements an autogenerated interface.
    return (ServerRpcController) controller;
  }
}
