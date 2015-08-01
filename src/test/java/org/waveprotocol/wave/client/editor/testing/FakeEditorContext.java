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

package org.waveprotocol.wave.client.editor.testing;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Responsibility;
import org.waveprotocol.wave.client.editor.ResponsibilityManagerImpl;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;

/**
 * Representing a (possibly-POJO) implementation of the editor context interface,
 * by using members supplied on construction.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class FakeEditorContext implements EditorContext {
  private final CMutableDocument document;
  private final CaretAnnotations caretAnnotations;
  private final String imeCompositionState;
  private final SelectionHelper selectionHelper;
  private final Responsibility.Manager responsibility = new ResponsibilityManagerImpl();

  public FakeEditorContext(CMutableDocument doc, CaretAnnotations caret, String imeState,
      SelectionHelper selection) {
    this.document = doc;
    this.caretAnnotations = caret;
    this.imeCompositionState = imeState;
    this.selectionHelper = selection;
  }

  @Override
  public void blur() {
    // NO-OP
  }

  @Override
  public void focus(boolean collapsed) {
    // NO-OP
  }

  @Override
  public CaretAnnotations getCaretAnnotations() {
    return caretAnnotations;
  }

  @Override
  public CMutableDocument getDocument() {
    return document;
  }

  @Override
  public String getImeCompositionState() {
    return imeCompositionState;
  }

  @Override
  public SelectionHelper getSelectionHelper() {
    return selectionHelper;
  }

  @Override
  public boolean isEditing() {
    return false;
  }

  @Override
  public Responsibility.Manager getResponsibilityManager() {
    return responsibility;
  }

  @Override
  public void addUpdateListener(EditorUpdateListener listener) {
    throw new AssertionError("Not implemented");
  }

  @Override
  public void removeUpdateListener(EditorUpdateListener listener) {
    throw new AssertionError("Not implemented");
  }

  @Override
  public void undoableSequence(Runnable cmd) {
    cmd.run();
  }
}
