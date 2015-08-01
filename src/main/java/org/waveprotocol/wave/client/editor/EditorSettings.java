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

/**
 * Configuration settings for the editor
 *
 * Updating a settings object that is given to the editor has undefined behaviour, i.e. it
 * is not defined, for each given setting, whether or not the editor will update accordingly.
 * Therefore it is best to never change existing objects given to editors, and to supply new
 * ones when wanting to change.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class EditorSettings {
  /** Default settings */
  public static final EditorSettings DEFAULT = new EditorSettings() {
    private static final String EXCEPTION_MSG = "DEFAULT settings is immutable";
    @Override
    public EditorSettings setHasDebugDialog(boolean hasDebugDialog) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setUndoEnabled(boolean undoEnabled) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setUseFancyCursorBias(boolean useFancyCursorBias) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setInstrumentor(EditorInstrumentor instrumentor) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setUseSemanticCopyPaste(boolean useSemanticCopyPaste) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setUseWhitelistInEditor(boolean useWhitelistInEditor) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setUseWebkitCompositionEvents(boolean useWebkitCompositionEvents) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }

    @Override
    public EditorSettings setCloseSuggestionsMenuDelayMs(int closeSuggestionsMenuDelayMs) {
      throw new IllegalStateException(EXCEPTION_MSG);
    }
  };

  private boolean hasDebugDialog = true;
  private boolean undoEnabled = true;
  private boolean useFancyCursorBias = true;
  private EditorInstrumentor instrumentor = EditorInstrumentor.NOOP;
  private boolean useSemanticCopyPaste = true;
  private boolean useWhitelistInEditor = false;
  private boolean useWebkitCompositionEvents = true;
  private int closeSuggestionsMenuDelayMs = 500;

  public boolean hasDebugDialog() {
    return hasDebugDialog;
  }

  public boolean undoEnabled() {
    return undoEnabled;
  }

  public boolean useFancyCursorBias() {
    return useFancyCursorBias;
  }

  public EditorInstrumentor getInstrumentor() {
    return instrumentor;
  }

  public boolean useSemanticCopyPaste() {
    return useSemanticCopyPaste;
  }

  public boolean useWhitelistInEditor() {
    return useWhitelistInEditor;
  }

  public boolean useWebkitCompositionEvents() {
    return useWebkitCompositionEvents;
  }

  public int closeSuggestionsMenuDelayMs() {
    return closeSuggestionsMenuDelayMs;
  }

  public EditorSettings setHasDebugDialog(boolean hasDebugDialog) {
    this.hasDebugDialog = hasDebugDialog;
    return this;
  }

  public EditorSettings setUndoEnabled(boolean undoEnabled) {
    this.undoEnabled  = undoEnabled;
    return this;
  }

  public EditorSettings setUseFancyCursorBias(boolean useFancyCursorBias) {
    this.useFancyCursorBias = useFancyCursorBias;
    return this;
  }

  public EditorSettings setInstrumentor(EditorInstrumentor instrumentor) {
    assert instrumentor != null : "Can't have a null instrumentor, use EditorInstrumentor.NOOP";
    this.instrumentor = instrumentor;
    return this;
  }

  public EditorSettings setUseSemanticCopyPaste(boolean useSemanticCopyPaste) {
    this.useSemanticCopyPaste = useSemanticCopyPaste;
    return this;
  }

  public EditorSettings setUseWhitelistInEditor(boolean useWhitelistInEditor) {
    this.useWhitelistInEditor = useWhitelistInEditor;
    return this;
  }

  public EditorSettings setUseWebkitCompositionEvents(boolean useWebkitCompositionEvents) {
    this.useWebkitCompositionEvents = useWebkitCompositionEvents;
    return this;
  }

  public EditorSettings setCloseSuggestionsMenuDelayMs(int closeSuggestionsMenuDelayMs) {
    this.closeSuggestionsMenuDelayMs = closeSuggestionsMenuDelayMs;
    return this;
  }
}
