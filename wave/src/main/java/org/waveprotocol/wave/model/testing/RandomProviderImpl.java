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

import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Implementation of RandomProvider for use in classes intended for use in
 * GWT, which doesn't support java.util.Random.
 *
 * The implementation is simplistic and not well tested, so don't use it for
 * any usage that depends on the quality of the generated pseudorandom numbers.
 *
 * The implementation is based on the recommendations in
 * Knuth: The Art of Computer Programming, Volume 2, Section 3.6.
 *
 */
public class RandomProviderImpl implements RandomProvider {

  public static RandomProviderImpl ofSeed(int seed) {
    return new RandomProviderImpl(seed);
  }

  private int next32;

  public RandomProviderImpl(int seed) {
    next32 = seed;
  }

  @Override
  public int nextInt(int upperBound) {
    Preconditions.checkArgument(upperBound > 0, "upperBound must be positive");

    // 0x77DD9E95 is a random number from http://www.fourmilab.ch/hotbits/
    // satisfying 0x77DD9E95 % 8 == 5
    // TODO: check if this multiplier passes the spectral test and other tests in Knuth's book
    next32 = (int) (0x77DD9E95L * (long) next32 + 1L);
    // NOTE(2010/06/08): the casts above were necessary to work around a Gwt miscompilation
    // problem, in Java a simpler expression works: next32 = 0x77DD9E95L * next32 + 1;

    // convert the signed 32 bit content into a floating point number
    // between 0 (inclusive) and 1 (exclusive)
    double d = (((double) next32) + 2147483648.0) / 4294967296.0;

    // truncate the multiplum of d and upperBound to get an integer
    // between 0 (inclusive) and upperBound (exclusive)
    return (int) (d * (double) upperBound);
  }

  @Override
  public boolean nextBoolean() {
    return nextInt(2) != 0;
  }
}
