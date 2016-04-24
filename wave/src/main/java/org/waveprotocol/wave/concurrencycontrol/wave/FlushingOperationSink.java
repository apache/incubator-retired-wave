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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * A {@code FlushingOperationSink} receives notifications of operations
 * before they are consumed.
 *
 */
public interface FlushingOperationSink<T extends Operation<?>> extends SilentOperationSink<T> {
  /**
   * Brings the operation target into a consistent state.
   *
   * @param operation is a sneak peek at the next incoming operation.
   *     It is the operation that will be passed to the next call to onServerOperation,
   *     except if it is changed or eliminated by operational transformation against
   *     any outgoing client operations in the meantime.
   * @param resume is a callback that must be called later, if flush returns false,
   *     to resume the flow of incoming operations.
   * @return false if the editor does not want to consume an operation at this
   *     point in time and will call the resume callback later.
   */
  boolean flush(T operation, Runnable resume);
}
