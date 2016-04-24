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

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;

/**
 * Presents a {@link ReadableBlipData} as a {@link BlipData}, where
 * all mutation methods throw {@link UnsupportedOperationException}.
 *
 */
public final class UnmodifiableBlipData extends ForwardingReadableBlipData implements BlipData {

  private static final UnmodifiableWaveletData.Factory WAVELET_FACTORY =
      new UnmodifiableWaveletData.Factory();
  private final ReadableBlipData data;
  private UnmodifiableWaveletData wavelet; // populated lazily

  public UnmodifiableBlipData(ReadableBlipData data) {
    this.data = data;
  }

  @Override
  protected ReadableBlipData delegate() {
    return data;
  }

  /**
   * {@inheritDoc}
   *
   * Overrides the implementation from {@link ForwardingReadableBlipData} in
   * order to return a subtype of {@link WaveletData}, as required by the
   * {@link BlipData} interface.
   */
  @Override
  public UnmodifiableWaveletData getWavelet() {
    if (wavelet == null) {
      wavelet = WAVELET_FACTORY.create(data.getWavelet());
    }
    return wavelet;
  }

  @Override
  final public void submit() {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support submit");
  }

  @Override
  final public long setLastModifiedTime(long newTime) {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support setLastModifiedTime");
  }

  @Override
  final public long setLastModifiedVersion(long newVersion) {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support setLastModifiedVersion");
  }

  @Deprecated
  @Override
  public void onRemoteContentModified() {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support onRemoteContentModified");
  }

  @Override
  public void addContributor(ParticipantId participant) {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support addContributor");
  }

  @Override
  public void removeContributor(ParticipantId participant) {
    throw new UnsupportedOperationException(
        "UnmodifiableBlipData doesn't support removeContributor");
  }
}
