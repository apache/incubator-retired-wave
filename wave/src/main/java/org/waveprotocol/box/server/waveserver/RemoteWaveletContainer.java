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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import org.waveprotocol.wave.federation.FederationException;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * Remote wavelets differ from local ones in that deltas are not submitted for OT,
 * rather they are updated when a remote wave service provider has applied and sent
 * a delta.
 */
interface RemoteWaveletContainer extends WaveletContainer {

  /**
   * Manufactures remote wavelet containers.
   */
  interface Factory extends WaveletContainer.Factory<RemoteWaveletContainer> { }

  /**
   * Update the state of the remote wavelet. This acts somewhat like a high
   * water mark - if the provided deltas would continue a contiguous block from
   * version zero, then they will be immediately transformed and returned to the
   * client. If they do not, then an asynchronous callback will be kicked off to
   * request the missing deltas.
   *
   * @param deltas the list of (serialized applied) deltas for the update
   * @param domain the listener domain where these deltas were received
   * @param federationProvider the provider where missing data may be sourced
   * @param certificateManager for verifying signatures and requesting signer info
   * @return future which is set after the deltas are applied to the local
   *         state or a failure occurs.
   *         Any failure is reported as a {@link FederationException}.
   */
  ListenableFuture<Void> update(List<ByteString> deltas, String domain,
      WaveletFederationProvider federationProvider, CertificateManager certificateManager);

  /**
   * Is called when a commit notice is received from the wavelet host.
   */
  void commit(HashedVersion version);
}
