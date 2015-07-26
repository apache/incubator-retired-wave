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

package org.waveprotocol.wave.model.testing;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;


public class RandomProviderImplTest extends TestCase {

  static final int NUMBERS[] = { 0, 1, 2, 0x10000, 314159262, 0x7fffffff, 0x80000000,  0xffffffff };

  static final int ITERATIONS = 1000;

  static final double MAX_DEVIATION = 0.5;  // 50% (takes about 5K iterations to lower this to 25%)

  public void testNextInt1() {
    assertEquals(0, RandomProviderImpl.ofSeed(42).nextInt(1));
  }

  public void testNextIntThrowsIllegalArgumentException() {
    RandomProvider rp = RandomProviderImpl.ofSeed(42);
    try {
      rp.nextInt(0);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
    try {
      rp.nextInt(0x80000000);
      fail("should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
  }

  public void testNextIntDistribution() {
    for (int seed : NUMBERS) {
      for (int bound : new int[]{ 2, 3, 4, 7, 16, 33 }) {
        RandomProvider rp = RandomProviderImpl.ofSeed(seed);
        int counts[] = new int[bound];
        for (int i = 0; i < ITERATIONS; i++) {
          counts[rp.nextInt(bound)]++; // fails with index-out-of-range if nextInt goes wrong
        }
        for (int j = 0; j < bound; j++) {
          assertTrue("seed " + seed + ", bound " + bound + ", index " + j + ", count " + counts[j],
              counts[j] >= (1.0 - MAX_DEVIATION) / bound * ITERATIONS);
          assertTrue("seed " + seed + ", bound " + bound + ", index " + j + ", count " + counts[j],
              counts[j] <= (1.0 + MAX_DEVIATION) / bound * ITERATIONS);
        }
      }
    }
  }

  public void testNextBooleanDistribution() {
    for (int seed : NUMBERS) {
      RandomProvider rp = RandomProviderImpl.ofSeed(seed);
      int trueCount = 0;
      for (int i = 0; i < ITERATIONS; i++) {
        if (rp.nextBoolean()) {
          trueCount++;
        }
      }
      assertTrue("seed " + seed + ", trueCount " + trueCount,
          trueCount >= (1.0 - MAX_DEVIATION) * 0.5 * ITERATIONS);
      assertTrue("seed " + seed + ", trueCount " + trueCount,
          trueCount <= (1.0 + MAX_DEVIATION) * 0.5 * ITERATIONS);
    }
  }

}
