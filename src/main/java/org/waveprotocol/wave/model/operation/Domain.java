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

package org.waveprotocol.wave.model.operation;

/**
 * NOTE(danilatos): In many senses, D is a subtype of O. But for syntactic
 * simplicity, and implementation practicality, we avoid representing that in
 * the type system, and instead provide an apply and an asOperation method,
 * rather than only using compose.
 *
 * @param <D> Data type
 * @param <O> Operations on the data type
 * @author danilatos@google.com (Daniel Danilatos)
 */

public interface Domain<D, O> {

  /**
   * @return initial (empty) data state for this domain
   */
  D initialState();

  /**
   * Applies the op to the given state, modifying it.
   *
   * This is basically the same as compose, but see the note in the class
   * javadoc.
   *
   * @param op
   * @param state
   */
  void apply(O op, D state) throws OperationException;

  /**
   * Composes two operations.
   *
   * @param f
   * @param g
   * @return (f o g)
   */
  O compose(O f, O g) throws OperationException;

  /**
   * Transforms two operations.
   *
   * @param clientOp
   * @param serverOp
   * @return The transformed pair of operations
   */
  OperationPair<O> transform(O clientOp, O serverOp) throws TransformException;

  /**
   * Where possible, invert an operation, such that (invert x) . x == id when
   * applying to a document (the implications for transform are not defined).
   *
   * @param operation
   * @return the inverse operation
   * @throws UnsupportedOperationException if the operation cannot be inverted
   */
  O invert(O operation);

  /**
   * @param state
   * @return the state represented as an operation from the initial state to the
   *         given state. Whether this is a view or a copy is not defined.
   */
  O asOperation(D state);

  /**
   * Determines if two states are equivalent.
   *
   * Equivalence should be determined by checking equality on the normalised
   * representation of each state.
   *
   * Note that this says nothing of the equivalence of the states represented
   * as operations under transform or composition.
   *
   * @param state1
   * @param state2
   * @return true if the two objects are equivalent
   */
  boolean equivalent(D state1, D state2);
}
