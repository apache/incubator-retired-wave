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

import com.google.common.util.concurrent.ListenableFuture;

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Provides a subscription service for changes to wavelets that can cause
 * modification of the per user wave view. It is an adapter interface for {@link WaveBus}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public interface PerUserWaveViewBus {

  /**
   * Receives per user wave view bus messages.
   */
  interface Listener {

    /**
     * Notifies the subscriber of an user added to wavelet.
     *
     * @param waveletName the wavelet name.
     * @param participant the participant that was added.
     * @return the future that allows to be notified of the update completion.
     */
    ListenableFuture<Void> onParticipantAdded(WaveletName waveletName, ParticipantId participant);

    /**
     * Notifies the subscriber of an user removed from wavelet.
     *
     * @param waveletName the wavelet name.
     * @param participant the participant that was added.
     * @return the future that allows to be notified of the update completion.
     */
    ListenableFuture<Void> onParticipantRemoved(WaveletName waveletName, ParticipantId participant);

    /**
     * Notifies the subscriber of a new wavelet that should be indexed.
     *
     * @param waveletName the wavelet name.
     * @return the future that allows to be notified of the update completion.
     */
    ListenableFuture<Void> onWaveInit(WaveletName waveletName);
  }

  /**
   * Subscribes to the bus, if the subscriber is not already subscribed.
   */
  void addListener(Listener listener);

  /**
   * Unsubscribes from the bus, if the subscriber is currently subscribed.
   */
  void removeListener(Listener listener);
}
