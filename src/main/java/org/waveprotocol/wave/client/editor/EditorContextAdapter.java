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

import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Editor context that presents its context by delegating to a wrapped editor implementation.
 * The underlying editor can then be swapped at runtime.
 *
 * Note that all members retrieved are volatile, there is no guarentee that the objects returned
 * will be the same each time.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class EditorContextAdapter implements EditorContext {

  /** Editor instance being wrapped. */
  private EditorContext editor;

  /** Constructs a context, taking the initial editor to wrap. */
  public EditorContextAdapter(EditorContext editor) {
    this.editor = editor;
  }

  /**
   * @throws IllegalStateException if the current instance being wrapped is not
   *         the given one.
   */
  public void checkEditor(EditorContext editor) {
    Preconditions.checkState(this.editor == editor, "wrong editor");
  }

  /** Silently switches the wrapped editor with a new instance. */
  public void switchEditor(EditorContext newEditor) {
    this.editor = newEditor;
  }

  @Override
  public CMutableDocument getDocument() {
    return editor == null ? null : editor.getDocument();
  }

  @Override
  public SelectionHelper getSelectionHelper() {
    if (editor == null) {
      // Something is trying to get/set the selection of a closed editor
      // - this should never happen! This code is lined up to be removed once the causes are fixed
      //   until then, it is safe to act as though we have no selection.
      EditorStaticDeps.logger.error().log("Don't access editor selection with context not attached "
          + "to an editor!");
      return SelectionHelper.NOP_IMPL;
    }
    return editor.getSelectionHelper();
  }

  @Override
  public CaretAnnotations getCaretAnnotations() {
    return editor == null ? null : editor.getCaretAnnotations();
  }

  @Override
  public String getImeCompositionState() {
    return editor == null ? null : editor.getImeCompositionState();
  }

  @Override
  public boolean isEditing() {
    return editor == null ? false : editor.isEditing();
  }

  @Override
  public void blur() {
    if (editor != null) {
      editor.blur();
    }
  }

  @Override
  public void focus(boolean collapsed) {
    if (editor != null) {
      editor.focus(collapsed);
    }
  }

  @Override
  public void addUpdateListener(EditorUpdateListener listener) {
    if (editor != null) {
      editor.addUpdateListener(listener);
    }
  }

  @Override
  public void removeUpdateListener(EditorUpdateListener listener) {
    if (editor != null) {
      editor.removeUpdateListener(listener);
    }
  }

  @Override
  public Responsibility.Manager getResponsibilityManager() {
    return (editor != null) ? editor.getResponsibilityManager() : null;
  }

  @Override
  public void undoableSequence(Runnable cmd) {
    Preconditions.checkState(editor != null, "editor must not be null");
    editor.undoableSequence(cmd);
  }
}
