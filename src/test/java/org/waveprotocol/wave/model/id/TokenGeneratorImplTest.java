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

import org.waveprotocol.wave.model.util.CharBase64;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Unit tests for {@link TokenGeneratorImpl}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author soren@google.com (Soren Lassen)
 */

public class TokenGeneratorImplTest extends TestCase {

  private TokenGenerator generator;

  @Override
  protected void setUp() throws Exception {
    this.generator = new TokenGeneratorImpl(new Random());
  }

  public void testLengths() {
    for (int i = 1; i < 100; i++) {
      assertEquals(i, generator.generateToken(i).length());
    }
  }

  public void testUniqueness() {
    Set<String> set = new HashSet<String>();
    for (int i = 1; i < 10; i++) {
      set.clear();
      for (int j = 1; j < 100; j++) {
        set.add(generator.generateToken(i * 4));
      }
      // This is pretty weak, but tests must be almost rock
      // solid to be useful.
      assertTrue("Improbable 10 collisions", set.size() > (100 - 4));
    }
  }

  public void testCharacterSpreadInLongId() {
    final int length = 40000;
    final double variance = 0.2; // % difference
    int[] histogram = new int[256];
    String id = generator.generateToken(length);
    for (byte b : id.getBytes()) {
      histogram[b + 128]++;
    }
    verifyBase64Spread(histogram, id.length(), variance);
  }

  public void testCharacterSpreadAtFixedCharPosition() {
    final int count = 10000;
    final double variance = 0.3; // % difference

    // Pick a fixed id length and a fixed char position therein.
    final int length = 4 + (int) (Math.random() * 16); // between 4 and 20 chars
    final int index = (int) (Math.random() * length); // index into id strings
    int[] histogram = new int[256];
    for (int i = 0; i < count; i++) {
      String id = generator.generateToken(length);
      byte b = id.getBytes()[index]; // char at fixed position
      histogram[b + 128]++;
    }
    verifyBase64Spread(histogram, count, variance);
  }

  /**
   * Verifies bounds of the frequency spread in a base64 histogram.
   *
   * @param histogram Maps the WEBSAFE_ALPHABET values to frequencies.
   * @param count Sum of the frequencies.
   * @param variance Maximum permitted variance.
   */
  private void verifyBase64Spread(int[] histogram, int count, double variance) {
    double average = count / 64.0;
    int accumulator = 0;
    for (char c : CharBase64.WEBSAFE_ALPHABET) {
      byte b = (byte) c;
      int frequency = histogram[b + 128];
      accumulator += frequency;
      String msg = "Char " + c + ", frequency " + frequency;
      assertTrue(msg, average * (1 - variance) < frequency);
      assertTrue(msg, average * (1 + variance) > frequency);
    }
    assertEquals(count, accumulator);
  }
}
