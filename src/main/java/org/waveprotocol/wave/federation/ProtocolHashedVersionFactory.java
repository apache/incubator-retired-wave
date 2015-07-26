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

package org.waveprotocol.wave.federation;

import com.google.protobuf.ByteString;

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for creating ProtocolHashedVersion instances from bytes.
 *
 * @author thorogood@google.com (Sam Thorogood)
 * @author jochen@google.com (Jochen Bekmann)
 */
public class ProtocolHashedVersionFactory {
  private static final IdURIEncoderDecoder URI_CODEC =
    new IdURIEncoderDecoder(new JavaUrlCodec());

  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);

  /** The first N bits of a SHA-256 hash are stored in the hash. Must not exceed 256. */
  private static final int HASH_SIZE_BITS = 160;

  /**
   * Utility class only, disallow construction.
   */
  private ProtocolHashedVersionFactory() {
  }

  private static ByteString nextHash(ByteString historyHash, ByteString appliedDeltaBytes) {
    MessageDigest sha256;
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    sha256.update(historyHash.toByteArray());
    return ByteString.copyFrom(
        sha256.digest(appliedDeltaBytes.toByteArray()), 0, HASH_SIZE_BITS / 8);
  }

  /**
   * Return the version zero hash for the given wavelet name.
   */
  public static ProtocolHashedVersion createVersionZero(WaveletName waveletName) {
    byte[] initialHash = HASH_FACTORY.createVersionZero(waveletName).getHistoryHash();
    return ProtocolHashedVersion.newBuilder()
        .setVersion(0)
        .setHistoryHash(ByteString.copyFrom(initialHash))
        .build();
  }

  /**
   * Return the updated ProtocolHashedVersion for the given input.
   *
   * @param appliedDeltaBytes raw bytes of appliedDelta
   * @param appliedAt version delta was applied at, must match appliedDelta
   * @param operationsApplied number of operations applied, must match appliedDelta
   * @return new ProtocolHashedVersion
   */
  public static ProtocolHashedVersion create(ByteString appliedDeltaBytes,
      ProtocolHashedVersion appliedAt, int operationsApplied) {
    // TODO(thorogood): verify appliedAt and operationsApplied against the raw bytes?
    return ProtocolHashedVersion.newBuilder()
        .setVersion(appliedAt.getVersion() + operationsApplied)
        .setHistoryHash(nextHash(appliedAt.getHistoryHash(), appliedDeltaBytes))
        .build();
  }

}
