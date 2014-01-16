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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

/**
 * Forwards wave notifications to wave bus subscribers and remote wave servers.
 *
 * Swallows any runtime exception from a wave bus subscriber but not removes that
 * subscriber. The wave server used to do this swallowing but really things are
 * in bad shape if a subscriber throws a runtime exception.
 * TODO(anorth): Remove this catch and let the server crash.
 *
 * @author soren@google.com (Soren Lassen)
 */
class WaveletNotificationDispatcher implements WaveBus, WaveletNotificationSubscriber {

  private static final Log LOG = Log.get(WaveletNotificationDispatcher.class);

  /** Picks out the transformed deltas from a list of delta records. */
  private static ImmutableList<TransformedWaveletDelta> transformedDeltasOf(
      Iterable<WaveletDeltaRecord> deltaRecords) {
    ImmutableList.Builder<TransformedWaveletDelta> transformedDeltas = ImmutableList.builder();
    for (WaveletDeltaRecord deltaRecord : deltaRecords) {
      transformedDeltas.add(deltaRecord.getTransformedDelta());
    }
    return transformedDeltas.build();
  }

  /** Picks out the byte strings of the applied deltas from a list of delta records. */
  private static ImmutableList<ByteString> serializedAppliedDeltasOf(
      Iterable<WaveletDeltaRecord> deltaRecords) {
    ImmutableList.Builder<ByteString> serializedAppliedDeltas = ImmutableList.builder();
    for (WaveletDeltaRecord deltaRecord : deltaRecords) {
      serializedAppliedDeltas.add(deltaRecord.getAppliedDelta().getByteString());
    }
    return serializedAppliedDeltas.build();
  }

  private final ImmutableSet<String> localDomains;
  private final WaveletFederationListener.Factory federationHostFactory;
  private final CopyOnWriteArraySet<WaveBus.Subscriber> subscribers =
      new CopyOnWriteArraySet<WaveBus.Subscriber>();

  /** Maps remote domains to wave server stubs for those domains. */
  private final LoadingCache<String, WaveletFederationListener> federationHosts =
      CacheBuilder.newBuilder().build(new CacheLoader<String, WaveletFederationListener>() {
    @Override
    public WaveletFederationListener load(String domain) {
      return federationHostFactory.listenerForDomain(domain);
    }
  });

  /**
   * Constructor.
   *
   * @param certificateManager knows what the local domains are
   * @param federationHostFactory manufactures federation host instances for
   *        remote domains
   */
  @Inject
  public WaveletNotificationDispatcher(
      CertificateManager certificateManager,
      @FederationHostBridge WaveletFederationListener.Factory federationHostFactory) {
    this.localDomains = certificateManager.getLocalDomains();
    this.federationHostFactory = federationHostFactory;
  }

  @Override
  public void subscribe(Subscriber s) {
    subscribers.add(s);
  }

  @Override
  public void unsubscribe(Subscriber s) {
    subscribers.remove(s);
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, ImmutableList<WaveletDeltaRecord> deltas,
      ImmutableSet<String> domainsToNotify) {
    DeltaSequence sequence = DeltaSequence.of(transformedDeltasOf(deltas));
    for (WaveBus.Subscriber s : subscribers) {
      try {
        s.waveletUpdate(wavelet, sequence);
      } catch (RuntimeException e) {
        LOG.severe("Runtime exception in update to wave bus subscriber " + s, e);
      }
    }

    Set<String> remoteDomainsToNotify = Sets.difference(domainsToNotify, localDomains);
    if (!remoteDomainsToNotify.isEmpty()) {
      ImmutableList<ByteString> serializedAppliedDeltas = serializedAppliedDeltasOf(deltas);
      for (String domain : remoteDomainsToNotify) {
        try {
          federationHosts.get(domain).waveletDeltaUpdate(WaveletDataUtil.waveletNameOf(wavelet),
              serializedAppliedDeltas, federationCallback("delta update"));
        } catch (ExecutionException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version,
      ImmutableSet<String> domainsToNotify) {
    for (WaveBus.Subscriber s : subscribers) {
      try {
        s.waveletCommitted(waveletName, version);
      } catch (RuntimeException e) {
        LOG.severe("Runtime exception in commit to wave bus subscriber " + s, e);
      }
    }

    Set<String> remoteDomainsToNotify = Sets.difference(domainsToNotify, localDomains);
    if (!remoteDomainsToNotify.isEmpty()) {
      ProtocolHashedVersion serializedVersion = CoreWaveletOperationSerializer.serialize(version);
      for (String domain : remoteDomainsToNotify) {
        try {
          federationHosts.get(domain).waveletCommitUpdate(
              waveletName, serializedVersion, federationCallback("commit notice"));
        } catch (ExecutionException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private WaveletFederationListener.WaveletUpdateCallback federationCallback(
      final String description) {
    return new WaveletFederationListener.WaveletUpdateCallback() {
      @Override
      public void onSuccess() {
        LOG.info(description + " success");
      }

      @Override
      public void onFailure(FederationError error) {
        LOG.warning(description + " failure: " + error);
      }
    };
  }
}