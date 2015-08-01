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

package org.waveprotocol.wave.model.wave.data.impl;

import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.DocumentImpl;
import org.waveprotocol.wave.model.document.util.MutableDocumentProxy;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

/**
 * Mutable document that supports injection into a wave model. In order to be
 * used in a wave model, a mutable document's output sink must be directed into
 * the wave model. However, since there is a circular dependency between the
 * document and its sink, the sink is {@link #init(SilentOperationSink)
 * injected} after construction.
 *
 * Also supports schema validation
 *
 */
public class PluggableMutableDocument extends MutableDocumentProxy.DocumentProxy
    implements DocumentOperationSink {
  private static class DocumentCreationContext {
    final DocInitialization content;
    final DocumentSchema schema;
    final DocumentHandler<Node, Element, Text> handlerManager;

    DocumentCreationContext(DocInitialization content, DocumentSchema schema,
        DocumentHandler<Node, Element, Text> handlerManager) {
      this.content = content;
      this.schema = schema;
      this.handlerManager = handlerManager;
    }

    IndexedDocument<Node, Element, Text> createDocument() throws OperationException {
      return DocProviders.POJO.build(content, schema, handlerManager);
    }
  }

  /**
   * Factory.
   */
  public static DocumentFactory<? extends PluggableMutableDocument> createFactory(
      final SchemaProvider schemas) {
      return new DocumentFactory<PluggableMutableDocument>() {
        @Override
        public PluggableMutableDocument create(WaveletId waveletId, String docId,
            DocInitialization content) {
          return new PluggableMutableDocument(content, schemas.getSchemaForId(waveletId, docId));
        }
      };
  }

  /**
   * Dumb sequencer
   */
  private final static class BasicSequencer implements OperationSequencer<Nindo> {
    private final SuperSink sink;
    private final SilentOperationSink<? super DocOp> outputSink;

    private BasicSequencer(SuperSink sink,
        SilentOperationSink<? super DocOp> outputSink) {
      this.sink = sink;
      this.outputSink = outputSink;
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }

    /**
     * Applies the operation to this document, and then sends it to the output
     * sink.
     *
     * @param op mutation to apply
     */
    @Override
    public void consume(Nindo op) {
      try {
        DocOp docOp = sink.consumeAndReturnInvertible(op);
        if (outputSink != null) {
          outputSink.consume(docOp);
        }
      } catch (OperationException e) {
        throw new OperationRuntimeException(
            "DocumentOperationSink constructed by DocumentOperationSinkFactory "
                + "generated an OperationException when attempting to apply " + op, e);
      }
    }
  }

  /**
   *  Substrate underlying this document, it is only initialized when needed.  Once it is created,
   *  the variable never changes.
   */
  private IndexedDocument<Node, Element, Text> substrateDocument = null;

  /**
   * The contented needed to create the underlying substrate.  Only used until substrateDocument is
   * created and is freed immediately after.
   */
  private DocumentCreationContext documentCreationContext;

  private SilentOperationSink<? super DocOp> outputSink;

  /**
   * Creates a mutable document. This document will not be observable.
   *
   * @param content initialization content
   */
  private PluggableMutableDocument(DocInitialization content, DocumentSchema schema) {
    this(content, schema, null);
  }

  private IndexedDocument<Node, Element, Text> getDocument() {
    if (substrateDocument == null) {
      try {
        createSubstrateDocument();
      } catch (OperationException e) {
        throw new OperationRuntimeException(
            "Document initialization failed when applying operation: " +
            documentCreationContext.content, e);
      }
    }
    return substrateDocument;
  }

  /**
   * @throws OperationException
   */
  protected void createSubstrateDocument() throws OperationException {
    substrateDocument = documentCreationContext.createDocument();
    documentCreationContext = null;
  }

  /**
   * Creates a mutable document, where events are sent to a handler.
   *
   * @param content initialization content
   * @param handlerManager direct event receiver
   */
  protected PluggableMutableDocument(DocInitialization content, DocumentSchema schema,
      DocumentHandler<Node, Element, Text> handlerManager) {
    super(null, "Impossible");
    this.documentCreationContext = new DocumentCreationContext(content, schema, handlerManager);
  }

  @Override
  public void init(final SilentOperationSink<? super DocOp> outputSink) {
    Preconditions.checkState(this.outputSink == null, "Output sink may only be set once");
    Preconditions.checkArgument(outputSink != null, "Output sink may not be null");
    this.outputSink = outputSink;
  }

  @Override
  protected MutableDocument<N, E, T> getDelegate() {
    if (!hasDelegate()) {
      SilentOperationSink<? super DocOp> delegateSink;
      if (outputSink != null) {
        delegateSink = outputSink;
      } else {
        // Output sink not set yet.  That's ok, so long as ops aren't caused yet.
        delegateSink = new SilentOperationSink<DocOp>() {
          @Override
          public void consume(DocOp op) {
            Preconditions.checkState(outputSink != null, "Output sink not yet initialized");
            outputSink.consume(op);
          }
        };
      }
      DocumentImpl delegate =
          new DocumentImpl(new BasicSequencer(getDocument(), delegateSink), getDocument());
      setDelegate(delegate);
    }
    return super.getDelegate();
  }

  @Override
  public Document getMutableDocument() {
    // To allow downcast to observable...
    return this;
  }

  @Override
  public DocInitialization asOperation() {
    return getDocument().asOperation();
  }

  @Override
  public void consume(DocOp op) throws OperationException {
    getDocument().consume(op);
  }
}
