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

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.io.IOException;

/**
 * Forwards calls to a delegate {@link WaveletDeltaRecordReader}.
 *
 * @author soren@google.com (Soren Lassen)
 */
public abstract class ForwardingWaveletDeltaRecordReader implements WaveletDeltaRecordReader {

  protected abstract WaveletDeltaRecordReader delegate();

  @Override
  public WaveletName getWaveletName() {
    return delegate().getWaveletName();
  }

  @Override
  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @Override
  public HashedVersion getEndVersion() {
    return delegate().getEndVersion();
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) throws IOException {
    return delegate().getDelta(version);
  }

  @Override
  public WaveletDeltaRecord getDeltaByEndVersion(long version) throws IOException {
    return delegate().getDeltaByEndVersion(version);
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) throws IOException {
    return delegate().getAppliedAtVersion(version);
  }

  @Override
  public HashedVersion getResultingVersion(long version) throws IOException {
    return delegate().getResultingVersion(version);
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version)
      throws IOException {
    return delegate().getAppliedDelta(version);
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) throws IOException {
    return delegate().getTransformedDelta(version);
  }
}
