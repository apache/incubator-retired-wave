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

package org.waveprotocol.wave.model.id;


import junit.framework.TestCase;

/**
 * Tests for wave and wavelet ids.
 *
 * @author anorth@google.com (Alex North)
 */

public class WaveAndWaveletIdTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testEmptyDomainRejected() {
    assertIdRejected(null, "id");
    assertIdRejected("", "id");
  }

  public void testEmptyIdRejected() {
    assertIdRejected("example.com", null);
    assertIdRejected("example.com", "");
  }

  public void testSeparatorIsNotAllowedInDomainOrId() {
    String illegal = "id" + LegacyIdSerialiser.PART_SEPARATOR + "suffix";
    assertIdRejected(illegal, "id");
    assertIdRejected("example.com", illegal);
  }

  // Escaping is tested more in IdSerialiserTest.

  public void testWaveIdCompareDiffDomain() {
    assertIdsCompare("google.com", "efg", "yahoo.com", "abc");
  }

  public void testWaveIdCompareSameDomain() {
    assertIdsCompare("google.com", "abc", "google.com", "efg");
  }

  public void testWaveIdEquals() {
    String domain = "example.com";
    String id = "efg";
    {
      WaveId idA = WaveId.of(domain, id);
      WaveId idB = WaveId.of(domain, id);

      assertEquals(0, idA.compareTo(idB));
      assertEquals(0, idB.compareTo(idA));

      assertEquals(idA, idA);
      assertEquals(idB, idB);
      assertEquals(idA, idB);
      assertEquals(idA.hashCode(), idB.hashCode());
    }
    {
      WaveletId idA = WaveletId.of(domain, id);
      WaveletId idB = WaveletId.of(domain, id);

      assertEquals(0, idA.compareTo(idB));
      assertEquals(0, idB.compareTo(idA));

      assertEquals(idA, idA);
      assertEquals(idB, idB);
      assertEquals(idA, idB);
      assertEquals(idA.hashCode(), idB.hashCode());
    }
  }

  /**
   * Checks that neither a wave or wavelet id may be constructed
   * with a domain and id.
   */
  private static void assertIdRejected(String domain, String id) {
    try {
      WaveId.of(domain, id);
      fail("Expected wave id construction to throw an exception");
    } catch (NullPointerException expected) {
    } catch (IllegalArgumentException expected) {
    }

    try {
      WaveletId.of(domain, id);
      fail("Expected wavelet id construction to throw an exception");
    } catch (NullPointerException expected) {
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * Checks that a pair of wave[let] ids with specified domain and id
   * compare such that A &lt; B.
   */
  private static void assertIdsCompare(String domainA, String idA, String domainB, String idB) {
    {
      WaveId a = WaveId.of(domainA, idA);
      WaveId b = WaveId.of(domainB, idB);

      assertTrue(a.compareTo(b) < 0);
      assertTrue(b.compareTo(a) > 0);
      assertFalse(a.equals(b));
    }

    {
      WaveletId a = WaveletId.of(domainA, idA);
      WaveletId b = WaveletId.of(domainB, idB);

      assertTrue(a.compareTo(b) < 0);
      assertTrue(b.compareTo(a) > 0);
      assertFalse(a.equals(b));
    }
  }
}
