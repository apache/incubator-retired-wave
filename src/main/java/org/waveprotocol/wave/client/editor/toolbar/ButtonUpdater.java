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

package org.waveprotocol.wave.client.editor.toolbar;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

/**
 * An {@link EditorUpdateListener} to assist implementing standard rich text
 * toolbar buttons whose state is kept synchronized with the current selection.
 * <p>
 * See {@link org.waveprotocol.wave.client.wavepanel.impl.toolbar.EditToolbar}
 * for example uses.
 * <p>
 * Note: each {@link Controller} could have been an {@link EditorUpdateListener}
 * on its own, but each one would have had to ask the editor for the current
 * selection, whereas this class does so once only for all controllers, helping
 * sharing costs. Also, grouping all toolbar buttons that way makes it easy to
 * enable/disable the whole toolbar when you switch the editor's editing state.
 * 
 * @author kalman@google.com (Benjamin Kalman)
 */
public class ButtonUpdater implements EditorUpdateListener {

  /** Something that needs updating. */
  public interface Controller {
    void update(Range selectionRange);
  }

  // CopyOnWriteSet is used to ensure that the collection does not change
  // while iterating.
  private final CopyOnWriteSet<Controller> updateables = CopyOnWriteSet.create();

  /** The editor this updater is attached to. */
  private final EditorContext editor;

  public ButtonUpdater(EditorContext editor) {
    this.editor = editor;
  }

  /**
   * Adds a controller to the update list.
   * 
   * @return the controller, for convenience.
   */
  public <T extends Controller> T add(T controller) {
    updateables.add(controller);
    return controller;
  }

  @Override
  public void onUpdate(EditorUpdateEvent event) {
    updateButtonStates();
  }

  public void updateButtonStates() {
    if (updateables.isEmpty()) {
      return;
    }
    Range selectionRange = editor.getSelectionHelper().getOrderedSelectionRange();
    if (selectionRange == null) {
      return;
    }

    for (Controller update : updateables) {
      update.update(selectionRange);
    }
  }
}