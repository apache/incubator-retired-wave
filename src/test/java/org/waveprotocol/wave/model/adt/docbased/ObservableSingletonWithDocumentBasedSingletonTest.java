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

package org.waveprotocol.wave.model.adt.docbased;

import org.waveprotocol.wave.model.adt.ObservableSingletonTestBase;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.UncheckedDocOpBuffer;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Tests for the document-based singleton.
 *
 * @author anorth@google.com (Alex North)
 */
public class ObservableSingletonWithDocumentBasedSingletonTest extends ObservableSingletonTestBase {

  private static final String TAG = "int";
  private static final String ATTR = "v";
  private static final Factory<Doc.E, Integer, String> FACTORY = TestUtil.newIntegerFactory(ATTR);

  private ObservableDocument doc;
  private DocumentBasedSingleton<Integer, String> target;
  private Listener listener;
  private DocEventRouter router;

  @Override
  protected DocumentBasedSingleton<Integer, String> createSingleton() {
    return createSingleton(DefaultDocEventRouter.create(BasicFactories.createDocument(
        DocumentSchema.NO_SCHEMA_CONSTRAINTS)));
  }

  /**
   * Creates a singleton backed by a document root.
   */
  private static DocumentBasedSingleton<Integer, String> createSingleton(DocEventRouter router) {
    return DocumentBasedSingleton.create(router, router.getDocument().getDocumentElement(),
        TAG, FACTORY);
  }

  // Document-based implementation specific tests.

  @Override
  public void setUp() {
    super.setUp();
    target = createSingleton();
    doc = target.getDocument();
    router = target.getEventRouter();
    listener = new Listener();
  }

  // Initialization.
  public void testInitialStateIsReadable() {
    remoteInsertCanonicalValue("42");
    DocumentBasedSingleton<Integer, String> other = createSingleton(router);
    assertValue(other, 42);
  }

  // Concurrent behaviour and events.

  public void testRemoteChangesUpdateState() {
    remoteInsertCanonicalValue("42");
    assertValue(target, 42);

    remoteInsertCanonicalValue("43");
    assertValue(target, 43);

    remoteClearValue();
    assertNoValue(target);
  }

  public void testRemoteChangesGenerateEvents() {
    target.addListener(listener);

    remoteInsertCanonicalValue("42");
    listener.verifyValueChanged(null, 42);

    remoteInsertCanonicalValue("43");
    listener.verifyValueChanged(42, 43);

    remoteClearValue();
    listener.verifyValueChanged(43, null);
  }

  public void testRemoteChangeAfterInitializationGeneratesEvent() {
    remoteInsertCanonicalValue("42");
    DocumentBasedSingleton<Integer, String> other = createSingleton(router);

    other.addListener(listener);
    remoteInsertCanonicalValue("43");
    listener.verifyValueChanged(42, 43);
  }

  public void testAdditionOfNonCanonicalValueChangesNothing() {
    target.set("42");
    target.addListener(listener);
    remoteInsertRedundantValue("43");
    assertValue(target, 42);
    listener.verifyNoEvent();
  }

  public void testRemovalOfNonCanonicalValueChangesNothing() {
    target.set("42");
    remoteInsertRedundantValue("43");
    target.addListener(listener);
    remoteRemoveRedundantValues();
    assertValue(target, 42);
    listener.verifyNoEvent();
  }

  public void testRemovalOfCanonicalValueSetsNewValue() {
    target.set("42");
    remoteInsertRedundantValue("43");
    target.addListener(listener);
    remoteRemoveCanonicalValue();
    assertValue(target, 43);
    listener.verifyValueChanged(42, 43);
  }

  // Cleanup.

  public void testClearRemovesRedundantValues() {
    target.set("42");
    remoteInsertRedundantValue("43");
    target.addListener(listener);
    target.clear();

    assertNoValue(target);
    listener.verifyValueChanged(42, null);
  }

  public void testSetClearsRedundantValues() {
    target.set("42");
    remoteInsertRedundantValue("43");
    target.addListener(listener);
    target.set("44");

    listener.verifyValueChanged(42, 44);
    assertSame(DocHelper.getElementWithTagName(doc, TAG),
        DocHelper.getLastElementWithTagName(doc, TAG));
  }

  public void testAtomicReplacementOfEquivalentFiresNoEvents() {
    target.set("42");
    DocOp replacement = createReplaceOp(createErasureOp(), createRestoreOp());
    target.addListener(listener);
    doc.hackConsume(Nindo.fromDocOp(replacement, false));
    listener.verifyNoEvent();
  }

  public void testAtomicReplacementFiresSingleEvent() {
    // Build "insert 42" state.
    target.set("42");
    DocOp restore = createRestoreOp();

    // Build "delete 43" state.
    target.set("43");
    DocOp erasure = createErasureOp();

    DocOp restoration = createReplaceOp(erasure, restore);
    target.addListener(listener);
    doc.hackConsume(Nindo.fromDocOp(restoration, false));
    listener.verifyValueChanged(43, 42);
  }

  /**
   * Inserts a first-child element.
   */
  private void remoteInsertCanonicalValue(String value) {
    Doc.N firstChild = doc.getFirstChild(doc.getDocumentElement());
    Point<Doc.N> insertion = Point.<Doc.N>inElement(doc.getDocumentElement(), firstChild);
    doc.createElement(insertion, TAG, CollectionUtils.immutableMap(ATTR, value));
  }

  /**
   * Inserts a last-child element.
   */
  private void remoteInsertRedundantValue(String value) {
    doc.createChildElement(doc.getDocumentElement(), TAG,
        CollectionUtils.immutableMap(ATTR, value));
  }

  private void remoteClearValue() {
    Doc.E toDelete = DocHelper.getLastElementWithTagName(doc, TAG, doc.getDocumentElement());
    while (toDelete != null) {
      doc.deleteNode(toDelete);
      toDelete = DocHelper.getLastElementWithTagName(doc, TAG, doc.getDocumentElement());
    }
  }

  private void remoteRemoveRedundantValues() {
    Doc.E canonical = DocHelper.getElementWithTagName(doc, TAG);
    Doc.E toDelete = DocHelper.getLastElementWithTagName(doc, TAG, doc.getDocumentElement());
    while (toDelete != canonical) {
      doc.deleteNode(toDelete);
      toDelete = DocHelper.getLastElementWithTagName(doc, TAG, doc.getDocumentElement());
    }
  }

  private void remoteRemoveCanonicalValue() {
    Doc.E canonical = DocHelper.getElementWithTagName(doc, TAG);
    doc.deleteNode(canonical);
  }

  /** Creates an op that restores the doc's current state. */
  private DocOp createRestoreOp() {
    UncheckedDocOpBuffer builder = new UncheckedDocOpBuffer();
    doc.toInitialization().apply(builder);
    return builder.finish();
  }

  /** Creates an op that deletes the doc's current state. */
  private DocOp createErasureOp() {
    UncheckedDocOpBuffer builder = new UncheckedDocOpBuffer();
    doc.toInitialization().apply(builder);
    return DocOpInverter.invert(builder.finish());
  }

  private static DocOp createReplaceOp(DocOp erase, DocOp restore) {
    try {
      return Composer.compose(erase, restore);
    } catch (OperationException e) {
      // If the code above fails, then there is a bug in the operation code, not
      // these tests. Fail with an exception, not with a JUnit fail().
      throw new RuntimeException(e);
    }
  }
}
