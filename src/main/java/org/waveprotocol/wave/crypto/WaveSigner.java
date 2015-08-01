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

import com.google.protobuf.ByteString;

import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature.SignatureAlgorithm;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;

/**
 * Class that can sign payloads (i.e., byte arrays).
 */
public class WaveSigner {

  private final SignatureAlgorithm algorithm;
  private final SignerInfo signerInfo;
  private final PrivateKey signingKey;

  /**
   * Public constructor.
   * @param alg the signature algorithm that this signer will use on all of its
   *   signatures.
   * @param signingKey the signing key that this signer will use for all its
   *   signatures.
   * @param signerInfo the signer info of this signer, i.e., the cert chain for
   *   this signer.
   * @throws SignatureException if the private key provided can't be used, or
   *   for some other reason we can't initialize the signer properly.
   */
  public WaveSigner(SignatureAlgorithm alg, PrivateKey signingKey,
      SignerInfo signerInfo) throws SignatureException {

    this.algorithm = alg;
    this.signerInfo = signerInfo;
    this.signingKey = signingKey;

    try {

      // we'll check here whether we can make such a signer, but we won't use
      // it. We'll (re-)make a new signer object in the sign() method in order
      // to be thread-safe.
      Signature signer = Signature.getInstance(AlgorithmUtil.getJceName(alg));
      signer.initSign(signingKey);

    } catch (InvalidKeyException e) {
      throw new SignatureException("private key does not match algorithm " +
          alg.toString(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new SignatureException("can not generate signatures of type " +
          alg.toString(), e);
    }
  }

  /**
   * Signs a payload and returns a {@link ProtocolSignature} object
   * representing the signature.
   * @param payload the bits that are to be signed.
   * @return the {@link SignerInfo} object.
   */
  public ProtocolSignature sign(byte[] payload) {

    try {
      Signature signer = Signature.getInstance(
          AlgorithmUtil.getJceName(algorithm));
      signer.initSign(signingKey);
      signer.update(payload);
      return ProtocolSignature.newBuilder()
          .setSignatureBytes(ByteString.copyFrom(signer.sign()))
          .setSignerId(ByteString.copyFrom(signerInfo.getSignerId()))
          .setSignatureAlgorithm(algorithm)
          .build();

    } catch (java.security.SignatureException e) {

      // This is thrown if the signer object isn't properly initialized.
      // Since we just made that object from scratch and initialized it, this
      // really shouldn't happen
      throw new IllegalStateException(e);

    } catch (InvalidKeyException e) {

      // we checked for this in the constructor - this really shouldn't happen
      throw new IllegalStateException(e);

    } catch (NoSuchAlgorithmException e) {

      // we checked for this in the constructor - this really shouldn't happen
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the {@link SignerInfo} (i.e., basically its certificate chain)
   */
  public SignerInfo getSignerInfo() {
    return signerInfo;
  }
}
