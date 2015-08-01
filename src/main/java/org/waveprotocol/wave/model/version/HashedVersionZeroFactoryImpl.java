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

package org.waveprotocol.wave.model.version;

import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;

import java.io.UnsupportedEncodingException;

/**
 * Hashed version factory only capable of calculating hashes for version zero. A
 * version-zero hash requires no cryptographic calculations.
 */
public class HashedVersionZeroFactoryImpl implements HashedVersionFactory {

  private final IdURIEncoderDecoder uriCodec;

  public HashedVersionZeroFactoryImpl(IdURIEncoderDecoder uriCodec) {
    this.uriCodec = uriCodec;
  }

  @Override
  public HashedVersion createVersionZero(WaveletName waveletName) {
    try {
      // Same encoding as used protobuf/CodedOutputSteam to serialize a String to byte[].
      // http://code.google.com/p/protobuf/source/browse/trunk/java/src/main/java/com/google/protobuf/CodedOutputStream.java
      byte[] historyHash = uriCodec.waveletNameToURI(waveletName).getBytes("UTF-8");
      return HashedVersion.of(0, historyHash);
    } catch (EncodingException e) {
      throw new IllegalArgumentException("Bad wavelet name " + waveletName, e);
    } catch (UnsupportedEncodingException e) { // From getBytes().
      throw new IllegalStateException("UTF-8 unsupported in creating version zero hash", e);
    }
  }

  /**
   * Versions != zero are not supported by this factory.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public HashedVersion create(byte[] appliedDeltaBytes, HashedVersion hashedVersionAppliedAt,
      int operationsApplied) {
    // For lightweight users of this factory there are no deps on crypto code.
    throw new UnsupportedOperationException("This factory can only create hashed version zero.");
  }
}
