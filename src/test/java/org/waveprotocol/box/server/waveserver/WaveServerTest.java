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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

import com.typesafe.config.Config;
import junit.framework.TestCase;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.box.server.waveserver.LocalWaveletContainer.Factory;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.concurrent.Executor;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class WaveServerTest extends TestCase {
  private static final HashedVersionFactory V0_HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(new IdURIEncoderDecoder(new JavaUrlCodec()));

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = WaveId.of(DOMAIN, "abc123");
  private static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "conv+root");
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  private static final ParticipantId USER1 = ParticipantId.ofUnsafe("user1@" + DOMAIN);
  private static final ParticipantId USER2 = ParticipantId.ofUnsafe("user2@" + DOMAIN);

  private static final WaveletOperationContext CONTEXT =
      new WaveletOperationContext(USER1, 1234567890, 1);

  private static WaveletOperation addParticipantToWavelet(ParticipantId user) {
    return new AddParticipant(CONTEXT, user);
  }

  @Mock private SignatureHandler localSigner;
  @Mock private WaveletFederationProvider federationRemote;
  @Mock private WaveletNotificationDispatcher notifiee;
  @Mock private RemoteWaveletContainer.Factory remoteWaveletContainerFactory;
  @Mock private Config config;

  private CertificateManager certificateManager;
  private DeltaAndSnapshotStore waveletStore;
  private WaveMap waveMap;
  private WaveServerImpl waveServer;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(localSigner.getDomain()).thenReturn(DOMAIN);
    when(localSigner.getSignerInfo()).thenReturn(null);
    when(localSigner.sign(Matchers.<ByteStringMessage<ProtocolWaveletDelta>>any()))
        .thenReturn(ImmutableList.<ProtocolSignature>of());

    when(config.getBoolean("federation.waveserver_disable_verification")).thenReturn(true);
    certificateManager = new CertificateManagerImpl(config, localSigner, null, null);
    final DeltaStore deltaStore = new MemoryDeltaStore();
    final Executor waveletLoadExecutor = MoreExecutors.sameThreadExecutor();
    final Executor persistExecutor = MoreExecutors.sameThreadExecutor();
    final Executor storageContinuationExecutor = MoreExecutors.sameThreadExecutor();
    Factory localWaveletContainerFactory = new LocalWaveletContainer.Factory() {
      @Override
      public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
          WaveletName waveletName, String waveDomain) {
        return new LocalWaveletContainerImpl(waveletName, notifiee,
            WaveServerModule.loadWaveletState(waveletLoadExecutor, deltaStore, waveletName, persistExecutor),
            waveDomain, storageContinuationExecutor);
      }
    };

    waveletStore = new DeltaStoreBasedSnapshotStore(deltaStore);
    Executor lookupExecutor = MoreExecutors.sameThreadExecutor();
    waveMap =
        new WaveMap(waveletStore, notifiee, localWaveletContainerFactory,
            remoteWaveletContainerFactory, "example.com", lookupExecutor);
    waveServer =
        new WaveServerImpl(MoreExecutors.sameThreadExecutor(), certificateManager,
            federationRemote, waveMap);
    waveServer.initialize();
  }

  public void testWaveIdsList() throws WaveServerException {
    waveMap.getOrCreateLocalWavelet(WAVELET_NAME);
    ExceptionalIterator<WaveId, WaveServerException> waves = waveServer.getWaveIds();
    assertTrue(waves.hasNext());
    assertEquals(WAVE_ID, waves.next());
  }

  public void testWaveletNotification() {
    submitDeltaToNewWavelet(WAVELET_NAME, USER1, addParticipantToWavelet(USER2));

    verify(notifiee).waveletUpdate(Matchers.<ReadableWaveletData>any(),
        Matchers.<ImmutableList<WaveletDeltaRecord>>any(), eq(ImmutableSet.of(DOMAIN)));
    verify(notifiee).waveletCommitted(eq(WAVELET_NAME), Matchers.<HashedVersion>any(),
        eq(ImmutableSet.of(DOMAIN)));
  }

  private void submitDeltaToNewWavelet(WaveletName name, ParticipantId user,
      WaveletOperation... ops) {
    HashedVersion version = V0_HASH_FACTORY.createVersionZero(name);

    WaveletDelta delta = new WaveletDelta(user, version, ImmutableList.copyOf(ops));

    ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);

    // Submitting the request will require the certificate manager to sign the delta. We'll just
    // leave it unsigned.
    ProtocolSignedDelta signedProtoDelta =
        ProtocolSignedDelta.newBuilder().setDelta(protoDelta.toByteString()).build();

    waveServer.submitRequest(name, protoDelta, new SubmitRequestListener() {
      @Override
      public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
          long applicationTimestamp) {
        // Wavelet was submitted.
      }

      @Override
      public void onFailure(String errorMessage) {
        fail("Could not submit callback");
      }
    });
  }
}
