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

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Tests {@link DeltaSequence}.
 */
public class DeltaSequenceTest extends TestCase {
  private static final long START_VERSION = 23;
  private static final ParticipantId USER = new ParticipantId("user@host.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(USER);

  private final TransformedWaveletDelta delta1 =
      UTIL.makeTransformedDelta(0L, HashedVersion.unsigned(START_VERSION + 5), 5);
  private final TransformedWaveletDelta delta2 =
      UTIL.makeTransformedDelta(0L, HashedVersion.unsigned(START_VERSION + 10), 5);
  private final TransformedWaveletDelta delta3 =
      UTIL.makeTransformedDelta(0L, HashedVersion.unsigned(START_VERSION + 15), 5);
  private final List<TransformedWaveletDelta> twoDeltas = ImmutableList.of(delta1, delta2);

  public void testEmptySequence() {
    DeltaSequence empty = DeltaSequence.empty();
    assertEquals(ImmutableList.<ProtocolWaveletDelta>of(), empty);
    assertTrue(empty.isEmpty());
    assertEquals(0, empty.size());
    try {
      empty.getStartVersion();
      fail("Expected illegal state exception");
    } catch (IllegalStateException expected) {
    }
    try {
      empty.getEndVersion();
      fail("Expected illegal state exception");
    } catch (IllegalStateException expected) {
    }
  }

  public void testValidSequenceAllowed() {
    DeltaSequence deltaseq = DeltaSequence.of(twoDeltas);
    assertEquals(2, deltaseq.size());
    assertEquals(START_VERSION, deltaseq.getStartVersion());
    assertEquals(delta2.getResultingVersion(), deltaseq.getEndVersion());
  }

  public void testInvalidSequencesRejected() {
    // Repeated version.
    assertSequenceInvalid(delta1, delta1);
    // Skipped version.
    assertSequenceInvalid(delta1, delta3);
  }

  public void testJoinValidSequencesAllowed() {
    DeltaSequence s1 = DeltaSequence.of(delta1);
    DeltaSequence s2 = DeltaSequence.of(delta2, delta3);
    DeltaSequence joined = DeltaSequence.join(s1, s2);
    assertEquals(delta1.getAppliedAtVersion(), joined.getStartVersion());
    assertEquals(delta3.getResultingVersion(), joined.getEndVersion());
    assertEquals(ImmutableList.of(delta1, delta2, delta3), joined);
  }

  public void testJoinInvalidSequencesRejected() {
    assertJoinInvalid(DeltaSequence.of(delta1), DeltaSequence.of(delta1));
    assertJoinInvalid(DeltaSequence.of(delta1), DeltaSequence.of(delta3));
  }

  /**
   * Tests DeltaSequence.subList() on both empty and nonempty delta sequences.
   */
  public void testSubList() {
    DeltaSequence empty = DeltaSequence.empty();
    assertEquals(empty, empty.subList(0, 0));

    DeltaSequence deltaseq = DeltaSequence.of(twoDeltas);
    assertEquals(twoDeltas, deltaseq.subList(0, twoDeltas.size()));

    assertEquals(empty, deltaseq.subList(0, 0));

    // Construct a sublist with just the first delta.
    DeltaSequence subDeltas = deltaseq.subList(0, 1);
    assertEquals(START_VERSION + 5, subDeltas.getEndVersion().getVersion());
    assertEquals(deltaseq.getStartVersion(), subDeltas.getStartVersion());
    assertEquals(ImmutableList.of(delta1), subDeltas);
  }

  private static void assertSequenceInvalid(TransformedWaveletDelta... deltas) {
    try {
      DeltaSequence.of(deltas);
      fail("Expected delta sequence construction to fail");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void assertJoinInvalid(DeltaSequence first, DeltaSequence... rest) {
    try {
      DeltaSequence.join(first, rest);
      fail("Expected delta sequence join to fail");
    } catch (IllegalArgumentException expected) {
    }
  }
}
