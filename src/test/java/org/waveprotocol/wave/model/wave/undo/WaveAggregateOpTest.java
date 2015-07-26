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

import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */

public class WaveAggregateOpTest extends TestCase {
  private static final String CREATOR1_ID = "creator1";
  private static final String CREATOR2_ID = "creator2";
  private static final String TARGET1 = "target1";
  private static final String TARGET2 = "target2";

  private static final WaveletOperationContext CREATOR1_CONTEXT =
      new WaveletOperationContext(new ParticipantId(CREATOR1_ID), 0, 0);

  private static final WaveletOperationContext CREATOR2_CONTEXT =
      new WaveletOperationContext(new ParticipantId(CREATOR2_ID), 0, 0);

  private DocOp insertDocOp(int location, int size) {
    return new DocOpBuilder()
        .retain(location)
        .characters("a")
        .retain(size - location)
        .build();
  }

  private DocOp deleteDocOp(int location, int size) {
    return new DocOpBuilder()
        .retain(location)
        .deleteCharacters("a")
        .retain(size - location)
        .build();
  }

  public void testCompose() {
    {
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2 = new WaveOpBuilder(CREATOR2_ID).addParticipant(TARGET2).build();

      WaveAggregateOp composed = compose(op1, op2);

      List<WaveletOperation> expected = Arrays.<WaveletOperation>asList(
          new AddParticipant(CREATOR1_CONTEXT, new ParticipantId(TARGET1)),
          new AddParticipant(CREATOR2_CONTEXT, new ParticipantId(TARGET2))
      );
      assertEquals(expected, composed.toWaveletOperations());
    }

    {
      DocOp insertDocOp = insertDocOp(1, 5);
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2 = new WaveOpBuilder(CREATOR2_ID).docOp("doc", insertDocOp).build();


      WaveAggregateOp composed = compose(op1, op2);

      List<WaveletOperation> expected = Arrays.<WaveletOperation>asList(
          new AddParticipant(CREATOR1_CONTEXT, new ParticipantId(TARGET1)),
          new WaveletBlipOperation("doc", new BlipContentOperation(CREATOR2_CONTEXT, insertDocOp))
      );
      assertEquals(expected, composed.toWaveletOperations());
    }

    // Test compose of document op from the same creator
    {
      DocOp insertDocOp1 = insertDocOp(1, 3);
      DocOp insertDocOp2 = insertDocOp(3, 4);
      DocOp deleteDocOp = deleteDocOp(2, 5);
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).docOp("doc", insertDocOp1).build();
      WaveAggregateOp op2 = new WaveOpBuilder(CREATOR1_ID)
          .docOp("doc", insertDocOp2)
          .docOp("doc", deleteDocOp)
          .build();

      WaveAggregateOp composed = compose(op1, op2);

      DocOp expectedDocOp = new DocOpBuilder()
          .retain(1)
          .characters("a")
          .deleteCharacters("a")
          .characters("a")
          .retain(1)
          .build();

      List<WaveletOperation> expected = Arrays.<WaveletOperation>asList(
          new WaveletBlipOperation("doc", new BlipContentOperation(CREATOR1_CONTEXT, expectedDocOp))
      );
      assertEquals(expected, composed.toWaveletOperations());
    }
  }

  public void testTransform() {
    // Test that orthogonal ops are preserved.
    {
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2 = new WaveOpBuilder(CREATOR2_ID).addParticipant(TARGET2).build();

      OperationPair<WaveAggregateOp> transformed;
      try {
        transformed = WaveAggregateOp.transform(op1, op2);
      } catch (TransformException e) {
        fail("transform failed:" + e);
        return;
      }

      List<WaveletOperation> expectedClient = Arrays.<WaveletOperation> asList(
          new AddParticipant(CREATOR1_CONTEXT, new ParticipantId(TARGET1)));
      List<WaveletOperation> expectedServer = Arrays.<WaveletOperation> asList(
          new AddParticipant(CREATOR2_CONTEXT, new ParticipantId(TARGET2)));

      assertEquals(expectedClient, transformed.clientOp().toWaveletOperations());
      assertEquals(expectedServer, transformed.serverOp().toWaveletOperations());
    }

    // Test case where client ops are transformed away, and some server ops are kept
    {
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2 =
          new WaveOpBuilder(CREATOR2_ID).addParticipant(TARGET1).addParticipant(TARGET2).build();

      OperationPair<WaveAggregateOp> transformed;
      try {
        transformed = WaveAggregateOp.transform(op1, op2);
      } catch (TransformException e) {
        fail("transform failed:" + e);
        return;
      }

      List<WaveletOperation> expectedServer =
          Arrays.<WaveletOperation> asList(new AddParticipant(CREATOR2_CONTEXT, new ParticipantId(
              TARGET2)));

      assertEquals(0, transformed.clientOp().toWaveletOperations().size());
      assertEquals(expectedServer, transformed.serverOp().toWaveletOperations());
    }

    // Same as above, but server ops originate from 2 different creators. Test
    // that the creators are preserved correctly through transform and
    // composition.
    {
      WaveAggregateOp op1 = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2a =
          new WaveOpBuilder(CREATOR2_ID).addParticipant(TARGET1).build();
      WaveAggregateOp op2b = new WaveOpBuilder(CREATOR1_ID).addParticipant(TARGET2).build();
      WaveAggregateOp op2 = compose(op2a, op2b);

      OperationPair<WaveAggregateOp> transformed;
      try {
        transformed = WaveAggregateOp.transform(op1, op2);
      } catch (TransformException e) {
        fail("transform failed:" + e);
        return;
      }

      List<WaveletOperation> expectedServer =
          Arrays.<WaveletOperation> asList(new AddParticipant(CREATOR1_CONTEXT, new ParticipantId(
              TARGET2)));

      assertEquals(0, transformed.clientOp().toWaveletOperations().size());
      assertEquals(expectedServer, transformed.serverOp().toWaveletOperations());
    }
  }

  private static class WaveOpBuilder {
    final ParticipantId creator;

    List<AggregateOperation> ops = new ArrayList<AggregateOperation>();

    WaveOpBuilder(String userName) {
      creator = new ParticipantId(userName);
    }

    public WaveOpBuilder docOp(String docId, DocOp docOp) {
      ops.add(new AggregateOperation(new CoreWaveletDocumentOperation(docId, docOp)));
      return this;
    }

    WaveOpBuilder aggregateOp(AggregateOperation op) {
      ops.add(op);
      return this;
    }

    WaveOpBuilder addParticipant(String participantId) {
      ops.add(AggregateOpTestUtil.addParticipant(participantId));
      return this;
    }

    WaveOpBuilder removeParticipant(String participantId) {
      ops.add(AggregateOpTestUtil.removeParticipant(participantId));
      return this;
    }

    WaveOpBuilder insert(String id, int location, int size) {
      ops.add(AggregateOpTestUtil.insert(id, location, size));
      return this;
    }

    WaveOpBuilder delete(String id, int location, int size) {
      ops.add(AggregateOpTestUtil.delete(id, location, size));
      return this;
    }

    WaveAggregateOp build() {
      return new WaveAggregateOp(AggregateOperation.compose(ops), creator);
    }
  }

  private static WaveAggregateOp compose(WaveAggregateOp... ops) {
    return WaveAggregateOp.compose(Arrays.asList(ops));
  }

  private void assertEquals(List<WaveletOperation> ops1, List<WaveletOperation> ops2) {
    if (ops1.size() != ops2.size()) {
      fail("size differ: " + "(" + ops1.size() + ", " + ops2.size() + ") " + ops1 + " " + ops2);
    }

    for (int i = 0; i < ops1.size(); ++i) {
      assertEquals(ops1.get(i), ops2.get(i));
    }
  }

  private void assertEquals(WaveletOperation op1, WaveletOperation op2) {
    if (!haveSameCreator(op1, op2)) {
      fail("creator differ ( " + op1.getContext().getCreator() + ", "
          + op2.getContext().getCreator() + ") ");
    }

    if (!op1.equals(op2)) {
      fail("op differ ( " + op1 + ", " + op2);
    }
  }

  private boolean haveSameCreator(WaveletOperation op1, WaveletOperation op2) {
    return op1.getContext().getCreator().equals(op2.getContext().getCreator());
  }
}
