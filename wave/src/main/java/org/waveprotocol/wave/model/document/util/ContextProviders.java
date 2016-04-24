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

import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.indexed.AnnotationSetListener;
import org.waveprotocol.wave.model.document.indexed.AnnotationTree;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.indexed.ObservableIndexedDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext.MiscListener;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.util.Box;

import java.util.Iterator;
import java.util.Map;

/**
 * Document context providers
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ContextProviders {

  /**
   * Extension for testing purposes, exposing some more internals
   */
  public interface TestDocumentContext<N, E extends N, T extends N>
      extends DocumentContext<N, E, T> {

    public interface MiscListener {
//      void onBegin();
//      void onFinish();
      void onSchedulePaint(Node node);
    }

    RawDocument<N, E, T> getFullRawDoc();

    RawDocument<N, E, T> getPersistentRawDoc();

    /** Gets the indexed doc */
    IndexedDocument<N, E, T> getIndexedDoc();
  }

  public static class LocalDocImpl<N, E extends N, T extends N> extends IdentityView<N, E, T>
      implements LocalDocument<N, E, T> {

    private final WritableLocalDocument<N, E, T> writable;

    public LocalDocImpl(RawDocument<N, E, T> fullDoc, WritableLocalDocument<N, E, T> local) {
      super(fullDoc);
      this.writable = local;
    }
    @Override
    public <T> T getProperty(Property<T> property, E element) {
      return writable.getProperty(property, element);
    }
    @Override
    public boolean isDestroyed(E element) {
      return writable.isDestroyed(element);
    }
    @Override
    public <T> void setProperty(Property<T> property, E element, T value) {
      writable.setProperty(property, element, value);
    }
    @Override
    public T transparentCreate(String text, E parent, N nodeAfter) {
      return writable.transparentCreate(text, parent, nodeAfter);
    }
    @Override
    public E transparentCreate(String tagName, Map<String, String> attributes,
        E parent, N nodeAfter) {
      return writable.transparentCreate(tagName, attributes, parent, nodeAfter);
    }
    @Override
    public void transparentSetAttribute(E element, String name, String value) {
      writable.transparentSetAttribute(element, name, value);
    }
    @Override
    public void transparentDeepRemove(N node) {
      writable.transparentDeepRemove(node);
    }
    @Override
    public void transparentMove(E newParent, N fromIncl, N toExcl, N refChild) {
      writable.transparentMove(newParent, fromIncl, toExcl, refChild);
    }
    @Override
    public N transparentSlice(N splitAt) {
      return writable.transparentSlice(splitAt);
    }
    @Override
    public void transparentUnwrap(E element) {
      writable.transparentUnwrap(element);
    }
    @Override
    public void markNodeForPersistence(N localNode, boolean lazy) {
      writable.markNodeForPersistence(localNode, lazy);
    }
    @Override
    public boolean isTransparent(N node) {
      return writable.isTransparent(node);
    }
  }

  /**
   * @param docHandler
   * @return a self-contained document context suitable for testing
   */
  public static TestDocumentContext<Node, Element, Text> createTestPojoContext2(
      final String initialInnerXml,
      final DocumentHandler<Node, Element, Text> docHandler,
      final AnnotationRegistry annotationRegistry,
      final MiscListener miscListener,
      final DocumentSchema schemaConstraints) {

    final Box<TestDocumentContext<Node, Element, Text>> box = Box.create();

    return box.boxed = createTestPojoContext(initialInnerXml,
        docHandler, new AnnotationSetListener<Object>() {
            @Override
            public void onAnnotationChange(int start, int end, String key, Object newValue) {
              Iterator<AnnotationMutationHandler> handlers = annotationRegistry.getHandlers(key);
              while (handlers.hasNext()) {
                handlers.next().handleAnnotationChange(
                    box.boxed, start, end, key, newValue);
              }
            }
          }, miscListener, schemaConstraints);
  }

  /**
   * @param docHandler
   * @param annotationSetListener
   * @return a self-contained document context suitable for testing
   */
  public static TestDocumentContext<Node, Element, Text> createTestPojoContext(
      final String initialInnerXml,
      final DocumentHandler<Node, Element, Text> docHandler,
      final AnnotationSetListener<Object> annotationSetListener,
      final MiscListener miscListener,
      final DocumentSchema schemaConstraints) {

    return createTestPojoContext(
        // FIXME(ohler): it's a bit weird that we parse into an IndexedDocument just to
        // get its asOperation().
        DocProviders.POJO.parse(initialInnerXml).asOperation(),
        docHandler, annotationSetListener, miscListener, schemaConstraints);
  }

  /**
   * @param docHandler
   * @param annotationSetListener
   * @return a self-contained document context suitable for testing
   */
  public static TestDocumentContext<Node, Element, Text> createTestPojoContext(
      final DocInitialization initialContent,
      final DocumentHandler<Node, Element, Text> docHandler,
      final AnnotationSetListener<Object> annotationSetListener,
      final MiscListener miscListener,
      final DocumentSchema schemaConstraints) {

    final AnnotationSetListener<Object> annotationListener = annotationSetListener != null
        ? annotationSetListener : new AnnotationSetListener<Object>() {
          @Override
          public void onAnnotationChange(int start, int end, String key, Object newValue) {
            // Do nothing
          }
        };

    TestDocumentContext<Node, Element, Text> documentContext =
        new TestDocumentContext<Node, Element, Text>() {
          private final RawDocument<Node, Element, Text> fullDoc =
              RawDocumentImpl.PROVIDER.create("doc", Attributes.EMPTY_MAP);
          private final PersistentContent<Node, Element, Text> persistentDoc =
              new RepaintingPersistentContent<Node, Element, Text>(
                  fullDoc, Element.ELEMENT_MANAGER) {
                @Override
                protected void schedulePaint(Node node) {
                  if (miscListener != null) {
                    miscListener.onSchedulePaint(node);
                  }
                }
              };



          AnnotationTree<Object> fullAnnotations = new AnnotationTree<Object>(
              "a", "b", annotationListener);

          private final LocalDocImpl<Node, Element, Text> localDoc =
              new LocalDocImpl<Node, Element, Text>(fullDoc, persistentDoc);

          private final IndexedDocument<Node, Element, Text> indexedDoc =
              new ObservableIndexedDocument<Node, Element, Text, Void>(
                  docHandler, persistentDoc, fullAnnotations, schemaConstraints) {
//              @Override
//              public void begin() {
//                super.begin();
//                if (miscListener != null) {
//                  miscListener.onBegin();
//                }
//              }
//
//              @Override
//              public void finish() {
//                if (miscListener != null) {
//                  miscListener.onFinish();
//                }
//                super.finish();
//              }
            };

          private final MutableDocument<Node, Element, Text> mutableDoc =
              new MutableDocumentImpl<Node, Element, Text>(
                  DocProviders.createTrivialSequencer(indexedDoc, null), indexedDoc);

          private final LocalAnnotationSetImpl localAnnotations =
              new LocalAnnotationSetImpl(fullAnnotations);

          @Override
          public LocalDocument<Node, Element, Text> annotatableContent() {
            return localDoc;
          }
          @Override
          public MutableDocument<Node, Element, Text> document() {
            return mutableDoc;
          }
          @Override
          public ElementManager<Element> elementManager() {
            return Element.ELEMENT_MANAGER;
          }
          @Override
          public MutableAnnotationSet.Local localAnnotations() {
            return localAnnotations;
          }
          @Override
          public LocationMapper<Node> locationMapper() {
            return indexedDoc;
          }
          @Override
          public ReadableDocumentView<Node, Element, Text> persistentView() {
            return persistentDoc;
          }
          @Override
          public ReadableDocumentView<Node, Element, Text> hardView() {
            return persistentDoc.hardView();
          }
          @Override
          public TextNodeOrganiser<Text> textNodeOrganiser() {
            return indexedDoc;
          }
          @Override
          public IndexedDocument<Node, Element, Text> getIndexedDoc() {
            return indexedDoc;
          }
          @Override
          public RawDocument<Node, Element, Text> getFullRawDoc() {
            return fullDoc;
          }
          @Override
          public RawDocument<Node, Element, Text> getPersistentRawDoc() {
            return persistentDoc;
          }
        };

    try {
      documentContext.getIndexedDoc().consume(initialContent);
    } catch (OperationException e) {
      throw new OperationRuntimeException("Invalid constructing op", e);
    }

    return documentContext;
  }
}
