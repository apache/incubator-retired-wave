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
 * Represents an operation that can be applied to a target.
 *
 * @param <T> type to which this operation can be applied
 *
 */
public interface Operation<T> {
  /**
   * Applies this operation to the given target
   *
   * @param target   target on which this operation applies itself
   * @throws OperationException if the execution of this operation fails.
   */
  public void apply(T target) throws OperationException;
}
