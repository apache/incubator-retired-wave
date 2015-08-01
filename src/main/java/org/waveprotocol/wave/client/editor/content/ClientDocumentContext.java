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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.model.document.util.DocumentContext;

/**
 * Everything doodads should need to do their business
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ClientDocumentContext
    extends DocumentContext<ContentNode, ContentElement, ContentTextNode> {

  /**
   * Concerns specific to editing
   */
  public interface EditingConcerns {

    /**
     * @return an interface to the user's selection
     */
    SelectionHelper getSelectionHelper();

    SuggestionsManager getSuggestionsManager();

    /**
     * @return true if the document is inside an editor.
     *
     *         NOTE: The document may be inside an editor when not in edit mode,
     *         don't use this function to check edit mode, use
     *         {@link #isEditing()}
     */
    boolean hasEditor();

    // Maybe merge EditingConcerns and EditorContext?
    // The latter is a bit big.
    EditorContext editorContext();
  }

  /**
   * Concerns specific to HTML rendering
   */
  public interface RenderingConcerns {
    ContentView getRenderedContentView();
    HtmlView getFilteredHtmlView();
    HtmlView getFullHtmlView();
    NodeManager getNodeManager();
    /** The repairer */
    // TODO(danilatos) get rid of this
    Repairer getRepairer();
  }

  @Override CMutableDocument document();
  @Override public ContentView persistentView();

  /**
   * @return true if in "edit mode"
   */
  boolean isEditing();

  /**
   * @return editing concerns if the document is inside an editor, a stub
   *         implementation if it is not. The stub implementation should throw
   *         exceptions for all methods that return a value (non-void) and do
   *         nothing for void methods.
   *
   *         To check for whether we have a stub implementation or not, use
   *         {@link EditingConcerns#hasEditor()}
   */
  EditingConcerns editing();

  /**
   * @return rendering concerns if in edit mode, null if not
   */
  RenderingConcerns rendering();

  /**
   * @return a unique string for this document.
   *
   * NOTE: Not thread safe
   */
  String getDocumentId();

  /**
   * @param name
   * @return element with given name attribute, or null
   */
  // TODO(danilatos) move this somewhere else
  ContentElement getElementByName(String name);

  /**
   * Use this instead of GWT's scheduler when scheduling a task in the context
   * of a single document. The task's execution will be guarded by the
   * appropriate setup and teardown behaviour (e.g. ignoring mutations and
   * preserving selections).
   *
   * If a single task is to run in the context of more than one document,
   * schedule it separately, but guard the execution of each part that affects
   * each document with a call to {@link #beginDeferredMutation()} and
   * {@link #endDeferredMutation()}
   *
   * @param task
   */
  void scheduleFinally(Scheduler.Task task);

  /**
   * Guard a mutation to the HTML for the associated document. Must be called
   * for each document's HTML being modified.
   */
  void beginDeferredMutation();

  /**
   * Complement of {@link #beginDeferredMutation()}
   */
  void endDeferredMutation();
}
