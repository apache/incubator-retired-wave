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

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableDocumentOperationSink;

/**
 * Extension of a mutable-document wrapper that exposes listener addition and
 * removal (i.e., makes it observable).
 *
 */
public class ObservablePluggableMutableDocument extends PluggableMutableDocument implements
    ObservableDocumentOperationSink<Doc.N, Doc.E, Doc.T>,
    ObservableDocument {

  /**
   * Factory.
   */
  public static DocumentFactory<ObservablePluggableMutableDocument> createFactory(
      final SchemaProvider schemas) {
    return new DocumentFactory<ObservablePluggableMutableDocument>() {
        @Override
        public ObservablePluggableMutableDocument create( // \u2620
            WaveletId waveletId, String docId, DocInitialization content) {
          return new ObservablePluggableMutableDocument(
              schemas.getSchemaForId(waveletId, docId), content);
        }
      };
  }

  /**
   * Fanning broadcast handler
   */
  private final static class DocumentHandlerManager // \u2620
      implements DocumentHandler<Node, Element, Text> {
    private final CopyOnWriteSet<DocumentHandler<Doc.N, Doc.E, Doc.T>> handlers =
        CopyOnWriteSet.create();
    private boolean isPaused = false;

    // Conversion from Node -> Doc.N, etc. This is safe because Doc<N, E, T> types are
    // covariant in their node type parameters, it's just this cannot be expressed
    // at definition time due to a limitation of java, and must be expressed at use
    // time with wild cards (which then propagate everywhere, causing nastiness).
    //
    // Avoiding use of call-site wild cards with an unchecked conversion instead.
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onDocumentEvents(EventBundle event) {
      if (isPaused) {
        return;
      }
      for (DocumentHandler<Doc.N, Doc.E, Doc.T> handler : handlers) {
        handler.onDocumentEvents(event);
      }
    }

    void addHandler(DocumentHandler<Doc.N, Doc.E, Doc.T> h) {
      handlers.add(h);
    }

    void removeHandler(DocumentHandler<Doc.N, Doc.E, Doc.T> h) {
      handlers.remove(h);
    }

    private void setPaused(boolean isPaused) {
      this.isPaused = isPaused;
    }
  }

  /** The direct listener to inject into the (observable) indexed document. */
  private final DocumentHandlerManager handlerManager;

  /**
   * Creates an observable document.
   *
   * @param content  initialization content
   */
  public ObservablePluggableMutableDocument(DocumentSchema schema, DocInitialization content) {
    this(schema, content, new DocumentHandlerManager());
  }

  /**
   * Creates an observable document
   *
   * @param content  initialization content
   * @param handlerManager  event broadcaster
   */
  private ObservablePluggableMutableDocument(DocumentSchema schema, DocInitialization content,
      DocumentHandlerManager handlerManager) {
    super(content, schema, handlerManager);
    this.handlerManager = handlerManager;
  }

  @Override
  public void with(final ObservableMutableDocument.Action actionToRunWithDocument) {
    super.with(new MutableDocument.Action() {
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        actionToRunWithDocument.exec(ObservablePluggableMutableDocument.this);
      }});
  }

  @Override
  public <V> V with(final ObservableMutableDocument.Method<V> methodToRunWithDocument) {
    return super.with(new MutableDocument.Method<V>() {
      public <N, E extends N, T extends N> V exec(MutableDocument<N, E, T> doc) {
        return methodToRunWithDocument.exec(ObservablePluggableMutableDocument.this);
      }
    });
  }

  @Override
  public void addListener(DocumentHandler<Doc.N, Doc.E, Doc.T> listener) {
    handlerManager.addHandler(listener);
  }

  @Override
  public void removeListener(DocumentHandler<Doc.N, Doc.E, Doc.T> listener) {
    handlerManager.removeHandler(listener);
  }

  @Override
  protected void createSubstrateDocument() throws OperationException {
    handlerManager.setPaused(true);
    super.createSubstrateDocument();
    handlerManager.setPaused(false);
  }
}
