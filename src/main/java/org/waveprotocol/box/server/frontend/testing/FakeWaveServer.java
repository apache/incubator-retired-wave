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

package org.waveprotocol.box.server.frontend.testing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.comms.WaveClientRpc;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A fake single-user wave server which only echoes back submitted deltas and
 * corresponding index wave deltas.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class FakeWaveServer extends FakeClientFrontend {
  /** Fake application timestamp for confirming a successful submit. */
  private static final long APP_TIMESTAMP = 0;

  /** Known wavelet states, excluding index wavelets. */
  private final Map<WaveId, Map<WaveletId, WaveletData>> waves = Maps.newHashMap();

  /** A history of submitted deltas, per wavelet. Does not store generated index deltas. */
  private final ListMultimap<WaveletName, TransformedWaveletDelta> deltas =
      ArrayListMultimap.create();

  /** The current versions of the user's wavelets, including index wavelets */
  private final Map<WaveletName, HashedVersion> versions = Maps.newHashMap();

  /** The user that is connected to this server */
  private ParticipantId user = null;


  @Override
  public void openRequest(ParticipantId participant, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    if (user == null) {
      user = participant;
    } else {
      Preconditions.checkArgument(participant.equals(user), "Unexpected user");
    }

    super.openRequest(participant, waveId, waveletIdFilter, knownWavelets, openListener);

    Map<WaveletId, WaveletData> wavelets = waves.get(waveId);
    if (wavelets != null) {
      // Send any deltas we have in this wave to the client, in the order we got
      // them.
      for (WaveletData wavelet : wavelets.values()) {
        WaveletName name = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());
        waveletUpdate(wavelet, DeltaSequence.of(deltas.get(name)));
      }
    }
  }

  @Override
  public void submitRequest(ParticipantId loggedInUser, WaveletName waveletName,
      ProtocolWaveletDelta delta, @Nullable String channelId, SubmitRequestListener listener) {
    super.submitRequest(loggedInUser, waveletName, delta, channelId, listener);

    Map<WaveletId, WaveletData> wavelets = waves.get(waveletName.waveId);
    if (wavelets == null) {
      wavelets = Maps.newHashMap();
      waves.put(waveletName.waveId, wavelets);
    }

    WaveletData wavelet = wavelets.get(waveletName.waveletId);
    if (wavelet == null) {
      long dummyCreationTime = System.currentTimeMillis();
      wavelet = WaveletDataUtil.createEmptyWavelet(
          waveletName, ParticipantId.ofUnsafe(delta.getAuthor()),
          HashedVersion.unsigned(0), dummyCreationTime);
      wavelets.put(waveletName.waveletId, wavelet);
    }

    // Add the delta to the history and update the wavelet's version.
    HashedVersion resultingVersion = updateAndGetVersion(waveletName, delta.getOperationCount());
    TransformedWaveletDelta versionedDelta = CoreWaveletOperationSerializer.deserialize(delta,
        resultingVersion, APP_TIMESTAMP);
    deltas.put(waveletName, versionedDelta);

    // Confirm submit success.
    doSubmitSuccess(waveletName, resultingVersion, APP_TIMESTAMP);
    // Send an update echoing the submitted delta. Note: the document state is
    // ignored.
    waveletUpdate(wavelet, DeltaSequence.of(versionedDelta));
    // Send a corresponding update of the index wave.
  }

  /**
   * Updates and returns the version of a given wavelet.
   *
   * @param waveletName of the wavelet whose version to update.
   * @param operationsCount applied to the wavelet.
   * @return the new hashed version of the wavelet.
   */
  private HashedVersion updateAndGetVersion(WaveletName waveletName, int operationsCount) {
    // Get the current version.
    HashedVersion version = versions.get(waveletName);

    // Calculate the new version.
    if (version != null) {
      version = HashedVersion.unsigned(version.getVersion() + operationsCount);
    } else {
      version = HashedVersion.unsigned(operationsCount);
    }

    // Store and return the new version.
    versions.put(waveletName, version);
    return version;
  }
}
