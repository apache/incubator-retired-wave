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
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class capable of verifying signatures, by looking up certificate chains in
 * a store, and using a caching cert chain validator.
 */
public class WaveSignatureVerifier {

  // regexp that picks out a Common Name out of a X.500 Distinguished Name
  private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

  // 2 represents an AlternativeSubjectName of type DNS
  private static final Integer ALT_NAME_TYPE_DNS = Integer.valueOf(2);

  // The cert chain validator. This object can tell us whether a given cert
  // chain checks out ok.
  private final WaveCertPathValidator pathValidator;

  // The store that has the cert chains. This object maps from signer ids to
  // cert chains.
  private final CertPathStore pathStore;

  public WaveSignatureVerifier(WaveCertPathValidator validator, CertPathStore store) {
    this.pathValidator = validator;
    this.pathStore = store;
  }

  /**
   * Verifies the signature on some signed payload.
   * @param signedPayload the payload on which we're verifiying the signature.
   * @param signatureInfo the signature provided with the payload.
   * @param authority name of the authority that we expect the target
   *   certificate to be issued to.
   *
   * @throws SignatureException if the signature can't be verified, either
   *   because it simply didn't check out, or because of other reasons, like us
   *   not supporting the signature algorithm specified.
   * @throws UnknownSignerException if we can't find the cert chain in the local
   *   cert-path store.
   */
  public void verify(byte[] signedPayload, ProtocolSignature signatureInfo,
      String authority) throws SignatureException, UnknownSignerException {

    SignerInfo signer = pathStore.getSignerInfo(
        signatureInfo.getSignerId().toByteArray());

    if (signer == null) {
      throw new UnknownSignerException("could not find information about signer "
          + Base64.encodeBase64(signatureInfo.getSignerId().toByteArray()));
    }

    verifySignerInfo(signer);

    Signature verifier;
    try {
      verifier = Signature.getInstance(AlgorithmUtil.getJceName(
          signatureInfo.getSignatureAlgorithm()));
    } catch (NoSuchAlgorithmException e) {
      throw new SignatureException("can't verify signatures of type " +
          signatureInfo.getSignatureAlgorithm().toString(), e);
    }

    X509Certificate cert = signer.getCertificates().get(0);

    try {
      verifier.initVerify(cert);
    } catch (InvalidKeyException e) {
      throw new SignatureException("certificate of signer was not issued for " +
          "message signing");
    }

    try {
      verifier.update(signedPayload);
    } catch (java.security.SignatureException e) {
      // this is thrown if the verifier object is not properly initialized.
      // this shouldn't happen as we _just_ initialized it on the previous line.
      throw new IllegalStateException(e);
    }

    try {
      if (!verifier.verify(signatureInfo.getSignatureBytes().toByteArray())) {
        throw new SignatureException("signature did not verify");
      }
    } catch (java.security.SignatureException e) {
      throw new SignatureException(e);
    }

    verifyMatchingAuthority(authority, cert);
  }

  /**
   * Verifies that the {@link SignerInfo} (i.e., the cerificate chain) checks
   * out, i.e., chains up to a trusted CA, and has certificates that aren't
   * expired.
   *
   * @throws SignatureException if the certificate chain in the
   *   {@link SignerInfo} does't verify.
   */
  public void verifySignerInfo(SignerInfo signer) throws SignatureException {
    pathValidator.validate(signer.getCertificates());
  }

  /**
   * Verifies that the given certificate was issued to the given authority.
   * @param authority the authority to which the certificate was issued,
   *   e.g., a domain name.
   * @param certificate the {@link X509Certificate}
   * @throws SignatureException if the authority doesn't match the certificate.
   */
  private void verifyMatchingAuthority(String authority,
      X509Certificate certificate) throws SignatureException {

    String cn = getCommonNameFromDistinguishedName(
        certificate.getSubjectX500Principal().getName());

    if (cn == null) {
      throw new SignatureException("no common name found in signer " +
          "certificate " + certificate.getSubjectDN().toString());
    }

    if (cn.equals(authority)) {
      return;
    }

    if (authorityMatchesSubjectAlternativeNames(authority, certificate)) {
      return;
    }

    if (authorityMatchesWildcardCN(authority, cn)) {
      return;
    }

    throw new SignatureException("expected " + authority +
        " as CN or alternative name in cert, but didn't find it");

  }

  /**
   * Returns true if the authority given matches any of the
   * SubjectAlternativeNames present in the certificate, false otherwise.
   */
  private boolean authorityMatchesSubjectAlternativeNames(String authority,
      X509Certificate certificate) {

    Collection<List<?>> subjAltNames = null;
    try {
      subjAltNames = certificate.getSubjectAlternativeNames();
    } catch (CertificateParsingException e) {

      // This is a bit strange - it means that the AubjectAlternativeNames
      // extension wasn't properly encoded in this cert. We'll leave subjAltNames null.
    }

    if (subjAltNames == null) {
      return false;
    }

    for (List<?> altName : subjAltNames) {

      Integer nameType = (Integer) altName.get(0);

      // We're only interested in alternative names that denote domain names.
      if (!ALT_NAME_TYPE_DNS.equals(nameType)) {
        continue;
      }

      String dnsName = (String) altName.get(1);
      if (authority.equals(dnsName)) {
        return true;
      }
    }

    // None of the names matched.
    return false;
  }

  private String getCommonNameFromDistinguishedName(String dn) {
    Matcher m = CN_PATTERN.matcher(dn);
    if (m.find()) {
      return m.group(1);
    } else {
      return null;
    }
  }

  /**
   * Returns true if the authority given matches a CN with a wildcard
   * expression.
   *
   * @author pablojan (pablojan@gmail.com)
   *
   * @param authority
   * @param certificate
   * @return
   */
  private boolean authorityMatchesWildcardCN(String authority, String commonName) {

    // check for a wildcard expression
    if (!commonName.startsWith("*.")) {
      return false;
    }

    // second-level domain
    String sndLevelName = commonName.substring(2, commonName.length());


    // trim authority name
    String sndLevelAuth = authority.substring(authority.indexOf(".") + 1, authority.length());

    return sndLevelAuth.equals(sndLevelName);

  }
}
