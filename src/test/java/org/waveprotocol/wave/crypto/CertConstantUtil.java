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

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CertConstantUtil {

  public static final X509Certificate SERVER_PUB_CERT;
  public static final X509Certificate INTERMEDIATE_PUB_CERT;
  public static final X509Certificate CA_PUB_CERT;

  public static final PrivateKey SERVER_PRIV_KEY;
  public static final PrivateKey INTERMEDIATE_PRIV_KEY;
  public static final PrivateKey CA_PRIV_KEY;

  static {
    try {
      SERVER_PUB_CERT = CertUtil.getCertFromBytes(CertConstants.SERVER_PUB);
      INTERMEDIATE_PUB_CERT = CertUtil.getCertFromBytes(CertConstants.INTERMEDIATE_PUB);
      CA_PUB_CERT = CertUtil.getCertFromBytes(CertConstants.CA_PUB);
      SERVER_PRIV_KEY = CertUtil.getPrivateKeyFromBytes(CertConstants.SERVER_PRIV);
      INTERMEDIATE_PRIV_KEY = CertUtil.getPrivateKeyFromBytes(CertConstants.INTERMEDIATE_PRIV);
      CA_PRIV_KEY = CertUtil.getPrivateKeyFromBytes(CertConstants.CA_PRIV);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private CertConstantUtil() {
  }
}
