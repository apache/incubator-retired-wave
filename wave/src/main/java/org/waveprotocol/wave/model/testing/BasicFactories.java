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

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.DocumentImpl;
import org.waveprotocol.wave.model.document.util.DocumentProvider;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.MuteDocumentFactory;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.PluggableMutableDocument;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.Map;

/**
 * Static factory for creating various document factories and builders whose
 * document schemas come from the provider set using
 * {@link #setSchemaProvider(SchemaProvider)}. If no provider is set the empty
 * provider is used. This should only be used for tests of the core model.
 *
 */
public class BasicFactories {

  /**
   * Provider of {@link Document}s based on the {@link DocProviders#POJO} DOM
   * implementation.
   */
  private static final DocumentProvider<Document> DOC_PROVIDER = new DocumentProvider<Document>() {
    @Override
    public Document create(String tagName, Map<String, String> attributes) {
      IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.create(tagName, attributes);
      return new DocumentImpl(DocProviders.createTrivialSequencer(doc), doc);
    }

    @Override
    public Document parse(String text) {
      IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse(text);
      return new DocumentImpl(DocProviders.createTrivialSequencer(doc), doc);
    }
  };

  /**
   * Provider of {@link ObservableDocument}s based on the
   * {@link DocProviders#POJO} DOM implementation and a trivial sequence.
   */
  private static final DocumentProvider<ObservablePluggableMutableDocument> OBS_DOC_PROVIDER =
      new DocumentProvider<ObservablePluggableMutableDocument>() {
        @Override
        public ObservablePluggableMutableDocument create(
            String tagName, Map<String, String> attributes) {
          // FIXME(ohler): this is inefficient.
          return build(DocProviders.POJO.create(tagName, attributes).asOperation());
        }

        @Override
        public ObservablePluggableMutableDocument parse(String text) {
          // FIXME(ohler): this is inefficient.
          return build(DocProviders.POJO.parse(text).asOperation());
        }

        private ObservablePluggableMutableDocument build(DocInitialization init) {
          ObservablePluggableMutableDocument doc =
              new ObservablePluggableMutableDocument(DocumentSchema.NO_SCHEMA_CONSTRAINTS, init);
          doc.init(SilentOperationSink.VOID);
          return doc;
        }
      };

  private static SchemaProvider schemas = SchemaCollection.empty();

  /**
   * Sets the schema provider that will provide schemas for the factories
   * returned from the methods of this class.
   */
  public static void setSchemaProvider(SchemaProvider value) {
    schemas = value;
  }

  /**
   * Returns the current schema provider.
   */
  protected static SchemaProvider getSchemas() {
    return schemas;
  }

  /**
   * Returns a new fake wave view builder whose document schemas comes from the
   * current provider.
   */
  public static FakeWaveView.Builder fakeWaveViewBuilder() {
    return FakeWaveView.builder(getSchemas());
  }

  /**
   * Returns a new op-based wavelet factory builder whose document schemas comes
   * from the current provider.
   */
  public static OpBasedWaveletFactory.Builder opBasedWaveletFactoryBuilder() {
    return OpBasedWaveletFactory.builder(getSchemas());
  }

  /**
   * Returns a new wavelet data impl factory whose document schemas comes from
   * the current provider.
   */
  public static WaveletDataImpl.Factory waveletDataImplFactory() {
    return WaveletDataImpl.Factory.create(observablePluggableMutableDocumentFactory());
  }

  /**
   * Returns a mute document factory whose document schemas comes from the
   * current provider.
   */
  public static MuteDocumentFactory muteDocumentFactory() {
    return new MuteDocumentFactory(getSchemas());
  }

  /**
   * Returns a fake document factory whose document schemas comes from the
   * current provider.
   */
  public static FakeDocument.Factory fakeDocumentFactory() {
    return FakeDocument.Factory.create(getSchemas());
  }

  /**
   * Returns a plugable mutable document factory whose document schemas comes
   * from the current provider.
   */
  public static DocumentFactory<? extends PluggableMutableDocument>
      pluggableMutableDocumentFactory() {
    return PluggableMutableDocument.createFactory(getSchemas());
  }

  /**
   * Returns an observable pluggable mutable document factory whose document
   * schemas comes from the current provider.
   */
  public static DocumentFactory<? extends ObservablePluggableMutableDocument>
      observablePluggableMutableDocumentFactory() {
    return ObservablePluggableMutableDocument.createFactory(getSchemas());
  }

  /**
   * Returns a provider of {@link Document}s.
   *
   * Provided documents have no schema constraints: consider using
   * {@link MuteDocumentFactory} instead.
   *
   * TODO(anorth): Remove this method in favor of one specifying a schema.
   */
  public static DocumentProvider<Document> documentProvider() {
    return DOC_PROVIDER;
  }

  /**
   * Returns a provider of observable mutable documents.
   *
   * Provided documents have no schema constraints: consider using
   * {@link MuteDocumentFactory} instead.
   *
   *  TODO(anorth): Change generic type to ObservableDocument after fixing
   * callers.
   *
   * TODO(anorth): Remove this method in favor of one specifying a schema.
   */
  public static DocumentProvider<ObservablePluggableMutableDocument> observableDocumentProvider() {
    return OBS_DOC_PROVIDER;
  }

  /**
   * Creates an observable mutable document with some schema, content, and sink.
   */
  public static ObservableDocument createDocument(DocumentSchema schema,
      String initialContent, SilentOperationSink<? super DocOp> sink) {
    Preconditions.checkNotNull(sink, "Sink can't be null");
    DocInitialization init = DocProviders.POJO.parse(initialContent).asOperation();
    ObservablePluggableMutableDocument doc = new ObservablePluggableMutableDocument(schema, init);
    doc.init(sink);
    return doc;
  }

  /**
   * Creates an observable mutable document with some schema and a sink.
   */
  public static ObservableDocument createDocument(
      DocumentSchema schema, SilentOperationSink<? super DocOp> sink) {
    return createDocument(schema, "", sink);
  }

  /**
   * Creates an observable mutable document with some schema.
   */
  public static ObservableDocument createDocument(DocumentSchema schema) {
    return createDocument(schema, "", SilentOperationSink.VOID);
  }

  /**
   * Creates an observable mutable document with some schema and initial content
   */
  public static ObservableDocument createDocument(
      DocumentSchema schema, String initialContent) {
    return createDocument(schema, initialContent, SilentOperationSink.VOID);
  }

  protected BasicFactories() {
  }
}
