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

package org.waveprotocol.wave.model.wave.data;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationSink;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.wave.OpaqueDocument;

/**
 * An extension of the {@link OpaqueDocument} interface that includes an opaque
 * mutator.
 *
 */
public interface DocumentOperationSink extends OpaqueDocument, OperationSink<DocOp> {

  /**
   * Sets the sink to which the {@link #getMutableDocument() mutable document}
   * sends generated operations. This must be called before a mutable document
   * is extracted, and may only be called once.
   *
   * HACK(user): this is a temporary measure to allow domain-specific
   * document implementations to be injected into the model, while still being
   * hooked up with operations. Ideally, this layer of the stack has no
   * knowledge of operations, mutable views, nor operation sinks. This mechanism
   * will be removed once the model has been reimplemented such that it no
   * longer requires DocumentOperationSink.
   *
   * TODO(hearnden, danilatos) : Remove this hack once the model properly allows
   * for MutableDocuments.
   *
   * @param outputSink
   * @throws IllegalStateException if called more than once, or with null.
   */
  void init(SilentOperationSink<? super DocOp> outputSink);

  /**
   *
   * Gets a mutable view of this document.
   *
   * This method is only optionally supported - if you are calling this, make
   * sure you know what you are doing!
   *
   * This may only be called after this document has been
   * {@link #init(SilentOperationSink) initialized}.
   *
   * May throw exceptions in most contexts.
   *
   * HACK(user): this is a temporary measure to allow domain-specific
   * document implementations to be injected into the model, while still being
   * hooked up with operations. Ideally, this layer of the stack has no
   * knowledge of operations, mutable views, nor operation sinks. This mechanism
   * will be removed once the model has been reimplemented such that it no
   * longer requires DocumentOperationSink.
   *
   * TODO(hearnden, danilatos) : Remove this hack once the model properly allows
   * for MutableDocuments.
   *
   * @return a mutable document.
   * @throws IllegalStateException if not initialised
   * @throws UnsupportedOperationException if mutable documents are not
   *         supported in the calling context.
   */
  Document getMutableDocument();
}
