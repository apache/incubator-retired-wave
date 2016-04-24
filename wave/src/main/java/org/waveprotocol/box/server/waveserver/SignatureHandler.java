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

import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;

/**
 * Instances of this class provide signature-related information and decide how
 * deltas should be signed.
 */
public interface SignatureHandler {
  interface Factory {
   SignatureHandler getInstance();
  }
  
  /**
   * @return the domain of this signer.
   */
  String getDomain();

  /**
   * Returns the signer info associated with this signer or null if none is
   * available.
   */
  SignerInfo getSignerInfo();

  /**
   * Returns a list of the appropriate signatures for the specified delta.
   */
  Iterable<ProtocolSignature> sign(ByteStringMessage<ProtocolWaveletDelta> delta);

}
