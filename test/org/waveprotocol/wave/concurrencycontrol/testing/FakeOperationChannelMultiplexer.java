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

package org.waveprotocol.wave.concurrencycontrol.testing;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannelMultiplexer;

import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Collection;

/**
 * A minimal fake multiplexer.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FakeOperationChannelMultiplexer implements OperationChannelMultiplexer {

  @Override
  public void close() {
    // Does nothing
  }

  @Override
  public void createOperationChannel(WaveletId waveletId,
      org.waveprotocol.wave.model.wave.ParticipantId creator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void open(Listener muxListener, IdFilter waveletFilter,
      Collection<KnownWavelet> knownWavelets) {
    if (!knownWavelets.isEmpty()) {
      for (final KnownWavelet knownWavelet : knownWavelets) {
        Preconditions.checkNotNull(knownWavelet.snapshot,
            "Snapshot has no wavelet");
        Preconditions.checkNotNull(knownWavelet.committedVersion,
            "Known wavelet has null committed version");


        muxListener.onOperationChannelCreated(new FakeOperationChannel(),
            knownWavelet.snapshot, knownWavelet.accessibility);
      }
      // consider the wave as if open has finished.
      muxListener.onOpenFinished();
    }
  }

  @Override
  public void open(Listener muxListener, IdFilter waveletFilter) {
  }
}
