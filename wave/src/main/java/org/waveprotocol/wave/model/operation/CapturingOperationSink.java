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

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * A {@link SilentOperationSink} which builds up a list of operations it has
 * consumed.
 *
 */
public final class CapturingOperationSink<T extends Operation<?>> implements SilentOperationSink<
    T> {
  private final List<T> captured;

  /** Creates a sink for capturing operations. */
  public CapturingOperationSink() {
    captured = CollectionUtils.newArrayList();
  }

  @Override
  public void consume(T op) {
    captured.add(op);
  }

  /**
   * Gets a view of the operations that have been captured by this sink, in the
   * order they were captured.
   *
   * @return a list of operations captured so far that will update as more
   *         operations are consumed.
   */
  public List<T> getOps() {
    return Collections.unmodifiableList(captured);
  }

  /**
   * Clears the list of operations captured by this sink.
   */
  public void clear() {
    captured.clear();
  }
}
