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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;

/**
 * A DOM-style document with indexed nodes.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author alexmah@google.com (Alexandre Mah)
 *
 * @param <N> The type of DOM nodes.
 * @param <E> The type of DOM Element nodes.
 * @param <T> The type of DOM Text nodes.
 */
public interface IndexedDocument<N, E extends N, T extends N> extends
    ReadableWDocument<N, E, T>, TextNodeOrganiser<T>, SuperSink {

  /**
   * Specialization of {@link ReadableDocument.Provider} for {@link IndexedDocument}.
   *
   * @param <D> document type produced
   */
  interface Provider<D extends IndexedDocument<?,?,?>> extends ReadableDocument.Provider<D> {

    /**
     * Creates a document from the provided operation
     *
     * The initialization MUST match the schema
     *
     * @return The created document
     */
    D build(DocInitialization operation, DocumentSchema schema);
  }

  DocumentSchema getSchema();
}
