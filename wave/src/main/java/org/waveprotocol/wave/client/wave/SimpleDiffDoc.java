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

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;

/**
 * Represents a diff document as a pair of an initialization (containing all the
 * read information) and an op (containing all the unread information).
 * <p>
 * This object is mutable by operation consumption.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SimpleDiffDoc implements DiffSink {
  /** The base state.  Never null. */
  private DocInitialization state;

  /** The diff.  Null means no-op. */
  private DocOpCollector diff;

  /**
   * Creates a diff initialization.
   */
  private SimpleDiffDoc(DocInitialization base) {
    this.state = base;
  }

  /**
   * Creates a diff initialization.
   */
  public static SimpleDiffDoc create(DocInitialization base, DocOp diff) {
    Preconditions.checkNotNull(base);
    SimpleDiffDoc init = new SimpleDiffDoc(base);
    if (diff != null) {
      init.consumeAsDiff(diff);
    }
    return init;
  }

  @Override
  public void consume(DocOp op) {
    // Rebase is not supported.
    Preconditions.checkState(diff == null, "Can not apply non-diff ops while diffs still exist");
    state = compose(state, op);
  }

  @Override
  public void consumeAsDiff(DocOp op) {
    if (diff == null) {
      diff = new DocOpCollector();
    }
    diff.add(op);
  }

  @Override
  public void clearDiffs() {
    state = asOperation();
    diff = null;
  }

  /**
   * Applies this state (base + diff) to another diff-aware target.
   *
   * @param target target to which this diff state is pushed
   */
  void applyTo(DiffSink target) {
    target.consume(state);
    if (diff != null) {
      target.consumeAsDiff(diff.composeAll());
    }
  }

  @Override
  public DocInitialization asOperation() {
    return compose(state, diff != null ? diff.composeAll() : null);
  }

  /** @return true iff this is just a diff (there is no base state). */
  boolean isCompleteDiff() {
    return state.size() == 0;
  }

  @VisibleForTesting
  boolean isCompleteState() {
    return state.size() > 0 && (diff == null || diff.isEmpty());
  }

  /**
   * @return this state into a single operation.
   */
  private static DocInitialization compose(DocInitialization state, DocOp diff) {
    try {
      return diff != null ? Composer.compose(state, diff) : state;
    } catch (OperationException e) {
      throw new OperationRuntimeException("error occurred during diff compaction", e);
    }
  }
}
