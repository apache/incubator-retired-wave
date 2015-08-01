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

package org.waveprotocol.wave.client.editor.harness;

import com.google.common.base.Preconditions;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * This class maintains diff state and consumes diff ops
 *
 */
public final class HighlightingDiffState {

  // The corresponding editor
  private final Editor editor;

  // Keeps track of the currentContentDoc associated with this DiffState.
  // Loaded lazily, because an editor may or may not have a document at any time.
  private ContentDocument currentContentDoc;

  // The diffFilter corresponding to the current content document. If the
  // contentDocument is changed, this needs to be reinitialized.
  private DiffHighlightingFilter diffFilter;

  public HighlightingDiffState(Editor editor) {
    this.editor = editor;
  }

  /**
   * Clears diffs.
   */
  public void clearDiffs() {
    // This null check is needed when the editor is not rendered, hence
    // diffFilter not attached.
    getDiffFilter().clearDiffs();
  }

  /**
   * Consumes a diff op.
   *
   * @param operation
   * @throws OperationException
   */
  public void consume(DocOp operation) throws OperationException {
    getDiffFilter().consume(operation);
  }

  private DiffHighlightingFilter getDiffFilter() {
    Preconditions.checkNotNull(editor.getContent());
    if (editor.getContent() != currentContentDoc) {
      currentContentDoc = editor.getContent();
      diffFilter = new DiffHighlightingFilter(currentContentDoc.getDiffTarget());
    }

    return diffFilter;
  }
}
