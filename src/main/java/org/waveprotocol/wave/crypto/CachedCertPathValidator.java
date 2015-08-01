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

package org.waveprotocol.wave.crypto;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A cert path (aka cert chain) validator that stores validation results in
 * a cache. It will also first attempt to look up validation results in the
 * cache, before performing a full-blown cert chain verification.
 */
public class CachedCertPathValidator implements WaveCertPathValidator {

  private static final String VALIDATOR_TYPE = "PKIX";
  private static final String CERTIFICATE_TYPE = "X.509";

  // the cache that stores, for a limited amount of time, cert chain
  // verification result.
  private final VerifiedCertChainCache certPathCache;

  // source for current time, so that expiration of certificates can be checked
  private final TimeSource timeSource;

  // set of trusted Certification Authorities
  private final Set<TrustAnchor> trustRoots;

  public CachedCertPathValidator(VerifiedCertChainCache certPathCache,
      TimeSource timeSource, TrustRootsProvider trustRootsProvider) {
    this.certPathCache = certPathCache;
    this.timeSource = timeSource;
    this.trustRoots = getTrustRoots(trustRootsProvider);
  }

  @Override
  public void validate(List<? extends X509Certificate> certs) throws SignatureException {
    if (!certPathCache.contains(certs)) {
      validateNoCache(certs);

      // we don't get here if certs didn't validate
      certPathCache.add(certs);
    }
  }

  private Set<TrustAnchor> getTrustRoots(TrustRootsProvider provider) {
    List<TrustAnchor> anchors = Lists.newArrayList();
    for (X509Certificate c : provider.getTrustRoots()) {
      anchors.add(new TrustAnchor(c, null));
    }
    return ImmutableSet.copyOf(anchors);
  }

  private static WaveOidChecker WAVE_OID_CHECKER = new WaveOidChecker();

  private void validateNoCache(List<? extends X509Certificate> certs)
      throws SignatureException {
    try {
      CertPathValidator validator = CertPathValidator.getInstance(
          VALIDATOR_TYPE);
      PKIXParameters params = new PKIXParameters(trustRoots);
      params.addCertPathChecker(WAVE_OID_CHECKER);
      params.setDate(timeSource.now());

      // turn off default revocation-checking mechanism
      params.setRevocationEnabled(false);

      // TODO: add a way for clients to add certificate revocation checks,
      // perhaps by letting them pass in PKIXCertPathCheckers. This can also be
      // useful to check for Wave-specific certificate extensions.

      CertificateFactory certFactory = CertificateFactory.getInstance(
          CERTIFICATE_TYPE);
      CertPath certPath = certFactory.generateCertPath(certs);
      validator.validate(certPath, params);
    } catch (GeneralSecurityException e) {
      throw new SignatureException("Certificate validation failure", e);
    }
  }

  private static class WaveOidChecker extends PKIXCertPathChecker {

    private static final String WAVE_OID = "1.3.6.1.4.1.11129.2.1.1";
    private static final Set<String> SUPPORTED_EXTENSIONS =
        ImmutableSet.of(WAVE_OID);

    @Override
    public void check(Certificate cert, Collection<String> unresolvedCritExts) {
      // We know about the WAVE OID - if it's in the unresolved critical
      // extensions, that should not cause an error. In fact, in the future
      // we might _require_ this extension
      unresolvedCritExts.remove(WAVE_OID);
    }

    @Override
    public Set<String> getSupportedExtensions() {
      return SUPPORTED_EXTENSIONS;
    }

    @Override
    public void init(boolean forward) {
      // nothing to do
    }

    @Override
    public boolean isForwardCheckingSupported() {
      return true;
    }
  }
}
