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


import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.model.document.operation.DocOp;

/**
 * Interface for an UndoManager adapted for editor use.
 *
 * This undoManager is bound to an editor and the checkpoint method optionally
 * accept a range, which should be restored when undo/redo is applied.
 *
 */
public interface EditorUndoManager {

  /**
   * Places into the undo manager an operation that should be undone by undos.
   *
   * @param op the operation that should be undone by an appropriate undo
   */
  void undoableOp(DocOp op);

  /**
   * Places into the undo manager an operation that should not be undone by undos.
   *
   * @param op the operation that should not be undone by any undos
   */
  void nonUndoableOp(DocOp op);

  /**
   * Adds an undo checkpoint if any undoable ops have been applied.
   */
  void maybeCheckpoint();

  /**
   * Adds an undo checkpoint if any undoable ops have been applied.
   */
  void maybeCheckpoint(int startLocation, int endLocation);

  /**
   * Undo the the sequence of undoable operations since the last checkpoint.
   *
   * Restores the selection given to checkpoint if possible.
   */
  void undo();

  /**
   * Redo the the operation that was undone.
   *
   * Restores the selection at checkpoint if possible.
   */
  void redo();

  /**
   * Implementation that does almost nothing- (trace logs on calls to undo/redo)
   */
  public static final EditorUndoManager NOP_IMPL = new EditorUndoManager() {
    private final LoggerBundle logger = EditorStaticDeps.logger;

    @Override
    public void nonUndoableOp(DocOp op) {
    }

    @Override
    public void undoableOp(DocOp op) {
    }

    @Override
    public void maybeCheckpoint() {
    }

    @Override
    public void maybeCheckpoint(int startLocation, int endLocation) {
    }

    @Override
    public void undo() {
      logger.log(Level.TRACE, "No Undo For You!");
    }

    @Override
    public void redo() {
      logger.log(Level.TRACE, "No Redo For You!");
    }
  };
}
