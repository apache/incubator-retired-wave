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
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the WaveView rpcs as a channel.
 * Lifecycle:
 * ({@link #open(Listener, IdFilter, Map)} then {@link #close()}.
 *
 */
public interface ViewChannel {
  /**
   * Callback interface for asynchronous notification of connection events,
   * which corresponds to the following regular expression:
   * <pre>
   *   Connection = onConnected onUpdate* [onOpenFinished] onUpdate* [onFailure] onClosed
   * </pre>
   * Note that each event can only occur at most once.
   */
  interface Listener {
    /**
     * Notifies this listener that the channel is now connected, and deltas
     * can be submitted.
     */
    void onConnected();

    /**
     * Notifies this listener that the initial set of updates have been
     * received.
     */
    void onOpenFinished() throws ChannelException;

    /**
     * Notifies this listener that an exception has occurred. The ViewChannel is not
     * closed yet when this method is called, but it will be closed after this call.
     *
     * @param ex The exception that occured whilst handling responses from the server.
     */
    void onException(ChannelException ex);

    /**
     * Notifies this listener that the connection has terminated.
     *
     * Note: "terminated" is interpreted as client-side termination, either
     *   through a {@link #close() requested close} or through a failure.
     *   It does not necessarily mean that the server has acknowledge the
     *   channel closure.  Indeed, there may be update messages in transit
     *   both to and from the server.  It is up to a higher layer of the
     *   communication stack to deal with such outstanding messages.
     */
    void onClosed();

    /**
     * Notifies this listener of a snapshot on the stream.
     *
     * @param waveletId             id of the wavelet being updated
     * @param wavelet               optional update message (may be null)
     * @param lastCommittedVersion  optional committed version (may be null)
     * @param currentSignedVersion  optional current/latest version on the server
     */
    void onSnapshot(WaveletId waveletId, ObservableWaveletData wavelet,
        HashedVersion lastCommittedVersion,
        HashedVersion currentSignedVersion)
        throws ChannelException;

    /**
     * Notifies this listener of an update on the stream.
     *
     * @param waveletId             id of the wavelet being updated
     * @param waveletDeltas                optional deltas (may be empty)
     * @param lastCommittedVersion  optional committed version (may be null)
     * @param currentSignedVersion  optional current/latest version on the server
     */
    void onUpdate(WaveletId waveletId, List<TransformedWaveletDelta> waveletDeltas,
        HashedVersion lastCommittedVersion, HashedVersion currentSignedVersion)
        throws ChannelException;
  }

  /**
   * Opens this WaveView channel, requesting that, for some wavelets, only
   * deltas should be received.
   *
   * Can be called only once.
   *
   * @param viewListener     listener for connection lifecycle events and incoming updates
   * @param waveletFilter    filter specifying wavelets to open
   * @param knownWavelets    map of wavelet ids to lists of known
   *                         versions, from one of which deltas should be
   *                         streamed
   */
  void open(Listener viewListener, IdFilter waveletFilter,
      Map<WaveletId, List<HashedVersion>> knownWavelets);

  /**
   * Closes this WaveView channel.
   */
  void close();

  /**
   * Submits a delta on this channel.
   *
   * @param waveletId       id of the target wavelet
   * @param delta           delta to apply
   * @param callback        callback to notify of the submit response
   */
  void submitDelta(WaveletId waveletId, WaveletDelta delta, SubmitCallback callback);

  /**
   * @param waveletId The wavelet that we want to get debug info.
   * @return Debug string that details profile information regarding data being sent.
   */
  String debugGetProfilingInfo(WaveletId waveletId);
}
