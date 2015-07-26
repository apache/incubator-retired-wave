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

package org.waveprotocol.wave.model.wave.undo;

import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.addParticipant;
import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.areEqual;
import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.compose;
import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.delete;
import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.insert;
import static org.waveprotocol.wave.model.wave.undo.AggregateOpTestUtil.removeParticipant;

import org.waveprotocol.wave.model.undo.UndoManager;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.Pair;

/**
 * Tests for UndoManagerFactory. This also tests the package-private aggregate
 * operations and their composition, transform, and inversion algorithms.
 *
 */

public class UndoManagerFactoryTest extends TestCase {

  public void testUndoRedo() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 5, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 12));
    assertOpsEqual(delete("i", 4, 12), undoManager.undo());
    assertOpsEqual(delete("i", 5, 11), undoManager.undo());
    assertOpsEqual(delete("i", 3, 10), undoManager.undo());
    assertOpsEqual(insert("i", 3, 10), undoManager.redo());
    assertOpsEqual(insert("i", 5, 11), undoManager.redo());
    assertOpsEqual(insert("i", 4, 12), undoManager.redo());
    assertOpsEqual(delete("i", 4, 12), undoManager.undo());
    assertOpsEqual(delete("i", 5, 11), undoManager.undo());
    assertOpsEqual(delete("i", 3, 10), undoManager.undo());
  }

  public void testUndoRedoWithNonundoableOps() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 5, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 14));
    undoManager.nonUndoableOp(insert("i", 1, 15));
    assertOpsEqual(delete("i", 5, 15), undoManager.undo());
    assertOpsEqual(delete("i", 7, 14), undoManager.undo());
    assertOpsEqual(delete("i", 6, 13), undoManager.undo());
    assertOpsEqual(insert("i", 6, 13), undoManager.redo());
    assertOpsEqual(insert("i", 7, 14), undoManager.redo());
    assertOpsEqual(insert("i", 5, 15), undoManager.redo());
  }

  public void testUndoRedoWithConsecutiveNonundoableOps() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.nonUndoableOp(insert("i", 2, 12));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 6, 13));
    undoManager.nonUndoableOp(delete("i", 10, 13));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.nonUndoableOp(insert("i", 10, 14));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 5, 15));
    undoManager.nonUndoableOp(insert("i", 1, 16));
    assertOpsEqual(delete("i", 6, 16), undoManager.undo());
    assertOpsEqual(delete("i", 8, 15), undoManager.undo());
    assertOpsEqual(delete("i", 7, 14), undoManager.undo());
    assertOpsEqual(insert("i", 7, 14), undoManager.redo());
    assertOpsEqual(insert("i", 8, 15), undoManager.redo());
    assertOpsEqual(insert("i", 6, 16), undoManager.redo());
  }

  public void testUndoRedoInterspersedWithNonundoableOps() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 5, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 14));
    undoManager.nonUndoableOp(insert("i", 1, 15));
    assertOpsEqual(delete("i", 5, 15), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 15));
    assertOpsEqual(delete("i", 8, 15), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 15));
    assertOpsEqual(delete("i", 8, 15), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 15));
    assertOpsEqual(insert("i", 9, 16), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(insert("i", 11, 18), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 19));
    assertOpsEqual(insert("i", 10, 20), undoManager.redo());
  }

  public void testUndoRedoWithNondenseCheckpointing() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 8, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.undoableOp(insert("i", 2, 14));
    undoManager.nonUndoableOp(insert("i", 10, 15));
    undoManager.undoableOp(insert("i", 6, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    undoManager.undoableOp(delete("i", 13, 17));
    undoManager.undoableOp(delete("i", 3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 5, 17), undoManager.undo());
    assertOpsEqual(delete("i", 7, 16), undoManager.undo());
    assertOpsEqual(delete("i", 7, 15), undoManager.undo());
    assertOpsEqual(insert("i", 7, 15), undoManager.redo());
    assertOpsEqual(insert("i", 7, 16), undoManager.redo());
    assertOpsEqual(insert("i", 5, 17), undoManager.redo());
  }

  public void testUndoRedoWithNondenseCheckpointsInterspersedWithNonundoableOps() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 8, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.undoableOp(insert("i", 2, 14));
    undoManager.nonUndoableOp(insert("i", 10, 15));
    undoManager.undoableOp(insert("i", 6, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    undoManager.undoableOp(delete("i", 13, 17));
    undoManager.undoableOp(delete("i", 3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 5, 17), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 8, 17), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 9, 17), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(insert("i", 10, 18), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 19));
    assertOpsEqual(insert("i", 11, 20), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 21));
    assertOpsEqual(insert("i", 10, 22), undoManager.redo());
  }

  public void testPlusMethods() {
    UndoManagerPlus<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 8, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.undoableOp(insert("i", 2, 14));
    undoManager.nonUndoableOp(insert("i", 10, 15));
    undoManager.undoableOp(insert("i", 6, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    undoManager.undoableOp(delete("i", 13, 17));
    undoManager.undoableOp(delete("i", 3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    Pair<AggregateOperation, AggregateOperation> pair = undoManager.undoPlus();
    assertOpsEqual(delete("i", 5, 17), pair.first);
    assertOpsEqual(insert("i", 1, 16), pair.second);
    undoManager.nonUndoableOp(insert("i", 1, 17));
    pair = undoManager.undoPlus();
    assertOpsEqual(delete("i", 8, 17), pair.first);
    assertOpsEqual(compose(
        insert("i", 1, 12),
        insert("i", 9, 13),
        insert("i", 1, 14),
        insert("i", 1, 15),
        insert("i", 1, 16)
    ), pair.second);
    undoManager.nonUndoableOp(insert("i", 1, 17));
    pair = undoManager.undoPlus();
    assertOpsEqual(delete("i", 9, 17), pair.first);
    assertOpsEqual(compose(
        insert("i", 1, 10),
        insert("i", 1, 11),
        insert("i", 8, 12),
        insert("i", 1, 13),
        insert("i", 1, 14),
        insert("i", 1, 15),
        insert("i", 1, 16)
    ), pair.second);
    undoManager.nonUndoableOp(insert("i", 1, 17));
    pair = undoManager.redoPlus();
    assertOpsEqual(insert("i", 10, 18), pair.first);
    assertOpsEqual(insert("i", 1, 18), pair.second);
    undoManager.nonUndoableOp(insert("i", 1, 19));
    pair = undoManager.redoPlus();
    assertOpsEqual(insert("i", 11, 20), pair.first);
    assertOpsEqual(compose(
        insert("i", 1, 18),
        insert("i", 1, 19),
        insert("i", 1, 20)
    ), pair.second);
    undoManager.nonUndoableOp(insert("i", 1, 21));
    pair = undoManager.redoPlus();
    assertOpsEqual(insert("i", 10, 22), pair.first);
    assertOpsEqual(compose(
        insert("i", 1, 18),
        insert("i", 1, 19),
        insert("i", 1, 20),
        insert("i", 1, 21),
        insert("i", 1, 22)
    ), pair.second);
  }

  public void testUndoRedoInvolvingMultipleWavelets() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.undoableOp(insert("j", 5, 10));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.nonUndoableOp(insert("j", 1, 11));
    undoManager.nonUndoableOp(insert("k", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 8, 12));
    undoManager.undoableOp(insert("j", 5, 12));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.undoableOp(insert("i", 2, 14));
    undoManager.nonUndoableOp(insert("i", 10, 15));
    undoManager.undoableOp(insert("i", 6, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    undoManager.undoableOp(delete("i", 13, 17));
    undoManager.undoableOp(delete("i", 3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 5, 17), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(delete("i", 8, 17), delete("j", 5, 12)), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(delete("i", 9, 17), delete("j", 6, 11)), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(insert("i", 10, 18), insert("j", 6, 11)), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 19));
    assertOpsEqual(compose(insert("i", 11, 20), insert("j", 5, 12)), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 21));
    assertOpsEqual(insert("i", 10, 22), undoManager.redo());
  }

  public void testUndoRedoInvolvingParticipants() {
    UndoManager<AggregateOperation> undoManager = UndoManagerFactory.createUndoManager();
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 3, 10));
    undoManager.undoableOp(addParticipant("x"));
    undoManager.undoableOp(insert("j", 5, 10));
    undoManager.undoableOp(addParticipant("y"));
    undoManager.nonUndoableOp(insert("i", 1, 11));
    undoManager.undoableOp(addParticipant("z"));
    undoManager.nonUndoableOp(insert("j", 1, 11));
    undoManager.undoableOp(removeParticipant("y"));
    undoManager.nonUndoableOp(insert("k", 1, 11));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 8, 12));
    undoManager.undoableOp(addParticipant("xx"));
    undoManager.nonUndoableOp(removeParticipant("xx"));
    undoManager.undoableOp(removeParticipant("yy"));
    undoManager.nonUndoableOp(addParticipant("yy"));
    undoManager.undoableOp(addParticipant("ww"));
    undoManager.undoableOp(insert("j", 5, 12));
    undoManager.undoableOp(removeParticipant("xxx"));
    undoManager.undoableOp(removeParticipant("yyy"));
    undoManager.undoableOp(removeParticipant("zzz"));
    undoManager.undoableOp(addParticipant("yyy"));
    undoManager.nonUndoableOp(insert("i", 1, 13));
    undoManager.undoableOp(insert("i", 2, 14));
    undoManager.nonUndoableOp(insert("i", 10, 15));
    undoManager.undoableOp(insert("i", 6, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    undoManager.undoableOp(delete("i", 13, 17));
    undoManager.undoableOp(delete("i", 3, 16));
    undoManager.checkpoint();
    undoManager.undoableOp(insert("i", 4, 16));
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(delete("i", 5, 17), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(
        delete("i", 8, 17),
        delete("j", 5, 12),
        removeParticipant("ww"),
        addParticipant("xxx"),
        addParticipant("zzz")
    ), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(
        delete("i", 9, 17),
        delete("j", 6, 11),
        removeParticipant("x"),
        removeParticipant("z")
    ), undoManager.undo());
    undoManager.nonUndoableOp(insert("i", 1, 17));
    assertOpsEqual(compose(
        insert("i", 10, 18),
        insert("j", 6, 11),
        addParticipant("x"),
        addParticipant("z")
    ), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 19));
    assertOpsEqual(compose(
        insert("i", 11, 20),
        insert("j", 5, 12),
        addParticipant("ww"),
        removeParticipant("xxx"),
        removeParticipant("zzz")
    ), undoManager.redo());
    undoManager.nonUndoableOp(insert("i", 1, 21));
    assertOpsEqual(insert("i", 10, 22), undoManager.redo());
  }

  private static void assertOpsEqual(AggregateOperation op1, AggregateOperation op2) {
    assertTrue(areEqual(op1, op2));
  }
}
