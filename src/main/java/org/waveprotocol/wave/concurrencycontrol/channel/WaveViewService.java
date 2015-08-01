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

package org.waveprotocol.wave.concurrencycontrol.channel;

import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.List;
import java.util.Map;

/**
 * A service that provides a model object-based interface to the Wave RPC service.
 * Implementations should convert the model objects passed in to the appropriate
 * serializable classes and send them across the wire.
 *
 */
public interface WaveViewService {
  /**
   * An interface representing a stream update's contents.
   * Only one of {channelId, snapshot, deltas} should be present.
   */
  interface WaveViewServiceUpdate {
    boolean hasChannelId();
    String getChannelId();

    boolean hasWaveletId();
    WaveletId getWaveletId();

    boolean hasLastCommittedVersion();
    HashedVersion getLastCommittedVersion();

    boolean hasCurrentVersion();
    HashedVersion getCurrentVersion();

    boolean hasWaveletSnapshot();
    ObservableWaveletData getWaveletSnapshot();

    boolean hasDeltas();
    List<TransformedWaveletDelta> getDeltaList();

    boolean hasMarker();
  }

  /**
   * Streaming callback for an open wave view connection to the server.
   */
  interface OpenCallback {
    /**
     * Called when a protocol error occurs in processing before the callback is called.
     */
    void onException(ChannelException e);

    /**
     * Called when an update arrives.
     *
     * @param update update object to return for this callback
     */
    public void onUpdate(WaveViewServiceUpdate update);

    /**
     * Called when the stream closes.
     *
     * @param response error message, or null if the stream closed normally
     */
    public void onSuccess(String response);

    /**
     * Called when the task fails.
     *
     * @param reason failure reason for this callback
     */
    public void onFailure(String reason);
  }

  /**
   * Callback for submitting a delta to the server.
   */
  interface SubmitCallback {
    void onSuccess(HashedVersion version, int opsApplied, String errorMessage,
        ResponseCode responseCode);

    void onFailure(String failure);
  }

  /**
   * Callback for closing a connection to the server.
   */
  interface CloseCallback {
    void onSuccess();

    void onFailure(String failure);
  }

  /**
   * Opens a wave connection to the server.
   */
  void viewOpen(IdFilter waveletFilter, Map<WaveletId, List<HashedVersion>> knownWavelets,
      OpenCallback callback);

  /**
   * Submits a delta to the server. On success, the server replies with
   * the latest version, the number of operations applied, and an error message
   * and/or response code.
   *
   * @return the request id that can be passed later into
   *     {@link #debugGetProfilingInfo(String)}
   */
  String viewSubmit(WaveletName wavelet, WaveletDelta delta, String channelId,
      SubmitCallback callback);

  /**
   * Closes a wave from the server.
   */
  void viewClose(WaveId waveId, String channelId, CloseCallback callback);

  /**
   * @param requestId The requestId that we want to get debug info.
   * @return Debug string that details profile information regarding data being sent.
   */
  String debugGetProfilingInfo(String requestId);
}
