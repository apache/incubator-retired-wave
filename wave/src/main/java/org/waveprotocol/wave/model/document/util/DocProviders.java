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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.IndexedDocProvider;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.OperationSink;

import java.util.Map;

/**
 * Useful open implementations of {@code DocumentProvider} go here
 */
public class DocProviders {
  /**
   * RawDocument provider based on RawDocumentImpl as the document
   * implementation.
   */
  public final static RawDocument.Provider<RawDocumentImpl> ROJO =
      RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER);

  /**
   * IndexedDocumentProvider with a substrate based on the "Rojo" dom
   * implementation
   */
  public final static IndexedDocProvider<Node, Element, Text, RawDocumentImpl> POJO =
      IndexedDocProvider.create(ROJO);

  /**
   * Provider of {@link MutableDocumentImpl}s with substrates based on the
   * "DocProviders.POJO" dom implementation and a trivial sequencer.
   */
  public final static DocumentProvider<MutableDocumentImpl<Node, Element, Text>> MOJO =
      new DocumentProvider<MutableDocumentImpl<Node, Element, Text>>() {
        @Override
        public MutableDocumentImpl<Node, Element, Text> create(String tagName,
            Map<String, String> attributes) {
          IndexedDocument<Node, Element, Text> doc = POJO.create(tagName, attributes);
          return new MutableDocumentImpl<Node, Element, Text>(createTrivialSequencer(doc), doc);
        }

        @Override
        public MutableDocumentImpl<Node, Element, Text> parse(String text) {
          IndexedDocument<Node, Element, Text> doc = POJO.parse(text);
          return new MutableDocumentImpl<Node, Element, Text>(createTrivialSequencer(doc), doc);
        }
      };

  /**
   * Creates a document which applies generated operations to a copy of
   * itself, thus ensuring those ops are valid when received remotely.
   */
  public static Document createValidatingDocument(DocumentSchema schema) {
    IndexedDocument<Node, Element, Text> indexedDoc =
        POJO.build(new DocInitializationBuilder().build(), schema);
    return new DocumentImpl(createCopyingSequencer(indexedDoc), indexedDoc);
  }

  /**
   * A simple sequencer
   * @param doc the document to apply non-invertible ops to, and get the invertible ones from
   */
  public final static <N, E extends N> OperationSequencer<Nindo> createTrivialSequencer(
      IndexedDocument<N, E, ? extends N> doc) {

    return createTrivialSequencer(doc, null);
  }

  /**
   * A simple sequencer
   * @param doc the document to apply non-invertible ops to, and get the invertible ones from
   * @param outputSink optional, may be null.
   */
  public final static <N, E extends N> OperationSequencer<Nindo> createTrivialSequencer(
      final IndexedDocument<N, E, ? extends N> doc, final OperationSink<DocOp> outputSink) {
    return new OperationSequencer<Nindo>() {
      @Override
      public void begin() {
      }

      @Override
      public void end() {
      }

      @Override
      public void consume(Nindo op) {
        try {
          DocOp docOp = doc.consumeAndReturnInvertible(op);
          if (outputSink != null) {
            outputSink.consume(docOp);
          }
        } catch (OperationException oe) {
          throw new OperationRuntimeException("DocProviders trivial sequencer consume failed.", oe);
        }
      }
    };
  }

  /**
   * Creates a sequencer that applies outgoing operations to a copy of the given
   * document, ensuring that both incoming and outgoing ops are valid.
   *
   * This is not implemented efficiently, intended for use in unit tests only.
   *
   * @param doc document to copy
   */
  public final static <N, E extends N> OperationSequencer<Nindo> createCopyingSequencer(
      IndexedDocument<N, E, ? extends N> doc) {
    final IndexedDocument<Node, Element, Text> copy = POJO.build(
        doc.asOperation(), doc.getSchema());
    return createTrivialSequencer(doc, new OperationSink<DocOp>() {
      @Override
      public void consume(DocOp op) throws OperationException {
        // Should throw exceptions if the op is invalid.
        copy.consume(op);
      }
    });
  }

  protected DocProviders() {
  }
}
