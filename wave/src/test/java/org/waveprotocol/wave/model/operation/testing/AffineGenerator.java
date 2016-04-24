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

import java.util.Random;

import org.waveprotocol.wave.model.operation.testing.RationalDomain.Affine;

public class AffineGenerator implements RandomOpGenerator<RationalDomain.Data, Affine> {

  @Override
  public Affine randomOperation(RationalDomain.Data state, Random random) {
    int neg1 = random.nextBoolean() ? -1 : 1;
    int neg2 = random.nextBoolean() ? -1 : 1;

    long mulNum, mulDenom;
//
//    final BigInteger TOO_BIG = BigInteger.valueOf(1000);
//
//    if (state.value.numerator) > TOO_BIG) {
//      mulDenom = state.value.numerator;
//    } else {
//      mulDenom =  random.nextInt(9) + 1;
//    }
//    if (state.value.denominator > TOO_BIG) {
//      mulNum = state.value.denominator;
//    } else {
//      mulNum = neg1 * (random.nextInt(9) + 1);
//    }

    return new Affine(
        state.value,
        new Rational(neg1 * (random.nextInt(9) + 1), random.nextInt(9) + 1),
        new Rational(neg2 * (random.nextInt(10)), random.nextInt(9) + 1)
        );
  }

}
