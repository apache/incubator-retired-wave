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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;

import java.security.cert.X509Certificate;


public class WaveSignatureVerifierTest extends TestCase {

  static private final String SIGNATURE =
      "TMX5+6tJnEfso3KnbWygPfGBKXtFjRk6K/SQHyj+O5/dMuGeh5n/Da3v/" +
      "Cq13LcRie18dxUWMginQUGrsgseqse5orT0C4i0P6ybSxwUZ8OfFnx3lD5K4ME" +
      "ceB+yAMCsnoUZA/F52ullE/aMpv9LIFmNl4QtlvKJmF3UlJCJe/M=";

  static private final String SIGNER_ID =
      "zBYbw+lLkXGao+LfNWbv/faS+yAlsAmUfCNqXBxeFtI=";

  static private final String DOMAIN = "example.com";

  static private final String AUTHORITY = "Server Cert";

  static private final byte[] MESSAGE = "hello".getBytes();

  private DefaultCertPathStore store;
  private WaveSignatureVerifier verifier;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Jan 31, 2009
    FakeTimeSource timeSource = new FakeTimeSource(1233465103000L);
    DefaultCacheImpl cache = new DefaultCacheImpl(timeSource);
    CachedCertPathValidator validator = new CachedCertPathValidator(cache,
        timeSource, new FakeTrustRootsProvider(CertConstantUtil.CA_PUB_CERT));
    store = new DefaultCertPathStore();
    verifier = new WaveSignatureVerifier(validator, store);
  }

  public void testVerify() throws Exception {

    storeSignerInfo(ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
        CertConstantUtil.INTERMEDIATE_PUB_CERT));

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(deBase64(SIGNER_ID)))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    verifier.verify(MESSAGE, signature, AUTHORITY);
  }

  public void testVerify_wrongAuthority() throws Exception {

    storeSignerInfo(ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
        CertConstantUtil.INTERMEDIATE_PUB_CERT));

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(deBase64(SIGNER_ID)))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    try {
      verifier.verify(MESSAGE, signature, "some_other_authority.com");
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testVerify_signerNotInStore() throws Exception {

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(deBase64(SIGNER_ID)))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    try {
      verifier.verify(MESSAGE, signature, AUTHORITY);
      fail("expected exception, but didn't get it");
    } catch (UnknownSignerException e) {
      // expected
    }
  }

  public void testVerify_tamperedPayload() throws Exception {

    storeSignerInfo(ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
        CertConstantUtil.INTERMEDIATE_PUB_CERT));

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(deBase64(SIGNER_ID)))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    try {
      verifier.verify("hullo".getBytes(), signature, AUTHORITY);
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testVerify_badCertChain() throws Exception {

    byte[] id = storeSignerInfo(ImmutableList.of(
        CertConstantUtil.SERVER_PUB_CERT));  // missing the intermediate cert

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(id))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    try {
      verifier.verify(MESSAGE, signature, AUTHORITY);
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testSpeed() throws Exception {
    storeSignerInfo(ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
        CertConstantUtil.INTERMEDIATE_PUB_CERT));

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignatureBytes(ByteString.copyFrom(deBase64(SIGNATURE)))
        .setSignerId(ByteString.copyFrom(deBase64(SIGNER_ID)))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .build();

    long start = System.currentTimeMillis();
    long ops = 0;
    while (System.currentTimeMillis() < start + 1000L) {
      verifier.verify(MESSAGE, signature, AUTHORITY);
      ++ops;
    }
    long stop = System.currentTimeMillis();
    System.out.println(String.format("%.2f ms per verification",
        (stop-start)/ (double)ops));
  }


  private byte[] storeSignerInfo(ImmutableList<X509Certificate> certs)
      throws Exception {
    SignerInfo info = new SignerInfo(HashAlgorithm.SHA256, certs, DOMAIN);
    store.putSignerInfo(info.toProtoBuf());
    return info.getSignerId();
  }

  private byte[] deBase64(String string) {
    return Base64.decodeBase64(string.getBytes());
  }
}
