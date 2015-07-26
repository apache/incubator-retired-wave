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

package org.waveprotocol.box.server.persistence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tests for all DeltaStore implementations.
 *
 * @author Joseph Gentle (josephg@gmail.com)
 */
public abstract class DeltaStoreTestBase extends TestCase {
  private final WaveletName WAVE1_WAVELET1 =
    WaveletName.of(WaveId.of("example.com", "wave1"), WaveletId.of("example.com", "wavelet1"));
  private final WaveletName WAVE2_WAVELET1 =
    WaveletName.of(WaveId.of("example.com", "wave2"), WaveletId.of("example.com", "wavelet1"));
  private final DeltaTestUtil UTIL = new DeltaTestUtil(TestingConstants.PARTICIPANT);


  /** Create and return a new delta store instance of the type being tested. */
  protected abstract DeltaStore newDeltaStore() throws Exception;

  public void testOpenNonexistantWavelet() throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);

    // Sanity check a bunch of values in the wavelet.
    assertTrue(wavelet.isEmpty());
    assertEquals(WAVE1_WAVELET1, wavelet.getWaveletName());
    assertNull(wavelet.getEndVersion());
    assertNull(wavelet.getDelta(0));
    assertNull(wavelet.getDeltaByEndVersion(0));
    assertNull(wavelet.getAppliedAtVersion(0));
    assertNull(wavelet.getResultingVersion(0));
    assertNull(wavelet.getAppliedDelta(0));
    assertNull(wavelet.getTransformedDelta(0));

    wavelet.close();
  }

  public void testWriteToNewWavelet() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;
    WaveletDeltaRecord record = pair.second;

    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);

    assertFalse(wavelet.isEmpty());
    assertEquals(WAVE1_WAVELET1, wavelet.getWaveletName());
    assertEquals(record.getResultingVersion(), wavelet.getEndVersion());
    assertEquals(record, wavelet.getDelta(0));
    assertEquals(record, wavelet.getDeltaByEndVersion(record.getResultingVersion().getVersion()));
    assertEquals(record.getAppliedAtVersion(), wavelet.getAppliedAtVersion(0));
    assertEquals(record.getAppliedDelta(), wavelet.getAppliedDelta(0));
    assertEquals(record.getTransformedDelta(), wavelet.getTransformedDelta(0));

    wavelet.close();
  }

  public void testDeleteWaveletRemovesDeltas() throws Exception {
    Pair<DeltaStore, WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    store.delete(WAVE1_WAVELET1);
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
    assertTrue(wavelet.isEmpty());
    wavelet.close();
  }

  public void testLookupReturnsWavelets() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    assertEquals(ImmutableSet.of(WAVE1_WAVELET1.waveletId), store.lookup(WAVE1_WAVELET1.waveId));
  }

  public void testLookupDoesNotReturnEmptyWavelets() throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
    wavelet.close();

    assertTrue(store.lookup(WAVE1_WAVELET1.waveId).isEmpty());
  }

  public void testLookupDoesNotReturnDeletedWavelets() throws Exception {
    Pair<DeltaStore, WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;
    store.delete(WAVE1_WAVELET1);

    assertTrue(store.lookup(WAVE1_WAVELET1.waveId).isEmpty());
  }

  public void testWaveIdIteratorReturnsWaveIds() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    ImmutableSet<WaveId> waveIds = setFromExceptionalIterator(store.getWaveIdIterator());

    assertEquals(ImmutableSet.of(WAVE1_WAVELET1.waveId), waveIds);
  }

  public void testWaveIdIteratorDoesNotReturnEmptyWavelets() throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
    wavelet.close();

    assertFalse(store.getWaveIdIterator().hasNext());
  }

  public void testWaveIdIteratorDoesNotReturnDeletedWavelets() throws Exception {
    Pair<DeltaStore, WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;
    store.delete(WAVE1_WAVELET1);

    assertFalse(store.getWaveIdIterator().hasNext());
  }

  public void testWaveIdIteratorLimits() throws Exception {
    Pair<DeltaStore,WaveletDeltaRecord> pair = newDeltaStoreWithRecord(WAVE1_WAVELET1);
    DeltaStore store = pair.first;

    DeltasAccess wavelet = store.open(WAVE2_WAVELET1);

    WaveletDeltaRecord record = createRecord();
    wavelet.append(ImmutableList.of(record));
    wavelet.close();

    ExceptionalIterator<WaveId, PersistenceException> iterator = store.getWaveIdIterator();
    assertTrue(iterator.hasNext());

    WaveId waveId1 = iterator.next();
    assertTrue(iterator.hasNext());

    WaveId waveId2 = iterator.next();

    // This is necessary because the order of waveIds is implementation specific.
    if (WAVE1_WAVELET1.waveId.equals(waveId1)) {
      assertEquals(WAVE2_WAVELET1.waveId, waveId2);
    } else {
      assertEquals(WAVE2_WAVELET1.waveId, waveId1);
      assertEquals(WAVE1_WAVELET1.waveId, waveId2);
    }

    assertFalse(iterator.hasNext());
    try {
      waveId1 = iterator.next();
      // Fail the test, it should have thrown an exception.
      fail();
    } catch (NoSuchElementException e) {
      // Test passes.
    }
  }

  // *** Helpers

  protected WaveletDeltaRecord createRecord() {
    HashedVersion targetVersion = HashedVersion.of(0, new byte[] {3, 2, 1});
    HashedVersion resultingVersion = HashedVersion.of(2, new byte[] {1, 2, 3});

    List<WaveletOperation> ops =
        ImmutableList.of(UTIL.noOp(), UTIL.addParticipant(TestingConstants.OTHER_PARTICIPANT));
    TransformedWaveletDelta transformed = TransformedWaveletDelta.cloneOperations(
        TestingConstants.PARTICIPANT, resultingVersion, 1234567890, ops);

    ProtocolWaveletDelta serializedDelta = CoreWaveletOperationSerializer.serialize(transformed);

    ProtocolSignature signature =
        ProtocolSignature.newBuilder().setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
            .setSignatureBytes(ByteString.copyFrom(new byte[] {1, 2, 3})).setSignerId(
                ByteString.copyFromUtf8("somebody")).build();
    ProtocolSignedDelta signedDelta =
        ProtocolSignedDelta.newBuilder().setDelta(
            ByteStringMessage.serializeMessage(serializedDelta).getByteString()).addAllSignature(
            ImmutableList.of(signature)).build();

    ProtocolAppliedWaveletDelta delta =
        ProtocolAppliedWaveletDelta.newBuilder().setApplicationTimestamp(1234567890)
            .setHashedVersionAppliedAt(CoreWaveletOperationSerializer.serialize(targetVersion))
            .setSignedOriginalDelta(signedDelta).setOperationsApplied(2).build();

    return new WaveletDeltaRecord(targetVersion, ByteStringMessage.serializeMessage(delta),
        transformed);
  }

  private Pair<DeltaStore, WaveletDeltaRecord> newDeltaStoreWithRecord(WaveletName waveletName)
      throws Exception {
    DeltaStore store = newDeltaStore();
    DeltasAccess wavelet = store.open(waveletName);

    WaveletDeltaRecord record = createRecord();
    wavelet.append(ImmutableList.of(record));
    wavelet.close();

    return new Pair<DeltaStore, WaveletDeltaRecord>(store, record);
  }

  private static <T, E extends Exception> ImmutableSet<T> setFromExceptionalIterator(
      ExceptionalIterator<T, E> iterator) throws E {
    ImmutableSet.Builder<T> builder = ImmutableSet.builder();
    while(iterator.hasNext()) {
      builder.add(iterator.next());
    }
    return builder.build();
  }
}
