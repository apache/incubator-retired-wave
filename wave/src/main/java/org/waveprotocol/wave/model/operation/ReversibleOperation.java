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

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.List;

/**
 * This represents an operation that is able to return the reverse operation of
 * itself after application.
 *
 *
 * @param <O> The Operation Class
 * @param <T> The Class on which apply() and applyAndReturnReverse() can be called.
 */
public interface ReversibleOperation<O extends Operation<T>, T> extends Operation<T> {

  /**
   * Applies the operation to a target and returns a sequence of operations
   * which can reverse the application.
   *
   * @param target The target onto which to apply the operation.
   * @return A sequence of operations that reverses the application of this
   *         operation. The returned sequence of operations, when applied in
   *         order after this operation is applied, should reverse the effect of
   *         this operation.
   */
  public List<? extends O> applyAndReturnReverse(T target) throws OperationException;

}
