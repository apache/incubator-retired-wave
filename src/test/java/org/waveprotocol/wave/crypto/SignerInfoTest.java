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
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;

import java.security.cert.X509Certificate;
import java.util.List;


public class SignerInfoTest extends TestCase {

  private static final String DOMAIN = "example.com";

  private List<X509Certificate> certChain;
  private SignerInfo signerInfo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    certChain = ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
        CertConstantUtil.INTERMEDIATE_PUB_CERT);
  }

  public void testGetSignerId() throws Exception {
    signerInfo = new SignerInfo(HashAlgorithm.SHA256, certChain, DOMAIN);
    assertEquals("zBYbw+lLkXGao+LfNWbv/faS+yAlsAmUfCNqXBxeFtI=",
        base64(signerInfo.getSignerId()));

    signerInfo = new SignerInfo(HashAlgorithm.SHA512, certChain, DOMAIN);
    assertEquals("wtbyS7wiCbIoLXJQjuyER6zTxJe9+pRYi3jxtCBl41eE6inQZBC" +
        "2Eu8V5AoirzWH271i8JXNdn+6x/eV/nog2g==",
        base64(signerInfo.getSignerId()));
  }

  public void testGetSignerId_fromProtobuf() throws Exception {
    ProtocolSignerInfo protobuf = ProtocolSignerInfo.newBuilder()
        .setHashAlgorithm(HashAlgorithm.SHA256)
        .addCertificate(ByteString.copyFrom(
            CertConstantUtil.SERVER_PUB_CERT.getEncoded()))
        .addCertificate(ByteString.copyFrom(
            CertConstantUtil.INTERMEDIATE_PUB_CERT.getEncoded()))
        .setDomain(DOMAIN)
        .build();

    signerInfo = new SignerInfo(protobuf);
    assertEquals("zBYbw+lLkXGao+LfNWbv/faS+yAlsAmUfCNqXBxeFtI=",
        base64(signerInfo.getSignerId()));
  }

  public void testGetSignerId_emptyCertChain() throws Exception {
    certChain = ImmutableList.of();
    try {
      new SignerInfo(HashAlgorithm.SHA256, certChain, DOMAIN);
      fail("expected exception, but didn't get it");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testGetHashAlgorithm() throws Exception {
    signerInfo = new SignerInfo(HashAlgorithm.SHA256, certChain, DOMAIN);
    assertEquals(HashAlgorithm.SHA256, signerInfo.getHashAlgorithm());
  }

  public void testGetCertificates() throws Exception {
    signerInfo = new SignerInfo(HashAlgorithm.SHA256, certChain, DOMAIN);
    assertEquals(certChain, signerInfo.getCertificates());
  }

  private String base64(byte[] bytes) {
    return new String(Base64.encodeBase64(bytes));
  }
}
