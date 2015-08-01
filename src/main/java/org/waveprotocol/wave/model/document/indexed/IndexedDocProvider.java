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

import static org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.NO_SCHEMA_CONSTRAINTS;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;

import java.util.Map;

/**
 * A factory for LookupNode-based indexed documents, parameterized by a
 * substrate factory for creating the underlying raw document.
 *
 */
public class IndexedDocProvider<N, E extends N, T extends N, D extends RawDocument<N, E, T>>
    implements IndexedDocument.Provider<IndexedDocument<N, E, T>> {

  private final RawDocument.Provider<D> substrateProvider;

  /**
   * @param substrateProvider
   * @return An IndexedDocProvider which will provide indexed docs based on the given
   *   substrate provider.
   */
  public static <N, E extends N, T extends N, D extends RawDocument<N, E, T>>
      IndexedDocProvider<N, E, T, D> create(RawDocument.Provider<D> substrateProvider) {
    return new IndexedDocProvider<N, E, T, D>(substrateProvider);
  }

  private IndexedDocProvider(RawDocument.Provider<D> substrateProvider) {
    this.substrateProvider = substrateProvider;
  }

  /**
   * Adapts a raw-document substrate as an indexed document (factory method).
   *
   * @param substrate  raw document to adapt
   */
  private IndexedDocument<N, E, T> adapt(D substrate, DocumentSchema schema) {
    AnnotationTree<Object> annotations =
      new AnnotationTree<Object>(ONE_OBJECT, ANOTHER_OBJECT, null);
    IndexedDocumentImpl<N, E, T, ?> doc = new IndexedDocumentImpl<N, E, T, Void>(substrate,
        annotations, schema);
    return doc;
  }

  @Override
  public IndexedDocument<N, E, T> create(String tag, Map<String, String> attributes) {
    return adapt(((RawDocument.Factory<D>) substrateProvider).create(tag, attributes),
        NO_SCHEMA_CONSTRAINTS);
  }

  @Override
  public IndexedDocument<N, E, T> parse(String text) {
    DocInitialization docInitialization;
    try {
      docInitialization = DocOpUtil.docInitializationFromXml(text);
    } catch (XmlParseException e) {
      throw new IllegalArgumentException(e);
    }
    return build(docInitialization, NO_SCHEMA_CONSTRAINTS);
  }

  private static final Object ONE_OBJECT = new Object();
  private static final Object ANOTHER_OBJECT = new Object();

  @Override
  public IndexedDocument<N, E, T> build(DocInitialization operation, DocumentSchema schema) {
    AnnotationTree<Object> annotations =
      new AnnotationTree<Object>(ONE_OBJECT, ANOTHER_OBJECT, null);
    IndexedDocumentImpl<N, E, T, ?> doc = new IndexedDocumentImpl<N, E, T, Void>(
        substrateProvider.create("doc", Attributes.EMPTY_MAP), annotations, schema);
    try {
      doc.consume(operation);
    } catch (OperationException e) {
      throw new OperationRuntimeException("Invalid initialization", e);
    }
    return doc;
  }

  /**
   * Creates an IndexedDocument from the provided operation, with the provided
   * handler installed to receive document events.
   */
  public IndexedDocument<N, E, T> build(DocInitialization operation, DocumentSchema schema,
      DocumentHandler<N, E, T> handler) throws OperationException {
    ObservableIndexedDocument<N, E, T, ?> doc =
        new ObservableIndexedDocument<N, E, T, Void>(handler,
            substrateProvider.create("doc", Attributes.EMPTY_MAP), schema);
    doc.consume(operation);
    return doc;
  }
}
