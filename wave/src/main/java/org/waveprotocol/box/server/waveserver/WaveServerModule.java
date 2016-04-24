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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.StorageContinuationExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.WaveletLoadExecutor;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.crypto.*;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Guice Module for the prototype Server.
 *
 */
public class WaveServerModule extends AbstractModule {
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);

  private final Executor waveletLoadExecutor;
  private final Executor storageContinuationExecutor;
  private final boolean enableFederation;


  @Inject
  WaveServerModule(Config config,
      @WaveletLoadExecutor Executor waveletLoadExecutor,
      @StorageContinuationExecutor Executor storageContinuationExecutor) {
    this.enableFederation = config.getBoolean("federation.enable_federation");
    this.waveletLoadExecutor = waveletLoadExecutor;
    this.storageContinuationExecutor = storageContinuationExecutor;
  }

  @Override
  protected void configure() {
    bind(TimeSource.class).to(DefaultTimeSource.class).in(Singleton.class);

    if (enableFederation) {
      bind(SignatureHandler.class)
      .toProvider(SigningSignatureHandler.SigningSignatureHandlerProvider.class);
    } else {
      bind(SignatureHandler.class)
      .toProvider(NonSigningSignatureHandler.NonSigningSignatureHandlerProvider.class);
    }

    try {
      bind(WaveSignatureVerifier.class).toConstructor(WaveSignatureVerifier.class.getConstructor(
          WaveCertPathValidator.class, CertPathStore.class));
      bind(VerifiedCertChainCache.class).to(DefaultCacheImpl.class).in(Singleton.class);
      bind(DefaultCacheImpl.class).toConstructor(
          DefaultCacheImpl.class.getConstructor(TimeSource.class));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }

    bind(WaveletNotificationDispatcher.class).in(Singleton.class);
    bind(WaveBus.class).to(WaveletNotificationDispatcher.class);
    bind(WaveletNotificationSubscriber.class).to(WaveletNotificationDispatcher.class);
    bind(TrustRootsProvider.class).to(DefaultTrustRootsProvider.class).in(Singleton.class);
    bind(CertificateManager.class).to(CertificateManagerImpl.class).in(Singleton.class);
    bind(DeltaAndSnapshotStore.class).to(DeltaStoreBasedSnapshotStore.class).in(Singleton.class);
    bind(WaveMap.class).in(Singleton.class);
    bind(WaveletProvider.class).to(WaveServerImpl.class).asEagerSingleton();
    bind(ReadableWaveletDataProvider.class).to(WaveServerImpl.class).in(Singleton.class);
    bind(HashedVersionFactory.class).toInstance(HASH_FACTORY);
  }

  @Provides
  @SuppressWarnings("unused")
  private LocalWaveletContainer.Factory provideLocalWaveletContainerFactory(
      final DeltaStore deltaStore) {
    return new LocalWaveletContainer.Factory() {
      @Override
      public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
          WaveletName waveletName, String waveDomain) {
        return new LocalWaveletContainerImpl(waveletName, notifiee, loadWaveletState(
            waveletLoadExecutor, deltaStore, waveletName, waveletLoadExecutor), waveDomain,
            storageContinuationExecutor);
      }
    };
  }

  @Provides
  @SuppressWarnings("unused")
  private RemoteWaveletContainer.Factory provideRemoteWaveletContainerFactory(
      final DeltaStore deltaStore) {
    return new RemoteWaveletContainer.Factory() {
      @Override
      public RemoteWaveletContainer create(WaveletNotificationSubscriber notifiee,
          WaveletName waveletName, String waveDomain) {
        return new RemoteWaveletContainerImpl(waveletName, notifiee, loadWaveletState(
            waveletLoadExecutor, deltaStore, waveletName, waveletLoadExecutor),
            storageContinuationExecutor);
      }
    };
  }

    @Provides
    @SuppressWarnings("unused")
    private WaveCertPathValidator provideWaveCertPathValidator(Config config,
       TimeSource timeSource,
       VerifiedCertChainCache certCache,
       TrustRootsProvider trustRootsProvider) {
        if (config.getBoolean("federation.waveserver_disable_signer_verification")) {
            return new DisabledCertPathValidator();
        } else {
            return new CachedCertPathValidator(certCache, timeSource, trustRootsProvider);
        }
    }

  /**
   * Returns a future whose result is the state of the wavelet after it has been
   * loaded from storage. Any failure is reported as a
   * {@link PersistenceException}.
   */
  @VisibleForTesting
  static ListenableFuture<DeltaStoreBasedWaveletState> loadWaveletState(Executor executor,
      final DeltaStore deltaStore, final WaveletName waveletName, final Executor persistExecutor) {
    ListenableFutureTask<DeltaStoreBasedWaveletState> task =
        ListenableFutureTask.create(
           new Callable<DeltaStoreBasedWaveletState>() {
             @Override
             public DeltaStoreBasedWaveletState call() throws PersistenceException {
               return DeltaStoreBasedWaveletState.create(deltaStore.open(waveletName),
                                                                persistExecutor);
             }
           });
    executor.execute(task);
    return task;
  }
}
