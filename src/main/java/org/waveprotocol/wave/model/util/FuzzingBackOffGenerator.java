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

package org.waveprotocol.wave.model.util;


/**
 * Simiple fibonacci back off with fuzzing. This is important for reconnection so that
 * everyone doesn't retry at the same time.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FuzzingBackOffGenerator {
  public static class BackOffParameters {
    public final int targetDelay;
    public final int minimumDelay;

    private BackOffParameters(int targetDelay, int minimumDelay) {
      this.targetDelay = targetDelay;
      this.minimumDelay = minimumDelay;
    }
  }

  /** Randomisation factor. Must be between 0 and 1. */
  private final double randomisationFactor;

  /** The first time we back off. */
  private final int initialBackOff;

  /** The max back off value, it'll be fuzzed. */
  private final int maxBackOff;

  /** The next time we've backed off. */
  private int nextBackOffTime;

  /** The current back off time. */
  private int backOffTime;

  /**
   * @param initialBackOff Initial value to back off. This class does not interpret the meaning of
   *    this value. must be > 0
   * @param maxBackOff Max value to back off
   * @param randomisationFactor between 0 and 1 to control the range of randomness.
   */
  public FuzzingBackOffGenerator(int initialBackOff, int maxBackOff, double randomisationFactor) {
    if (randomisationFactor < 0 || randomisationFactor > 1) {
      throw new IllegalArgumentException("randomisationFactor must be between 0 and 1. actual " +
          randomisationFactor);
    }

    if (initialBackOff <= 0) {
      throw new IllegalArgumentException("initialBackOff must be between 0 and 1. actual " +
          initialBackOff);
    }

    this.randomisationFactor = randomisationFactor;
    this.initialBackOff = initialBackOff;
    this.maxBackOff = maxBackOff;
    this.nextBackOffTime = initialBackOff;
    this.backOffTime = 0;
  }

  /** Gets the next back off time. Until maxBackOff is reached. */
  public BackOffParameters next() {
    int ret = Math.min(nextBackOffTime, maxBackOff);
    nextBackOffTime += backOffTime;
    backOffTime = ret;

    int randomizeTime = (int) (backOffTime * (1.0 + (Math.random() * randomisationFactor)));
    int minAllowedTime = (int)Math.round(randomizeTime - backOffTime * randomisationFactor);

    return new BackOffParameters(randomizeTime, minAllowedTime);
  }

  /**
   * Resets the back off.
   */
  public void reset() {
    nextBackOffTime = initialBackOff;
    backOffTime = 0;
  }
}
