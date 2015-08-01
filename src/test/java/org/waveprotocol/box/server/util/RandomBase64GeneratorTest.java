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

package org.waveprotocol.box.server.util;

import junit.framework.TestCase;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;

/**
 * Tests {@link RandomBase64Generator}.
 *
 *
 */
public class RandomBase64GeneratorTest extends TestCase {

  private final RandomBase64Generator[] generators = {
    new RandomBase64Generator(new Random()),
    new RandomBase64Generator(new SecureRandom())
  };

  public void testBase64IdLengths() {
    for (RandomBase64Generator generator : generators)
      for (int i = 0; i < 100; i++)
        assertEquals(i, generator.next(i).length());
  }

  public void testUniqueness() {
    for (RandomBase64Generator generator : generators) {
      HashSet<String> set = new HashSet<String>();
      // Len starts at 4 because collisions in really small strings are
      // actually likely.
      for (int len = 4; len < 10; len++) {
        // Expect at most 1 collision among (2^i)+1 pseudo-random strings
        // of length i
        set.clear();
        int collisions = 0;
        for (int j = 0; j < (1 << len) + 1; j++) {
          if (!set.add(generator.next(len))) collisions++;
        }
        assertTrue("improbable " + collisions + " collisions in set of " + set.size(),
            collisions <= 1);
      }
    }
  }

  public void testBase64CharacterSpreadInLongId() {
    final int chars = 40000;
    final double variance = 0.2;  // % difference
    for (RandomBase64Generator generator : generators) {
      int[] histogram = new int[256];
      String id = generator.next(chars);
      for (byte b : id.getBytes())
        histogram[b + 128]++;
      verifyBase64Spread(histogram, id.length(), variance);
    }
  }

  public void testBase64IdCharacterSpreadAtFixedCharPosition() {
    final int count = 40000;
    final double variance = 0.3;  // % difference
    // Pick a fixed id length (in quads) and a fixed char position therein.
    final int chars = 4 + (int) (Math.random() * 10);      // between 4 and 14 characters
    final int index = (int) (Math.random() * chars);  // index into id strings
    for (RandomBase64Generator generator : generators) {
      int[] histogram = new int[256];
      for (int i = 0; i < count; i++) {
        String id = generator.next(chars);
        byte b = id.getBytes()[index];  // char at fixed position
        histogram[b + 128]++;
      }
      verifyBase64Spread(histogram, count, variance);
    }
  }

  /**
   * Verifies bounds of the frequency spread in a base64 histogram.
   * @param histogram Maps the WEB64_ALPHABET values to frequencies.
   * @param count Sum of the frequencies.
   * @param variance Maximum permitted variance.
   */
  private void verifyBase64Spread(int[] histogram, int count, double variance) {
    double average = count / 64.0;
    int accumulator = 0;
    for (byte b : String.valueOf(RandomBase64Generator.WEB64_ALPHABET).getBytes()) {
      int frequency = histogram[b + 128];
      accumulator += frequency;
      String msg = "Char " + Character.toChars(b).toString() + ", frequency " + frequency
          + ", average " + average + ", count " + count;
      assertTrue(msg, average * (1 - variance) < frequency);
      assertTrue(msg, average * (1 + variance) > frequency);
    }
    assertEquals(count, accumulator);
  }
}
