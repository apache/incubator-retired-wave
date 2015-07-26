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
package org.waveprotocol.box.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;
import java.util.RandomAccess;

/**
 * An immutable sequence of transformed deltas.
 *
 * This class enforces that the deltas are contiguous.
 */
public final class DeltaSequence extends ForwardingList<TransformedWaveletDelta>
    implements RandomAccess {
  private final ImmutableList<TransformedWaveletDelta> deltas;

  /**
   * Creates an empty delta sequence. This sequence will not have an end version.
   */
  public static DeltaSequence empty() {
    return new DeltaSequence(ImmutableList.<TransformedWaveletDelta>of(), false);
  }

  /** Creates a delta sequence from contiguous deltas. */
  public static DeltaSequence of(Iterable<TransformedWaveletDelta> deltas) {
    return new DeltaSequence(ImmutableList.copyOf(deltas), true);
  }

  /** Creates a delta sequence from contiguous deltas. */
  public static DeltaSequence of(TransformedWaveletDelta... deltas) {
    return new DeltaSequence(ImmutableList.copyOf(deltas), true);
  }

  /** Creates a delta sequence by concatenating contiguous sequences. */
  public static DeltaSequence join(DeltaSequence first, DeltaSequence... rest) {
    ImmutableList.Builder<TransformedWaveletDelta> builder = ImmutableList.builder();
    builder.addAll(first);
    long expectedBeginVersion = first.getEndVersion().getVersion();
    for (DeltaSequence s : rest) {
      Preconditions.checkArgument(s.getStartVersion() == expectedBeginVersion,
          "Sequences are not contiguous, expected start version %s for sequence %s",
          expectedBeginVersion, s);
      builder.addAll(s);
      expectedBeginVersion = s.getEndVersion().getVersion();
    }
    return new DeltaSequence(builder.build(), false);
  }

  private DeltaSequence(ImmutableList<TransformedWaveletDelta> deltas, boolean checkVersions) {
    this.deltas = deltas;
    if (checkVersions) {
      checkDeltaVersions();
    }
  }

  /**
   * @throws IllegalArgumentException if any of the deltas' end version disagrees
   *         with the next delta's version.
   */
  private void checkDeltaVersions() {
    for (int i = 0; i < deltas.size(); i++) {
      TransformedWaveletDelta delta = deltas.get(i);
      long deltaEndVersion = delta.getResultingVersion().getVersion();
      if (i + 1 < deltas.size()) {
        long nextVersion = deltas.get(i + 1).getAppliedAtVersion();
        Preconditions.checkArgument(deltaEndVersion == nextVersion,
            "Delta %s / %s ends at version %s, next begins at %s",
            i + 1, deltas.size(), deltaEndVersion, nextVersion);
      }
    }
  }

  @Override
  protected List<TransformedWaveletDelta> delegate() {
    return deltas;
  }

  @Override
  public DeltaSequence subList(int start, int end) {
    return new DeltaSequence(deltas.subList(start, end), false);
  }

  /**
   * Gets the version at which the first delta applied.
   *
   * @precondition the sequence is non-empty
   */
  public long getStartVersion() {
    Preconditions.checkState(!deltas.isEmpty(), "Empty delta sequence has no start version");
    return deltas.get(0).getAppliedAtVersion();
  }

  /**
   * Gets the resulting version of this sequence.
   *
   * @precondition the sequence is non-empty
   */
  public HashedVersion getEndVersion() {
    Preconditions.checkState(!deltas.isEmpty(), "Empty delta sequence has no end version");
    return deltas.get(deltas.size() - 1).getResultingVersion();
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "[DeltaSequence empty]";
    }
    return "[DeltaSequence " + deltas.size() + " deltas, v " + getStartVersion() + " -> "
        + getEndVersion() + ": " + deltas + "]";
  }
}
