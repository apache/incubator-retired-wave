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

package org.waveprotocol.wave.model.wave.opbased;

import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.WaveletListener;

/**
 * Vacuous implementation of a {@link WaveletListener}.
 *
 */
public class WaveletListenerImpl implements WaveletListener {

  @Override
  public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {}

  @Override
  public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {}

  @Override
  public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {}

  @Override
  public void onBlipTimestampModified(
      ObservableWavelet wavelet, Blip blip, long oldTime, long newTime) {}

  @Override
  public void onBlipVersionModified(
      ObservableWavelet wavelet, Blip blip, Long oldVersion, Long newVersion) {}

  @Override
  public void onBlipContributorAdded(
      ObservableWavelet wavelet, Blip blip, ParticipantId contributor) {}

  @Override
  public void onBlipContributorRemoved(
      ObservableWavelet wavelet, Blip blip, ParticipantId contributor) {}

  @Override
  public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {}

  @Override
  public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {}

  @Override
  public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {}

  @Override
  public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {}

  @Override
  public void onHashedVersionChanged(ObservableWavelet wavelet,
      HashedVersion oldHashedVersion, HashedVersion newHashedVersion) {}

  @Override
  @Deprecated
  public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {}
}
