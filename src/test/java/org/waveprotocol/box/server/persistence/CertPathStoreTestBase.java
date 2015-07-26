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

package org.waveprotocol.box.server.persistence;

import junit.framework.TestCase;

import org.waveprotocol.box.server.waveserver.testing.Certificates;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Testcases for the {@link CertPathStore}.
 *
 *  TODO(ljvderijk): Tests for replacing an existing certificate and confirm
 * that non-parsing certificates throw SignatureException.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public abstract class CertPathStoreTestBase extends TestCase {

  private final SignerInfo realSignerInfo;
  private final SignerInfo exampleSignerInfo;

  public CertPathStoreTestBase() throws Exception {
    realSignerInfo = Certificates.getRealSignerInfo();
    exampleSignerInfo = Certificates.getExampleSignerInfo();
  }

  /**
   * Returns an empty {@link CertPathStore}.
   */
  protected abstract CertPathStore newCertPathStore();

  public void testCertificatesAreStored() throws Exception {
    CertPathStore certPathStore = newCertPathStore();

    ProtocolSignerInfo realSignerInfoProto = realSignerInfo.toProtoBuf();
    certPathStore.putSignerInfo(realSignerInfoProto);

    ProtocolSignerInfo exampleSignerInfoProto = exampleSignerInfo.toProtoBuf();
    certPathStore.putSignerInfo(exampleSignerInfoProto);

    checkCertificateExists(realSignerInfo, certPathStore);
    checkCertificateExists(exampleSignerInfo, certPathStore);
  }

  public void testNotExistingSignerIdGivesNull() throws SignatureException {
    assertNull("Expected Null for a non-existing Signer Id",
        newCertPathStore().getSignerInfo(new byte[1]));
  }

  /**
   * Checks whether for the given {@link SignerInfo} the certificates retrieved
   * from the {@link CertPathStore} match up.
   *
   * @param signerInfo the {@link SignerInfo} to get the certificates from the
   *        {@link CertPathStore} for
   * @param certPathStore the {@link CertPathStore} to retrieve the certificates
   *        from.
   */
  private void checkCertificateExists(SignerInfo signerInfo, CertPathStore certPathStore)
      throws SignatureException {
    List<X509Certificate> retrievedCerts =
        certPathStore.getSignerInfo(signerInfo.getSignerId()).getCertificates();
    assertEquals(signerInfo.getCertificates(), retrievedCerts);
  }
}
