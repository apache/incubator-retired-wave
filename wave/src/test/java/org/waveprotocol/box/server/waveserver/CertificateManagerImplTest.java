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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.waveprotocol.box.server.waveserver.Ticker.EASY_TICKS;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.waveserver.CertificateManager.SignerInfoPrefetchResultListener;
import org.waveprotocol.box.server.waveserver.testing.Certificates;
import org.waveprotocol.wave.crypto.CachedCertPathValidator;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCacheImpl;
import org.waveprotocol.wave.crypto.DefaultTrustRootsProvider;
import org.waveprotocol.wave.crypto.DisabledCertPathValidator;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.crypto.TimeSource;
import org.waveprotocol.wave.crypto.TrustRootsProvider;
import org.waveprotocol.wave.crypto.UnknownSignerException;
import org.waveprotocol.wave.crypto.VerifiedCertChainCache;
import org.waveprotocol.wave.crypto.WaveCertPathValidator;
import org.waveprotocol.wave.crypto.WaveSignatureVerifier;
import org.waveprotocol.wave.crypto.WaveSigner;
import org.waveprotocol.wave.crypto.WaveSignerFactory;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CertificateManagerImplTest extends TestCase {

  private static final String OTHER_DOMAIN = "other.org";

  private static final FederationError GENERIC_ERROR =
      FederationErrors.badRequest("It's not my fault!");

  private CertPathStore store;
  private CertificateManager manager;
  private Ticker ticker;
  private Config config;

  /*
   * These belong to the example.com domain.
   */
  public static final String DOMAIN = "example.com";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    store = new MemoryStore();
    config = ConfigFactory.parseString("federation.waveserver_disable_verification : " + false);
    manager = new CertificateManagerImpl(config, getSigner(), getVerifier(store, true), store);
    ticker = new Ticker();
  }

  /*
   * TESTS
   */
  public void testSignature() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getProtocolHashedVersion())
        .setAuthor("bob@example.com")
        .build();
    ByteStringMessage<ProtocolWaveletDelta> canonicalDelta = ByteStringMessage.serializeMessage(delta);

    ProtocolSignedDelta signedDelta = manager.signDelta(canonicalDelta);

    manager.storeSignerInfo(getSignerInfo().toProtoBuf());
    ByteStringMessage<ProtocolWaveletDelta> compare = manager.verifyDelta(signedDelta);

    assertEquals(canonicalDelta, compare);
  }

  public void testSignature_missingSignerInfo() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getProtocolHashedVersion())
        .setAuthor("bob@example.com")
        .build();
    ByteStringMessage<ProtocolWaveletDelta> canonicalDelta = ByteStringMessage.serializeMessage(delta);
    manager = new CertificateManagerImpl(config, getSigner(), getVerifier(store, false), store);
    ProtocolSignedDelta signedDelta = manager.signDelta(canonicalDelta);

    try {
      manager.verifyDelta(signedDelta);
      fail("expected UnknownSignerException, but didn't get it");
    } catch (UnknownSignerException e) {
      // expected
    } catch (Exception e) {
      fail("expected UnknownSignerExeception, but got " + e);
    }
  }

  public void testSignature_authorNotMatching() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getProtocolHashedVersion())
        .setAuthor("bob@someotherdomain.com")
        .build();
    ByteStringMessage<ProtocolWaveletDelta> canonicalDelta = ByteStringMessage.serializeMessage(delta);

    ProtocolSignedDelta signedDelta = manager.signDelta(canonicalDelta);

    manager.storeSignerInfo(getSignerInfo().toProtoBuf());

    try {
      manager.verifyDelta(signedDelta);
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testRealSignature() throws Exception {
    manager = new CertificateManagerImpl(config, getSigner(), getRealVerifier(store), store);
    manager.storeSignerInfo(Certificates.getRealSignerInfo().toProtoBuf());
    ByteStringMessage<ProtocolWaveletDelta> compare = manager.verifyDelta(getFakeSignedDelta());
    assertEquals(compare, getFakeDelta());
  }

  /**
   * Test prefetchDeltaSignerInfo for a single request on a single domain, and that subsequent
   * requests on the same domain return instantly.
   */
  public void test_prefetchDeltaSignerInfo1() throws Exception {
    SignerInfoPrefetchResultListener mockListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSuccessfulProvider(), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, mockListener);
    verify(mockListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());

    // Shouldn't get a NPE from the null provider because the callback should not be used

    manager.prefetchDeltaSignerInfo(null, getRealSignerId(), getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null,
        mockListener);
    verify(mockListener, times(2)).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
  }

  /**
   * Test prefetchDeltaSignerInfo for multiple requests on a single domain where the first one
   * does not terminate.  The entire request should fail.
   */
  public void test_prefetchDeltaSignerInfo2() throws Exception {
    // The dead listener won't return
    SignerInfoPrefetchResultListener deadListener = mock(SignerInfoPrefetchResultListener.class);
    manager.prefetchDeltaSignerInfo(getDeadProvider(), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, deadListener);
    verifyZeroInteractions(deadListener);

    // But this will.  However, it shouldn't be called since the other was added first, and only
    // 1 request is started per domain
    SignerInfoPrefetchResultListener aliveListener = mock(SignerInfoPrefetchResultListener.class);
    manager.prefetchDeltaSignerInfo(getSuccessfulProvider(), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, aliveListener);

    verifyZeroInteractions(aliveListener);
  }

  /**
   * Test prefetchDeltaSignerInfo for multiple requests on different domains where the first one
   * does not terminate.  However the second should terminate, and both callbacks called.
   */
  public void test_prefetchDeltaSignerInfo3() throws Exception {
    // This will never return, but the callback will run later
    SignerInfoPrefetchResultListener deadListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getDeadProvider(), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, deadListener);
    verifyZeroInteractions(deadListener);

    // This should succeed later, after some number of ticks
    SignerInfoPrefetchResultListener slowSuccessListener =
        mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSlowSuccessfulProvider(ticker, EASY_TICKS),
        getRealSignerId(), getFakeWaveletName(OTHER_DOMAIN), null, slowSuccessListener);
    verifyZeroInteractions(slowSuccessListener);

    // This would succeed right now if it didn't have to wait for the slow success
    SignerInfoPrefetchResultListener successListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSuccessfulProvider(), getRealSignerId(),
        getFakeWaveletName(OTHER_DOMAIN), null, successListener);
    verifyZeroInteractions(successListener);

    // After ticking, each callback should run
    ticker.tick(EASY_TICKS);
    verify(deadListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
    verify(slowSuccessListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
    verify(successListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());

    // Subsequent calls should also succeed immediately without calling the callback
    SignerInfoPrefetchResultListener nullListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(null, getRealSignerId(), getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null,
        nullListener);
    verify(nullListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
  }

  /**
   * Test prefetchDeltaSignerInfo for failing requests -- the failure should be propagated to
   * the prefetch listener, and requests on the same domain should fail.
   */
  public void test_prefetchDeltaSignerInfo4() throws Exception {
    // This will fail later
    SignerInfoPrefetchResultListener failListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSlowFailingProvider(ticker, EASY_TICKS), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, failListener);

    // This would succeed later if it weren't for the previous one failing
    SignerInfoPrefetchResultListener successListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSuccessfulProvider(), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), null, successListener);
    verifyZeroInteractions(failListener);
    verifyZeroInteractions(successListener);

    // Both callbacks should fail after ticking
    ticker.tick(EASY_TICKS);
    verify(failListener).onFailure(GENERIC_ERROR);
    verify(successListener).onFailure(GENERIC_ERROR);
  }

  /**
   * Test prefetchDeltaSignerInfo for failing requests where a previous request on a different
   * domain has already succeeded.  The failing request should also appear to succeed.
   */
  public void test_prefetchDeltaSignerInfo5() throws Exception {
    // This would fail if the next (immediate) request didn't succeed
    SignerInfoPrefetchResultListener failListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSlowFailingProvider(ticker, EASY_TICKS), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), getHashedVersion(), failListener);
    verifyZeroInteractions(failListener);

    // This will succeed immediately
    SignerInfoPrefetchResultListener successListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSuccessfulProvider(), getRealSignerId(),
        getFakeWaveletName(OTHER_DOMAIN), getHashedVersion(), successListener);

    verify(successListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
    verify(failListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());

    // The failing listener shouldn't do anything, even after the ticks
    ticker.tick(EASY_TICKS);
    verifyNoMoreInteractions(failListener);
    verifyNoMoreInteractions(successListener);
  }

  /**
   * Test prefetchDeltaSignerInfo for failing requests -- even though the first request fails,
   * the second request on a different domain should succeed.
   */
  public void test_prefetchDeltaSignerInfo6() throws Exception {
    // This will fail later
    SignerInfoPrefetchResultListener failListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSlowFailingProvider(ticker, EASY_TICKS), getRealSignerId(),
        getFakeWaveletName(CertificateManagerImplTest.DOMAIN), getHashedVersion(), failListener);
    verifyZeroInteractions(failListener);

    // This will succeed later, after the failing one fails
    SignerInfoPrefetchResultListener successListener = mock(SignerInfoPrefetchResultListener.class);

    manager.prefetchDeltaSignerInfo(getSlowSuccessfulProvider(ticker, EASY_TICKS * 2),
        getRealSignerId(), getFakeWaveletName(OTHER_DOMAIN), getHashedVersion(), successListener);
    verifyZeroInteractions(successListener);

    // The failing request should fail, but successful request left alone
    ticker.tick(EASY_TICKS);
    verifyZeroInteractions(successListener);
    verify(failListener).onFailure(GENERIC_ERROR);

    // The successful request should now succeed
    ticker.tick(EASY_TICKS);
    verify(successListener).onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
    verifyNoMoreInteractions(failListener);
  }


  /*
   * UTILITIES
   */

  private HashedVersion getHashedVersion() {
    return HashedVersion.unsigned(3L);
  }

  private ProtocolHashedVersion getProtocolHashedVersion() {
    return CoreWaveletOperationSerializer.serialize(getHashedVersion());
  }

  private WaveSignatureVerifier getRealVerifier(CertPathStore store) throws Exception {
    TrustRootsProvider trustRoots = new DefaultTrustRootsProvider();
    VerifiedCertChainCache cache = new DefaultCacheImpl(getFakeTimeSource());
    WaveCertPathValidator validator = new CachedCertPathValidator(
      cache, getFakeTimeSource(), trustRoots);

    return new WaveSignatureVerifier(validator, store);
  }

  private WaveSignatureVerifier getVerifier(CertPathStore store,
      boolean disableSignerVerification) {
    VerifiedCertChainCache cache = new DefaultCacheImpl(getFakeTimeSource());
    WaveCertPathValidator validator;
    if (disableSignerVerification) {
      validator = new DisabledCertPathValidator();
    } else {
      validator = new CachedCertPathValidator(
          cache, getFakeTimeSource(), getTrustRootsProvider());
    }
    return new WaveSignatureVerifier(validator, store);
  }

  private TrustRootsProvider getTrustRootsProvider() {
    return new TrustRootsProvider() {
      @Override
      public Collection<X509Certificate> getTrustRoots() {
        try {
          return getSigner().getSignerInfo().getCertificates();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private SigningSignatureHandler getSigner() throws Exception {
    InputStream keyStream = new ByteArrayInputStream(Certificates.EXAMPLE_PRIVATE_KEY.getBytes());
    InputStream certStream = new ByteArrayInputStream(Certificates.EXAMPLE_CERTIFICATE.getBytes());
    List<InputStream> certStreams = ImmutableList.of(certStream);

    WaveSignerFactory factory = new WaveSignerFactory();
    WaveSigner signer = factory.getSigner(keyStream, certStreams, CertificateManagerImplTest.DOMAIN);
    return new SigningSignatureHandler(signer);
  }

  private SignerInfo getSignerInfo() throws Exception {
    return getSigner().getSignerInfo();
  }

  private TimeSource getFakeTimeSource() {
    return new TimeSource() {
      @Override
      public Date now() {
        return new Date(currentTimeMillis());
      }

      @Override
      public long currentTimeMillis() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2009, 11, 1);
        return cal.getTimeInMillis();
      }
    };
  }

  private ProtocolSignedDelta getFakeSignedDelta() throws Exception {
    return ProtocolSignedDelta.newBuilder()
        .setDelta(getFakeDelta().getByteString())
        .addSignature(getRealSignature())
        .build();
  }

  private ByteStringMessage<ProtocolWaveletDelta> getFakeDelta() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getProtocolHashedVersion())
        .setAuthor("bob@initech-corp.com")
        .build();
    return ByteStringMessage.serializeMessage(delta);
  }

  private ProtocolSignature getRealSignature() throws Exception {
    return ProtocolSignature.newBuilder()
        .setSignerId(ByteString.copyFrom(Certificates.getRealSignerInfo().getSignerId()))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .setSignatureBytes(ByteString.copyFrom(Certificates.REAL_SIGNATURE))
        .build();
  }

  private WaveletName getFakeWaveletName(String domain) {
    return WaveletName.of(WaveId.of(domain, "wave"), WaveletId.of(domain, "wavelet"));
  }

  private ByteString getRealSignerId() throws Exception {
    return ByteString.copyFrom(Certificates.getRealSignerInfo().getSignerId());
  }


  /*
   * Fake WaveletFederationProviders.
   */

  private abstract class WaveletSignerInfoProvider implements WaveletFederationProvider {
    @Override public void postSignerInfo(String destinationDomain, ProtocolSignerInfo signerInfo,
        PostSignerInfoResponseListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override  public void requestHistory(WaveletName waveletName, String domain,
        ProtocolHashedVersion startVersion, ProtocolHashedVersion endVersion, long lengthLimit,
        HistoryResponseListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override public void submitRequest(WaveletName waveletName, ProtocolSignedDelta delta,
        SubmitResultListener listener) {
      throw new UnsupportedOperationException();
    }
  }

  private WaveletFederationProvider getSuccessfulProvider() {
    return new WaveletSignerInfoProvider() {
      @Override public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
          ProtocolHashedVersion deltaEndVersion, DeltaSignerInfoResponseListener listener) {
        try {
          listener.onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private WaveletFederationProvider getSlowSuccessfulProvider(final Ticker ticker,
      final int ticks) {
    return new WaveletSignerInfoProvider() {
      @Override public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
          ProtocolHashedVersion deltaEndVersion, final DeltaSignerInfoResponseListener listener) {
        ticker.runAt(ticks, new Runnable() {
          @Override public void run() {
            try {
              listener.onSuccess(Certificates.getRealSignerInfo().toProtoBuf());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    };
  }

  private WaveletFederationProvider getSlowFailingProvider(final Ticker ticker, final int ticks) {
    return new WaveletSignerInfoProvider() {
      @Override public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
          ProtocolHashedVersion deltaEndVersion, final DeltaSignerInfoResponseListener listener) {
        ticker.runAt(ticks, new Runnable() {
          @Override public void run() {
            listener.onFailure(GENERIC_ERROR);
          }
        });
      }
    };
  }

  private WaveletFederationProvider getDeadProvider() {
    return new WaveletSignerInfoProvider() {
      @Override public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
          ProtocolHashedVersion deltaEndVersion, DeltaSignerInfoResponseListener listener) {
        // Never calls the callback
      }
    };
  }
}
