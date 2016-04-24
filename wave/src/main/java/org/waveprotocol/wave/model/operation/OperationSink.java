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
 * An operation sink is an opaque operation applier.  It has a single method through which
 * operations are passed, and must obey the contract that the intent of that operation gets
 * executed.  Note this is a weaker contract than actually executing the {@link
 * Operation#apply(Object)} method on that operation (for example, the operation may be transformed
 * into another operation instance, so that the new operation's {@code apply} method would be called
 * instead).
 *
 * A sink also abstracts away the locating of the appropriate object to which the operation is
 * applied.   Usually, a sink will either pass the operation to another sink, or be a manager for
 * instances of the target type to which the operation applies, and will identify the appropriate
 * instance and then execute the operation's {@code apply} method on that instance.
 *
 *
 * @param <T>
 */
public interface OperationSink<T extends Operation<?>> {
  /**
   * An operation sink which does nothing with consumed operations.
   */
  static final OperationSink<Operation<?>> VOID =
    new OperationSink<Operation<?>>() {
      @Override
      public void consume(Operation<?> op) {
      }
    };

  /**
   * Consumes an operation.  Usually, this will involve finding an appropriate target for the
   * operation, then calling {@link Operation#apply(Object)} on that target.  However, this is not
   * a strong guarantee.  The only contract for a sink is to ensure that the intent of the given
   * operation is effected.
   *
   * @param op  operation to apply
   */
  public void consume(T op) throws OperationException;
}
