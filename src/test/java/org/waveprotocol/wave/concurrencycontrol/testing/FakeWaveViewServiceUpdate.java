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

import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake WaveViewServiceUpdate implementation for providing canned responses.
 *
 */
public class FakeWaveViewServiceUpdate implements WaveViewService.WaveViewServiceUpdate {
  private final SchemaProvider schemas = SchemaCollection.empty();
  private final WaveletDataImpl.Factory dataFactory =
      WaveletDataImpl.Factory.create(ObservablePluggableMutableDocument.createFactory(schemas));

  public String channelId;
  public WaveletId waveletId;
  public HashedVersion lastCommittedVersion;
  public HashedVersion currentVersion;
  public ObservableWaveletData waveletSnapshot;
  public ArrayList<TransformedWaveletDelta> deltaList = CollectionUtils.newArrayList();
  public Boolean marker;

  public FakeWaveViewServiceUpdate setChannelId(String channelId) {
    this.channelId = channelId;
    return this;
  }

  public FakeWaveViewServiceUpdate setWaveletId(WaveletId waveletId) {
    this.waveletId = waveletId;
    return this;
  }

  public FakeWaveViewServiceUpdate setLastCommittedVersion(HashedVersion lastCommittedVersion) {
    this.lastCommittedVersion = lastCommittedVersion;
    return this;
  }

  public FakeWaveViewServiceUpdate setCurrentVersion(HashedVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }

  public FakeWaveViewServiceUpdate setWaveletSnapshot(WaveId waveId, ParticipantId creator,
      long creationTime, final HashedVersion hashedVersion) {
    assert hasWaveletId();
    waveletSnapshot = dataFactory.create(new EmptyWaveletSnapshot(waveId, waveletId, creator,
        hashedVersion, creationTime));
    return this;
  }

  public FakeWaveViewServiceUpdate addDelta(TransformedWaveletDelta delta) {
    deltaList.add(delta);
    return this;
  }

  public FakeWaveViewServiceUpdate setMarker(Boolean marker) {
    this.marker = marker;
    return this;
  }

  @Override public boolean hasChannelId() { return channelId != null; }
  @Override public String getChannelId() { return channelId; }

  @Override public boolean hasWaveletId() { return waveletId != null; }
  @Override public WaveletId getWaveletId() { return waveletId; }

  @Override public boolean hasLastCommittedVersion() { return lastCommittedVersion != null; }
  @Override public HashedVersion getLastCommittedVersion() { return lastCommittedVersion; }

  @Override public boolean hasCurrentVersion() { return currentVersion != null; }
  @Override public HashedVersion getCurrentVersion() { return currentVersion; }

  @Override public boolean hasWaveletSnapshot() { return waveletSnapshot != null; }
  @Override public ObservableWaveletData getWaveletSnapshot() { return waveletSnapshot; }

  @Override public boolean hasDeltas() { return !deltaList.isEmpty(); }
  @Override public List<TransformedWaveletDelta> getDeltaList() { return deltaList; }

  @Override public boolean hasMarker() { return marker != null; }
}
