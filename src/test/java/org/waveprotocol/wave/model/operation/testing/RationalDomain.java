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

import org.waveprotocol.wave.model.operation.Domain;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

public class RationalDomain implements Domain<RationalDomain.Data, RationalDomain.Affine> {

  /**
   * Mutable boxed number
   */
  public static class Data {
    Rational value;

    public Data(Rational value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }

  /**
   * f(x) = ax + b
   */
  public static final class Affine {

    // The requirement of this field makes for a very stupid operation,
    // but it allows us to break it in interesting ways in order to test the tester.
    final Rational initialState;
    final Rational finalState;

    final Rational a;
    final Rational b;

    public Affine(Rational initialState, Rational a, Rational b) {
      this.initialState = initialState;
      this.a = a;
      this.b = b;
      this.finalState = a.times(initialState).plus(b);
    }

    public Affine inverse() {
      // x = 1/a y - b/a
      return new Affine(finalState, a.reciprocal(), Rational.MINUS_ONE.times(b.dividedBy(a)));
    }

    public Affine of(Affine other, Rational newInitialState) throws OperationException {
      if (!other.finalState.equals(newInitialState)) {
        throw new OperationException("Op " + this + " cannot compose with op " + other);
      }
      return new Affine(other.initialState, a.times(other.a), a.times(other.b).plus(b));
    }

    public Affine of(Affine other) throws OperationException {
      if (!other.finalState.equals(initialState)) {
        throw new OperationException("Op " + this + " cannot compose with op " + other);
      }
      return new Affine(other.initialState, a.times(other.a), a.times(other.b).plus(b));
    }

    public void apply(Data state) throws OperationException {
      if (!state.value.equals(initialState)) {
        throw new OperationException("Op " + this + " does not apply to state " + state);
      }
      state.value = a.times(state.value).plus(b);
    }

    @Override
    public String toString() {
      return "(" + a + "*" + initialState + " + " + b + ") -> " + finalState;
    }
  }

  @Override
  public Data initialState() {
    return new Data(Rational.ZERO);
  }

  @Override
  public void apply(Affine op, Data state) throws OperationException {
    op.apply(state);
  }

  @Override
  public Affine compose(Affine f, Affine g) throws OperationException {
    return f.of(g);
  }

  @Override
  public OperationPair<Affine> transform(Affine clientOp, Affine serverOp)
      throws TransformException {
    try {
      Affine undoAndDoOther = serverOp.of(clientOp.inverse());
      return new OperationPair<Affine>(
          new Affine(serverOp.finalState, clientOp.a, clientOp.b),
          clientOp.of(undoAndDoOther, undoAndDoOther.finalState));
    } catch(OperationException e) {
      throw new TransformException(e.getMessage());
    }
  }

  @Override
  public Affine invert(Affine operation) {
    return operation.inverse();
  }

  @Override
  public Affine asOperation(Data state) {
    return new Affine(Rational.ZERO, Rational.ONE, state.value);
  }

  @Override
  public boolean equivalent(Data state1, Data state2) {
    return state1.value.equals(state2.value);
  }

}
