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

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * By default, we use the same CA roots that Java's SSL implementation uses.
 */
public class DefaultTrustRootsProvider implements TrustRootsProvider {

  // startcom.org provides free certificates, but is not included in the
  // default set of CAs shipped with the JDK. We'll include it in the set of
  // default CAs we trust.
  private static final String STARTCOM_CA =
      "-----BEGIN CERTIFICATE-----\n" +
      "MIIHyTCCBbGgAwIBAgIBATANBgkqhkiG9w0BAQUFADB9MQswCQYDVQQGEwJJTDEW\n" +
      "MBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0YWwg\n" +
      "Q2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3RhcnRDb20gQ2VydGlmaWNh\n" +
      "dGlvbiBBdXRob3JpdHkwHhcNMDYwOTE3MTk0NjM2WhcNMzYwOTE3MTk0NjM2WjB9\n" +
      "MQswCQYDVQQGEwJJTDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMi\n" +
      "U2VjdXJlIERpZ2l0YWwgQ2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3Rh\n" +
      "cnRDb20gQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwggIiMA0GCSqGSIb3DQEBAQUA\n" +
      "A4ICDwAwggIKAoICAQDBiNsJvGxGfHiflXu1M5DycmLWwTYgIiRezul38kMKogZk\n" +
      "pMyONvg45iPwbm2xPN1yo4UcodM9tDMr0y+v/uqwQVlntsQGfQqedIXWeUyAN3rf\n" +
      "OQVSWff0G0ZDpNKFhdLDcfN1YjS6LIp/Ho/u7TTQEceWzVI9ujPW3U3eCztKS5/C\n" +
      "Ji/6tRYccjV3yjxd5srhJosaNnZcAdt0FCX+7bWgiA/deMotHweXMAEtcnn6RtYT\n" +
      "Kqi5pquDSR3l8u/d5AGOGAqPY1MWhWKpDhk6zLVmpsJrdAfkK+F2PrRt2PZE4XNi\n" +
      "HzvEvqBTViVsUQn3qqvKv3b9bZvzndu/PWa8DFaqr5hIlTpL36dYUNk4dalb6kMM\n" +
      "Av+Z6+hsTXBbKWWc3apdzK8BMewM69KN6Oqce+Zu9ydmDBpI125C4z/eIT574Q1w\n" +
      "+2OqqGwaVLRcJXrJosmLFqa7LH4XXgVNWG4SHQHuEhANxjJ/GP/89PrNbpHoNkm+\n" +
      "Gkhpi8KWTRoSsmkXwQqQ1vp5Iki/untp+HDH+no32NgN0nZPV/+Qt+OR0t3vwmC3\n" +
      "Zzrd/qqc8NSLf3Iizsafl7b4r4qgEKjZ+xjGtrVcUjyJthkqcwEKDwOzEmDyei+B\n" +
      "26Nu/yYwl/WL3YlXtq09s68rxbd2AvCl1iuahhQqcvbjM4xdCUsT37uMdBNSSwID\n" +
      "AQABo4ICUjCCAk4wDAYDVR0TBAUwAwEB/zALBgNVHQ8EBAMCAa4wHQYDVR0OBBYE\n" +
      "FE4L7xqkQFulF2mHMMo0aEPQQa7yMGQGA1UdHwRdMFswLKAqoCiGJmh0dHA6Ly9j\n" +
      "ZXJ0LnN0YXJ0Y29tLm9yZy9zZnNjYS1jcmwuY3JsMCugKaAnhiVodHRwOi8vY3Js\n" +
      "LnN0YXJ0Y29tLm9yZy9zZnNjYS1jcmwuY3JsMIIBXQYDVR0gBIIBVDCCAVAwggFM\n" +
      "BgsrBgEEAYG1NwEBATCCATswLwYIKwYBBQUHAgEWI2h0dHA6Ly9jZXJ0LnN0YXJ0\n" +
      "Y29tLm9yZy9wb2xpY3kucGRmMDUGCCsGAQUFBwIBFilodHRwOi8vY2VydC5zdGFy\n" +
      "dGNvbS5vcmcvaW50ZXJtZWRpYXRlLnBkZjCB0AYIKwYBBQUHAgIwgcMwJxYgU3Rh\n" +
      "cnQgQ29tbWVyY2lhbCAoU3RhcnRDb20pIEx0ZC4wAwIBARqBl0xpbWl0ZWQgTGlh\n" +
      "YmlsaXR5LCByZWFkIHRoZSBzZWN0aW9uICpMZWdhbCBMaW1pdGF0aW9ucyogb2Yg\n" +
      "dGhlIFN0YXJ0Q29tIENlcnRpZmljYXRpb24gQXV0aG9yaXR5IFBvbGljeSBhdmFp\n" +
      "bGFibGUgYXQgaHR0cDovL2NlcnQuc3RhcnRjb20ub3JnL3BvbGljeS5wZGYwEQYJ\n" +
      "YIZIAYb4QgEBBAQDAgAHMDgGCWCGSAGG+EIBDQQrFilTdGFydENvbSBGcmVlIFNT\n" +
      "TCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTANBgkqhkiG9w0BAQUFAAOCAgEAFmyZ\n" +
      "9GYMNPXQhV59CuzaEE44HF7fpiUFS5Eyweg78T3dRAlbB0mKKctmArexmvclmAk8\n" +
      "jhvh3TaHK0u7aNM5Zj2gJsfyOZEdUauCe37Vzlrk4gNXcGmXCPleWKYK34wGmkUW\n" +
      "FjgKXlf2Ysd6AgXmvB618p70qSmD+LIU424oh0TDkBreOKk8rENNZEXO3SipXPJz\n" +
      "ewT4F+irsfMuXGRuczE6Eri8sxHkfY+BUZo7jYn0TZNmezwD7dOaHZrzZVD1oNB1\n" +
      "ny+v8OqCQ5j4aZyJecRDjkZy42Q2Eq/3JR44iZB3fsNrarnDy0RLrHiQi+fHLB5L\n" +
      "EUTINFInzQpdn4XBidUaePKVEFMy3YCEZnXZtWgo+2EuvoSoOMCZEoalHmdkrQYu\n" +
      "L6lwhceWD3yJZfWOQ1QOq92lgDmUYMA0yZZwLKMS9R9Ie70cfmu3nZD0Ijuu+Pwq\n" +
      "yvqCUqDvr0tVk+vBtfAii6w0TiYiBKGHLHVKt+V9E9e4DGTANtLJL4YSjCMJwRuC\n" +
      "O3NJo2pXh5Tl1njFmUNj403gdy3hZZlyaQQaRwnmDwFWJPsfvw55qVguucQJAX6V\n" +
      "um0ABj6y6koQOdjQK/W/7HW/lwLFCRsI3FU34oH7N4RDYiDK51ZLZer+bMEkkySh\n" +
      "NOsF/5oirpt9P/FlUQqmMGqz9IgcgA38corog14=\n" +
      "-----END CERTIFICATE-----\n";

  // startcom.org uses a different CA when issuing certificates through
  // xmpp.net for the XMPP community. This CA is also not installed as part of
  // the standard Java JDK.
  private final static String STARTCOM_XMPP_CA =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIFFjCCBH+gAwIBAgIBADANBgkqhkiG9w0BAQQFADCBsDELMAkGA1UEBhMCSUwx\n" +
    "DzANBgNVBAgTBklzcmFlbDEOMAwGA1UEBxMFRWlsYXQxFjAUBgNVBAoTDVN0YXJ0\n" +
    "Q29tIEx0ZC4xGjAYBgNVBAsTEUNBIEF1dGhvcml0eSBEZXAuMSkwJwYDVQQDEyBG\n" +
    "cmVlIFNTTCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJARYS\n" +
    "YWRtaW5Ac3RhcnRjb20ub3JnMB4XDTA1MDMxNzE3Mzc0OFoXDTM1MDMxMDE3Mzc0\n" +
    "OFowgbAxCzAJBgNVBAYTAklMMQ8wDQYDVQQIEwZJc3JhZWwxDjAMBgNVBAcTBUVp\n" +
    "bGF0MRYwFAYDVQQKEw1TdGFydENvbSBMdGQuMRowGAYDVQQLExFDQSBBdXRob3Jp\n" +
    "dHkgRGVwLjEpMCcGA1UEAxMgRnJlZSBTU0wgQ2VydGlmaWNhdGlvbiBBdXRob3Jp\n" +
    "dHkxITAfBgkqhkiG9w0BCQEWEmFkbWluQHN0YXJ0Y29tLm9yZzCBnzANBgkqhkiG\n" +
    "9w0BAQEFAAOBjQAwgYkCgYEA7YRgACOeyEpRKSfeOqE5tWmrCbIvNP1h3D3TsM+x\n" +
    "18LEwrHkllbEvqoUDufMOlDIOmKdw6OsWXuO7lUaHEe+o5c5s7XvIywI6Nivcy+5\n" +
    "yYPo7QAPyHWlLzRMGOh2iCNJitu27Wjaw7ViKUylS7eYtAkUEKD4/mJ2IhULpNYI\n" +
    "LzUCAwEAAaOCAjwwggI4MA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgHmMB0G\n" +
    "A1UdDgQWBBQcicOWzL3+MtUNjIExtpidjShkjTCB3QYDVR0jBIHVMIHSgBQcicOW\n" +
    "zL3+MtUNjIExtpidjShkjaGBtqSBszCBsDELMAkGA1UEBhMCSUwxDzANBgNVBAgT\n" +
    "BklzcmFlbDEOMAwGA1UEBxMFRWlsYXQxFjAUBgNVBAoTDVN0YXJ0Q29tIEx0ZC4x\n" +
    "GjAYBgNVBAsTEUNBIEF1dGhvcml0eSBEZXAuMSkwJwYDVQQDEyBGcmVlIFNTTCBD\n" +
    "ZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJARYSYWRtaW5Ac3Rh\n" +
    "cnRjb20ub3JnggEAMB0GA1UdEQQWMBSBEmFkbWluQHN0YXJ0Y29tLm9yZzAdBgNV\n" +
    "HRIEFjAUgRJhZG1pbkBzdGFydGNvbS5vcmcwEQYJYIZIAYb4QgEBBAQDAgAHMC8G\n" +
    "CWCGSAGG+EIBDQQiFiBGcmVlIFNTTCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAy\n" +
    "BglghkgBhvhCAQQEJRYjaHR0cDovL2NlcnQuc3RhcnRjb20ub3JnL2NhLWNybC5j\n" +
    "cmwwKAYJYIZIAYb4QgECBBsWGWh0dHA6Ly9jZXJ0LnN0YXJ0Y29tLm9yZy8wOQYJ\n" +
    "YIZIAYb4QgEIBCwWKmh0dHA6Ly9jZXJ0LnN0YXJ0Y29tLm9yZy9pbmRleC5waHA/\n" +
    "YXBwPTExMTANBgkqhkiG9w0BAQQFAAOBgQBscSXhnjSRIe/bbL0BCFaPiNhBOlP1\n" +
    "ct8nV0t2hPdopP7rPwl+KLhX6h/BquL/lp9JmeaylXOWxkjHXo0Hclb4g4+fd68p\n" +
    "00UOpO6wNnQt8M2YI3s3S9r+UZjEHjQ8iP2ZO1CnwYszx8JSFhKVU2Ui77qLzmLb\n" +
    "cCOxgN8aIDjnfg==\n" +
    "-----END CERTIFICATE-----\n";

  private final List<X509Certificate> roots;

  public DefaultTrustRootsProvider() {

    TrustManagerFactory factory;
    try {
      factory = TrustManagerFactory.getInstance("X509");

      // null keystore means we load the keystore defined through
      // -Djavax.net.ssl.trustStore, or if that is not set, a keystore on a
      // default path
      factory.init((KeyStore) null);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    } catch (KeyStoreException e) {
      throw new IllegalStateException(e);
    }
    TrustManager[] managers = factory.getTrustManagers();

    // there is only one trust manager for the SSL root certs
    X509TrustManager manager = (X509TrustManager) managers[0];

    this.roots = Lists.newArrayList(manager.getAcceptedIssuers());

    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      this.roots.add((X509Certificate) certFactory.generateCertificate(
          new ByteArrayInputStream(STARTCOM_CA.getBytes())));
      this.roots.add((X509Certificate) certFactory.generateCertificate(
          new ByteArrayInputStream(STARTCOM_XMPP_CA.getBytes())));
    } catch (CertificateException e) {
      throw new IllegalStateException(e);
    }
  }

  public Collection<X509Certificate> getTrustRoots() {
    return roots;
  }
}
