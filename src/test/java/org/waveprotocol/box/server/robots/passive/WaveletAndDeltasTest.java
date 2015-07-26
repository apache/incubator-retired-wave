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

package org.waveprotocol.box.server.robots.passive;

import junit.framework.TestCase;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Unit test for {@link WaveletAndDeltas}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class WaveletAndDeltasTest extends TestCase {

  private static final WaveletName WAVELET_NAME = WaveletName.of(
      "example.com", "waveid", "example.com", "waveletid");
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");
  private static final ParticipantId CAROL = ParticipantId.ofUnsafe("carol@example.com");

  private static final HashedVersion V1 = HashedVersion.unsigned(1);
  private static final HashedVersion V2 = HashedVersion.unsigned(2);
  private static final HashedVersion V3 = HashedVersion.unsigned(3);

  private WaveletAndDeltas wavelet;
  private ObservableWaveletData waveletData;

  private WaveletOperation addCarolOp;
  private WaveletOperation removeAlexOp;

  @Override
  protected void setUp() throws Exception {
    waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX, HashedVersion.unsigned(0),
        0L);
    waveletData.addParticipant(ALEX);

    AddParticipant addBobOp = new AddParticipant(new WaveletOperationContext(ALEX, 0L, 1, V1), BOB);

    addBobOp.apply(waveletData);
    TransformedWaveletDelta delta =
        new TransformedWaveletDelta(ALEX, V1, 0L, Arrays.asList(addBobOp));

    wavelet = WaveletAndDeltas.create(waveletData, DeltaSequence.of(delta));
    addCarolOp = new AddParticipant(new WaveletOperationContext(ALEX, 0L, 1, V2), CAROL);
    removeAlexOp = new RemoveParticipant(new WaveletOperationContext(ALEX, 0L, 1, V3), ALEX);
  }

  public void testgetSnapshotBeforeDeltas() throws Exception {
    ReadableWaveletData firstSnapshot = wavelet.getSnapshotBeforeDeltas();
    assertFalse("The operation to add bob should have been rolled back",
        firstSnapshot.getParticipants().contains(BOB));
    assertTrue("Operations should have been rolled back, together with the version",
        firstSnapshot.getVersion() == 0);
  }

  public void testgetSnapshotAfterDeltas() throws Exception {
    ReadableWaveletData latestSnapshot = wavelet.getSnapshotAfterDeltas();
    assertNotSame("A copy of the waveletdata must be made", waveletData, latestSnapshot);
    assertTrue("Bob should be a participant", latestSnapshot.getParticipants().contains(BOB));
    assertTrue(latestSnapshot.getVersion() == 1);
  }

  public void testGetVersionAfterDeltas() throws Exception {
    assertEquals(V1, wavelet.getVersionAfterDeltas());
  }

  public void testAppendDeltas() throws Exception {
    addCarolOp.apply(waveletData);
    HashedVersion hashedVersionTwo = HashedVersion.unsigned(2);

    TransformedWaveletDelta delta = new TransformedWaveletDelta(ALEX, hashedVersionTwo, 0L,
      Arrays.asList(addCarolOp));
    wavelet.appendDeltas(waveletData, DeltaSequence.of(delta));

    ReadableWaveletData firstSnapshot = wavelet.getSnapshotBeforeDeltas();
    assertFalse("Bob should not be a participant", firstSnapshot.getParticipants().contains(BOB));
    assertEquals(hashedVersionTwo, wavelet.getVersionAfterDeltas());

    ReadableWaveletData latestSnapshot = wavelet.getSnapshotAfterDeltas();
    assertNotSame("A copy of the waveletdata must be made", waveletData, latestSnapshot);

    Collection<ParticipantId> participants =
        Collections.unmodifiableCollection(Arrays.asList(BOB, CAROL));
    assertTrue("Bob and Carol should be participating",
        latestSnapshot.getParticipants().containsAll(participants));
  }

  public void testContiguousDeltas() throws Exception {
    addCarolOp.apply(waveletData);
    TransformedWaveletDelta deltaAdd = new TransformedWaveletDelta(ALEX, V2, 0L,
        Arrays.asList(addCarolOp));

    removeAlexOp.apply(waveletData);
    TransformedWaveletDelta deltaRemove = new TransformedWaveletDelta(ALEX, V3, 0L,
        Arrays.asList(removeAlexOp));

    DeltaSequence deltas = DeltaSequence.of(deltaAdd, deltaRemove);
    wavelet.appendDeltas(waveletData, deltas);
  }

  public void testNonContiguousDeltas() throws Exception {
    TransformedWaveletDelta deltaAdd = new TransformedWaveletDelta(ALEX, V1, 0L,
        Arrays.asList(new NoOp(new WaveletOperationContext(ALEX, 0L, 1, V1))));
    TransformedWaveletDelta deltaRemove = new TransformedWaveletDelta(ALEX, V2, 0L,
        Arrays.asList(new NoOp(new WaveletOperationContext(ALEX, 0L, 1, V2))));

    DeltaSequence deltas = DeltaSequence.of(deltaAdd, deltaRemove);

    try {
      wavelet.appendDeltas(waveletData, deltas);
      fail("Expected exception because deltas aren't contiguous");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }
}
