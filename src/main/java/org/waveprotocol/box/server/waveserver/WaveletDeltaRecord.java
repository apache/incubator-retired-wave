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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.annotation.Nullable;

/**
 * Bundles an applied delta (an original signed delta with information about how
 * it was applied) and its transformed operations.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class WaveletDeltaRecord {
  private final HashedVersion appliedAtVersion;
  @Nullable private final ByteStringMessage<ProtocolAppliedWaveletDelta> applied;
  private final TransformedWaveletDelta transformed;

  /**
   * @param appliedAtVersion the version which the transformed delta applies at
   * @param applied is the applied delta which transforms to {@code transformed}
   * @param transformed is the transformed result of {@code applied}
   */
  public WaveletDeltaRecord(
      HashedVersion appliedAtVersion,
      @Nullable ByteStringMessage<ProtocolAppliedWaveletDelta> applied,
      TransformedWaveletDelta transformed) {
    Preconditions.checkNotNull(transformed, "null appliedAtVersion");
    Preconditions.checkNotNull(transformed, "null transformed delta");
    this.appliedAtVersion = appliedAtVersion;
    this.applied = applied;
    this.transformed = transformed;
  }

  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta() {
    return applied;
  }

  public TransformedWaveletDelta getTransformedDelta() {
    return transformed;
  }

  // Convenience methods:

  /** @return true if the transformed delta has no operations */
  public boolean isEmpty() {
    return transformed.isEmpty();
  }

  /**
   * @return the hashed version which this delta was applied at
   */
  public HashedVersion getAppliedAtVersion() {
    return appliedAtVersion;
  }

  /** @return the author of the delta */
  public ParticipantId getAuthor() {
    return transformed.getAuthor();
  }

  /** @return the hashed version after the delta is applied */
  public HashedVersion getResultingVersion() {
    return transformed.getResultingVersion();
  }

  /** @return the timestamp when this delta was applied */
  public long getApplicationTimestamp() {
    return transformed.getApplicationTimestamp();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((applied == null) ? 0 : applied.hashCode());
    result = prime * result + ((transformed == null) ? 0 : transformed.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    WaveletDeltaRecord other = (WaveletDeltaRecord) obj;
    if (applied == null) {
      if (other.applied != null) {
        return false;
      }
    } else if (!applied.equals(other.applied)) {
      return false;
    }
    if (transformed == null) {
      if (other.transformed != null) {
        return false;
      }
    } else if (!transformed.equals(other.transformed)) {
      return false;
    }
    return true;
  }
}
