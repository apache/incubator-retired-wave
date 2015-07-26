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

import org.waveprotocol.box.common.comms.WaveClientRpc;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * The client front-end handles requests from clients and directs them to
 * appropriate back-ends.
 *
 * Provides updates for wavelets that a client has opened and access to.
 */
public interface ClientFrontend {

  /**
   * Listener provided to open requests.
   */
  interface OpenListener {
    /**
     * Called when an update is received.
     *
     * @param waveletName wavelet receiving the update
     * @param snapshot optional snapshot
     * @param deltas optional deltas, not necessarily contiguous
     * @param committedVersion optional commit notice
     * @param marker optional (true/false/absent) marker
     * @param channelId channel id (first message only)
     */
    void onUpdate(WaveletName waveletName, @Nullable CommittedWaveletSnapshot snapshot,
        List<TransformedWaveletDelta> deltas, @Nullable HashedVersion committedVersion,
        @Nullable Boolean marker, String channelId);

    /**
     * Called when the stream fails. No further updates will be received.
     */
    void onFailure(String errorMessage);
  }

  /**
   * Request submission of a delta.
   *
   * @param loggedInUser which is doing the requesting.
   * @param waveletName name of wavelet.
   * @param delta the wavelet delta to submit.
   * @param channelId the client's channel ID
   * @param listener callback for the result.
   */
  void submitRequest(ParticipantId loggedInUser, WaveletName waveletName,
      ProtocolWaveletDelta delta, String channelId, SubmitRequestListener listener);

  /**
   * Request to open a Wave. Optional waveletIdPrefixes allows the requester to
   * constrain which wavelets to include in the updates.
   *
   * @param loggedInUser which is doing the requesting.
   * @param waveId the wave id.
   * @param waveletIdFilter filter over wavelets to open
   * @param knownWavelets a collection of wavelet versions the client already
   *        knows
   * @param openListener callback for updates.
   */
  void openRequest(ParticipantId loggedInUser, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener);
}
