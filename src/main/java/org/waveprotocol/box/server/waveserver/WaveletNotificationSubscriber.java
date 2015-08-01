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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * Receives wave notifications.
 *
 * @author soren@google.com (Soren Lassen)
 */
public interface WaveletNotificationSubscriber {
  /**
   * Notifies of a wavelet update.
   *
   * @param wavelet the state of the wavelet after the deltas have
   *        been applied
   * @param deltas deltas applied to the wavelet
   * @param domainsToNotify domains who should know. Empty set if the wavelet is remote.
   */
  void waveletUpdate(ReadableWaveletData wavelet, ImmutableList<WaveletDeltaRecord> deltas,
      ImmutableSet<String> domainsToNotify);

  /**
   * Notifies that a wavelet has been committed to persistent storage.
   *
   * @param waveletName name of wavelet
   * @param version the version and hash of the wavelet as it was committed
   * @param domainsToNotify domains who should know. Empty set if the wavelet is remote.
   */
  void waveletCommitted(WaveletName waveletName, HashedVersion version,
      ImmutableSet<String> domainsToNotify);
}
