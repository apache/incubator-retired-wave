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

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;

/**
 * Wraps a {@link ContentDocument}, exposing its diff highlighting capabilities
 * in the language of a {@link DiffSink}.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class DiffContentDocument implements DiffSink {
  /** Regular document. */
  private final ContentDocument document;
  /** Sink that highlights ops that it consumes. */
  private final DiffHighlightingFilter differ;
  /** True when there are consumed diffs that have not been cleared. */
  private boolean hasDiffs;

  private DiffContentDocument(ContentDocument document, DiffHighlightingFilter differ) {
    this.document = document;
    this.differ = differ;
  }

  /**
   * Creates a diff-handling wrapper for a content document.
   */
  public static DiffContentDocument create(ContentDocument doc) {
    DiffHighlightingFilter differ = new DiffHighlightingFilter(doc.getDiffTarget());
    return new DiffContentDocument(doc, differ);
  }

  @Override
  public void consume(DocOp op) {
    Preconditions.checkState(!hasDiffs);
    document.consume(op);
  }

  @Override
  public void consumeAsDiff(DocOp op) {
    try {
      differ.consume(op);
    } catch (OperationException e) {
      throw new OperationRuntimeException("error applying diff op", e);
    }
    hasDiffs = true;
  }

  @Override
  public void clearDiffs() {
    differ.clearDiffs();
    hasDiffs = false;
  }

  @Override
  public DocInitialization asOperation() {
    return document.asOperation();
  }

  /** @return the underlying document. */
  ContentDocument getDocument() {
    return document;
  }
}
