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

package org.waveprotocol.wave.model.wave.opbased;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeSilentOperationSink;
import org.waveprotocol.wave.model.testing.MockParticipationHelper;
import org.waveprotocol.wave.model.testing.OpBasedWaveletFactory;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * White-box test for {@link OpBasedWavelet}.
 *
 * @author zdwang@google.com (David Wang)
 */
public abstract class OpBasedWaveletTestBase extends TestCase {

  private OpBasedWaveletFactory factory;

  /** Stub sink for catching operations. */
  private final FakeSilentOperationSink<WaveletOperation> sink =
    new FakeSilentOperationSink<WaveletOperation>();

  private OpBasedWavelet target;

  /**
   * Creates a wavelet for testing.
   */
  protected abstract ObservableWaveletData.Factory<?> createWaveletDataFactory();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    factory = buildFactory(createWaveletDataFactory(), sink);
    target = factory.create();
  }

  /**
   * Builds the wavelet factory, based on an underlying holder factory and an
   * output operation sink.
   */
  private static OpBasedWaveletFactory buildFactory(
      ObservableWaveletData.Factory<?> holderFactory,
      SilentOperationSink<WaveletOperation> sink) {
    return BasicFactories.opBasedWaveletFactoryBuilder().with(sink).with(holderFactory).build();
  }

  /**
   * Asserts that some operation is adding a certain participant.
   *
   * @param creator of the operation adding the participant.
   * @param participant that should be added.
   * @param op to check.
   */
  private static void assertAddParticipant(ParticipantId creator,
      ParticipantId participant, WaveletOperation op) {
    assertTrue("Expected AddParticipant but was " + op.getClass(),
        op instanceof AddParticipant);
    AddParticipant add = (AddParticipant) op;
    assertEquals(creator, add.getContext().getCreator());
    assertEquals(participant, add.getParticipantId());
  }

  public void testAddingCreatorAsParticipantProducesOperation() {
    ParticipantId creator = target.getCreatorId();
    target.addParticipant(creator);

    assertTrue(sink.getConsumedOp() instanceof AddParticipant);
    assertEquals(creator, ((AddParticipant) sink.getConsumedOp()).getParticipantId());
  }

  public void testAddingParticipantIdSetProducesManyOperations() {
    target.addParticipant(target.getCreatorId());
    Set<ParticipantId> participants = new HashSet<ParticipantId>();
    for (int i = 0; i < 20; i++) {
      ParticipantId participant = new ParticipantId("foo" + i + "@bar.com");
      participants.add(participant);
    }
    target.addParticipantIds(participants);
    List<WaveletOperation> operations = sink.getOps();
    for (WaveletOperation op : operations) {
      assertTrue("The operation was not an AddParticipant operator",
          op instanceof AddParticipant);
      participants.remove(((AddParticipant)op).getParticipantId());
    }
    assertEquals("Not all participants resulted in an operation",0, participants.size());
  }

  public void testAddingManyParticipantProducesManyOperations() {
    // Creator must be added as first op.
    target.addParticipant(target.getCreatorId());
    for (int i = 0; i < 20; i++) {
      ParticipantId participant = new ParticipantId("foo" + i + "@bar.com");
      target.addParticipant(participant);
      assertTrue(sink.getConsumedOp() instanceof AddParticipant);
      assertEquals(participant, ((AddParticipant) sink.getConsumedOp()).getParticipantId());
      sink.clear();
    }
  }

  /**
   * Tests that creating a blip doesn't produce an operation.
   */
  public void testCreatingRootBlipProducesNoOperation() {
    target.createBlip("b+fake");
    assertTrue(sink.getOps().isEmpty());
  }

  /**
   * Test that touching a wavelet produces an empty operation.
   */
  public void testTouchingProducesOperation() {
    target.touch();
    assertTrue(sink.getConsumedOp() instanceof NoOp);
  }

  /**
   * Tests that mutating a data document produces (only) a document mutation.
   *
   * This test is suppressed because the test setup
   * can't approporiately hook up the op-based wavelet as a document
   * listener. Enable when either the op-based wavelet installs itself
   * as a listener or the model test setup is refactored to allow this.
   */
  public void suppressedTestMutatingDataDocumentProducesDocumentOp() {
    MutableDocument<?, ?, ?> doc = target.getDocument("foo");
    // No op for data document creation.
    assertNull(sink.getConsumedOp());
    doc.with(new MutableDocument.Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        doc.createChildElement(
            doc.getDocumentElement(), "x", Collections.<String, String>emptyMap());
      }
    });

    assertTrue("expected WaveletBlipOperation but got " + sink.getConsumedOp(), // \u2620
        sink.getConsumedOp() instanceof WaveletBlipOperation);
    BlipOperation blipOp = ((WaveletBlipOperation) sink.getConsumedOp()).getBlipOp();
    assertTrue(blipOp instanceof BlipContentOperation);
  }

  /**
   * Tests the situation where someone not on a wavelet's participant list
   * attempts to perform some operation. Ensures that the model adds them as a
   * participant before the attempted operations go through.
   */
  public void testNonParticipantAutoAddedByAuthoriser() {
    final ParticipantId creator = target.getCreatorId();
    final ParticipantId bob = new ParticipantId("bob@example.com");

    target.addParticipant(target.getCreatorId());

    sink.clear();
    factory.getLastContextFactory().setParticipantId(bob);
    factory.getLastAuthoriser().program(new MockParticipationHelper.Frame(creator, bob, creator));
    target.addParticipant(ParticipantId.ofUnsafe("myfriend@example.com"));

    List<WaveletOperation> ops = sink.getOps();
    assertEquals(2, ops.size());
    assertAddParticipant(creator, bob, ops.get(0));

    assertTrue(ops.get(1) instanceof AddParticipant);
    assertEquals(bob, ops.get(1).getContext().getCreator());
  }

  /**
   * Like {@link #testNonParticipantAutoAddedByAuthoriser()}, but make sure
   * that when the attempted operation is adding themselves that only the
   * single add-participant operation is generated.  This should be in the form
   * of addParticipant suppressing the given operation and only performing the
   * auto-injected one.
   */
  public void testNonParticipantAutoAddedByAuthoriserSuppressesDuplicateAddParticipantOp() {
    final ParticipantId creator = target.getCreatorId();
    final ParticipantId fred = new ParticipantId("fred@google.com");

    target.addParticipant(target.getCreatorId());
    OpBasedBlip root = target.createBlip("b+fake");

    sink.clear();
    factory.getLastContextFactory().setParticipantId(fred);
    factory.getLastAuthoriser().program(new MockParticipationHelper.Frame(creator, fred, creator));
    target.addParticipant(fred);

    List<WaveletOperation> ops = sink.getOps();
    assertEquals(1, ops.size());
    assertAddParticipant(creator, fred, ops.get(0));
  }

  /**
   * Like {@link #testNonParticipantAutoAddedByAuthoriser()}, but tests that,
   * even though a user adding themselves suppresses duplicate operations, it
   * doesn't suppress all add-participant ops.
   */
  public void testNonParticipantAutoAddedByAuthoriserDoesntSuppressAddParticipantOp() {
    final ParticipantId creator = target.getCreatorId();
    final ParticipantId fred = new ParticipantId("fred@google.com");
    final ParticipantId gary = new ParticipantId("gary@google.com");

    target.addParticipant(target.getCreatorId());
    OpBasedBlip root = target.createBlip("b+fake");

    sink.clear();
    factory.getLastContextFactory().setParticipantId(fred);
    factory.getLastAuthoriser().program(new MockParticipationHelper.Frame(creator, fred, creator));
    target.addParticipant(gary);

    List<WaveletOperation> ops = sink.getOps();
    assertEquals(2, ops.size());
    assertAddParticipant(creator, fred, ops.get(0));
    assertAddParticipant(fred, gary, ops.get(1));
  }
}
