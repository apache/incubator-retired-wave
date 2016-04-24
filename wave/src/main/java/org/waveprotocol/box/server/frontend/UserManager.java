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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * Collects active wave view subscriptions for a single participant.
 */
final class UserManager {
  private final ListMultimap<WaveId, WaveViewSubscription> subscriptions =
      LinkedListMultimap.create();

  /** The listeners interested in the specified wavelet. */
  @VisibleForTesting
  synchronized List<WaveViewSubscription> matchSubscriptions(WaveletName waveletName) {
    List<WaveViewSubscription> result = Lists.newArrayList();
    for (WaveViewSubscription subscription : subscriptions.get(waveletName.waveId)) {
      if (subscription.includes(waveletName.waveletId)) {
        result.add(subscription);
      }
    }
    return result;
  }

  /** Returns the subscription (if it exists) for a given wavelet and channel */
  private synchronized WaveViewSubscription findSubscription(WaveletName waveletName, String channelId) {
    for (WaveViewSubscription subscription : subscriptions.get(waveletName.waveId)) {
      if (subscription.includes(waveletName.waveletId)) {
        if (subscription.getChannelId().equals(channelId)) {
          return subscription;
        }
      }
    }
    return null;
  }

  /**
   * Receives additional deltas for the specified wavelet, of which we must be a
   * participant. Delta updates must be received in contiguous version order.
   */
  public synchronized void onUpdate(WaveletName waveletName, DeltaSequence deltas) {
    Preconditions.checkNotNull(waveletName);
    if (deltas.isEmpty()) {
      return;
    }
    List<WaveViewSubscription> subscriptions = matchSubscriptions(waveletName);
    for (WaveViewSubscription subscription : subscriptions) {
      subscription.onUpdate(waveletName, deltas);
    }
  }

  /**
   * Receives notification that the specified wavelet has been committed at the
   * specified version.
   */
  public void onCommit(WaveletName waveletName, HashedVersion version) {
    Preconditions.checkNotNull(waveletName);
    Preconditions.checkNotNull(version);
    List<WaveViewSubscription> listeners = matchSubscriptions(waveletName);
    for (WaveViewSubscription listener : listeners) {
      listener.onCommit(waveletName, version);
    }
  }

  /**
   * Subscribes a listener to updates on a wave, filtered by waveletId.
   *
   * @return a subscription
   */
  public synchronized WaveViewSubscription subscribe(WaveId waveId, IdFilter waveletIdFilter,
      String channelId, ClientFrontend.OpenListener listener) {
    WaveViewSubscription subscription =
        new WaveViewSubscription(waveId, waveletIdFilter, channelId, listener);
    subscriptions.put(waveId, subscription);
    return subscription;
  }

  /**
   * Tell the user manager that we have a submit request outstanding. While a
   * submit request is outstanding, all wavelet updates are queued.
   *
   * @param channelId the channel identifying the specific client
   * @param waveletName the name of the wavelet
   */
  public void submitRequest(String channelId, WaveletName waveletName) {
    WaveViewSubscription subscription = findSubscription(waveletName, channelId);
    if (subscription != null) {
      subscription.submitRequest(waveletName);
    }
  }

  /**
   * Signal the user manager that a submit response has been sent for the given
   * wavelet and version. Any pending wavelet updates will be sent. A matching
   * wavelet update for the given wavelet name and version will be discarded.
   *
   * @param channelId the channel identifying the specific client
   * @param waveletName the name of the wavelet
   * @param hashedVersionAfterApplication the version of the wavelet in the
   *        response (or null if the submit request failed)
   */
  public void submitResponse(String channelId, WaveletName waveletName,
      HashedVersion hashedVersionAfterApplication) {
    WaveViewSubscription subscription = findSubscription(waveletName, channelId);
    if (subscription != null) {
      subscription.submitResponse(waveletName, hashedVersionAfterApplication);
    }
  }
}
