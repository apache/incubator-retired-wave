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

import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import java.util.Collections;
import java.util.List;

/**
 * Wavelet Undo handles multilevel undo/redo for wavelets.
 *
 */
public class WaveletUndo {
  private final UndoManagerPlus<WaveAggregateOp> undoManager =
      UndoManagerFactory.createWUndoManager();

  /**
   * Handle a non-undoable op.
   * @param op
   */
  public void nonUndoable(WaveletOperation op) {
    if (OpUtils.isNoop(op)) {
      return;
    }
    undoManager.nonUndoableOp(WaveAggregateOp.createAggregate(op));
  }

  /**
   * Handle an undoable op.
   * @param op
   */
  public void undoable(WaveletOperation op) {
    if (OpUtils.isNoop(op)){
      return;
    }
    undoManager.undoableOp(WaveAggregateOp.createAggregate(op));
  }

  /**
   * Returns a list of operations that undoes operations since the last
   * checkpoint.
   */
  public List<WaveletOperation> undo() {
    WaveAggregateOp undo = undoManager.undo();
    if (undo != null) {
      return undo.toWaveletOperations();
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Returns and pops the list of operations off the top of the redo stack.
   *
   * NOTE(user): A redo entry is pushed whenever we call undo. It is cleared
   * when undoable operations are applied.
   */
  public List<WaveletOperation> redo() {
    WaveAggregateOp redo = undoManager.redo();
    if (redo != null) {
      return redo.toWaveletOperations();
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Adds a checkpoint to indicate the boundary of an undo chunk.
   */
  public void checkpoint() {
    undoManager.checkpoint();
  }
}
