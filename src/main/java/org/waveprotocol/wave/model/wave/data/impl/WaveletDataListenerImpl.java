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

import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletDataListener;

/**
 * Vacuous implementation of a {@link WaveletDataListener}.
 *
 */
public class WaveletDataListenerImpl implements WaveletDataListener {

  @Override
  public void onBlipDataAdded(WaveletData waveletData, BlipData blip) {}

  @Override
  public void onBlipDataSubmitted(WaveletData waveletData, BlipData blip) {}

  @Override
  public void onBlipDataTimestampModified(
      WaveletData waveletData, BlipData b, long oldTime, long newTime) {}

  @Override
  public void onBlipDataVersionModified(
      WaveletData waveletData, BlipData blip, long oldVersion, long newVersion) {}

  @Override
  public void onBlipDataContributorAdded(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {}

  @Override
  public void onBlipDataContributorRemoved(
      WaveletData waveletData, BlipData blip, ParticipantId contributor) {}

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participant) {}

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participant) {}

  @Override
  public void onLastModifiedTimeChanged(WaveletData waveletData, long oldTime, long newTime) {}

  @Override
  public void onVersionChanged(WaveletData wavelet, long oldVersion, long newVersion) {}

  @Override
  public void onHashedVersionChanged(WaveletData waveletData, HashedVersion oldHashedVersion,
      HashedVersion newHashedVersion) {}

  @Override
  @Deprecated
  public void onRemoteBlipDataContentModified(WaveletData waveletData, BlipData blip) {}
}
