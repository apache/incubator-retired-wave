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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.operation.Operation;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A place where you can get a concrete OperationSink.Silent for testing.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FakeSilentOperationSink<T extends Operation<?>> implements SilentOperationSink<T> {
  private LinkedList<T> ops = new LinkedList<T>();

  /**
   * For unit testing
   * @return the most recently consumed op
   */
  public T getConsumedOp() {
    int size = ops.size();
    return (size == 0) ? null : (ops.get(size - 1));
  }

  /**
   * {@inheritDoc}
   */
  public void consume(T op) {
    ops.addLast(op);
  }

  /**
   * Clears the list of saved operations.
   */
  public void clear() {
    ops.clear();
  }

  /**
   * Gets the list of operations consumed by this sink since it was last
   * cleared.
   *
   * @return the ops, from first consumed through most recently consumed.
   */
  public List<T> getOps() {
    return Collections.unmodifiableList(ops);
  }
}
