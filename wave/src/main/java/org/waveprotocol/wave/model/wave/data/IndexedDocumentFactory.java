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
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuffer;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.SchemaProvider;

/**
 * A factory for plain old non-observable indexed documents.
 *
 * A {@link DocumentOperationSink} produced by this factory does
 * not provides a mutable {@link Document}.
 *
 */
public final class IndexedDocumentFactory implements DocumentFactory<DocumentOperationSink> {

  private final SchemaProvider schemas;

  public IndexedDocumentFactory(SchemaProvider schemas) {
    this.schemas = schemas;
  }

  @Override
  public DocumentOperationSink create(final WaveletId waveletId, final String docId,
      final DocInitialization content) {
    return new DocumentOperationSink() {
      private final DocumentSchema schema = schemas.getSchemaForId(waveletId, docId);
      private final IndexedDocument<Node, Element, Text> document =
          DocProviders.POJO.build(content, schema);

      @Override
      public DocInitialization asOperation() {
        DocInitializationBuffer builder = new DocInitializationBuffer();
        document.asOperation().apply(builder);
        return builder.finish();
      }

      @Override
      public void consume(DocOp op) throws OperationException {
        document.consume(op);
      }

      @Override
      public Document getMutableDocument() {
        throw new UnsupportedOperationException(
            "This document implementation does not support mutable documents");
      }

      @Override
      public void init(SilentOperationSink<? super DocOp> outputSink) {
        throw new UnsupportedOperationException(
            "This document implementation does not support mutable documents");
      }
    };
  }
}
