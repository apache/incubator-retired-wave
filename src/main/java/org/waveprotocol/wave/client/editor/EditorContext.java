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

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;

/**
 * Context served up by the editor, containing information about the
 * current editor state.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public interface EditorContext {
  /**
   * @return The mutable document within the editor.
   */
  CMutableDocument getDocument();

  /**
   * @return an interface to the user's selection, may be null if no helper is attached.
   */
  SelectionHelper getSelectionHelper();

  /**
   * @return The annotations in the editor for the current selection.
   */
  CaretAnnotations getCaretAnnotations();

  /**
   * @return the current uncommitted IME composition state. null if the editor
   *         is not currently in a composition state.
   */
  String getImeCompositionState();

  /**
   * @return true if the editor is in edit mode
   */
  boolean isEditing();

  /// FOCUS management

  /**
   * Give focus to the editor the context comes from, including adding back any selection
   * lost on previous blur.
   *
   * @param collapsed whether or not the restored selection must be collapsed.
   */
  void focus(boolean collapsed);

  /**
   * Remove focus from the editor, preserving the selection.
   */
  void blur();

  /**
   * Adds a listener to the EditorUpdateEvents.
   *
   * These events are fired asynchronously when the editor content or selection
   * is modified, and are throttled to not fire above a certain rate.
   *
   * @param listener
   */
  void addUpdateListener(EditorUpdateEvent.EditorUpdateListener listener);

  /**
   * Removes the EditorUpdateEvents listener.
   */
  void removeUpdateListener(EditorUpdateEvent.EditorUpdateListener listener);

  /**
   * See Responsibility.Manger
   */
  Responsibility.Manager getResponsibilityManager();

  /**
   * Runs cmd as an undoable sequence.
   *
   * This checkpoints the current document state and wraps the command in an
   * undoable scope so that modifications to the document are grouped as an undo
   * step.
   *
   * This method attempts to keep the responsibility state consistent even if
   * executing cmd throws an exception.
   *
   * @param cmd
   */
  void undoableSequence(Runnable cmd);
}
