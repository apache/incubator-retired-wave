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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOp.IsDocOp;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * An operation consumer that can consume operations in an additional mode (diff mode).
 * <p>
 * The relative semantics of {@link #consume} and {@link #consumeAsDiff} are:
 * <pre>
 *   consume(R1); ...; consume(Rn);
 *   consumeAsDiff(U1); ...; consumeAsDiff(Um);
 *   clearDiffs();
 * </pre>
 * is equivalent to:
 * <code>
 *   consume(R1 ; ... ; Rn);
 *   consumeAsDiff(U1 ; ... ; Um);
 *   clearDiffs();
 * </code>
 * which is also equivalent to:
 * <code>
 *   consume(R1 ; ... ; Rn ; U1 ; ... ; Um);
 * </code>
 * In short, consume() commutes with composition, consumeAsDiff() commutes
 * with composition, and clearDiffs() removes the specialness of the diff mode,
 * bringing this sink back into the same state it would have been if all ops
 * had been consumed regularly.
 * <p>
 * Not all interleavings of {@link #consume}, {@link #consumeAsDiff}, and
 * {@link #clearDiffs} are valid.  Specifically, the behavior of
 * {@link #consume} after {@link #consumeAsDiff}, without an intervening
 * {@link #clearDiffs} call, is undefined.  e.g.
 * <pre>
 *   consumeAsDiff(U1);
 *   consume(R1);
 * </prev>
 * Implementations will typically throw an IllegalStateException.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface DiffSink extends SilentOperationSink<DocOp>, IsDocOp {
  /**
   * Consumes an operation as a diff.
   *
   * @param op
   */
  void consumeAsDiff(DocOp op);

  /**
   * Removes the effect of any diffs, as if they had been {@link #consume(DocOp)
   * consumed} normally.
   */
  void clearDiffs();
}
