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
 * Tests for an id serialiser.
 *
 * @author zdwang@google.com (David Wang)
 */
public class LegacyIdSerialiserTest extends TestCase {

  private IdSerialiser serialiser;

  @Override
  public void setUp() {
    serialiser = LegacyIdSerialiser.INSTANCE;
  }

  public void testIdSerialiser() throws InvalidIdException {
    checkSerialise("example.com", "id+part", "example.com!id+part");
    // NOTE(anorth): Our code is wrong, but we won't fix this as we're moving
    // to a new escaping/serialisation scheme.
    // checkSerialise("example.com", "i~~d+part", "example.com!i~~~~d+part");
    // checkSerialise("example.com", "i!d", "example.com!i~!d");

    try {
      checkSerialise("", "id", "example.com!id");
      fail("Shouldn't be able to serialise empty domain");
    } catch (IllegalArgumentException expected) {
      // Can't serialise empty domain.
    }

    checkDeserialise("example.com!id", "example.com", "id");
    // NOTE(anorth): See above.
    // checkDeserialise("example.com!i~~~~d+part", "example.com", "i~~d+part");
    // checkDeserialise("example.com!i~!d", "example.com", "i!d");

    try {
      checkDeserialise("id", "", "");
      fail("Shouldn't be able to de-serialise empty domain");
    } catch (IllegalArgumentException ex) {
      // Expected to not be able to deserialise something without domain.
    }
  }

  private void checkSerialise(String domain, String id, String expectedSerialised) {
    String serialisedWaveId = serialiser.serialiseWaveId(WaveId.of(domain, id));
    assertEquals(expectedSerialised, serialisedWaveId);

    String serialisedWaveletId = serialiser.serialiseWaveletId(WaveletId.of(domain, id));
    assertEquals(expectedSerialised, serialisedWaveletId);
  }

  private void checkDeserialise(String toDeserialise, String expectedDomain, String expectedId)
      throws InvalidIdException {
    assertEquals(WaveId.of(expectedDomain, expectedId),
        serialiser.deserialiseWaveId(toDeserialise));

    assertEquals(WaveletId.of(expectedDomain, expectedId),
        serialiser.deserialiseWaveletId(toDeserialise));
  }
}
