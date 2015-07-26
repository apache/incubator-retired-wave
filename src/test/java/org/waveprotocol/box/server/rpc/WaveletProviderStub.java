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

package org.waveprotocol.box.server.rpc;

import com.google.common.collect.ImmutableSet;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.util.TestDataUtil;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;

/**
 * Stub of {@link WaveletProvider} for testing.
 *
 * It currently hosts a single wavelet, which contains a single document.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveletProviderStub implements WaveletProvider {
  private WaveletData wavelet = null;
  private HashedVersion currentVersionOverride;
  private HashedVersion committedVersion;
  private boolean allowsAccess = true;

  public WaveletProviderStub() {
    wavelet = TestDataUtil.createSimpleWaveletData();
    setCommittedVersion(HashedVersion.unsigned(0));
  }

  @Override
  public void initialize() {
  }

  @Override
  public ExceptionalIterator<WaveId, WaveServerException> getWaveIds() {
    return ExceptionalIterator.FromIterator.create(
        Collections.singleton(wavelet.getWaveId()).iterator());
  }

  @Override
  public ImmutableSet<WaveletId> getWaveletIds(WaveId waveId) {
    return (waveId.equals(wavelet.getWaveId())) ? ImmutableSet.of(wavelet.getWaveletId())
        : ImmutableSet.<WaveletId> of();
  }

  @Override
  public CommittedWaveletSnapshot getSnapshot(WaveletName waveletName) {
    final byte[] JUNK_BYTES = new byte[] {0, 1, 2, 3, 4, 5, -128, 127};

    if (waveletName.waveId.equals(getHostedWavelet().getWaveId())
        && waveletName.waveletId.equals(getHostedWavelet().getWaveletId())) {
      HashedVersion version =
          (currentVersionOverride != null) ? currentVersionOverride : HashedVersion.of(
              getHostedWavelet().getVersion(), JUNK_BYTES);
      return new CommittedWaveletSnapshot(getHostedWavelet(), getCommittedVersion());
    } else {
      return null;
    }
  }

  @Override
  public void getHistory(WaveletName waveletName, HashedVersion versionStart, HashedVersion versionEnd,
      Receiver<TransformedWaveletDelta> receiver) throws WaveServerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void submitRequest(
      WaveletName waveletName, ProtocolWaveletDelta delta, SubmitRequestListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean checkAccessPermission(WaveletName waveletName, ParticipantId participantId) {
    return allowsAccess;
  }

  /**
   * @return the wavelet
   */
  public WaveletData getHostedWavelet() {
    return wavelet;
  }

  /**
   * @param currentVersionOverride the currentVersionOverride to set
   */
  public void setVersionOverride(HashedVersion currentVersionOverride) {
    this.currentVersionOverride = currentVersionOverride;
  }

  /**
   * @param committedVersion the committedVersion to set
   */
  public void setCommittedVersion(HashedVersion committedVersion) {
    this.committedVersion = committedVersion;
  }

  /**
   * @return the committedVersion
   */
  public HashedVersion getCommittedVersion() {
    return committedVersion;
  }

  /**
   * @param allowsAccess whether or not users have access permissions
   */
  public void setAllowsAccess(boolean allowsAccess) {
    this.allowsAccess = allowsAccess;
  }
}
