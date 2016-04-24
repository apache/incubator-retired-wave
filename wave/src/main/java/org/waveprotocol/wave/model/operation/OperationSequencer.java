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
 * An extension of the basic sink that takes hints about grouping operations together (perhaps
 * for batching, merging, etc).  If passing a group of operations to a sequencer, call
 * {@link #begin()} before the first operation, and {@link #end()} after the last operation.
 * Groups can be nested.
 *
 * @param <T> operation type
 * @author danilatos@google.com
 */
public interface OperationSequencer<T> {

  /**
   * Begin a set of executed operations.
   * Set up any state or other tasks as necessary.
   */
  void begin();

  /**
   * End a set of executed operations.
   * Perform any notifications or other tasks as necessary.
   */
  void end();

  /**
   * Consumes an operation
   * @param op
   */
  void consume(T op);
}
