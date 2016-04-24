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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;


public class CertpathTest extends TestCase {

  // an actual Google cert, with the Wave critical extension in it
  private static String GOOGLE_CERT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIDTzCCArigAwIBAgIKaWOAQgADAAAdfDANBgkqhkiG9w0BAQUFADBGMQswCQYD\n" +
    "VQQGEwJVUzETMBEGA1UEChMKR29vZ2xlIEluYzEiMCAGA1UEAxMZR29vZ2xlIElu\n" +
    "dGVybmV0IEF1dGhvcml0eTAeFw0xMDA5MDkwMzE4MzBaFw0xMTA5MDkwMzI4MzBa\n" +
    "MGkxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1N\n" +
    "b3VudGFpbiBWaWV3MRMwEQYDVQQKEwpHb29nbGUgSW5jMRgwFgYDVQQDEw93YXZl\n" +
    "c2FuZGJveC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANP0Aji0yGYG\n" +
    "r0z8AdW1Jv6MwN9h1mBfqG/+CHjsQDSoWvvorqoG8wt70dMQMVQWpQKXjn9o9DG4\n" +
    "TytMX9C/pQ80Z9ix66YdlwkU3HHHabA/2gODzHR746dd5Bio69UORi331OEicxd+\n" +
    "mGEqrTT59NqYuFBrvoG9cmth577dhFsjAgMBAAGjggEfMIIBGzAdBgNVHQ4EFgQU\n" +
    "ZM/I8EHEr2UQD/q+8JTQyXYgBr4wHwYDVR0jBBgwFoAUv8Aw6/VDET5nup6R+/xq\n" +
    "2uNrEiQwWwYDVR0fBFQwUjBQoE6gTIZKaHR0cDovL3d3dy5nc3RhdGljLmNvbS9H\n" +
    "b29nbGVJbnRlcm5ldEF1dGhvcml0eS9Hb29nbGVJbnRlcm5ldEF1dGhvcml0eS5j\n" +
    "cmwwZgYIKwYBBQUHAQEEWjBYMFYGCCsGAQUFBzAChkpodHRwOi8vd3d3LmdzdGF0\n" +
    "aWMuY29tL0dvb2dsZUludGVybmV0QXV0aG9yaXR5L0dvb2dsZUludGVybmV0QXV0\n" +
    "aG9yaXR5LmNydDAUBgorBgEEAdZ5AgEBAQH/BAMCAQEwDQYJKoZIhvcNAQEFBQAD\n" +
    "gYEAKwhIj+BxnEt5hopwX7to+1VxJc5NPOJZxlfH64Tn/uPFiWsq8E46rD+HCs19\n" +
    "gmZOKbZlU2vGyqC73JNIIULhzY/3ykUb8Lg285hst3c1jMdHhMP+2uI7+4AL8Oul\n" +
    "SNPNsyMZkehIiMHKEELlpvnkxLuB5pY2e+qszrawWwx61gg=\n" +
    "-----END CERTIFICATE-----";

  // the actual Google intermediate cert
  private static String INTERMEDIATE_CERT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIICsDCCAhmgAwIBAgIDC2dxMA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVT\n" +
    "MRAwDgYDVQQKEwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0\n" +
    "aWZpY2F0ZSBBdXRob3JpdHkwHhcNMDkwNjA4MjA0MzI3WhcNMTMwNjA3MTk0MzI3\n" +
    "WjBGMQswCQYDVQQGEwJVUzETMBEGA1UEChMKR29vZ2xlIEluYzEiMCAGA1UEAxMZ\n" +
    "R29vZ2xlIEludGVybmV0IEF1dGhvcml0eTCBnzANBgkqhkiG9w0BAQEFAAOBjQAw\n" +
    "gYkCgYEAye23pIucV+eEPkB9hPSP0XFjU5nneXQUr0SZMyCSjXvlKAy6rWxJfoNf\n" +
    "NFlOCnowzdDXxFdF7dWq1nMmzq0yE7jXDx07393cCDaob1FEm8rWIFJztyaHNWrb\n" +
    "qeXUWaUr/GcZOfqTGBhs3t0lig4zFEfC7wFQeeT9adGnwKziV28CAwEAAaOBozCB\n" +
    "oDAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFL/AMOv1QxE+Z7qekfv8atrjaxIk\n" +
    "MB8GA1UdIwQYMBaAFEjmaPkr0rKV10fYIyAQTzOYkJ/UMBIGA1UdEwEB/wQIMAYB\n" +
    "Af8CAQAwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL2NybC5nZW90cnVzdC5jb20v\n" +
    "Y3Jscy9zZWN1cmVjYS5jcmwwDQYJKoZIhvcNAQEFBQADgYEAuIojxkiWsRF8YHde\n" +
    "BZqrocb6ghwYB8TrgbCoZutJqOkM0ymt9e8kTP3kS8p/XmOrmSfLnzYhLLkQYGfN\n" +
    "0rTw8Ktx5YtaiScRhKqOv5nwnQkhClIZmloJ0pC3+gz4fniisIWvXEyZ2VxVKfml\n" +
    "UUIuOss4jHg7y/j7lYe8vJD5UDI=\n" +
    "-----END CERTIFICATE-----";

  private CachedCertPathValidator validator;

  private List<X509Certificate> certs;

  @Override
  protected void setUp() throws Exception {
    TimeSource time = new DefaultTimeSource();
    VerifiedCertChainCache cache = new DefaultCacheImpl(time);
    validator = new CachedCertPathValidator(cache, time, new DefaultTrustRootsProvider());

    CertificateFactory fac = CertificateFactory.getInstance("X509");
    X509Certificate ourCert =
        (X509Certificate) fac.generateCertificate(
            new ByteArrayInputStream(GOOGLE_CERT.getBytes()));
    X509Certificate intermediateCert =
        (X509Certificate) fac.generateCertificate(
            new ByteArrayInputStream(INTERMEDIATE_CERT.getBytes()));

    certs = ImmutableList.of(ourCert, intermediateCert);
  }

 public void testValidator_canGrokCriticalExtension() throws Exception {
    // TODO (user) Enable back this test after fixing certificate.
    // validator.validate(certs);
  }
}
