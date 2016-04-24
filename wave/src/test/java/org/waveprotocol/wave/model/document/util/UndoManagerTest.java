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

import org.waveprotocol.wave.model.undo.UndoManager;
import org.waveprotocol.wave.model.undo.UndoManagerFactory;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.util.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for UndoManager.
 *
 */

public class UndoManagerTest extends TestCase {

  public void testUndoRedo() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(5, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 12));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(4, 12), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 11), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(3, 10), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(3, 10), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(5, 11), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(4, 12), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(4, 12), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 11), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(3, 10), undoManager.undo()));
  }

  public void testUndoRedoWithNonundoableOps() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(5, 12));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 14));
    undoManager.nonUndoableOp(insert(1, 15));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 15), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(7, 14), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(6, 13), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(6, 13), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(7, 14), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(5, 15), undoManager.redo()));
  }

  public void testUndoRedoWithConsecutiveNonundoableOps() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.nonUndoableOp(insert(2, 12));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(6, 13));
    undoManager.nonUndoableOp(delete(10, 13));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.nonUndoableOp(insert(10, 14));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(5, 15));
    undoManager.nonUndoableOp(insert(1, 16));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(6, 16), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(8, 15), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(7, 14), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(7, 14), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(8, 15), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(6, 16), undoManager.redo()));
  }

  public void testUndoRedoInterspersedWithNonundoableOps() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(5, 12));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 14));
    undoManager.nonUndoableOp(insert(1, 15));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 15), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 15));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(8, 15), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 15));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(8, 15), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 15));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(9, 16), undoManager.redo()));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(11, 18), undoManager.redo()));
    undoManager.nonUndoableOp(insert(1, 19));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(10, 20), undoManager.redo()));
  }

  public void testUndoRedoWithNondenseCheckpointing() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(8, 12));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.undoableOp(insert(2, 14));
    undoManager.nonUndoableOp(insert(10, 15));
    undoManager.undoableOp(insert(6, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    undoManager.undoableOp(delete(13, 17));
    undoManager.undoableOp(delete(3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 17), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(7, 16), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(7, 15), undoManager.undo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(7, 15), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(7, 16), undoManager.redo()));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(5, 17), undoManager.redo()));
  }

  public void testUndoRedoWithNondenseCheckpointsInterspersedWithNonundoableOps() {
    UndoManager<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(8, 12));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.undoableOp(insert(2, 14));
    undoManager.nonUndoableOp(insert(10, 15));
    undoManager.undoableOp(insert(6, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    undoManager.undoableOp(delete(13, 17));
    undoManager.undoableOp(delete(3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 17), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(8, 17), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(9, 17), undoManager.undo()));
    undoManager.nonUndoableOp(insert(1, 17));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(10, 18), undoManager.redo()));
    undoManager.nonUndoableOp(insert(1, 19));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(11, 20), undoManager.redo()));
    undoManager.nonUndoableOp(insert(1, 21));
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(10, 22), undoManager.redo()));
  }

  public void testPlusMethods() {
    UndoManagerPlus<DocOp> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert(3, 10));
    undoManager.nonUndoableOp(insert(1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(8, 12));
    undoManager.nonUndoableOp(insert(1, 13));
    undoManager.undoableOp(insert(2, 14));
    undoManager.nonUndoableOp(insert(10, 15));
    undoManager.undoableOp(insert(6, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    undoManager.undoableOp(delete(13, 17));
    undoManager.undoableOp(delete(3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert(4, 16));
    undoManager.nonUndoableOp(insert(1, 17));
    Pair<DocOp, DocOp> pair = undoManager.undoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(5, 17), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 16)
    ), pair.second);
    undoManager.nonUndoableOp(insert(1, 17));
    pair = undoManager.undoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(8, 17), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 12),
        insert(9, 13),
        insert(1, 14),
        insert(1, 15),
        insert(1, 16)
    ), pair.second);
    undoManager.nonUndoableOp(insert(1, 17));
    pair = undoManager.undoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(delete(9, 17), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 10),
        insert(1, 11),
        insert(8, 12),
        insert(1, 13),
        insert(1, 14),
        insert(1, 15),
        insert(1, 16)
    ), pair.second);
    undoManager.nonUndoableOp(insert(1, 17));
    pair = undoManager.redoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(10, 18), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 18)
    ), pair.second);
    undoManager.nonUndoableOp(insert(1, 19));
    pair = undoManager.redoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(11, 20), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 18),
        insert(1, 19),
        insert(1, 20)
    ), pair.second);
    undoManager.nonUndoableOp(insert(1, 21));
    pair = undoManager.redoPlus();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(insert(10, 22), pair.first));
    compareDocOpList(Arrays.asList(
        insert(1, 18),
        insert(1, 19),
        insert(1, 20),
        insert(1, 21),
        insert(1, 22)
    ), pair.second);
  }

  private static void compareDocOpList(List<DocOp> ops, DocOp op) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(Composer.compose(ops), op));
  }

  private static DocOp insert(int location, int size) {
    return new DocOpBuilder()
        .retain(location)
        .characters("a")
        .retain(size - location)
        .build();
  }

  private static DocOp delete(int location, int size) {
    return new DocOpBuilder()
        .retain(location)
        .deleteCharacters("a")
        .retain(size - location)
        .build();
  }

}
