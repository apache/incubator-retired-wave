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

package org.waveprotocol.wave.client.editor;

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Stack;

/**
 * Simple implementation of {@link Responsibility.Manager}. The default
 * responsibility is indirect.
 *
 */
public class ResponsibilityManagerImpl implements Responsibility.Manager {

  private static enum SequenceType {
    DIRECT,
    INDIRECT;
  }

  /**
   * Keeps track of whether the op sequence should be routed as undoable, or
   * non-undoable. The top of the stack determines how ops get routed.
   */
  private final Stack<SequenceType> sequenceType = new Stack<SequenceType>();

  private SequenceType currentSequenceType() {
    // Indirect by default, as per javadoc (keep doc and code in sync)
    return sequenceType.isEmpty() ? SequenceType.INDIRECT : sequenceType.peek();
  }

  @Override
  public void startDirectSequence() {
    sequenceType.push(SequenceType.DIRECT);
  }

  @Override
  public void endDirectSequence() {
    Preconditions.checkState(!sequenceType.isEmpty()
        && sequenceType.peek() == SequenceType.DIRECT, "end undoable sequence without begin");
    sequenceType.pop();
  }

  @Override
  public void startIndirectSequence() {
    EditorStaticDeps.logger.trace().log("Depth: ", sequenceType.size());
    sequenceType.push(SequenceType.INDIRECT);
  }

  @Override
  public void endIndirectSequence() {
    Preconditions.checkState(!sequenceType.isEmpty()
        && sequenceType.peek() == SequenceType.INDIRECT,
        "end non-undoable sequence without begin");
    sequenceType.pop();
  }

  @Override
  public boolean withinDirectSequence() {
    return currentSequenceType() == SequenceType.DIRECT;
  }
}
