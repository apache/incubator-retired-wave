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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletDataListener;

/**
 * Manages a set of wavelet data listeners, forwarding each event on to a set
 * of client listeners.
 *
 * @author anorth@google.com (Alex North)
 */
public class WaveletDataListenerManager implements WaveletDataListener {

  /** Set of listeners for change events. */
  private final CopyOnWriteSet<WaveletDataListener> listeners = CopyOnWriteSet.create();

  @Override
  public void onBlipDataAdded(WaveletData waveletData, BlipData blip) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataAdded(waveletData, blip);
    }
  }

  @Override
  public void onBlipDataTimestampModified(
      WaveletData waveletData, BlipData blip, long oldTime, long newTime) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataTimestampModified(waveletData, blip, oldTime, newTime);
    }
  }

  @Override
  public void onBlipDataVersionModified(
      WaveletData waveletData, BlipData blip, long oldVersion, long newVersion) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataVersionModified(waveletData, blip, oldVersion, newVersion);
    }
  }

  @Override
  public void onBlipDataContributorAdded(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataContributorAdded(waveletData, blip, contributor);
    }
  }

  @Override
  public void onBlipDataContributorRemoved(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataContributorRemoved(waveletData, blip, contributor);
    }
  }

  @Override
  public void onBlipDataSubmitted(WaveletData waveletData, BlipData blip) {
    for (WaveletDataListener l : listeners) {
      l.onBlipDataSubmitted(waveletData, blip);
    }
  }

  @Override
  public void onLastModifiedTimeChanged(WaveletData waveletData, long oldTime, long newTime) {
    for (WaveletDataListener l : listeners) {
      l.onLastModifiedTimeChanged(waveletData, oldTime, newTime);
    }
  }

  @Override
  public void onVersionChanged(WaveletData waveletData, long oldVersion, long newVersion) {
    for (WaveletDataListener l : listeners) {
      l.onVersionChanged(waveletData, oldVersion, newVersion);
    }
  }

  @Override
  public void onHashedVersionChanged(WaveletData waveletData, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion) {
    for (WaveletDataListener l : listeners) {
      l.onHashedVersionChanged(waveletData, oldHashedVersion, newHashedVersion);
    }
  }

  @Override
  public void onParticipantAdded(WaveletData waveletData, ParticipantId participant) {
    for (WaveletDataListener l : listeners) {
      l.onParticipantAdded(waveletData, participant);
    }
  }

  @Override
  public void onParticipantRemoved(WaveletData waveletData, ParticipantId participant) {
    for (WaveletDataListener l : listeners) {
      l.onParticipantRemoved(waveletData, participant);
    }
  }

  @Deprecated
  @Override
  public void onRemoteBlipDataContentModified(WaveletData waveletData, BlipData blip) {
    for (WaveletDataListener l : listeners) {
      l.onRemoteBlipDataContentModified(waveletData, blip);
    }
  }

  /**
   * Adds a new client listener to receive events received here. Adding the
   * same listener twice has no effect.
   *
   * @param listener a wavelet data listener
   */
  public void addListener(WaveletDataListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes a client listener, so it will no longer receive events. Removing
   * an unregistered listener has no effect.
   *
   * @param listener wavelet data listener to remove
   */
  public void removeListener(WaveletDataListener listener) {
    listeners.remove(listener);
  }
}
