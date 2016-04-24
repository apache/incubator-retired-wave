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

package org.waveprotocol.wave.model.wave.undo;

import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Buffers undoable operations, so that they can be withheld from CC and
 * reverted later.
 *
 */
public class OneStepBuffer {
  /** List of buffered undoable ops. */
  private final List<WaveAggregateOp> undoable = new ArrayList<WaveAggregateOp>();

  /**
   * Transforms a non undoable operation against the operations in the undo
   * stack.
   *
   * If updateUndoStack is true, also transform and update the undo stack,
   * otherwise, leave the undo stack untouched.
   *
   * @param op
   * @param updateUndoStack
   * @return returns the transformed nonundoable operation
   */
  public List<WaveletOperation> transformNonUndoable(WaveletOperation op, boolean updateUndoStack) {
    if (!hasOperations() || OpUtils.isNoop(op)) {
      // If there are no buffered operations, or if the op is not important,
      // then we don't need to transform.
      return Collections.singletonList(op);
    }

    WaveAggregateOp nonUndoable = WaveAggregateOp.createAggregate(op);
    WaveAggregateOp composed = WaveAggregateOp.compose(undoable);

    final WaveAggregateOp transformedNonUndoable;
    final WaveAggregateOp transformedUndoable;
    try {
      OperationPair<WaveAggregateOp> transform =
          WaveAggregateOp.transform(nonUndoable, composed);
      transformedNonUndoable = transform.clientOp();
      transformedUndoable = transform.serverOp();
    } catch (TransformException e) {
      throw new RuntimeException("Transform exception while transforming nonUndoable", e);
    }

    // Update buffer
    undoable.clear();
    if (updateUndoStack) {
      undoable.add(transformedUndoable);
    } else {
      // As an optimization, since we have composed the operations in the undo
      // stack, replace it with the composed op.
      undoable.add(composed);
    }

    WaveletOperationContext originalContext = op.getContext();
    return transformedNonUndoable.toWaveletOperationsWithVersions(originalContext
        .getVersionIncrement(), originalContext.getHashedVersion());
  }

  /**
   * Buffer an undoable operation
   * @param op
   */
  public void undoable(WaveletOperation op) {
    undoable.add(WaveAggregateOp.createAggregate(op));
  }

  /**
   * Flushes buffered operation by returning and clearing the buffer.
   */
  public List<WaveletOperation> flush() {
    List<WaveletOperation> ret = new ArrayList<WaveletOperation>();
    for (WaveAggregateOp op : undoable) {
      ret.addAll(op.toWaveletOperations());
    }
    undoable.clear();

    return ret;
  }

  /**
   * Reverts buffered operations by clearing the buffer and returning its
   * inverse.
   */
  public List<WaveletOperation> revert() {
    WaveAggregateOp composed = WaveAggregateOp.compose(undoable);
    WaveAggregateOp invert = composed.invert();
    undoable.clear();
    return invert.toWaveletOperations();
  }

  /**
   * Returns whether the buffer has operations.
   */
  public boolean hasOperations() {
    return !undoable.isEmpty();
  }
}
