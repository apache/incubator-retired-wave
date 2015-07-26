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
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * A transformed wavelet delta is the result of a {@link WaveletDelta} being
 * applied at the server.
 *
 * @author anorth@google.com (Alex North)
 */
public final class TransformedWaveletDelta extends AbstractList<WaveletOperation> {
  /** Author of the operations. */
  private final ParticipantId author;

  /** Wavelet version after the delta applied. */
  private final HashedVersion resultingVersion;

  /** Timestamp when the delta was applied. */
  private final long applicationTimestamp;

  /** List of operations in the order they were applied. */
  private final List<WaveletOperation> ops;

  /**
   * Clones ops from a client delta to a transformed delta, replacing their
   * contexts with that implied by the delta's metadata.
   */
  public static TransformedWaveletDelta cloneOperations(HashedVersion resultingVersion,
      long applicationTimestamp, WaveletDelta delta) {
    return cloneOperations(delta.getAuthor(), resultingVersion, applicationTimestamp, delta);
  }

  /**
   * Clones a list of operations into a transformed delta, replacing their
   * contexts with that implied by the delta's metadata.
   */
  public static TransformedWaveletDelta cloneOperations(ParticipantId author,
      HashedVersion resultingVersion, long applicationTimestamp,
      List<? extends WaveletOperation> ops) {
    List<WaveletOperation> transformedOps = CollectionUtils.newArrayList();
    WaveletOperationContext initialContext =
        new WaveletOperationContext(author, applicationTimestamp, 1);
    for (int i = 0; i < ops.size() - 1; ++i) {
      transformedOps.add(WaveletOperation.cloneOp(ops.get(i), initialContext));
    }
    if (ops.size() > 0) {
      WaveletOperationContext finalContext =
        new WaveletOperationContext(author, applicationTimestamp, 1, resultingVersion);
      transformedOps.add(WaveletOperation.cloneOp(ops.get(ops.size() - 1), finalContext));
    }
    return new TransformedWaveletDelta(author, resultingVersion, applicationTimestamp,
        transformedOps);
  }

  /**
   * Create new delta from an author and a sequence of operations.
   *
   * @param author of the operations
   * @param resultingVersion hashed version after the delta applied
   * @param applicationTimestamp timestamp at which the delta applied
   * @param ops operations
   */
  public TransformedWaveletDelta(ParticipantId author, HashedVersion resultingVersion,
      long applicationTimestamp, Iterable<? extends WaveletOperation> ops) {
    this.author = author;
    this.resultingVersion = resultingVersion;
    this.applicationTimestamp = applicationTimestamp;
    this.ops = Collections.unmodifiableList(CollectionUtils.newArrayList(ops));

    // Check that the op contexts are right. (Everything in there is
    // redundant...)
    for (int i = 0; i < this.ops.size(); i++) {
      WaveletOperationContext c = this.ops.get(i).getContext();
      Preconditions.checkArgument(c.getCreator().equals(author),
          "Context creator %s doesn't match delta author %s", c.getCreator(), author);
      Preconditions.checkArgument(c.getVersionIncrement() == 1,
          "Invalid context version increment %s", c.getVersionIncrement());
      Preconditions.checkArgument(c.hasHashedVersion() == (i == this.ops.size() - 1),
          "[Un]expected hashed version on op %s of %s", i + 1, this.ops.size());
      Preconditions.checkArgument((i != this.ops.size() - 1)
          || c.getHashedVersion().equals(resultingVersion),
          "Context hashed version %s doesn't match delta hashed version %s", c.getHashedVersion(),
          resultingVersion);
      Preconditions.checkArgument(c.getTimestamp() == applicationTimestamp,
          "Context timestamp %s doesn't match delta timestamp %s",
          c.getTimestamp(), applicationTimestamp);
    }
  }

  /** Returns the author of the delta. */
  public ParticipantId getAuthor() {
    return author;
  }

  /** Returns the wavelet version to which the delta applied. */
  public long getAppliedAtVersion() {
    return resultingVersion.getVersion() - ops.size();
  }

  /** Returns the wavelet version after the delta applied. */
  public HashedVersion getResultingVersion() {
    return resultingVersion;
  }

  /** Returns the timestamp when the delta was applied. */
  public long getApplicationTimestamp() {
    return applicationTimestamp;
  }

  @Override
  public int size() {
    return ops.size();
  }

  @Override
  public WaveletOperation get(int index) {
    return ops.get(index);
  }


  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + author.hashCode();
    result = 31 * result + resultingVersion.hashCode();
    result = 31 * result + Long.valueOf(applicationTimestamp).hashCode();
    for (WaveletOperation op : ops) {
      result = 31 * result + op.hashCode();
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof TransformedWaveletDelta) {
      TransformedWaveletDelta wd = (TransformedWaveletDelta) obj;
      return author.equals(wd.author)
          && resultingVersion.equals(wd.resultingVersion)
          && (applicationTimestamp == wd.applicationTimestamp)
          && ops.equals(wd.ops);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TransformedWaveletDelta(").append(author).append(", ");
    builder.append(resultingVersion).append(", ");
    builder.append(applicationTimestamp).append(" -> ");
    if (ops.isEmpty()) {
      builder.append("[]");
    } else {
      builder.append(ops.size()).append(" ops: [").append(ops.get(0));
      for (int i = 1; i < ops.size(); i++) {
        builder.append(",").append(ops.get(i));
      }
      builder.append("]");
    }
    builder.append(")");
    return builder.toString();
  }
}
