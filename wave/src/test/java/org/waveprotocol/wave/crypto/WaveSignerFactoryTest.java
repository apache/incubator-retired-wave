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
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo.HashAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;


public class WaveSignerFactoryTest extends TestCase {

  private static final String PRIVATE_KEY =
      "-----BEGIN PRIVATE KEY-----\n" +
      "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKueRG+YuGX6Fifk\n" +
      "JpYR+Gh/qF+PpGLSYVR7CzhGNh5a8RayKwPM8YNqsfKAT8VqLdAk19x//cf03Cgc\n" +
      "UwLQsuUo3zxK4E110L96lVX6oF12FiIpSCVN+E93qin2W7VXw2JtfvQ4BllwdNMj\n" +
      "/yNPl+bHuhtOjFAPpWEhCkSJP6NlAgMBAAECgYAaRocP1wAUjO+rd+D4hRPVXAY5\n" +
      "a1Kt1qwUNSqImSdcCmxzHyA62rv3dPR9vmt4PEN7ZMiv9+CxJqo2ce+7tJxO/Xq1\n" +
      "lPTh8IVX+NUPI8LWtek9VZlXZ16nY5qXZ0i32vrwOz+GaZMfchAK05eTaiUJTN4P\n" +
      "T2Wskp6jnlDGZYeNmQJBANXMPa70jf2M6zHq0dKBg+4I3XZ1x59G0fUnho1Ck+Q5\n" +
      "ixo5GpFbbx2YgQmbFNUHhMNAJvLTduV5S3+CopqB3FMCQQDNfpUYQrmrAOvAZiQ0\n" +
      "uX/BtorjvSoTkj4g2JegaGWUVAc8As9d3VrBf8l2ovJRuzVSGqHpzke7T8wGwaGr\n" +
      "cEpnAkBFz+N0dbbHzHQgYKUTL+d8mrh2Lg95Gw8EFlwBVHQmWgPqFCtwu4KVD29T\n" +
      "S6iJx2K6vv/42sRAOlNE18tw2GaxAkBAKakGBTeR5Fy4G2xspgr1AjlFuLfdmokZ\n" +
      "mmdlp5MoCECmBT6YUVhYGL1f9KryyCBy/WvW5BjTrKvI5EbFj+87AkAobTHhq+D7\n" +
      "TOQBpaA5v45z6HNsFdCovQkQokJbirQ0KDIopo5IT7Qtz7+Gi3S0uYl3xooAsCRc\n" +
      "Zj50nIvr3txX\n" +
      "-----END PRIVATE KEY-----\n";

  private static final String CERTIFICATE =
      "-----BEGIN CERTIFICATE-----\n" +
      "MIIC9TCCAl6gAwIBAgIJALQVfb0zIz6bMA0GCSqGSIb3DQEBBQUAMFsxCzAJBgNV\n" +
      "BAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n" +
      "aWRnaXRzIFB0eSBMdGQxFDASBgNVBAMTC2V4YW1wbGUuY29tMB4XDTA5MDcxODA2\n" +
      "MjIyNloXDTEwMDcxODA2MjIyNlowWzELMAkGA1UEBhMCVVMxEzARBgNVBAgTClNv\n" +
      "bWUtU3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDEUMBIG\n" +
      "A1UEAxMLZXhhbXBsZS5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKue\n" +
      "RG+YuGX6FifkJpYR+Gh/qF+PpGLSYVR7CzhGNh5a8RayKwPM8YNqsfKAT8VqLdAk\n" +
      "19x//cf03CgcUwLQsuUo3zxK4E110L96lVX6oF12FiIpSCVN+E93qin2W7VXw2Jt\n" +
      "fvQ4BllwdNMj/yNPl+bHuhtOjFAPpWEhCkSJP6NlAgMBAAGjgcAwgb0wHQYDVR0O\n" +
      "BBYEFD2DmpOW+OiFr6U3Nu7NuDGuBSJgMIGNBgNVHSMEgYUwgYKAFD2DmpOW+OiF\n" +
      "r6U3Nu7NuDGuBSJgoV+kXTBbMQswCQYDVQQGEwJVUzETMBEGA1UECBMKU29tZS1T\n" +
      "dGF0ZTEhMB8GA1UEChMYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMRQwEgYDVQQD\n" +
      "EwtleGFtcGxlLmNvbYIJALQVfb0zIz6bMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcN\n" +
      "AQEFBQADgYEAS7H+mB7lmEihX5lOWp9ZtyI7ua7MYVK05bbuBZJLAhO1mApu5Okg\n" +
      "DqcybVV8ijPLJkII75dn+q7olpwMmgyjjsozEKY1N0It9nRsb9fW2tKGp2qlCMA4\n" +
      "zP29U9091ZRH/xL1RPVzhkRHqfNJ/x+iTC4laSLBtwlsjjkd8Us6xrg=\n" +
      "-----END CERTIFICATE-----\n";

  private static final byte[] MESSAGE = "hello".getBytes();

  private static final byte[] SIGNATURE = deBase64(
      "T4r3qxL2gWkZz7ziNe7veEzEoDdVOg+fAA+3obCO3m2DncQmDD8hkYuma" +
      "gK14895OHk+cU1T3ObTiXTb84ghmoAgY86m0SDYLl52Hb5rNakiagHqXGEnIyndkEX" +
      "X6nWc2b/opJscNFuolS9kBR6kx+DGtsK20QipU07P/1feAXU=");

  public void testGetSigner() throws Exception {

    InputStream keyStream = new ByteArrayInputStream(PRIVATE_KEY.getBytes());
    InputStream certStream = new ByteArrayInputStream(CERTIFICATE.getBytes());
    List<InputStream> certStreams = ImmutableList.of(certStream);
    String domain = "example.com";

    WaveSignerFactory factory = new WaveSignerFactory();
    WaveSigner signer = factory.getSigner(keyStream, certStreams, domain);

    ProtocolSignature signature = signer.sign(MESSAGE);

    assertTrue(Arrays.equals(SIGNATURE,
        signature.getSignatureBytes().toByteArray()));

    assertTrue(Arrays.equals(getSignerInfo().getSignerId(),
        signature.getSignerId().toByteArray()));
  }

  private SignerInfo getSignerInfo() throws Exception {
    CertificateFactory fac = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) fac.generateCertificate(
        new ByteArrayInputStream(CERTIFICATE.getBytes()));

    return new SignerInfo(HashAlgorithm.SHA256, ImmutableList.of(cert),
        "example.com");
  }

  private static byte[] deBase64(String string) {
    return Base64.decodeBase64(string.getBytes());
  }
}
