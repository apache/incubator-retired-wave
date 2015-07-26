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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.federation.Proto;
import org.waveprotocol.box.common.ListReceiver;
import org.waveprotocol.box.common.Receiver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link WaveletState} implementations.
 *
 * @author anorth@google.com (Alex North)
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public abstract class WaveletStateTestBase extends TestCase {

  private static final WaveletName NAME = WaveletName.of(WaveId.of("example.com", "waveid"),
      WaveletId.of("example.com", "waveletid"));
  private static final ParticipantId AUTHOR = ParticipantId.ofUnsafe("author@example.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(AUTHOR);
  private static final long TS = 1234567890L;
  private static final long TS2 = TS + 1;
  private static final long TS3 = TS2 + 1;

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final HashedVersion V0 = HASH_FACTORY.createVersionZero(NAME);


  /**
   * Creates a new, empty wavelet state.
   */
  protected abstract WaveletState createEmptyState(WaveletName name) throws Exception;

  /**
   * Waits for all pending persistence operations to be completed. All
   * persistence listener callbacks must be completed before this method
   * returns.
   */
  protected abstract void awaitPersistence() throws Exception;

  private WaveletDeltaRecord d1;
  private WaveletDeltaRecord d2;
  private WaveletDeltaRecord d3;

  private WaveletState target;

  @Override
  public void setUp() throws Exception {
    d1 = makeDelta(V0, TS, 2);
    d2 = makeDelta(d1.getResultingVersion(), TS2, 2);
    d3 = makeDelta(d2.getResultingVersion(), TS3, 1);

    target = createEmptyState(NAME);
  }

  public void testReportsWaveletName() {
    assertEquals(NAME, target.getWaveletName());
  }

  public void testEmptyStateIsEmpty() {
    assertNull(target.getSnapshot());
    assertEquals(V0, target.getCurrentVersion());
    assertEquals(V0, target.getHashedVersion(0));

    assertNull(target.getTransformedDelta(V0));
    assertNull(target.getAppliedDelta(V0));
  }

  public void testSnapshotMetadataReflectsDeltas() throws Exception {
    HashedVersion v2 = d1.getResultingVersion();
    appendDeltas(d1);

    assertEquals(v2, target.getCurrentVersion());
    ReadableWaveletData snapshot = target.getSnapshot();
    assertEquals(AUTHOR, snapshot.getCreator());
    assertEquals(v2, snapshot.getHashedVersion());
    assertEquals(TS, snapshot.getCreationTime());
    assertEquals(TS, snapshot.getLastModifiedTime());
    assertEquals(2, snapshot.getVersion());

    HashedVersion v4 = d2.getResultingVersion();
    appendDeltas(d2);

    assertEquals(v4, target.getCurrentVersion());
    snapshot = target.getSnapshot();
    assertEquals(v4, snapshot.getHashedVersion());
    assertEquals(4, snapshot.getVersion());
    // Last-modified-time doesn't change due to unworthiness.
  }

  public void testHashedVersionAccessibleOnDeltaBoundaries() throws Exception {
    appendDeltas(d1, d2, d3);
    assertEquals(V0, target.getHashedVersion(0));
    assertEquals(d1.getResultingVersion(), target.getHashedVersion(2));
    assertEquals(d2.getResultingVersion(), target.getHashedVersion(4));
    assertEquals(d3.getResultingVersion(), target.getHashedVersion(5));
    assertNull(target.getHashedVersion(1));
    assertNull(target.getHashedVersion(3));
    assertNull(target.getHashedVersion(6));
  }

  public void testDeltasAccessibleByBeginVersion() throws Exception {
    appendDeltas(d1, d2, d3);
    assertEquals(d1.getTransformedDelta(), target.getTransformedDelta(V0));
    assertEquals(d1.getAppliedDelta(), target.getAppliedDelta(V0));

    assertEquals(d2.getTransformedDelta(), target.getTransformedDelta(d1.getResultingVersion()));
    assertEquals(d2.getAppliedDelta(), target.getAppliedDelta(d1.getResultingVersion()));

    assertEquals(d3.getTransformedDelta(), target.getTransformedDelta(d2.getResultingVersion()));
    assertEquals(d3.getAppliedDelta(), target.getAppliedDelta(d2.getResultingVersion()));

    // Wrong hashes return null.
    assertNull(target.getTransformedDelta(HashedVersion.unsigned(0)));
    assertNull(target.getAppliedDelta(HashedVersion.unsigned(0)));
  }

  public void testDeltasAccesssibleByEndVersion() throws Exception {
    appendDeltas(d1, d2, d3);
    for (WaveletDeltaRecord d : Arrays.asList(d1, d2, d3)) {
      assertEquals(d.getTransformedDelta(),
          target.getTransformedDeltaByEndVersion(d.getResultingVersion()));
      assertEquals(d.getAppliedDelta(),
          target.getAppliedDeltaByEndVersion(d.getResultingVersion()));
    }

    // Wrong hashes return null.
    assertNull(target.getTransformedDeltaByEndVersion(
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
    assertNull(target.getAppliedDeltaByEndVersion(
        HashedVersion.unsigned(d1.getResultingVersion().getVersion())));
  }

  public void testDeltaHistoryRequiresCorrectHash() throws Exception {
    appendDeltas(d1);
    target.persist(d1.getResultingVersion());
    Receiver<TransformedWaveletDelta> transformedDeltasReceiver =
        new ListReceiver<TransformedWaveletDelta>();
    Receiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>> appliedDeltasReceiver =
        new ListReceiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>>();
    // Wrong start hash.
    checkGetTransformedDeltasThrowsException(HashedVersion.unsigned(0), d1.getResultingVersion(), transformedDeltasReceiver,
        IllegalArgumentException.class);
    checkGetAppliedDeltasThrowsException(HashedVersion.unsigned(0), d1.getResultingVersion(), appliedDeltasReceiver,
        IllegalArgumentException.class);

    // Wrong end hash.
    checkGetTransformedDeltasThrowsException(V0, HashedVersion.unsigned(d1.getResultingVersion().getVersion()),
        transformedDeltasReceiver, IllegalArgumentException.class);
    checkGetAppliedDeltasThrowsException(V0, HashedVersion.unsigned(d1.getResultingVersion().getVersion()),
        appliedDeltasReceiver, IllegalArgumentException.class);
  }

  @SuppressWarnings("rawtypes")
  private void checkGetTransformedDeltasThrowsException(HashedVersion startVersion, HashedVersion endVersion,
      Receiver<TransformedWaveletDelta> receiver, Class exceptionClass) {
    try {
      target.getTransformedDeltaHistory(startVersion, endVersion, receiver);
      fail("Expected exception not thrown.");
    } catch (Exception ex) {
      assertEquals(IllegalArgumentException.class, exceptionClass);
    }
  }

  @SuppressWarnings("rawtypes")
  private void checkGetAppliedDeltasThrowsException(HashedVersion startVersion, HashedVersion endVersion,
      Receiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>> receiver, Class exceptionClass) {
    try {
      target.getAppliedDeltaHistory(startVersion, endVersion, receiver);
      fail("Expected exception not thrown.");
    } catch (Exception ex) {
      assertEquals(IllegalArgumentException.class, exceptionClass);
    }
  }

  public void testSingleDeltaHistoryAccessible() throws Exception {
    appendDeltas(d1);
    target.persist(d1.getResultingVersion());
    ListReceiver<TransformedWaveletDelta> transformedDeltasReceiver =
        new ListReceiver<TransformedWaveletDelta>();
    target.getTransformedDeltaHistory(V0, d1.getResultingVersion(), transformedDeltasReceiver);
    assertEquals(1, transformedDeltasReceiver.size());
    assertEquals(d1.getTransformedDelta(), transformedDeltasReceiver.get(0));

    ListReceiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>> appliedDeltasReceiver =
        new ListReceiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>>();
    target.getAppliedDeltaHistory(V0, d1.getResultingVersion(), appliedDeltasReceiver);
    assertEquals(1, appliedDeltasReceiver.size());
    assertEquals(d1.getAppliedDelta(), Iterables.getOnlyElement(appliedDeltasReceiver));
  }

  public void testDeltaHistoryQueriesCorrectHistory() throws Exception {
    appendDeltas(d1, d2, d3);
    target.persist(d3.getResultingVersion());

    checkHistoryForDeltas(d1);
    checkHistoryForDeltas(d1, d2);
    checkHistoryForDeltas(d2, d3);
    checkHistoryForDeltas(d1, d2, d3);
  }

  public void testDeltaHistoryInterruptQueriesCorrectHistory() throws Exception {
    appendDeltas(d1, d2, d3);
    target.persist(d3.getResultingVersion());

    checkHistoryForDeltasWithInterrupt(0, d1);
    checkHistoryForDeltasWithInterrupt(1, d1, d2);
    checkHistoryForDeltasWithInterrupt(2, d1, d2, d3);
  }

  /**
   * Checks that a request for the deltas spanning a contiguous sequence of
   * delta facets produces correct results.
   */
  private void checkHistoryForDeltas(WaveletDeltaRecord... deltas) {
    HashedVersion beginVersion = deltas[0].getAppliedAtVersion();
    HashedVersion endVersion = deltas[deltas.length - 1].getTransformedDelta().getResultingVersion();

    {
      List<TransformedWaveletDelta> expected = Lists.newArrayListWithExpectedSize(deltas.length);
      for (WaveletDeltaRecord d : deltas) {
        expected.add(d.getTransformedDelta());
      }
      ListReceiver<TransformedWaveletDelta> transformedDeltasReceiver =
        new ListReceiver<TransformedWaveletDelta>();
      target.getTransformedDeltaHistory(beginVersion, endVersion, transformedDeltasReceiver);
      assertTrue(Iterables.elementsEqual(expected, transformedDeltasReceiver));
    }
    {
      List<ByteStringMessage<ProtocolAppliedWaveletDelta>> expected =
          Lists.newArrayListWithExpectedSize(deltas.length);
      for (WaveletDeltaRecord d : deltas) {
        expected.add(d.getAppliedDelta());
      }
    ListReceiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>> appliedDeltasReceiver =
        new ListReceiver<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>>();
    target.getAppliedDeltaHistory(beginVersion, endVersion, appliedDeltasReceiver);
      assertTrue(Iterables.elementsEqual(expected, appliedDeltasReceiver));
    }
  }

private void checkHistoryForDeltasWithInterrupt(final int interruptIndex, WaveletDeltaRecord... deltas) {
    HashedVersion beginVersion = deltas[0].getAppliedAtVersion();
    HashedVersion endVersion = deltas[interruptIndex].getTransformedDelta().getResultingVersion();

    {
      List<TransformedWaveletDelta> expected = Lists.newArrayListWithExpectedSize(interruptIndex+1);
      for (int i=0; i <= interruptIndex; i++) {
        expected.add(deltas[i].getTransformedDelta());
      }
      final AtomicInteger index = new AtomicInteger(0);
      final List<TransformedWaveletDelta> transformedDeltas = new ArrayList<TransformedWaveletDelta>();
      target.getTransformedDeltaHistory(beginVersion, endVersion, new Receiver<TransformedWaveletDelta>() {

        @Override
        public boolean put(TransformedWaveletDelta delta) {
          transformedDeltas.add(delta);
          return index.getAndIncrement() < interruptIndex;
        }
      });
      assertTrue(Iterables.elementsEqual(expected, transformedDeltas));
    }
    {
      List<ByteStringMessage<ProtocolAppliedWaveletDelta>> expected =
          Lists.newArrayListWithExpectedSize(interruptIndex+1);
      for (int i=0; i <= interruptIndex; i++) {
        expected.add(deltas[i].getAppliedDelta());
      }
      final AtomicInteger index = new AtomicInteger(0);
      final List<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>> appliedDeltas =
          new ArrayList<ByteStringMessage<Proto.ProtocolAppliedWaveletDelta>>();
      target.getAppliedDeltaHistory(beginVersion, endVersion,
          new Receiver<ByteStringMessage<ProtocolAppliedWaveletDelta>>() {

        @Override
        public boolean put(ByteStringMessage<ProtocolAppliedWaveletDelta> delta) {
          appliedDeltas.add(delta);
          return index.getAndIncrement() < interruptIndex;
        }
      });
      assertTrue(Iterables.elementsEqual(expected, appliedDeltas));
    }
  }

  public void checkSingleDeltaPersistFutureDone() throws Exception {
    appendDeltas(d1);
    Future<Void> future = target.persist(d1.getResultingVersion());
    awaitPersistence();
    assertTrue(future.isDone());
    assertEquals(null, future.get());
    assertEquals(d1.getResultingVersion(), target.getLastPersistedVersion());
  }

  public void checkManyDeltasPersistFutureDone() throws Exception {
    appendDeltas(d1, d2, d3);
    Future<Void> future = target.persist(d3.getResultingVersion());
    awaitPersistence();
    assertTrue(future.isDone());
    assertEquals(null, future.get());
    assertEquals(d3.getResultingVersion(), target.getLastPersistedVersion());
  }

  public void testCanPersistOnlySomeDeltas() throws Exception {
    appendDeltas(d1, d2, d3);
    Future<Void> future = target.persist(d2.getResultingVersion());
    awaitPersistence();
    assertTrue(future.isDone());
    assertEquals(null, future.get());
    assertEquals(d2.getResultingVersion(), target.getLastPersistedVersion());

    future = target.persist(d3.getResultingVersion());
    awaitPersistence();
    assertTrue(future.isDone());
    assertEquals(null, future.get());
    assertEquals(d3.getResultingVersion(), target.getLastPersistedVersion());
  }

  /**
   * Applies a delta to the target.
   */
  private void appendDeltas(WaveletDeltaRecord... deltas) throws InvalidProtocolBufferException,
      OperationException {
    for (WaveletDeltaRecord delta : deltas) {
      target.appendDelta(delta);
    }
  }


  /**
   * Creates a delta of no-ops and builds the corresponding applied and
   * transformed delta objects.
   */
  private static WaveletDeltaRecord makeDelta(HashedVersion appliedAtVersion, long timestamp,
      int numOps) throws InvalidProtocolBufferException {
    // Use no-op delta so the ops can actually apply.
    WaveletDelta delta = UTIL.makeNoOpDelta(appliedAtVersion, timestamp, numOps);
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        WaveServerTestUtil.buildAppliedDelta(delta, timestamp);
    TransformedWaveletDelta transformedDelta =
        AppliedDeltaUtil.buildTransformedDelta(appliedDelta, delta);
    return new WaveletDeltaRecord(appliedAtVersion, appliedDelta, transformedDelta);
  }
}
