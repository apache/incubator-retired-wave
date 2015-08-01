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


package org.waveprotocol.wave.client.wave;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.MutableDocumentProxy;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * A document implementation that materializes a {@link ContentDocument} on
 * demand.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class LazyContentDocument extends MutableDocumentProxy<Doc.N, Doc.E, Doc.T>
    implements CcDocument, Document, InteractiveDocument {

  /** Event handlers. */
  private final Registries base;

  /** Lazily-loaded document implementation. Never cleared once set. */
  private DiffContentDocument document;

  /** Initial document state. Nulled after initialization. */
  private SimpleDiffDoc spec;

  /** Sink to which local mutations are sent. Never cleared once set. */
  private SilentOperationSink<? super DocOp> outputSink;

  /**
   * True iff the entire document is a diff. Monotonically cleared by
   * {@link #clearDiffs}.
   */
  private boolean isCompleteDiff;

  @VisibleForTesting
  LazyContentDocument(Registries base, SimpleDiffDoc initial, boolean isCompleteDiff) {
    this.base = base;
    this.spec = initial;
    this.isCompleteDiff = isCompleteDiff;
  }

  public static LazyContentDocument create(Registries base, SimpleDiffDoc initial) {
    return new LazyContentDocument(base, initial, initial.isCompleteDiff());
  }

  /**
   * Loads the real document implementation with a particular set of registries.
   */
  // Type conversion with flattened generics in setDelegate();
  @SuppressWarnings("unchecked")
  private void loadWith(Registries registries) {
    assert !isLoaded() : "already loaded";
    ContentDocument core = new ContentDocument(DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    document = DiffContentDocument.create(core);
    if (outputSink != null) {
      core.setOutgoingSink(outputSink);
    }
    core.setRegistries(registries);
    setDelegate((MutableDocument) core.getMutableDoc());
    if (spec != null) {
      spec.applyTo(document);
      spec = null;
    }
  }

  private boolean isLoaded() {
    assert (document == null) || (spec == null) : "document non-null should mean spec is null";
    return document != null;
  }

  private void ensureLoaded() {
    if (!isLoaded()) {
      // ContentDocument currently requires a base set of registries in order to
      // function. Line-container stuff in particular.
      loadWith(base);
      assert isLoaded();
    }
  }

  private DiffSink getTarget() {
    return isLoaded() ? document : spec;
  }

  @Override
  public ContentDocument getDocument() {
    ensureLoaded();
    return document.getDocument();
  }

  /**
   * {@inheritDoc}
   *
   * The document is materialized if necessary.
   */
  @Override
  public Document getMutableDocument() {
    ensureLoaded();
    return this;
  }

  @Override
  public void init(SilentOperationSink<? super DocOp> outputSink) {
    this.outputSink = outputSink;
  }

  @Override
  public DocInitialization asOperation() {
    return getTarget().asOperation();
  }

  @Override
  public void consume(DocOp op) {
    if (shouldShowDiffs()) {
      getTarget().consumeAsDiff(op);
    } else {
      getTarget().consume(op);
    }
  }

  @Override
  public boolean flush(Runnable resume) {
    return isLoaded() ? document.getDocument().flush(resume) : true;
  }

  @Override
  public void startRendering(Registries registries, LogicalPanel logicalPanel) {
    ContentDocument document;
    if (!isLoaded()) {
      loadWith(registries);
      document = this.document.getDocument();
    } else {
      document = this.document.getDocument();
      document.setRegistries(registries);
    }
    document.setInteractive(logicalPanel);
    // ContentDocument does not render synchronously, so we have to force it
    // to finish, rather than reveal half-rendered content at the end of the
    // event cycle.
    AnnotationPainter.flush(document.getContext());
  }

  @Override
  public void stopRendering() {
    Preconditions.checkState(isLoaded());
    document.getDocument().setShelved();
  }

  //
  // Diff control.
  //

  private boolean diffsSuppressed;
  private boolean diffsRetained;

  @Override
  public boolean isCompleteDiff() {
    return isCompleteDiff;
  }

  private boolean shouldShowDiffs() {
    return !diffsSuppressed;
  }

  @Override
  public void startDiffSuppression() {
    Preconditions.checkState(!diffsSuppressed, "bad diff scope: ", diffsSuppressed, diffsRetained);
    diffsSuppressed = true;
    assert !shouldShowDiffs();
    getTarget().clearDiffs();
    isCompleteDiff = false;
  }

  @Override
  public void stopDiffSuppression() {
    Preconditions.checkState(diffsSuppressed, "bad diff scope: ", diffsSuppressed, diffsRetained);
    diffsSuppressed = false;
  }

  @Override
  public void startDiffRetention() {
    Preconditions.checkState(!diffsRetained, "bad diff scope: ", diffsSuppressed, diffsRetained);
    diffsRetained = true;
  }

  @Override
  public void stopDiffRetention() {
    Preconditions.checkState(diffsRetained, "bad diff scope: ", diffsSuppressed, diffsRetained);
    diffsRetained = false;
  }

  @Override
  public void clearDiffs() {
    if (diffsSuppressed) {
      // Clearing diffs is unnecessary.
    } else {
      if (diffsRetained) {
        // Clearing diffs is undesirable.
      } else {
        getTarget().clearDiffs();
        isCompleteDiff = false;
      }
    }
  }
}
