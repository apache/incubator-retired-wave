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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

/**
 * A document implementation and factory for use in tests.
 *
 */
public class FakeDocument extends ObservablePluggableMutableDocument {

  public static class Factory implements DocumentFactory<FakeDocument> {

    private final SchemaProvider schemas;

    public static Factory create(SchemaProvider schemas) {
      return new Factory(schemas);
    }

    private Factory(SchemaProvider schemas) {
      this.schemas = schemas;
    }

    private DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
      DocumentSchema result = schemas.getSchemaForId(waveletId, documentId);
      return (result != null) ? result : DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    }

    @Override
    public FakeDocument create(final WaveletId waveletId, final String blipId,
        DocInitialization content) {
      return new FakeDocument(content, getSchemaForId(waveletId, blipId));
    }
  }

  private DocOp consumed;

  public FakeDocument(DocInitialization initial, DocumentSchema schema) {
    super(schema, initial);
  }

  @Override
  public void consume(DocOp op) throws OperationException {
    super.consume(op);
    this.consumed = op;
  }

  public DocOp getConsumed() {
    return consumed;
  }

  @Override
  public String toString() {
    return toXmlString();
  }
}
