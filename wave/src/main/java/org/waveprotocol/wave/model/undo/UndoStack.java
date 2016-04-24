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
 * An undo stack.
 *
 * TODO(user): This can be heavily optimised.
 *
 *
 * @param <T> The type of operations.
 */
final class UndoStack<T> {

  private static final class StackEntry<T> {

    final T op;
    List<T> nonUndoables = new ArrayList<T>();

    StackEntry(T op) {
      this.op = op;
    }

  }

  // TODO(user): Switch to a Deque when GWT supports it.
  private final Stack<StackEntry<T>> stack = new Stack<StackEntry<T>>();

  private final UndoManagerImpl.Algorithms<T> algorithms;

  UndoStack(UndoManagerImpl.Algorithms<T> algorithms) {
    this.algorithms = algorithms;
  }

  /**
   * Pushes an operation onto the undo stack.
   *
   * @param op the operation to push onto the undo stack
   */
  void push(T op) {
    stack.push(new StackEntry<T>(op));
  }

  /**
   * Pops an operation from the undo stack and returns the operation that
   * effects the undo and the transformed non-undoable operation.
   *
   * @return a pair containeng the operation that accomplishes the desired undo
   *         and the transformed non-undoable operation
   */
  Pair<T, T> pop() {
    if (stack.isEmpty()) {
      return null;
    }
    StackEntry<T> entry = stack.pop();
    T op = algorithms.invert(entry.op);
    if (entry.nonUndoables.isEmpty()) {
      return new Pair<T, T>(op, null);
    }
    OperationPair<T> pair;
    try {
      pair = algorithms.transform(op, algorithms.compose(entry.nonUndoables));
    } catch (TransformException e) {
      throw new IllegalStateException("invalid operation transformation encountered", e);
    }
    // TODO(user): Change the ternary expression to just stack.peek() after
    // switching from Stack to Deque.
    StackEntry<T> nextEntry = stack.isEmpty() ? null : stack.peek();
    if (nextEntry != null) {
      nextEntry.nonUndoables.add(pair.serverOp());
    }
    return new Pair<T, T>(pair.clientOp(), pair.serverOp());
  }

  /**
   * Intermingles intervening operations that should not be undone.
   *
   * @param op the operation that should not be undone
   */
  void nonUndoableOperation(T op) {
    if (!stack.isEmpty()) {
      stack.peek().nonUndoables.add(op);
    }
  }

  /**
   * Clear the stack.
   */
  void clear() {
    stack.clear();
  }

}
