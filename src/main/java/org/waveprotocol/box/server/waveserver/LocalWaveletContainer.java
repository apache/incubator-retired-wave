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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * A local wavelet may be updated by submits. The local wavelet will perform
 * operational transformation on the submitted delta and assign it the latest
 * version of the wavelet.
 *
 *
 */
interface LocalWaveletContainer extends WaveletContainer {

  /**
   * Manufactures local wavelet containers.
   */
  interface Factory extends WaveletContainer.Factory<LocalWaveletContainer> { }

  /**
   * Request that a given delta is submitted to the wavelet.
   *
   * @param waveletName name of wavelet.
   * @param delta to be submitted to the server.
   * @return result of application to the wavelet, both the applied result and the transformed
   *         result.
   * @throws OperationException
   * @throws InvalidProtocolBufferException
   * @throws InvalidHashException
   * @throws PersistenceException
   * @throws WaveletStateException
   */
  public WaveletDeltaRecord submitRequest(WaveletName waveletName, ProtocolSignedDelta delta)
      throws OperationException, InvalidProtocolBufferException, InvalidHashException,
      PersistenceException, WaveletStateException;

  /**
   * Check whether a submitted delta (identified by its hashed version after application) was
   * signed by a given signer. This (by design) should additionally validate that the history hash
   * is valid for the delta.
   *
   * @param hashedVersion to check whether in the history of the delta
   * @param signerId of the signer
   */
  boolean isDeltaSigner(HashedVersion hashedVersion, ByteString signerId);
}
