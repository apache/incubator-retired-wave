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

package org.waveprotocol.wave.concurrencycontrol.client;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.SubmitBlip;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test case for the temporary simple merging logic
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class MergingSequenceTest extends TestCase {

  private Map<String, SuperSink> docs;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    docs = new HashMap<String, SuperSink>();
  }

  public void testOptimiseEmptyDoesNothing() {
    MergingSequence delta = new MergingSequence();
    delta.optimise();
    assertEquals(0, delta.size());
  }

  public void testOptimisesDocumentOpsOnSameBlipWithLastTimestamp() throws OperationException {
    MergingSequence delta = new MergingSequence();

    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        makeBlipContentOp("a", 3)
    ));

    delta.optimise();

    assertEquals(1, delta.size());
    assertTimestamp(3, delta);
  }

  public void testOptimisesOpsOnSeparateBlips1() throws OperationException {
    MergingSequence delta = new MergingSequence();

    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        makeBlipContentOp("b", 3)
    ));

    delta.optimise();

    assertEquals(2, delta.size());
  }

  public void testOptimisesOpsOnSeparateBlips2() throws OperationException {
    MergingSequence delta = new MergingSequence();

    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        makeBlipContentOp("b", 3),
        makeBlipContentOp("b", 4),
        makeBlipContentOp("b", 5),
        makeBlipContentOp("a", 6),
        makeBlipContentOp("a", 7)
    ));

    delta.optimise();

    assertEquals(3, delta.size());
  }

  public void testOptimisesDeltasWithNonDocOps() throws OperationException {
    MergingSequence delta = new MergingSequence();

    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 2),
        makeBlipContentOp("a", 1),
        makeOtherBlipOp("a", 3)
    ));

    delta.optimise();

    assertEquals(2, delta.size());
  }

  public void testOptimisesDeltasWithNonBlipOps1() throws OperationException {
    MergingSequence delta = new MergingSequence();
    ParticipantId jim = new ParticipantId("jim");
    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        new AddParticipant(new WaveletOperationContext(jim, 5L, 1L), jim)
    ));

    delta.optimise();

    assertEquals(2, delta.size());
  }

  public void testOptimisesDeltasWithNonBlipOps2() throws OperationException {
    MergingSequence delta = new MergingSequence();
    ParticipantId jim = new ParticipantId("jim");
    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        new AddParticipant(new WaveletOperationContext(jim, 5L, 1L), jim),
        makeBlipContentOp("a", 4),
        makeBlipContentOp("a", 5)
    ));

    delta.optimise();

    assertEquals(3, delta.size());
  }

  public void testOptimisesDeltasWithNonBlipOps3() throws OperationException {
    MergingSequence delta = new MergingSequence();
    ParticipantId jim = new ParticipantId("jim");
    delta.addAll(Arrays.asList(
        makeBlipContentOp("a", 1),
        makeBlipContentOp("a", 2),
        new AddParticipant(new WaveletOperationContext(jim, 5L, 1L), jim),
        makeBlipContentOp("b", 4),
        makeBlipContentOp("b", 5)
    ));

    delta.optimise();

    assertEquals(3, delta.size());
  }

  private static void assertTimestamp(long timestamp, MergingSequence delta) {
    assertEquals(timestamp,
        ((WaveletBlipOperation) delta.get(0)).getBlipOp().getContext().getTimestamp());
  }

  private WaveletOperation makeBlipContentOp(String id, long timestamp) throws OperationException {
    if (!docs.containsKey(id)) {
      docs.put(id, DocProviders.POJO.parse("<x></x>"));
    }
    Nindo nindo = Nindo.insertCharacters(1, "hi");
    DocOp op = docs.get(id).consumeAndReturnInvertible(nindo);
    return new WaveletBlipOperation(id, new BlipContentOperation(getContext(timestamp), op));
  }

  private WaveletOperation makeOtherBlipOp(String id, long timestamp) {
    return new WaveletBlipOperation(id, new SubmitBlip(getContext(timestamp)));
  }

  private WaveletOperationContext getContext(long timestamp) {
    return new WaveletOperationContext(new ParticipantId("blah"), timestamp, 1L);
  }
}
