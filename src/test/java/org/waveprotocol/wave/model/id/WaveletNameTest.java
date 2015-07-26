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
 * Tests for wavelet name.
 *
 */

public class WaveletNameTest extends TestCase {

  public void testNullPartsAreRejected() {
    try {
      WaveletName.of(null, WaveletId.of("example.com", "id"));
      fail("Expected NPE from wavelet name with null wave id");
    } catch (NullPointerException expected) {
    }

    try {
      WaveletName.of(WaveId.of("example.com", "id"), null);
      fail("Expected NPE from wavelet name with null wavelet id");
    } catch (NullPointerException expected) {
    }
  }

  public void testOfToString() throws Exception {
    final WaveId waveId = WaveId.of("example.com", "w+abcd1234");
    final WaveletId waveletId = WaveletId.of("acmewave.com", "conv+blah");
    WaveletName name = WaveletName.of(waveId, waveletId);
    String expected = "[WaveletName example.com/w+abcd1234/acmewave.com/conv+blah]";
    assertEquals(expected, name.toString());
  }
}
