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

package org.waveprotocol.wave.model.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveIdentifiers;

/**
 * Test cases for the {@link ParticipantIdUtil} class.
 *
 */

public class ParticipantIdUtilTest extends TestCase {
  public void testIsValidDomain() {
    assertTrue(WaveIdentifiers.isValidDomain(0, "google.com"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "a.gwave.com"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "googlewave.com"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "google.org"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "google.co.uk"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "a-1-2-3.com"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "my-domain.com"));
    assertTrue(WaveIdentifiers.isValidDomain(0, "sd8fud.a0s8df7as.mil"));
    // NOTE(user): Not valid according to RFC1035 but it exists.
    assertTrue(WaveIdentifiers.isValidDomain(0, "76.com"));

    assertFalse(WaveIdentifiers.isValidDomain(0, ""));
    assertFalse(WaveIdentifiers.isValidDomain(0, "google..com"));
    assertFalse(WaveIdentifiers.isValidDomain(0, "dom*ain.com"));
    assertFalse(WaveIdentifiers.isValidDomain(0, "trailing-.dash.com"));
    assertFalse(WaveIdentifiers.isValidDomain(0, "trailing.dash.com-"));
    assertFalse(WaveIdentifiers.isValidDomain(0, "google.com."));

    // The long domain case. Check that 253 is fine, but 254 fails.
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 249; ++i) {
      builder.append('a');
    }
    String longDomain = builder.append(".com").toString();
    assertEquals(253, longDomain.length());
    assertTrue(WaveIdentifiers.isValidDomain(0, longDomain));
    assertFalse(WaveIdentifiers.isValidDomain(0, "a" + longDomain));
  }

  public void testIsNormalizedAddress() {
    assertTrue(ParticipantIdUtil.isNormalizedAddress("a@b.c"));
    // Should probably be false.
    assertTrue(ParticipantIdUtil.isNormalizedAddress("a@b.123"));
    // Multi-level domains are not a must.
    assertTrue(ParticipantIdUtil.isNormalizedAddress("a@b"));
    assertTrue(ParticipantIdUtil.isNormalizedAddress("a.b@b"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("@b.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress(":@)."));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("A@b.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@B.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@b.C"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("A@B.C"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("@"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("@b"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("@a@b.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@@b.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@b@.c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@b.@c"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("a@b.c@"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress(""));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("123"));

    assertTrue(ParticipantIdUtil.isNormalizedAddress("foo@bar.com"));
    assertTrue(ParticipantIdUtil.isNormalizedAddress("foo@a.gwave.com"));

    assertFalse(ParticipantIdUtil.isNormalizedAddress("foo.a.gwave.com"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("you@here@google.com"));
    assertFalse(ParticipantIdUtil.isNormalizedAddress("NOT@normalized.COM"));
  }

  public void testValidDomainAddress() {
    assertTrue(ParticipantIdUtil.isDomainAddress("@example.com"));
  }

  public void testValidDomainAddressWithSubdomain() {
    assertTrue(ParticipantIdUtil.isDomainAddress("@subdomain.example.com"));
  }

  public void testInvalidDomainAddressWithInvalidDomain() {
    assertFalse(ParticipantIdUtil.isDomainAddress("account@example@weird.com"));
  }

  public void testNormalAddressIsNotDomainAddress() {
    assertFalse(ParticipantIdUtil.isDomainAddress("account@example.com"));
  }

  public void testMakeDomainAddress() {
    assertEquals("@example.com", ParticipantIdUtil.makeDomainAddress("example.com"));
  }

  public void testMakeDomainAddressFailsWithInvalidDomain() {
    try {
      ParticipantIdUtil.makeDomainAddress("@example.com");
      fail();
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

}
