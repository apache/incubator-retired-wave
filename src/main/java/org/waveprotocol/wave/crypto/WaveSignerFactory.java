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

import com.google.common.collect.Lists;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;


/**
 * Reads certificate and private key information from {@link InputStream}s and
 * creates a WaveSigner object.
 *
 * The certificates must be X.509-certificates, either DER or PEM encoded, and
 * the private key must be PKCS#8-PEM-encoded.
 *
 * This signer will use the SHA1_RSA signing algorithm for signatures, and use
 * the SHA256 hash algorithm to calculate its id (which is the hash of the
 * PkiPath-encoded certificate chain).
 */
public class WaveSignerFactory {

  private static final String CERTIFICATE_TYPE = "X.509";

  /**
   * Returns a WaveSigner.
   * @param privateKeyStream the stream from which to read the private key. The
   *   key must be in PKCS#8-PEM-encoded format.
   * @param certStreams a list of streams from which to read the certificate
   *   chain. The first stream in the list must have the target certificate
   *   (i.e., the certificate issued to the signer).
   * @param domain The domain for which the certificate was issued. This should
   *   match the CN in the targetcertificate.
   * @return a WaveSigner
   * @throws SignatureException if the private key or certificates cannot be
   *   parsed.
   */
  public WaveSigner getSigner(InputStream privateKeyStream,
      Iterable<? extends InputStream> certStreams, String domain)
      throws SignatureException {

    PrivateKey privateKey = getPrivateKey(privateKeyStream);
    List<X509Certificate> certs = getCertificates(certStreams);

    SignerInfo signerInfo = new SignerInfo(HashAlgorithm.SHA256, certs, domain);
    return new WaveSigner(SignatureAlgorithm.SHA1_RSA, privateKey, signerInfo);
  }

  private List<X509Certificate> getCertificates(
      Iterable<? extends InputStream> certStreams) throws SignatureException {

    try {
      List<X509Certificate> certs = Lists.newArrayList();
      for (InputStream stream : certStreams) {
        certs.add(getCertificate(stream));
      }
      return certs;

    } catch (CertificateException e) {
      throw new SignatureException(e);
    }
  }

  private X509Certificate getCertificate(InputStream stream)
      throws CertificateException {
    CertificateFactory factory =
        CertificateFactory.getInstance(CERTIFICATE_TYPE);
    return (X509Certificate) factory.generateCertificate(stream);
  }

  private PrivateKey getPrivateKey(InputStream privateKeyStream)
      throws SignatureException {
    try {
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
          readBase64Bytes(privateKeyStream));
      KeyFactory keyFac = KeyFactory.getInstance("RSA");
      return keyFac.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException e) {
      throw new SignatureException(e);
    } catch (InvalidKeySpecException e) {
      throw new SignatureException(e);
    } catch (IOException e) {
      throw new SignatureException(e);
    }
  }

  private byte[] readBase64Bytes(InputStream stream) throws IOException {
    StringBuilder builder = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();

      // read past ----BEGIN PRIVATE KEY and ----END PRIVATE KEY lines
      if (line.startsWith("-----BEGIN") || line.startsWith("-----END")) {
        continue;
      }
      builder.append(line);
    }
    return Base64.decodeBase64(builder.toString().getBytes());
  }
}
