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

package org.waveprotocol.wave.client.editor.content;


import org.waveprotocol.wave.client.editor.testing.TestEditors;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.DomOperationUtil;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.ReadableTreeWalker;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Tests some transparent/persistent operations on a ContentDocument
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class LazyPersistentContentDocumentGwtTest extends EditorGwtTestCase {
  /** Simplest use of persistence, creating a node locally then persisting it. */
  public void testSimpleForcedPersist() {
    // set up document, with queue for outgoing messages.
    final Queue<DocOp> opMessageQueue = new LinkedList<DocOp>();
    ContentDocument doc = TestEditors.createTestDocument();
    // Note: this replaces the output sink that the editor has injected into the
    // document, breaking an editor assumption.
    doc.replaceOutgoingSink(new SilentOperationSink<DocOp>(){
      @Override
      public void consume(DocOp op) {
        opMessageQueue.add(op);
      }
    });

    // initial document, with simple content:
    doc.consume(DocProviders.POJO.parse("<root></root>").asOperation());
    assertTrue(opMessageQueue.isEmpty()); // came from server, nothing sent out
    LocalDocument<ContentNode, ContentElement, ContentTextNode> local
        = doc.getContext().annotatableContent();
    ContentElement root = local.getFirstChild(local.getDocumentElement()).asElement();

    // create local text node:
    ContentElement child = local.transparentCreate("child",
        Collections.<String, String>emptyMap(), root, null);
    assertTrue(opMessageQueue.isEmpty()); // local, nothing used.
    assertFalse(checkDomEqual(doc.getMutableDoc(), local));

    // forcibly persist:
    local.markNodeForPersistence(child, false);
    popServerOp(opMessageQueue, "__1; << child {}; >>; __1; "); // sent
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));
    ContentElement mutableChild = doc.getMutableDoc().getFirstChild(root).asElement();
    assertTrue(child == mutableChild); // exact same object

    // persist again, just to make sure it's fine
    local.markNodeForPersistence(child, false);
    assertTrue(opMessageQueue.isEmpty()); // nothing new to send
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));
  }

  /**
   * Simple use of lazy persistence, creating a node locally, marking to persist,
   * then persisting due to (say) a user action.
   */
  public void testSimpleLazyPersist() {
    // set up document, with queue for outgoing messages.
    final Queue<DocOp> opMessageQueue = new LinkedList<DocOp>();
    ContentDocument doc = TestEditors.createTestDocument();
    // Note: this replaces the output sink that the editor has injected into the
    // document, breaking an editor assumption.
    doc.replaceOutgoingSink(new SilentOperationSink<DocOp>(){
      @Override
      public void consume(DocOp op) {
        opMessageQueue.add(op);
      }
    });

    // initial document, with simple content:
    doc.consume(DocProviders.POJO.parse("<root></root>").asOperation());
    assertTrue(opMessageQueue.isEmpty()); // came from server, nothing sent out
    LocalDocument<ContentNode, ContentElement, ContentTextNode> local
        = doc.getContext().annotatableContent();
    ContentElement root = local.getFirstChild(local.getDocumentElement()).asElement();

    // create local text node:
    ContentElement child = local.transparentCreate("child",
        Collections.<String, String>emptyMap(), root, null);
    assertTrue(opMessageQueue.isEmpty()); // local, nothing used.
    assertFalse(checkDomEqual(doc.getMutableDoc(), local));

    // Make sure it doesn't get included in a filter:
    Point<ContentNode> filtered = DocHelper.getFilteredPoint(
        doc.getPersistentView(), Point.start(doc.getFullContentView(), child));
    // filter container to be the root persisted node
    assertTrue(filtered.getContainer() == root);

    // mark to lazily persist:
    local.markNodeForPersistence(child, true);
    assertTrue(opMessageQueue.isEmpty()); // still local, nothing used.
    assertFalse(checkDomEqual(doc.getMutableDoc(), local));

    // now require it to be persisted:
    filtered = DocHelper.getFilteredPoint(
        doc.getPersistentView(), Point.start(doc.getFullContentView(), child));

    popServerOp(opMessageQueue, "__1; << child {}; >>; __1; "); // sent
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));
    ContentElement mutableChild = doc.getMutableDoc().getFirstChild(root).asElement();
    assertTrue(child == mutableChild); // exact same object
    assertTrue(filtered.getContainer() == child); // different container now.

    // persist again, just to make sure it's fine
    local.markNodeForPersistence(child, false);
    assertTrue(opMessageQueue.isEmpty()); // nothing new to send
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));
  }

  /** Using a complex tree, make sure state is correct before and after an opaque persist call. */
  public void testComplexSingleOpaquePersist() {
    // set up document, with queue for outgoing messages.
    final Queue<DocOp> opMessageQueue = new LinkedList<DocOp>();
    ContentDocument doc = TestEditors.createTestDocument();
    // Note: this replaces the output sink that the editor has injected into the
    // document, breaking an editor assumption.
    doc.replaceOutgoingSink(new SilentOperationSink<DocOp>(){
      @Override
      public void consume(DocOp op) {
        opMessageQueue.add(op);
      }
    });

    // initial document, with content and annotation:
    doc.consume(DocProviders.POJO.parse("<a><b>hi</b> world</a>").asOperation());
    assertTrue(opMessageQueue.isEmpty()); // came from server, nothing sent out
    doc.getMutableDoc().setAnnotation(1, 5, "a", "b");
    popServerOp(opMessageQueue, "__1; || { \"a\": null -> \"b\" }; __4; || { \"a\" }; __7; ");

    // save some places:
    LocalDocument<ContentNode, ContentElement, ContentTextNode> local
      = doc.getContext().annotatableContent();
    ContentElement A = local.getDocumentElement().getFirstChild().asElement();
    ContentNode B = local.getFirstChild(A);

    // writing to mutable sends to server:
    doc.getMutableDoc().insertText(2, "!");
    popServerOp(opMessageQueue, "__2; ++\"!\"; __10; ");

    // local mutations, nothing sent to server:
    ContentNode afterB = local.getNextSibling(B);
    ContentElement X = local.transparentCreate("c", Collections.singletonMap("K", "V"), A, afterB);
    ContentTextNode T = local.transparentCreate(":)", X, null);
    assertTrue(opMessageQueue.isEmpty());

    // documents are different:
    assertFalse(checkDomEqual(doc.getMutableDoc(), local));

    // persist the local subtree
    local.markNodeForPersistence(X, true);
    assertTrue(opMessageQueue.isEmpty()); // marked for persistence, not actually used yet.
    doc.getPersistentView().onBeforeFilter(Point.inElement((ContentNode) X, null));
    popServerOp(opMessageQueue, "__6; << c { K=\"V\" }; ++\":)\"; >>; __7; ");
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));

    // change attribute on newly persisted doc:
    doc.getMutableDoc().setElementAttribute(X, "K", "V2");
    popServerOp(opMessageQueue, "__6; u@ { K: \"V\" -> \"V2\" }; __10; ");

    // check annotations moved correctly:
    int sz = doc.getMutableDoc().size();
    assertEquals( 1, doc.getMutableDoc().firstAnnotationChange(0, sz, "a", null));
    assertEquals(10, doc.getMutableDoc().firstAnnotationChange(3, sz, "a", "b"));

    // check *exact* equality
    ContentElement mA = doc.getMutableDoc().getDocumentElement().getFirstChild().asElement();
    ContentNode mB = doc.getMutableDoc().getFirstChild(mA);
    ContentNode mX = doc.getMutableDoc().getNextSibling(mB);
    ContentNode mT = doc.getMutableDoc().getFirstChild(mX);
    assertTrue(mA == A);
    assertTrue(mB == B);
    assertTrue(mX == X);
    assertTrue(mT == T);

    // try inserting text through mutable document too:
    int at = doc.getMutableDoc().getLocation(X);
    doc.getMutableDoc().insertText(at + 1, "#");
    popServerOp(opMessageQueue, "__7; ++\"#\"; __10; ");
    assertTrue(checkDomEqual(doc.getMutableDoc(), local));
  }

  /** Checks multiple calls to opaque persist with related nodes. */
  public void testMultipleChildOpaquePersists() {
    // set up document, with queue for outgoing messages.
    final Queue<DocOp> opMessageQueue = new LinkedList<DocOp>();
    ContentDocument doc = TestEditors.createTestDocument();
    // Note: this replaces the output sink that the editor has injected into the
    // document, breaking an editor assumption.
    doc.replaceOutgoingSink(new SilentOperationSink<DocOp>(){
      @Override
      public void consume(DocOp op) {
        opMessageQueue.add(op);
      }
    });
    Map<String, String> empty = Collections.<String, String>emptyMap();

    // initial document, many child nodes
    doc.consume(DocProviders.POJO.parse("<P></P>").asOperation()); // just parent
    assertTrue(opMessageQueue.isEmpty()); // came from server, nothing sent out
    LocalDocument<ContentNode, ContentElement, ContentTextNode> local
        = doc.getContext().annotatableContent();
    CMutableDocument mutable = doc.getMutableDoc();
    ContentElement P = local.getDocumentElement().getFirstChild().asElement(); // parent

    // create four children locally, one after the other.
    final ContentElement C1 = local.transparentCreate("C1", empty, P, null);
    ContentElement C2 = local.transparentCreate("C2", Collections.singletonMap("K", "V"), P, null);
    ContentElement C3 = local.transparentCreate("C3", empty, P, null);
    ContentElement C4 = local.transparentCreate("C4", Collections.singletonMap("X", "Y"), P, null);
    local.transparentCreate("a", C1, null);
    local.transparentCreate("b", C4, null);
    String localDom = "<doc><P><C1>a</C1><C2 K=\"V\"/><C3/><C4 X=\"Y\">b</C4></P></doc>";

    assertEquals(localDom, toSimpleString(local));
    assertEquals("<doc><P/></doc>", toSimpleString(mutable));

    // persist C4
    local.markNodeForPersistence(C4, false);
    assertEquals(localDom, toSimpleString(local));
    assertEquals("<doc><P><C4 X=\"Y\">b</C4></P></doc>", toSimpleString(mutable));
    assertTrue(mutable.getFirstChild(mutable.getFirstChild(mutable.getDocumentElement())) == C4);
    popServerOp(opMessageQueue, "__1; << C4 { X=\"Y\" }; ++\"b\"; >>; __1; ");

    // persist C1's child
    local.markNodeForPersistence(C1.getFirstChild(), false);
    assertEquals(localDom, toSimpleString(local));
    assertEquals("<doc><P>a<C4 X=\"Y\">b</C4></P></doc>", toSimpleString(mutable));
    assertTrue(mutable.getFirstChild(mutable.getFirstChild(mutable.getDocumentElement())) ==
        C1.getFirstChild());
    popServerOp(opMessageQueue, "__1; ++\"a\"; __4; ");

    // forcibly persist C3
    local.markNodeForPersistence(C3, false);
    assertEquals(localDom, toSimpleString(local));
    assertEquals("<doc><P>a<C3/><C4 X=\"Y\">b</C4></P></doc>", toSimpleString(mutable));
    popServerOp(opMessageQueue, "__2; << C3 {}; >>; __4; ");

    // forcibly persist C2
    local.markNodeForPersistence(C2, false);
    assertEquals(localDom, toSimpleString(local));
    assertEquals("<doc><P>a<C2 K=\"V\"/><C3/><C4 X=\"Y\">b</C4></P></doc>",
        toSimpleString(mutable));
    popServerOp(opMessageQueue, "__2; << C2 { K=\"V\" }; >>; __6; ");

    // check that they're different by the C1 tag:
    assertTrue(new ReadableTreeWalker<ContentNode, ContentElement, ContentTextNode>(
        local, local.getDocumentElement()) {
          @Override
          protected void progress() {
            super.progress();
            if (currentNode == C1) {
              super.progress(); // skip C1
            }
          }
        }.checkWalk(mutable, mutable.getDocumentElement()));
  }

  //
  // Utilities
  //

  // removes serialized op from the server queue and checks against expected string.
  private static void popServerOp(Queue<DocOp> queue, String op) {
    assertTrue("Empty queue, " + op + " expected", !queue.isEmpty());
    String next = DocOpUtil.toConciseString(queue.poll());
    assertEquals(next + " sent, " + op + " expected", next, op);
  }

  // makes sure the two given documents have identical DOM structure.
  private static boolean checkDomEqual(
      ReadableDocument<ContentNode, ContentElement, ContentTextNode> A,
      ReadableDocument<ContentNode, ContentElement, ContentTextNode> B) {
    return new ReadableTreeWalker<ContentNode, ContentElement, ContentTextNode>(
        A, A.getDocumentElement()).checkWalk(B, B.getDocumentElement());
  }

  // converts the DOM to a string.
  private static String toSimpleString(ReadableDocument<ContentNode, ?, ?> doc) {
    DocOpBuffer opBuffer = new DocOpBuffer();
    DomOperationUtil.buildDomInitializationFromSubtree(doc, doc.getDocumentElement(), opBuffer);
    return DocOpUtil.toXmlString(DocOpUtil.asInitialization(opBuffer.finish()));
  }
}
