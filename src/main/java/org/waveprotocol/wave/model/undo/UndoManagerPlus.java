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

import org.waveprotocol.wave.model.util.Pair;

/**
 * An <code>UndoManager</code> that provides versions of the undo and redo
 * methods which return more information.
 *
 *
 * @param <T> The type of operations.
 */
public interface UndoManagerPlus<T> extends UndoManager<T> {

  /**
   * Effects an undo. Returns null if there are no operations to undo.
   *
   * NOTE(user): Warning. This interface method may change.
   *
   * @return a pair containing the operation that will effect an undo and the
   *         relevant transformed non-undoable operation (which may be null if
   *         no such operation exists)
   *
   * NOTE(user): Returning null is probably slightly harder to use than
   * returning an operation that does nothing.
   */
  Pair<T, T> undoPlus();

  /**
   * Effects a redo. Returns null if there are no operations to redo.
   *
   * NOTE(user): Warning. This interface method may change.
   *
   * @return a pair containing the operation that will effect a redo and the
   *         relevant transformed non-undoable operation (which may be null if
   *         no such operation exists)
   *
   * NOTE(user): Returning null is probably slightly harder to use than
   * returning an operation that does nothing.
   */
  Pair<T, T> redoPlus();

}
