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

package org.waveprotocol.wave.crypto;

import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

/**
 * A simple store for signer infos (i.e., certificate chains).  Implementations must be
 * thread safe.
 */
public interface CertPathStore {

  /**
   * Given the of a signer (essentially the hash of a cert chain), returns the
   * full signer info, or null if the signerId is not found in the path store.
   * @param signerId the id of the cert chain. This is Base64-encoded PkiPath-
   *   encoding of the cert chain.
   */
  public SignerInfo getSignerInfo(byte[] signerId) throws SignatureException;

  /**
   * Add a new SignerInfo to the store. The object will be stored under the key
   * obtained by calling {@link SignerInfo#getSignerId()}.
   *
   * @throws SignatureException if the signer info could not be parsed into
   *   into a chain of X509Certificates, or if the domain in the signer info
   *   did not match the target certificate.
   */
  public void putSignerInfo(ProtocolSignerInfo signerInfo) throws SignatureException;
}
