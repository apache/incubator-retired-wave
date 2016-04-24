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

import java.math.BigInteger;

public final class Rational {
  private static final BigInteger BI_M1 = BigInteger.valueOf(-1);

  public static final Rational ZERO = new Rational(0, 1);
  public static final Rational ONE = new Rational(1, 1);
  public static final Rational MINUS_ONE = new Rational(-1, 1);
  public static final Rational TWO = new Rational(2, 1);

  final BigInteger numerator;
  final BigInteger denominator;

  public Rational(int numerator, int denominator) {
    this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  public Rational(BigInteger numerator, BigInteger denominator) {
    if (denominator.equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException("Denominator must != 0");
    } else if (denominator.compareTo(BigInteger.ZERO) < 0) {
      denominator = denominator.multiply(BI_M1);
      numerator = numerator.multiply(BI_M1);
    }
//    BigInteger negative = numerator.signum() < 0 ? minusOne : BigInteger.ONE;
//    numerator = numerator.multiply(negative);
    BigInteger gcd = numerator.gcd(denominator);
    this.numerator = numerator.divide(gcd);
    this.denominator = denominator.divide(gcd);
  }

  public Rational plus(Rational other) {
    return new Rational(
        numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
        denominator.multiply(other.denominator));
  }

  public Rational minus(Rational other) {
    return new Rational(
        numerator.multiply(other.denominator).add(
            BI_M1.multiply(other.numerator.multiply(denominator))),
        denominator.multiply(other.denominator));
  }

  public Rational times(Rational other) {
    return new Rational(
        numerator.multiply(other.numerator),
        denominator.multiply(other.denominator));
  }

  public Rational dividedBy(Rational other) {
    if (other.numerator.equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException("Division by zero");
    }
    return times(other.reciprocal());
  }

  public Rational reciprocal() {
    if (numerator.equals(BigInteger.ZERO)) {
      throw new IllegalArgumentException("Division by zero");
    }
    return new Rational(denominator, numerator);
  }

  @Override
  public String toString() {
    return numerator +
        (denominator.equals(BigInteger.ONE) || denominator.equals(BigInteger.ZERO)
        ? "" : "/" + denominator);
  }

  // eclipse generated

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((denominator == null) ? 0 : denominator.hashCode());
    result = prime * result + ((numerator == null) ? 0 : numerator.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Rational other = (Rational) obj;
    if (denominator == null) {
      if (other.denominator != null)
        return false;
    } else if (!denominator.equals(other.denominator))
      return false;
    if (numerator == null) {
      if (other.numerator != null)
        return false;
    } else if (!numerator.equals(other.numerator))
      return false;
    return true;
  }
}