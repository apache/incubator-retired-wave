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

package org.waveprotocol.communication;

import static org.waveprotocol.wave.communication.Codec.decode;
import static org.waveprotocol.wave.communication.Codec.encode;

import junit.framework.TestCase;

import java.util.Random;

/**
 * Tests the byte array to/from char array converter.
 */
public final class CodecTest extends TestCase {

  private static void assertEquals(byte[] expected, byte[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }

  public void testExpectedKindOfArray() {
    byte[] data = new byte[32];
    new Random(23).nextBytes(data);
    assertEquals(data, decode(encode(data)));
  }

  public void testEmptyArray() {
    byte[] data = {};
    assertEquals(data, decode(encode(data)));
  }

  public void testArrayWithZeroesNegativesAndExtremals() {
    byte[] data = {0, 0, 1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE, 0};
    assertEquals(data, decode(encode(data)));
  }

  public void testDecodeOfBadStringFails() {
    try {
      decode("Not hex");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
