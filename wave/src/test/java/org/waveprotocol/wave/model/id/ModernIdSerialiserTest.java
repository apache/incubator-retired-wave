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
 * Tests for modern wave[let] id/name serialisation.
 *
 * @author anorth@google.com (Alex North)
 */
public class ModernIdSerialiserTest extends TestCase {

  public void testValidIdsSerialiseAndDeserialise() throws InvalidIdException {
    expectSerialisation("example.com/id", "example.com", "id");
    expectSerialisation("example.com/prefix+user@example.com", "example.com",
        "prefix+user@example.com");
    expectSerialisation("example.com/some~2Fid", "example.com", "some~2Fid");
    expectSerialisation("example.com/prof+j\u00F6rg@t\u016Bdali\u0146.lv", "example.com",
        "prof+j\u00F6rg@t\u016Bdali\u0146.lv");
    // TODO(anorth): Test international domain when WaveId allows it.
  }

  public void testInvalidIdsFailDeserialisation() {
    expectFailedDeserialisation("example.com");
    expectFailedDeserialisation("example.com/");
    expectFailedDeserialisation("example.com//");
    expectFailedDeserialisation("/id");
    expectFailedDeserialisation("/example.com/id");
    expectFailedDeserialisation("example.com/waveid/waveletid");
    expectFailedDeserialisation("example.com/1/example.com/3/");
    expectFailedDeserialisation("example.com/id/example.com/id");
  }

  public void testValidNameSerialisation() throws InvalidIdException {
    expectSerialisation("example.com/waveid/example2.com/waveletid",
        "example.com", "waveid", "example2.com", "waveletid");
    expectSerialisation("example.com/waveid/~/waveletid",
        "example.com", "waveid", "example.com", "waveletid");
    expectSerialisation("example.com/equal/example2.com/equal",
        "example.com", "equal", "example2.com", "equal");
  }

  private static void expectSerialisation(String serialised, String domain, String id)
      throws InvalidIdException {
    WaveId waveId = WaveId.of(domain, id);
    WaveletId waveletId = WaveletId.of(domain, id);
    assertEquals(serialised, ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId));
    assertEquals(serialised, ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId));

    assertEquals(waveId, ModernIdSerialiser.INSTANCE.deserialiseWaveId(serialised));
    assertEquals(waveletId, ModernIdSerialiser.INSTANCE.deserialiseWaveletId(serialised));
  }

  private static void expectSerialisation(String serialised, String waveDomain, String waveId,
      String waveletDomain, String waveletId) throws InvalidIdException {
    WaveletName name =
        WaveletName.of(WaveId.of(waveDomain, waveId), WaveletId.of(waveletDomain, waveletId));
    assertEquals(serialised, ModernIdSerialiser.INSTANCE.serialiseWaveletName(name));
    assertEquals(name, ModernIdSerialiser.INSTANCE.deserialiseWaveletName(serialised));
  }

  private void expectFailedDeserialisation(String string) {
    try {
      ModernIdSerialiser.INSTANCE.deserialiseWaveId(string);
      fail("Expected " + string + " to fail deserialisation as wave id");
    } catch (InvalidIdException expected) {
    }
    try {
      ModernIdSerialiser.INSTANCE.deserialiseWaveletId(string);
      fail("Expected " + string + " to fail deserialisation as wavelet id");
    } catch (InvalidIdException expected) {
    }
    try {
      ModernIdSerialiser.INSTANCE.deserialiseWaveletName(string);
      fail("Expected " + string + " to fail deserialisation as wavelet name");
    } catch (InvalidIdException expected) {
    }
  }
}
