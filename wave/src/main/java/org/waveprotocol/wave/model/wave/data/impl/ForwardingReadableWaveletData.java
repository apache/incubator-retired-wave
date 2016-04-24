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

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Set;

/**
 * Forwards to a delegate. Implementations must implement the delegate() method
 * and override any appropriate methods.
 *
 */
public abstract class ForwardingReadableWaveletData implements ReadableWaveletData {

  protected abstract ReadableWaveletData delegate();

  @Override
  public long getCreationTime() {
    return delegate().getCreationTime();
  }

  @Override
  public ParticipantId getCreator() {
    return delegate().getCreator();
  }

  @Override
  public HashedVersion getHashedVersion() {
    return delegate().getHashedVersion();
  }

  @Override
  public ReadableBlipData getDocument(String documentName) {
    return delegate().getDocument(documentName);
  }

  @Override
  public Set<String> getDocumentIds() {
    return delegate().getDocumentIds();
  }

  @Override
  public long getLastModifiedTime() {
    return delegate().getLastModifiedTime();
  }

  @Override
  public Set<ParticipantId> getParticipants() {
    return delegate().getParticipants();
  }

  @Override
  public long getVersion() {
    return delegate().getVersion();
  }

  @Override
  public WaveId getWaveId() {
    return delegate().getWaveId();
  }

  @Override
  public WaveletId getWaveletId() {
    return delegate().getWaveletId();
  }
}
