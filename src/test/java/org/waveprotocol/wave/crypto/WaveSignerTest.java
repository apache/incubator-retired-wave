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

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;


public class WaveSignerTest extends TestCase {

  static private final String DOMAIN = "example.com";

  private WaveSigner signer;
  private SignerInfo signerInfo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();


    signerInfo = new SignerInfo(HashAlgorithm.SHA256,
        ImmutableList.of(CertConstantUtil.SERVER_PUB_CERT,
            CertConstantUtil.INTERMEDIATE_PUB_CERT), DOMAIN);
    signer = new WaveSigner(SignatureAlgorithm.SHA1_RSA,
        CertConstantUtil.SERVER_PRIV_KEY, signerInfo);
  }

  public void testSign() throws Exception {
    byte[] payload = "hello".getBytes();
    ProtocolSignature signature = signer.sign(payload);

    assertEquals(SignatureAlgorithm.SHA1_RSA,
        signature.getSignatureAlgorithm());
    assertEquals("zBYbw+lLkXGao+LfNWbv/faS+yAlsAmUfCNqXBxeFtI=",
        base64(signature.getSignerId().toByteArray()));
    assertEquals("TMX5+6tJnEfso3KnbWygPfGBKXtFjRk6K/SQHyj+O5/dMuGeh5n/Da3v/" +
        "Cq13LcRie18dxUWMginQUGrsgseqse5orT0C4i0P6ybSxwUZ8OfFnx3lD5K4ME" +
        "ceB+yAMCsnoUZA/F52ullE/aMpv9LIFmNl4QtlvKJmF3UlJCJe/M=",
        base64(signature.getSignatureBytes().toByteArray()));
  }

  public void testSpeed() throws Exception {
    byte[] payload = "hello".getBytes();
    long start = System.currentTimeMillis();
    long ops = 0;
    while (System.currentTimeMillis() < start + 1000L) {
      ProtocolSignature signature = signer.sign(payload);
      ++ops;
    }
    long stop = System.currentTimeMillis();
    System.out.println(String.format("%.2f ms per signature",
        (stop-start)/ (double)ops));
  }

  private String base64(byte[] bytes) {
    return new String(Base64.encodeBase64(bytes));
  }
}
