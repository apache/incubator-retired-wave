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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.FuzzingBackOffGenerator.BackOffParameters;

/**
 * Test for FuzzingBackOffGenerator.
 *
 * @author zdwang@google.com (David Wang)
 */

public class FuzzingBackOffGeneratorTest extends TestCase {

  public void testSimple() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2, 1000, 0.5);

    // this number is fibonacci
    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 3);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);

    next = generator.next();
    expectRange(next.targetDelay, 6, 9);
    expectRange(next.minimumDelay, 3, 6);

    for (int i = 0; i < 100; i++) {
     generator.next();
    }
    next = generator.next();
    expectRange(next.targetDelay, 1000, 1500);
    expectRange(next.minimumDelay, 500, 1000);
  }

  private void expectRange(int value, int min, int max) {
    assertTrue("expected " + min + " <= (value) " + value, min <= value);
    assertTrue("expected " + max + " >= (value) " + value, value <= max);
  }

  public void testIllegalArgument() {
    try {
      new FuzzingBackOffGenerator(2, 1000, 2);
      fail("Should not be able to create FuzzingBackOffGenerator with bad randomisationFactor");
    } catch (IllegalArgumentException ex) {
      // Expected
    }

    try {
      new FuzzingBackOffGenerator(0, 1000, 0.5);
      fail("Should not be able to create FuzzingBackOffGenerator with bad initialBackOff");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  public void testReset() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2, 1000, 0.5);

    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);

    generator.reset();
    next = generator.next();

    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);
  }

  /**
   * Rather than trying to inject a random number generator to such a tiny clas, such an overkill,
   * let's do something simple
   */
  public void testGeneratesRamdomNumber() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2000, 10000, 0.5);

    for (int i = 0; i < 100; i++) {
      if (generator.next().targetDelay != 2000) {
        return;
      }
    }
    fail("100x same value, it's not random.");
  }

  public void testMaxReachedImmediately() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(5, 2, 0.5);

    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);


  }

}
