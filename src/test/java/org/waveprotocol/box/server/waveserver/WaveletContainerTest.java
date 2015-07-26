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

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Tests for local and remote wavelet containers.
 *
 *
 */
public class WaveletContainerTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final Executor PERSIST_EXECUTOR = MoreExecutors.sameThreadExecutor();
  private static final Executor STORAGE_CONTINUATION_EXECUTOR = MoreExecutors.sameThreadExecutor();

  private static final String localDomain = "example.com";
  private static final WaveletName localWaveletName = WaveletName.of(
      WaveId.of(localDomain, "waveid"), WaveletId.of(localDomain, "waveletid"));
  private static final String remoteDomain = "example2.com";
  private static final WaveletName remoteWaveletName = WaveletName.of(
      WaveId.of(remoteDomain, "waveid"), WaveletId.of(remoteDomain, "waveletid"));
  private static final ParticipantId author = new ParticipantId("admin@" + localDomain);
  private static final Set<ParticipantId> participants = ImmutableSet.of(
      new ParticipantId("foo@" + localDomain), new ParticipantId("bar@example3.com"));
  private static final HashedVersion localVersion0 =
      HASH_FACTORY.createVersionZero(localWaveletName);
  private static final ByteString fakeSigner1 = ByteString.EMPTY;
  private static final ByteString fakeSigner2 = ByteString.copyFrom(new byte[] {1});
  private static final ProtocolSignature fakeSignature1 = ProtocolSignature.newBuilder()
      .setSignatureBytes(ByteString.EMPTY)
      .setSignerId(fakeSigner1)
      .setSignatureAlgorithm(ProtocolSignature.SignatureAlgorithm.SHA1_RSA)
      .build();
  private static final ProtocolSignature fakeSignature2 = ProtocolSignature.newBuilder()
      .setSignatureBytes(ByteString.copyFrom(new byte[] {1}))
      .setSignerId(fakeSigner2)
      .setSignatureAlgorithm(ProtocolSignature.SignatureAlgorithm.SHA1_RSA)
      .build();
  private static final WaveletOperationContext CONTEXT = new WaveletOperationContext(author,
      0, 1);

  private static final List<WaveletOperation> addParticipantOps = Lists.newArrayList();
  private static final List<WaveletOperation> removeParticipantOps = Lists.newArrayList();
  private static final List<WaveletOperation> doubleRemoveParticipantOps;

  static {
    for (ParticipantId p : participants) {
      addParticipantOps.add(new AddParticipant(CONTEXT, p));
      removeParticipantOps.add(new RemoveParticipant(CONTEXT, p));
    }

    Collections.reverse(removeParticipantOps);
    doubleRemoveParticipantOps = Lists.newArrayList(removeParticipantOps);
    doubleRemoveParticipantOps.addAll(removeParticipantOps);
  }

  private LocalWaveletContainerImpl localWavelet;
  private RemoteWaveletContainerImpl remoteWavelet;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    WaveletNotificationSubscriber notifiee = mock(WaveletNotificationSubscriber.class);
    DeltaStore deltaStore = new MemoryDeltaStore();
    WaveletState localWaveletState =
        DeltaStoreBasedWaveletState.create(deltaStore.open(localWaveletName), PERSIST_EXECUTOR);
    localWavelet = new LocalWaveletContainerImpl(localWaveletName, notifiee,
        Futures.immediateFuture(localWaveletState), localDomain, STORAGE_CONTINUATION_EXECUTOR);
    localWavelet.awaitLoad();
    WaveletState remoteWaveletState =
        DeltaStoreBasedWaveletState.create(deltaStore.open(remoteWaveletName), PERSIST_EXECUTOR);
    remoteWavelet = new RemoteWaveletContainerImpl(remoteWaveletName, notifiee,
        Futures.immediateFuture(remoteWaveletState), STORAGE_CONTINUATION_EXECUTOR);
    remoteWavelet.awaitLoad();
  }

  // Tests

  public void testLocalApplyWaveletOperation() throws Exception {
    assertSuccessfulApplyWaveletOperations(localWavelet);
  }

  public void testRemoteApplyWaveletOperation() throws Exception {
    assertSuccessfulApplyWaveletOperations(remoteWavelet);
  }

  public void testLocalFailedWaveletOperations() throws Exception {
    assertFailedWaveletOperations(localWavelet);
  }

  public void testRemoteFailedWaveletOperations() throws Exception {
    assertFailedWaveletOperations(localWavelet);
  }

  public void testSuccessfulLocalRequest() throws Exception {
    ProtocolSignedDelta addDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature1)
        .setDelta(addParticipantProtoDelta(localWavelet).toByteString())
        .build();
    localWavelet.submitRequest(localWaveletName, addDelta);
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 2);
    assertTrue(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner1));
    assertFalse(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner2));

    HashedVersion oldVersion = localWavelet.getCurrentVersion();
    ProtocolSignedDelta removeDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature2)
        .setDelta(ProtocolWaveletDelta.newBuilder(removeParticipantProtoDelta(localWavelet))
            .setHashedVersion(serialize(localWavelet.getCurrentVersion())).build().toByteString())
        .build();
    localWavelet.submitRequest(localWaveletName, removeDelta);
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 4);
    assertTrue(localWavelet.isDeltaSigner(oldVersion, fakeSigner1));
    assertFalse(localWavelet.isDeltaSigner(oldVersion, fakeSigner2));
    assertTrue(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner2));
    assertFalse(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner1));
  }

  public void testFailedLocalWaveletRequest() throws Exception {
    ProtocolSignedDelta removeDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature1)
        .setDelta(removeParticipantProtoDelta(localWavelet).toByteString())
        .build();
    try {
      localWavelet.submitRequest(localWaveletName, removeDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion(), localVersion0);

    ProtocolSignedDelta addDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature1)
        .setDelta(addParticipantProtoDelta(localWavelet).toByteString())
        .build();

    localWavelet.submitRequest(localWaveletName, addDelta);
    try {
      ProtocolSignedDelta addAgainDelta = ProtocolSignedDelta.newBuilder()
          .addSignature(fakeSignature2)
          .setDelta(ProtocolWaveletDelta.newBuilder(addParticipantProtoDelta(localWavelet))
              .setHashedVersion(serialize(localWavelet.getCurrentVersion()))
              .build().toByteString())
          .build();
      localWavelet.submitRequest(localWaveletName, addAgainDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion().getVersion(), 2);
    assertTrue(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner1));
    assertFalse(localWavelet.isDeltaSigner(
        localWavelet.getCurrentVersion(), fakeSigner2));

    HashedVersion oldVersion = localWavelet.getCurrentVersion();
    ProtocolSignedDelta rollbackDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature1)
        .setDelta(ProtocolWaveletDelta.newBuilder(doubleRemoveParticipantProtoDelta(localWavelet))
            .setHashedVersion(serialize(localWavelet.getCurrentVersion()))
            .build().toByteString())
        .build();
    try {
      localWavelet.submitRequest(localWaveletName, rollbackDelta);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(localWavelet.getCurrentVersion(), oldVersion);
  }

  public void testLocalEmptyDelta() throws Exception {
    ProtocolSignedDelta emptyDelta = ProtocolSignedDelta.newBuilder()
        .addSignature(fakeSignature1)
        .setDelta(ProtocolWaveletDelta.newBuilder()
            .setAuthor(author.toString())
            .setHashedVersion(serialize(localVersion0))
            .build().toByteString())
        .build();
    try {
      localWavelet.submitRequest(localWaveletName, emptyDelta);
      fail("Should fail");
    } catch (IllegalArgumentException e) {
      // Correct
    }
  }

  public void testOperationsOfDifferentSizes() throws Exception {
    String docId = "b+somedoc";
    DocOp docOp1 = new DocOpBuilder().characters("hi").build();
    WaveletDelta delta1 = createDelta(docId, docOp1, localVersion0);

    WaveServerTestUtil.applyDeltaToWavelet(localWavelet, delta1, 0L);
    try {
      DocOp docOp2 = new DocOpBuilder().characters("bye").build();
      WaveletDelta delta2 = createDelta(docId, docOp2, localWavelet.getCurrentVersion());

      WaveServerTestUtil.applyDeltaToWavelet(localWavelet, delta2, 0L);
      fail("Composition of \"hi\" and \"bye\" did not throw OperationException");
    } catch (OperationException expected) {
      // Correct
    }
  }

  // Utilities

  /**
   * Returns a {@link WaveletDelta} for the list of operations performed by
   * the author set in the constants.
   */
  private WaveletDelta createDelta(String docId, DocOp docOp, HashedVersion version) {
    return new WaveletDelta(author, version, Arrays.asList(new WaveletBlipOperation(
        docId, new BlipContentOperation(CONTEXT, docOp))));
  }

  /**
   * Check that a container succeeds when adding non-existent participants and removing existing
   * participants.
   */
  private void assertSuccessfulApplyWaveletOperations(WaveletContainerImpl with) throws Exception {
    WaveServerTestUtil.applyDeltaToWavelet(with, addParticipantDelta(with), 0L);
    assertEquals(with.accessSnapshot().getParticipants(), participants);

    WaveServerTestUtil.applyDeltaToWavelet(with, removeParticipantDelta(with), 0L);
    assertEquals(with.accessSnapshot().getParticipants(), Collections.emptySet());
  }

  /**
   * Check that a container fails when removing non-existent participants and adding duplicate
   * participants, and that the partipant list is preserved correctly.
   */
  private void assertFailedWaveletOperations(WaveletContainerImpl with) throws Exception {
    try {
      WaveServerTestUtil.applyDeltaToWavelet(with, removeParticipantDelta(with), 0L);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertNull(localWavelet.accessSnapshot());

    WaveServerTestUtil.applyDeltaToWavelet(with, addParticipantDelta(with), 0L);
    try {
      WaveServerTestUtil.applyDeltaToWavelet(with, addParticipantDelta(with), 0L);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(with.accessSnapshot().getParticipants(), participants);

    try {
      WaveServerTestUtil.applyDeltaToWavelet(with, doubleRemoveParticipantDelta(with), 0L);
      fail("Should fail");
    } catch (OperationException e) {
      // Correct
    }
    assertEquals(with.accessSnapshot().getParticipants(), participants);
  }

  private static WaveletDelta addParticipantDelta(WaveletContainerImpl target) {
    return new WaveletDelta(author, target.getCurrentVersion(), addParticipantOps);
  }

  private static ProtocolWaveletDelta addParticipantProtoDelta(WaveletContainerImpl target) {
    return serialize(addParticipantDelta(target));
  }

  private static WaveletDelta removeParticipantDelta(WaveletContainerImpl target) {
    return new WaveletDelta(author, target.getCurrentVersion(), removeParticipantOps);
  }

  private static ProtocolWaveletDelta removeParticipantProtoDelta(WaveletContainerImpl target) {
    return serialize(removeParticipantDelta(target));
  }

  private static WaveletDelta doubleRemoveParticipantDelta(WaveletContainerImpl target) {
    return new WaveletDelta(author, target.getCurrentVersion(),
        doubleRemoveParticipantOps);
  }

  private static ProtocolWaveletDelta doubleRemoveParticipantProtoDelta(
      WaveletContainerImpl target) {
    return serialize(doubleRemoveParticipantDelta(target));
  }

  private static ProtocolHashedVersion serialize(HashedVersion v) {
    return CoreWaveletOperationSerializer.serialize(v);
  }

  private static ProtocolWaveletDelta serialize(WaveletDelta d) {
    return CoreWaveletOperationSerializer.serialize(d);
  }
}
