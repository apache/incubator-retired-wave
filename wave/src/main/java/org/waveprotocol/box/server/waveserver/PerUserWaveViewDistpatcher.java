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
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Forwards wavelet notifications that can cause index changes to subscribers.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class PerUserWaveViewDistpatcher implements WaveBus.Subscriber, PerUserWaveViewBus {
  private static final Log LOG = Log.get(PerUserWaveViewDistpatcher.class);

  private static final CopyOnWriteArraySet<PerUserWaveViewBus.Listener> listeners =
      new CopyOnWriteArraySet<PerUserWaveViewBus.Listener>();

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas) {
    WaveletId waveletId = wavelet.getWaveletId();
    WaveId waveId = wavelet.getWaveId();
    WaveletName waveletName = WaveletName.of(waveId, waveletId);
    if(LOG.isInfoLoggable()) {
      LOG.info("Got update for " + waveId + " " + waveletId);
    }

    // Find whether participants were added/removed and update the views
    // accordingly.
    for (TransformedWaveletDelta delta : deltas) {
      for (WaveletOperation op : delta) {
        if (op instanceof AddParticipant) {
          if(LOG.isInfoLoggable()) {
            LOG.info("Update contains AddParticipant for " + ((AddParticipant)op).getParticipantId());
          }

          ParticipantId user = ((AddParticipant) op).getParticipantId();
          // Check first if we need to update views for this user.
          for (Listener listener : listeners) {
            listener.onParticipantAdded(waveletName, user);
          }
        } else if (op instanceof RemoveParticipant) {
          ParticipantId user = ((RemoveParticipant) op).getParticipantId();
          for (Listener listener : listeners) {
            listener.onParticipantRemoved(waveletName, user);
          }
        }
      }
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    // No op.
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
