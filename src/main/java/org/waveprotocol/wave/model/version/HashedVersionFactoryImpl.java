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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Factory for creating arbitrary hashed versions.
 */
public class HashedVersionFactoryImpl extends HashedVersionZeroFactoryImpl {

  /** The first N bits of a SHA-256 hash are stored in the hash must be <= 256 */
  private static final int HASH_SIZE_BITS = 160;

  @VisibleForTesting
  // Allow override for unit testing.
  static int hashSizeBits = HASH_SIZE_BITS;

  public HashedVersionFactoryImpl(IdURIEncoderDecoder uriCodec) {
    super(uriCodec);
  }

  private static byte[] calculateHash(byte[] historyHash, byte[] appliedDeltaBytes) {
    byte[] joined = new byte[appliedDeltaBytes.length + historyHash.length];
    byte[] result = new byte[hashSizeBits / 8];
    System.arraycopy(historyHash, 0, joined, 0, historyHash.length);
    System.arraycopy(appliedDeltaBytes, 0, joined, historyHash.length, appliedDeltaBytes.length);
    try {
      historyHash = MessageDigest.getInstance("SHA-256").digest(joined);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    System.arraycopy(historyHash, 0, result, 0, result.length);
    return result;
  }

  @Override
  public HashedVersion create(byte[] appliedDeltaBytes,
      HashedVersion hashedVersionAppliedAt, int operationsApplied) {

    return HashedVersion.of(hashedVersionAppliedAt.getVersion() + operationsApplied,
        calculateHash(hashedVersionAppliedAt.getHistoryHash(), appliedDeltaBytes));
  }
}
