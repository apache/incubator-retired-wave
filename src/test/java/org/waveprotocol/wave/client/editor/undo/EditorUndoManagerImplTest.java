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

package org.waveprotocol.wave.client.editor.undo;

import org.waveprotocol.wave.client.editor.EditorUndoManager;
import org.waveprotocol.wave.client.editor.EditorUndoManagerImpl;
import org.waveprotocol.wave.client.editor.Responsibility;
import org.waveprotocol.wave.client.editor.ResponsibilityManagerImpl;
import org.waveprotocol.wave.client.editor.testing.DocumentFreeSelectionHelper;
import org.waveprotocol.wave.model.undo.UndoManagerFactory;
import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;


public class EditorUndoManagerImplTest extends TestCase {
  IndexedDocument<Node, Element, Text> doc;
  SilentOperationSink<DocOp> silentOperationSink;
  DocumentFreeSelectionHelper selectionHelper;
  EditorUndoManager undoManager;
  Responsibility.Manager responsibility;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    doc = DocProviders.POJO.parse("<doc>hello</doc>");
    silentOperationSink =
        new SilentOperationSink<DocOp>() {
          public void consume(DocOp op) {
            try {
              doc.consume(op);
            } catch (OperationException e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          }
        };

    selectionHelper = new DocumentFreeSelectionHelper(0, 0);
    responsibility = new ResponsibilityManagerImpl();
    undoManager =
        new EditorUndoManagerImpl(UndoManagerFactory.createUndoManager(),
            silentOperationSink, selectionHelper);
  }

  public void testSimpleUndo() {
    undoManager.maybeCheckpoint(1, 1);
    responsibility.startDirectSequence();
    insert(1, 7);
    responsibility.endDirectSequence();
    undoManager.undo();
    assertTrue(selectionHelper.getSelectionRange().equals(new FocusedRange(1, 1)));
    assertEquals(XmlStringBuilder.innerXml(doc).toString(), "<doc>hello</doc>");
  }

  public void testMultipleSimpleUndo() {
    responsibility.startDirectSequence();
    undoManager.maybeCheckpoint(1, 1);
    insert(1, 7);
    undoManager.maybeCheckpoint(2, 2);
    insert(2, 8);
    responsibility.endDirectSequence();

    undoManager.undo();
    assertTrue(selectionHelper.getSelectionRange().equals(new FocusedRange(2, 2)));
    assertEquals(XmlStringBuilder.innerXml(doc).toString(), "<doc>ahello</doc>");

    undoManager.undo();
    assertTrue(selectionHelper.getSelectionRange().equals(new FocusedRange(1, 1)));
    assertEquals(XmlStringBuilder.innerXml(doc).toString(), "<doc>hello</doc>");
  }

  public void testMultipleSimpleUndo2() {
    responsibility.startDirectSequence();
    undoManager.maybeCheckpoint(1, 1);
    insert(1, 7);
    undoManager.maybeCheckpoint(2, 2);
    insert(2, 8);
    responsibility.endDirectSequence();

    undoManager.undo();
    assertTrue(selectionHelper.getSelectionRange().equals(new FocusedRange(2, 2)));
    assertEquals(XmlStringBuilder.innerXml(doc).toString(), "<doc>ahello</doc>");

    undoManager.undo();
    assertTrue(selectionHelper.getSelectionRange().equals(new FocusedRange(1, 1)));
    assertEquals(XmlStringBuilder.innerXml(doc).toString(), "<doc>hello</doc>");
  }

  public void testMutlipleUndoComplex() {
    responsibility.startDirectSequence();
    undoManager.maybeCheckpoint(1, 1);
    insert(1, 7);
    undoManager.maybeCheckpoint(2, 2);
    insert(2, 8);

    undoManager.undo();
    assertEquals(new FocusedRange(2, 2), selectionHelper.getSelectionRange());
    assertEquals("<doc>ahello</doc>", XmlStringBuilder.innerXml(doc).toString());

    undoManager.maybeCheckpoint(3, 4);
    insert(3, 8);

    undoManager.undo();
    assertEquals(new FocusedRange(3, 4), selectionHelper.getSelectionRange());
    assertEquals("<doc>ahello</doc>", XmlStringBuilder.innerXml(doc).toString());

    undoManager.undo();
    assertEquals(new FocusedRange(1, 1), selectionHelper.getSelectionRange());
    assertEquals("<doc>hello</doc>", XmlStringBuilder.innerXml(doc).toString());

    responsibility.endDirectSequence();
  }

  public void testUndoWithNoCheckpoints() {
    undoManager.undo();
    assertEquals(new FocusedRange(0, 0), selectionHelper.getSelectionRange());
    assertEquals("<doc>hello</doc>", XmlStringBuilder.innerXml(doc).toString());
  }

  public void testUndoWithNoSelection() {
    responsibility.startDirectSequence();
    selectionHelper.setSelectionRange(null);
    undoManager.maybeCheckpoint();
    insert(1, 7);
    undoManager.undo();
    assertEquals(null, selectionHelper.getSelectionRange());
    assertEquals("<doc>hello</doc>", XmlStringBuilder.innerXml(doc).toString());
    responsibility.endDirectSequence();
  }

  public void testUndoWithNonUndoableOps() {
    insert(2, 7);

    responsibility.startDirectSequence();
    undoManager.maybeCheckpoint();
    insert(5, 8);
    responsibility.endDirectSequence();

    insert(3, 9);

    undoManager.undo();

    assertEquals(new FocusedRange(0), selectionHelper.getSelectionRange());
    assertEquals("<doc>haaello</doc>", XmlStringBuilder.innerXml(doc).toString());
  }


  public void testUndoRedoSequence() {
    responsibility.startDirectSequence();
    undoManager.maybeCheckpoint(1, 1);
    insert(1, 7);
    undoManager.maybeCheckpoint(2, 2);
    insert(2, 8);

    FocusedRange oldSelection = selectionHelper.getSelectionRange();
    undoManager.undo();
    assertEquals(new FocusedRange(2, 2), selectionHelper.getSelectionRange());
    assertEquals("<doc>ahello</doc>", XmlStringBuilder.innerXml(doc).toString());

    undoManager.redo();
    assertEquals(oldSelection, selectionHelper.getSelectionRange());
    assertEquals("<doc>aahello</doc>", XmlStringBuilder.innerXml(doc).toString());

    selectionHelper.setSelectionRange(new FocusedRange(5));
    undoManager.undo();
    assertEquals(new FocusedRange(2, 2), selectionHelper.getSelectionRange());
    assertEquals("<doc>ahello</doc>", XmlStringBuilder.innerXml(doc).toString());
    responsibility.endDirectSequence();

    insert(1, 8);

    undoManager.redo();
    assertEquals(new FocusedRange(6), selectionHelper.getSelectionRange());
    assertEquals("<doc>aaahello</doc>", XmlStringBuilder.innerXml(doc).toString());

    undoManager.redo();
    assertEquals(new FocusedRange(6), selectionHelper.getSelectionRange());
    assertEquals("<doc>aaahello</doc>", XmlStringBuilder.innerXml(doc).toString());
  }

  private void insert(int location, int size) {
    DocOp op = new DocOpBuilder()
        .retain(location)
        .characters("a")
        .retain(size - location)
        .build();
    silentOperationSink.consume(op);
    if (responsibility.withinDirectSequence()) {
      undoManager.undoableOp(op);
    } else {
      undoManager.nonUndoableOp(op);
    }
  }
}
