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

import com.google.common.collect.MapMaker;

import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple implementation of a cert-path store. This implementation is
 * in-memory and will obviously lose certificate chains when the server is
 * shut down. It will also not share stored data among several instances of the
 * server.
 *
 * Replace this implementation by injecting a new {@link CertPathStore} using
 * Guice.
 */
public class DefaultCertPathStore implements CertPathStore {

  private final ConcurrentMap<ByteBuffer, SignerInfo> map = new MapMaker().makeMap();

  /*
   * @see CertPathStore#get(java.lang.String)
   */
  public SignerInfo getSignerInfo(byte[] signerId) {
    return map.get(ByteBuffer.wrap(signerId));
  }

  /*
   * @see CertPathStore#put(SignerInfo)
   */
  public void putSignerInfo(ProtocolSignerInfo protobuf) throws SignatureException {
    SignerInfo signerInfo = new SignerInfo(protobuf);
    map.put(ByteBuffer.wrap(signerInfo.getSignerId()), signerInfo);
  }
}
