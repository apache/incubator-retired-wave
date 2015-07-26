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

import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Collection;

/**
 * Multiplexes several {@link OperationChannel operation channels} together. The
 * lifecycle of this multiplexer is simply
 * {@link #open(Listener, IdFilter, Collection)} followed by {@link #close()}.
 *
 */
public interface OperationChannelMultiplexer {
  /**
   * Listener for handling multiplexer events.
   */
  interface Listener {
    /**
     * Notifies this listener that a new channel has been established. The new
     * channel, based on the snapshot given here, is ready to be used
     * immediately.
     *
     * @param channel new channel
     * @param snapshot initial state of the wavelet on the new channel
     * @param accessibility initial accessibility of the new wavelet
     */
    void onOperationChannelCreated(OperationChannel channel, ObservableWaveletData snapshot,
        Accessibility accessibility);

    /**
     * Notifies this listener that an existing channel has been destroyed. The dropped
     * channel is typically likely to be replaced by a new one.
     *
     * @param channel destroyed channel
     * @param waveletId the id of the wavelet serviced over this channel
     */
    void onOperationChannelRemoved(OperationChannel channel, WaveletId waveletId);

    /**
     * Notifies this listener that the initial open has finished.
     *
     * The "initial open" is defined as the point after which the channel
     * listener has been notified of all operation channels for some "initial
     * set" of wavelets as decided by the server. Note that the channel listener
     * may also be notified of other channels, interleaved amongst the channels
     * for the initial set, before {@link #onOpenFinished()} is called.
     */
    void onOpenFinished();

    /**
     * Notifies this listener that the multiplexer has failed. This may occur
     * before or after any channels are created, before or after
     * {@link #onOpenFinished()}.
     *
     * No further messages will be received by any callback or operation channel
     * after this message.
     */
    void onFailed(CorruptionDetail detail);
  }

  /**
   * Information required to open a wavelet at a known state.
   */
  public static final class KnownWavelet {
    /** Wavelet data. May not be null. */
    public final ObservableWaveletData snapshot;

    /** Last committed version of wavelet. May not be null. */
    public final HashedVersion committedVersion;

    /** The wavelet's accessibility to the user. May not be null. */
    public final Accessibility accessibility;

    public KnownWavelet(ObservableWaveletData snapshot, HashedVersion committedVersion,
        Accessibility accessibility) {
      this.snapshot = snapshot;
      this.committedVersion = committedVersion;
      this.accessibility = accessibility;
    }
  }

  /**
   * Opens this multiplexer. After it becomes open, operation channels will be
   * passed to the {@code muxListener} as they come into existence. The listener
   * may be notified as part of this call, or afterwards.
   *
   * @param muxListener listener for multiplexer events
   * @param waveletFilter filter specifying the wavelets to open
   * @param knownWaveletSnapshots wavelet channels already known about, a
   *        channel will be immediately opened for each known wavelet
   */
  public void open(Listener muxListener, IdFilter waveletFilter,
      Collection<KnownWavelet> knownWaveletSnapshots);

  /**
   * Opens this multiplexer with no known wavelets.
   *
   * @see #open(Listener, IdFilter, Collection)
   */
  public void open(Listener muxListener, IdFilter waveletFilter);

  /**
   * Closes this multiplexer. The channel listener will no longer be notified of
   * new channels, and no more operations will be received on existing channels.
   * The behavior of requests to create new channels, or the submission of
   * operations to existing channels, is undefined.
   */
  public void close();

  /**
   * Creates a new operation channel.  The listener will be notified of the new
   * channel during this method.
   *
   * @param waveletId wavelet id for the new operation channel
   * @param creator address of the wavelet creator
   */
  public void createOperationChannel(WaveletId waveletId, ParticipantId creator);
}
