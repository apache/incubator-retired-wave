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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.wave.federation.Proto.ProtocolDocumentOperation;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.MutateDocument;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.concurrent.Executor;


/**
 * Tests for LocalWaveletContainerImpl.
 *
 * @author arb@google.com (Anthony Baxter)
 */
public class LocalWaveletContainerImplTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final Executor PERSIST_EXECUTOR = MoreExecutors.sameThreadExecutor();
  private static final Executor STORAGE_CONTINUATION_EXECUTOR = MoreExecutors.sameThreadExecutor();

  private static final WaveletName WAVELET_NAME = WaveletName.of("a", "a", "b", "b");
  private static final ProtocolSignature SIGNATURE = ProtocolSignature.newBuilder()
      .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
      .setSignatureBytes(ByteString.EMPTY)
      .setSignerId(ByteString.EMPTY)
      .build();
  private static final String AUTHOR = "kermit@muppetshow.com";

  private static final HashedVersion HASHED_VERSION_ZERO =
      HASH_FACTORY.createVersionZero(WAVELET_NAME);
  private ProtocolWaveletOperation addParticipantOp;
  private static final String BLIP_ID = "b+muppet";
  private ProtocolWaveletOperation addBlipOp;
  private LocalWaveletContainerImpl wavelet;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addParticipantOp = ProtocolWaveletOperation.newBuilder()
        .setAddParticipant(AUTHOR)
        .build();
    // An empty blip operation - creates a new document.
    addBlipOp = ProtocolWaveletOperation.newBuilder().setMutateDocument(
        MutateDocument.newBuilder().setDocumentId(BLIP_ID).setDocumentOperation(
            ProtocolDocumentOperation.newBuilder().build())).build();

    WaveletNotificationSubscriber notifiee = mock(WaveletNotificationSubscriber.class);
    DeltaStore deltaStore = new MemoryDeltaStore();
    WaveletState waveletState = DeltaStoreBasedWaveletState.create(deltaStore.open(WAVELET_NAME),
        PERSIST_EXECUTOR);
    wavelet = new LocalWaveletContainerImpl(WAVELET_NAME, notifiee,
        Futures.immediateFuture(waveletState), null, STORAGE_CONTINUATION_EXECUTOR);
    wavelet.awaitLoad();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Tests that duplicate operations are a no-op.
   *
   * @throws Exception should not be thrown.
   */
  public void testDuplicateOperations() throws Exception {
    assertEquals(0L, wavelet.getCurrentVersion().getVersion());

    // create the wavelet.
    WaveletDeltaRecord v0Response = wavelet.submitRequest(
        WAVELET_NAME, createProtocolSignedDelta(addParticipantOp, HASHED_VERSION_ZERO));
    assertEquals(1L, wavelet.getCurrentVersion().getVersion());

    ProtocolSignedDelta psd = createProtocolSignedDelta(
        addBlipOp, v0Response.getResultingVersion());

    WaveletDeltaRecord dar1 = wavelet.submitRequest(WAVELET_NAME, psd);
    assertEquals(2L, wavelet.getCurrentVersion().getVersion());

    WaveletDeltaRecord dar2 = wavelet.submitRequest(WAVELET_NAME, psd);
    assertEquals(2L, wavelet.getCurrentVersion().getVersion());

    assertEquals(dar1.getResultingVersion(), dar2.getResultingVersion());
  }

  private ProtocolSignedDelta createProtocolSignedDelta(ProtocolWaveletOperation operation,
      HashedVersion protocolHashedVersion) {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setAuthor(AUTHOR)
        .setHashedVersion(CoreWaveletOperationSerializer.serialize(protocolHashedVersion))
        .addOperation(operation)
        .build();

    return ProtocolSignedDelta.newBuilder()
        .setDelta(delta.toByteString())
        .addSignature(SIGNATURE)
        .build();
  }
}
