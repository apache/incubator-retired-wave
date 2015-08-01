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

package org.waveprotocol.wave.model.operation.wave;

import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Encapsulates context information for a wave operation.
 *
 */
public final class WaveletOperationContext {

  /**
   * Factory for creating {@code WaveletOperationContext}s for
   * local operations created in a client.
   * The created contexts have no version information, as
   * the final version can only be inferred once the server
   * acknowledges the opertions.
   */
  public interface Factory {
    /**
     * Throws UnsupportedOperationException in methods that shouldn't be used
     * in read only wavelets.
     */
    static final Factory READONLY = new Factory() {
        @Override
        public WaveletOperationContext createContext() {
          throw new UnsupportedOperationException("Read only");
        }

        @Override
        public WaveletOperationContext createContext(ParticipantId creator) {
          throw new UnsupportedOperationException("Read only");
        }
      };

    /**
     * Creates a new context for an operation to be attributed to this factory's
     * choice of creator.
     *
     * @return a new operation context.
     */
    WaveletOperationContext createContext();

    /**
     * Creates a new operation context.
     *
     * @param creator whose operation this context is for.
     * @return a new operation context.
     */
    WaveletOperationContext createContext(ParticipantId creator);
  }

  /** Time at which an operation occurred. */
  private final long timestamp;

  /** The participant that caused an operation. */
  private final ParticipantId creator;

  /** Number of versions to increment after applying this operation. */
  private final long versionIncrement;

  /** Hashed version of the wavelet after applying this operation (optional). */
  private final HashedVersion hashedVersion;

  /**
   * Creates a context with no hashed version.
   *
   * @see #WaveletOperationContext(ParticipantId, long, long, HashedVersion)
   */
  public WaveletOperationContext(ParticipantId creator, long timestamp, long versionIncrement) {
    this(creator, timestamp, versionIncrement, null);
  }

  /**
   * Creates a context.
   *
   * @param creator operation creator
   * @param timestamp operation time
   * @param versionIncrement number of version increment
   * @param hashedVersion new hashed version (or null)
   */
  public WaveletOperationContext(ParticipantId creator, long timestamp, long versionIncrement,
      HashedVersion hashedVersion) {
    this.creator = creator;
    this.timestamp = timestamp;
    this.versionIncrement = versionIncrement;
    this.hashedVersion = hashedVersion;
  }

  /**
   * Tests if this context has a {@link #getTimestamp() timestamp}.
   *
   * @return true if this context has a timestamp
   */
  public boolean hasTimestamp() {
    return timestamp != Constants.NO_TIMESTAMP;
  }

  /**
   * Gets the time at which the operation was produced (by a client)
   * or applied (by a server). Only meaningful when
   * {@link #hasTimestamp()}.
   *
   * @return the timestamp of the operation occurred
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the participant that caused the operation.
   *
   * @return the participant that caused the operation
   */
  public ParticipantId getCreator() {
    return creator;
  }

  /**
   * Gets the version of the target to which the operation applies.
   *
   * @return number of versions to increment after this operation
   */
  public long getVersionIncrement() {
    return versionIncrement;
  }

  /**
   * Checks whether this context has a {@link #getHashedVersion() hashed
   * version} .
   */
  public boolean hasHashedVersion() {
    return hashedVersion != null;
  }

  /**
   * Gets the distinct version after this operation.
   */
  public HashedVersion getHashedVersion() {
    return hashedVersion;
  }

  @Override
  public String toString() {
    return "WaveletOperationContext(creator: " + creator + ", timestamp: " + timestamp
        + ", version increment: " + versionIncrement + "," + "hashedVersion: " + hashedVersion
        + ")";
  }
}
