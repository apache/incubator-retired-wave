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

package org.waveprotocol.wave.model.operation.testing;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.operation.testing.DomainRandomTester.FailureException;
import org.waveprotocol.wave.model.operation.testing.DomainRandomTester.Log;
import org.waveprotocol.wave.model.operation.testing.RationalDomain.Affine;

import java.math.BigInteger;


public class RandomTesterTest extends TestCase {

  private final int NUM_ITERATIONS = 50;
  DomainRandomTester<RationalDomain.Data, Affine> t;

  boolean expectFailure = false;

  public void testDataOpEquivalence() {
    BigInteger x = BigInteger.ZERO;
    createTester();
    t.testDataOperationEquivalence(NUM_ITERATIONS);
  }

  public void testInversion() {
    createTester();
    t.testOperationInversion(NUM_ITERATIONS);
  }

  public void testCompose() {
    createTester();
    t.testCompositionOnInitialState(NUM_ITERATIONS);
    t.testCompositionAssociativity(NUM_ITERATIONS);
    t.testSimpleComposition(NUM_ITERATIONS);
  }

  public void testTransform() {
    createTester();
    t.testTransformDiamondProperty(NUM_ITERATIONS);
  }

  protected void createTester() {
    RationalDomain d = new RationalDomain();
    AffineGenerator g = new AffineGenerator();
    createTester(d, g);
  }

  protected void createTester(RationalDomain d, AffineGenerator g) {
    t = new DomainRandomTester<RationalDomain.Data, Affine>(new Log() {
        @Override
        public void inconsistent(String... lines) {
          if (!expectFailure) {
            for (String line : lines) {
              System.err.println(line);
            }
          }
          throw new FailureException();
        }

        @Override
        public void fatal(Throwable exception, String... lines) {
          for (String line : lines) {
            System.err.println(line);
          }
          fail("EXCEPTION THROWN");
        }

        @Override
        public void info(String... lines) {
          for (String line : lines) {
            System.out.println(line);
          }
        }
      }, d, g);
  }
}
