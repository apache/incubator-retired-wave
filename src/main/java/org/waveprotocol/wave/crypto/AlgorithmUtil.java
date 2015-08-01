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

import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;

/**
 * Utilities for stringifying enums from the wave protocol protobufs.
 */
public class AlgorithmUtil {

  private AlgorithmUtil() {
  }

  public static String getJceName(HashAlgorithm hashAlg) {
    switch (hashAlg) {
      case SHA256:
        return "SHA-256";
      case SHA512:
        return "SHA-512";
      default:
        throw new IllegalArgumentException("unknown hash alg: " + hashAlg);
    }
  }

  public static String getJceName(SignatureAlgorithm sigAlg) {
    switch (sigAlg) {
      case SHA1_RSA:
        return "SHA1withRSA";
      default:
        throw new IllegalArgumentException("unknown signature alg: " + sigAlg);
    }
  }
}
