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

package org.waveprotocol.wave.model.undo;

import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * An undo manager implementation.
 *
 *
 * @param <T> The type of operations.
 */
public final class UndoManagerImpl<T> implements UndoManagerPlus<T> {

  /**
   * Algorithms required by the undo manager.
   *
   * @param <T> The type of operations.
   */
  public interface Algorithms<T> {

    /**
     * Inverts the given operation.
     *
     * @param operation The operation to invert.
     * @return The inverse of the given operation.
     */
    T invert(T operation);

    /**
     * Composes the given operations.
     *
     * @param operations The operations to compose.
     * @return The composition of the given operations.
     */
    T compose(List<T> operations);

    /**
     * Transforms the given operations.
     *
     * @param op1 The first concurrent operation.
     * @param op2 The second concurrent operation.
     * @return The result of transforming the given operations.
     * @throws TransformException If there was a problem with the transform.
     */
    OperationPair<T> transform(T op1, T op2) throws TransformException;

  }

  private static final class Checkpointer {

    // TODO(user): Switch to a Deque when GWT supports it.
    private final Stack<Integer> partitions = new Stack<Integer>();
    private int lastPartition = 0;

    void checkpoint() {
      if (lastPartition > 0) {
        partitions.push(lastPartition);
        lastPartition = 0;
      }
    }

    int releaseCheckpoint() {
      if (lastPartition > 0) {
        int value = lastPartition;
        lastPartition = 0;
        return value;
      }
      if (partitions.isEmpty()) {
        return 0;
      }
      return partitions.pop();
    }

    void increment() {
      ++lastPartition;
    }

  }

  private final Algorithms<T> algorithms;
  private final UndoStack<T> undoStack;
  private final UndoStack<T> redoStack;
  private final Checkpointer checkpointer = new Checkpointer();

  public UndoManagerImpl(Algorithms<T> algorithms) {
    this.algorithms = algorithms;
    undoStack = new UndoStack<T>(algorithms);
    redoStack = new UndoStack<T>(algorithms);
  }

  @Override
  public void undoableOp(T op) {
    undoStack.push(op);
    checkpointer.increment();
    redoStack.clear();
  }

  @Override
  public void nonUndoableOp(T op) {
    undoStack.nonUndoableOperation(op);
    redoStack.nonUndoableOperation(op);
  }

  @Override
  public void checkpoint() {
    checkpointer.checkpoint();
  }

  // TODO(user): This current implementation does more work than necessary.
  @Override
  public T undo() {
    Pair<T, T> undoPlus = undoPlus();
    return undoPlus == null ? null : undoPlus.first;
  }

  // TODO(user): This current implementation does more work than necessary.
  @Override
  public T redo() {
    Pair<T, T> redoPlus = redoPlus();
    return redoPlus == null ? null : redoPlus.first;
  }

  // TODO(user): This current implementation does more work than necessary.
  @Override
  public Pair<T, T> undoPlus() {
    int numToUndo = checkpointer.releaseCheckpoint();
    if (numToUndo == 0) {
      return null;
    }
    List<T> operations = new ArrayList<T>();
    for (int i = 0; i < numToUndo - 1; ++i) {
      operations.add(undoStack.pop().first);
    }
    Pair<T, T> ops = undoStack.pop();
    operations.add(ops.first);
    T op = algorithms.compose(operations);
    redoStack.push(op);
    return new Pair<T, T>(op, ops.second);
  }

  @Override
  public Pair<T, T> redoPlus() {
    Pair<T, T> ops = redoStack.pop();
    if (ops != null) {
      checkpointer.checkpoint();
      undoStack.push(ops.first);
      checkpointer.increment();
    }
    return ops;
  }

}
