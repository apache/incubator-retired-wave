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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * A wavelet delta is a collection of {@link WaveletOperation}s from a single
 * author, targeting a particular version of the wavelet.
 *
 * A delta is immutable (List mutation operations throw
 * {@link UnsupportedOperationException}).
 *
 * @author anorth@google.com (Alex North)
 */
public final class WaveletDelta extends AbstractList<WaveletOperation> {
  /** Author of the operations. */
  private final ParticipantId author;

  /** Wavelet version to which the delta applies. */
  private final HashedVersion targetVersion;

  /** List of operations in the order they are to be applied. */
  private final List<WaveletOperation> ops;

  /**
   * Create new delta from an author and a sequence of operations.
   *
   * @param author of the operations
   * @param targetVersion version to which the delta applies
   * @param ops operations
   */
  public WaveletDelta(ParticipantId author, HashedVersion targetVersion,
      Iterable<? extends WaveletOperation> ops) {
    this.author = author;
    this.targetVersion = targetVersion;
    this.ops = Collections.unmodifiableList(CollectionUtils.newArrayList(ops));
  }

  /** Returns the author of the delta. */
  public ParticipantId getAuthor() {
    return author;
  }

  /** Returns the wavelet version to which the delta applies. */
  public HashedVersion getTargetVersion() {
    return targetVersion;
  }

  /** Returns the expected wavelet version after application of this delta. */
  public long getResultingVersion() {
    return targetVersion.getVersion() + ops.size();
  }

  @Override
  public int size() {
    return ops.size();
  }

  @Override
  public WaveletOperation get(int i) {
    return ops.get(i);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + author.hashCode();
    result = 31 * result + targetVersion.hashCode();
    for (WaveletOperation op : ops) {
      result = 31 * result + op.hashCode();
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof WaveletDelta) {
      WaveletDelta wd = (WaveletDelta) obj;
      return author.equals(wd.author) && targetVersion.equals(wd.targetVersion)
          && ops.equals(wd.ops);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CoreWaveletDelta(").append(author).append(", ");
    builder.append(targetVersion).append(", ");
    if (ops.isEmpty()) {
      builder.append("[]");
    } else {
      builder.append(" ").append(ops.size()).append(" ops: [").append(ops.get(0));
      for (int i = 1; i < ops.size(); i++) {
        builder.append(",").append(ops.get(i));
      }
      builder.append("]");
    }
    builder.append(")");
    return builder.toString();
  }
}
