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

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * Provides a subscription service for changes to wavelets.
 *
 * @author anorth@google.com (Alex North)
 */
public interface WaveBus {
  /**
   * Receives wave bus messages.
   */
  interface Subscriber {
    /**
     * Notifies the subscriber of a wavelet update.
     *
     * @param wavelet the state of the wavelet after the deltas have
     *        been applied
     * @param deltas deltas applied to the wavelet
     */
    void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas);

    /**
     * Notifies the subscriber that a wavelet has been committed to persistent
     * storage.
     *
     * @param waveletName name of wavelet
     * @param version the version and hash of the wavelet as it was committed
     */
    void waveletCommitted(WaveletName waveletName, HashedVersion version);
  }

  /**
   * Subscribes to the bus, if the subscriber is not already subscribed.
   */
  void subscribe(Subscriber s);

  /**
   * Unsubscribes from the bus, if the subscriber is currently subscribed.
   */
  void unsubscribe(Subscriber s);
}
