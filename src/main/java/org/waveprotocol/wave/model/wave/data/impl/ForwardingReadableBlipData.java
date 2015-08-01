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
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Set;

/**
 * Forwards to a delegate. Implementations must implement the delegate() method
 * and override any appropriate methods.
 *
 */
public abstract class ForwardingReadableBlipData implements ReadableBlipData {

  protected abstract ReadableBlipData delegate();

  @Override
  public ReadableWaveletData getWavelet() {
    return delegate().getWavelet();
  }

  @Override
  public ParticipantId getAuthor() {
    return delegate().getAuthor();
  }

  @Override
  public Set<ParticipantId> getContributors() {
    return delegate().getContributors();
  }

  @Override
  public long getLastModifiedTime() {
    return delegate().getLastModifiedTime();
  }

  @Override
  public long getLastModifiedVersion() {
    return delegate().getLastModifiedVersion();
  }

  @Override
  public DocumentOperationSink getContent() {
    return delegate().getContent();
  }

  @Override
  public String getId() {
    return delegate().getId();
  }
}
