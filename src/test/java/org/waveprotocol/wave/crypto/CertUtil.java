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

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Parses byte arrays into certificates or private keys.
 */
public class CertUtil {

  private CertUtil() {
  }

  public static X509Certificate getCertFromBase64Bytes(String b64cert)
      throws GeneralSecurityException {
    return getCertFromBytes(Base64.decodeBase64(b64cert.getBytes()));
  }

  public static X509Certificate getCertFromBytes(byte[] derCert) throws GeneralSecurityException {
    CertificateFactory fac = CertificateFactory.getInstance("X509");
    ByteArrayInputStream in = new ByteArrayInputStream(derCert);
    X509Certificate cert = (X509Certificate)fac.generateCertificate(in);
    return cert;
  }

  public static PrivateKey getPrivateKeyFromBytes(byte[] derKey) throws GeneralSecurityException {
    KeyFactory fac = KeyFactory.getInstance("RSA");
    EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(derKey);
    return fac.generatePrivate(privKeySpec);
  }
}
