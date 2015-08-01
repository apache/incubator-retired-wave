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

import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.SelectionMaintainer.TextNodeChangeType;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager;

/**
 * Contains extra core-only stuff. Would be nice to move this into a separate
 * package that doodads do not depend on.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ExtendedClientDocumentContext extends ClientDocumentContext {

  /**
   * For use by core classes only
   */
  public interface LowLevelEditingConcerns extends EditingConcerns {

    /**
     * Returns the typing extractor
     */
    // TODO(danilatos): Remove from this interface
    TypingExtractor getTypingExtractor();

    /**
     * Changes to text nodes must call this method, to intelligently keep track of
     * what has happened in order to determine whether or not the selection needs
     * restoring.
     *
     * Must be called AFTER the change to the text node
     *
     * @param nodelet the text nodelet being affected
     * @param affectedAfterOffset offset from which the text node has changed due
     *        to an insertion or deletion or split
     * @param insertionAmount the amount of inserted/deleted data, in the case of
     *        data alteration.
     * @param changeType
     */
    void textNodeletAffected(Text nodelet, int affectedAfterOffset, int insertionAmount,
        TextNodeChangeType changeType);

    /**
     * Blank implementation. Getters throw exceptions (better than returning
     * null and then subsequently having a null pointer exception), except for
     * {@link #hasEditor()}. Void methods just do nothing.
     */
    static final LowLevelEditingConcerns STUB = new LowLevelEditingConcerns() {
      @Override
      public TypingExtractor getTypingExtractor() {
        throw new IllegalStateException("Not in an editing context");
      }

      @Override
      public void textNodeletAffected(Text nodelet, int affectedAfterOffset, int insertionAmount,
          TextNodeChangeType changeType) {
        // Do nothing
      }

      @Override
      public SelectionHelper getSelectionHelper() {
        throw new IllegalStateException("Not in an editing context");
      }

      @Override
      public SuggestionsManager getSuggestionsManager() {
        throw new IllegalStateException("Not in an editing context");
      }

      /** Returns false */
      @Override
      public boolean hasEditor() {
        return false;
      }

      @Override
      public EditorContext editorContext() {
        throw new IllegalStateException("Not in an editing context");
      }
    };
  }

  @Override LowLevelEditingConcerns editing();
}
