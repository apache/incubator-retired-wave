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

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tests {@link OperationQueue}.
 *
 */

public class OperationQueueTest extends TestCase {
  private static final ParticipantId BOB = new ParticipantId("bob@example.com");
  private static final WaveletOperation BOB_A = new NoOp(makeContext(BOB, 1000));
  private static final WaveletOperation BOB_B = new NoOp(makeContext(BOB, 2000));
  private static final WaveletOperation BOB_C = new NoOp(makeContext(BOB, 3000));

  private static final ParticipantId JIM = new ParticipantId("jim@example.com");
  private static final WaveletOperation JIM_A = new NoOp(makeContext(JIM, 1000));
  private static final WaveletOperation JIM_B = new NoOp(makeContext(JIM, 2000));
  private static final WaveletOperation JIM_C = new NoOp(makeContext(JIM, 3000));

  private static final ParticipantId TOM = new ParticipantId("tom@example.com");
  private static final WaveletOperation TOM_B = new NoOp(makeContext(TOM, 2000));

  private OperationQueue queue;
  private MockDeltaTransformer transformer;

  @Override
  protected void setUp() {
    transformer = new MockDeltaTransformer();
    queue = new OperationQueue(transformer);
  }

  private static WaveletOperationContext makeContext(ParticipantId participant, long timestamp) {
    return new WaveletOperationContext(participant, timestamp, 1);
  }

  /** Tests that newly created queues are empty. */
  public void testQueueStartsEmpty() {
    assertQueueIsEmpty();
  }

  /**
   * Tests that adding operations to an empty queue results in it being
   * non-empty until those operations are taken out again.
   */
  public void testQueueNonemptyAfterOperationsAddedUntilTaken() {
    queue.add(BOB_A);
    assertQueueSize(1);
    queue.take();
    assertQueueIsEmpty();
  }

  /**
   * Tests that inserting a new head delta into an empty queue makes the queue
   * non-empty if and only if the delta has ops in it.
   */
  public void testQueueNonemptyAfterInsertingNonemptyHead() {
    queue.insertHead(new MergingSequence());
    assertQueueIsEmpty();
    MergingSequence head = new MergingSequence();
    head.add(BOB_A);
    queue.insertHead(head);
    assertQueueSize(1);
  }

  /**
   * Tests that attempting to take a delta from an empty queue results in a
   * {@link NoSuchElementException} being thrown.
   */
  public void testThrowsExceptionTakingFromEmptyQueue() {
    try {
      queue.take();
      fail("take() from empty queue should fail");
    } catch (NoSuchElementException expected) {
    }
  }

  /**
   * Tests that adding multiple consecutive operations from a single author
   * results in them being merged into the one delta.
   */
  public void testMergesConsecutiveOperationsWithSameCreator() {
    queue.add(BOB_A);
    queue.add(BOB_B);
    assertQueueSizeBetween(1, 2);
    assertEquals(list(BOB_A, BOB_B), copyList(queue.take()));
    // Queue should now be empty, even though we did two adds and only one take
    assertQueueIsEmpty();
  }

  /** Tests that operations by different authors are not merged. */
  public void testDoesNotMergeOperationsDividedByOtherAuthor() {
    queue.add(BOB_A);
    queue.add(JIM_B);
    assertQueueSizeBetween(1, 2);
    assertEquals(list(BOB_A), copyList(queue.take()));
    assertEquals(list(JIM_B), copyList(queue.take()));
  }

  /**
   * Tests that two operations from a single author that are added one after
   * another are not merged if the calls to add() are separated by a take()
   * which extracts the first operation.
   */
  public void testOperationsNotMergedOverQueueEmptyPoints() {
    queue.add(BOB_A);
    assertEquals(list(BOB_A), copyList(queue.take()));
    assertQueueIsEmpty();
    queue.add(BOB_B);
    assertEquals(list(BOB_B), copyList(queue.take()));
  }

  /**
   * Tests that two operations from a single author that are added one after
   * another are still merge if the calls to add() are separated by a take()
   * which extracts previously added operations rather than either of the two
   * aforementioned operations.
   */
  public void testOperationsMergeDespiteTakeIfQueueNotMadeEmpty() {
    queue.add(JIM_A);
    queue.add(BOB_B);
    assertEquals(list(JIM_A), copyList(queue.take()));
    assertQueueSize(1);
    assertQueueSizeBetween(1, 2);
    queue.add(BOB_C);
    assertEquals(list(BOB_B, BOB_C), copyList(queue.take()));
    assertQueueIsEmpty();
  }

  /**
   * Tests that a new head delta can be inserted in at the start of an empty
   * queue. Future adds of ops by the same creator do not merge with it.
   */
  public void testHeadInsertedIntoEmptyQueueAllowsFutureMerging() {
    MergingSequence head = new MergingSequence();
    head.add(BOB_A);
    head.add(BOB_B);
    queue.insertHead(head);
    queue.add(BOB_C);
    assertQueueSizeBetween(1, 3);
    // Delta pushed to head is not mergable to a single delta.
    assertEquals(list(BOB_A, BOB_B), copyList(queue.take()));
    assertEquals(list(BOB_C), copyList(queue.take()));
    assertTrue(queue.isEmpty());
  }

  /**
   * Tests that a new head delta can be inserted in a non-empty queue.
   * It does not merge with the existing head.
   */
  public void testInsertHeadMergesIfSameCreatorAsExistingHead() {
    queue.add(BOB_C);
    MergingSequence head = new MergingSequence();
    head.add(BOB_A);
    head.add(BOB_B);
    queue.insertHead(head);
    assertQueueSizeBetween(1, 3);

    // Delta pushed to head is not mergable to a single delta.
    assertEquals(list(BOB_A, BOB_B), copyList(queue.take()));
    assertEquals(list(BOB_C), copyList(queue.take()));
    assertTrue(queue.isEmpty());
  }

  /**
   * Tests that a new head delta which is being inserted does not merge with the
   * existing head if the creators do not match, hence the old head is moved to
   * be the second delta but otherwise remains unmodified.
   */
  public void testInsertHeadMakesNewDeltaIfCreatorDiffersFromExistingHead() {
    queue.add(BOB_C);
    MergingSequence head = new MergingSequence();
    head.add(JIM_A);
    head.add(JIM_B);
    queue.insertHead(head);
    assertEquals(list(JIM_A, JIM_B), copyList(queue.take()));
    assertEquals(list(BOB_C), copyList(queue.take()));
    assertTrue(queue.isEmpty());
  }

  /**
   * Tests that the deltas provided by take() are optimised. Note that this test
   * makes assumptions about what type of deltas are used by
   * {@link OperationQueue} and what operations those deltas merge.
   */
  public void testProducesOptimisedDeltas() {
    queue.add(new WaveletBlipOperation("a", new BlipContentOperation(BOB_A.getContext(),
        new DocOpBuilder().retain(1).characters("hi").retain(1).build())));
    queue.add(new WaveletBlipOperation("a", new BlipContentOperation(BOB_B.getContext(),
        new DocOpBuilder().retain(1).characters("hi").retain(3).build())));
    assertQueueSizeBetween(1, 2);
    assertEquals(1, queue.take().size());
    assertQueueIsEmpty();
  }

  /**
   * Tests that deltas which are transformed make their way back into the queue,
   * replacing the untransformed versions and being sent out of take().
   */
  public void testTransformedDeltasReplaceOriginals() throws TransformException {
    queue.add(BOB_A);
    queue.add(BOB_B);
    queue.add(JIM_C);

    transformer.expect(BOB_A, BOB_B).transformTo(BOB_A);
    transformer.expect(JIM_C).echo();

    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));
    transformer.checkDone();
    assertQueueSizeBetween(1, 2);
    assertEquals(list(BOB_A), copyList(queue.take()));
    assertEquals(list(JIM_C), copyList(queue.take()));
    assertQueueIsEmpty();
  }

  /**
   * Tests that deltas which become empty due to a transform are discarded from
   * the queue.
   */
  public void testDeltasEmptyAfterTransformAreDiscarded() throws TransformException {
    queue.add(BOB_A);
    queue.add(JIM_A);
    queue.add(BOB_B);
    queue.add(TOM_B);
    queue.add(BOB_C);

    transformer.expect(BOB_A).kill();
    transformer.expect(JIM_A).echo();
    transformer.expect(BOB_B).kill();
    transformer.expect(TOM_B).echo();
    transformer.expect(BOB_C).kill();

    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));
    transformer.checkDone();
    assertQueueSizeBetween(1, 2);
    assertEquals(list(JIM_A), copyList(queue.take()));
    assertEquals(list(TOM_B), copyList(queue.take()));
    assertQueueIsEmpty();
  }

  /**
   * Tests that deltas with the same author are merged if they end up being
   * consecutive after deltas which were previously dividing them are discarded
   * in a transform.
   */
  public void testAdjacentDeltasBySameAuthorAfterTransformDiscardsAreMerged()
      throws TransformException {
    queue.add(BOB_A);
    queue.add(JIM_A);
    queue.add(BOB_B);
    queue.add(TOM_B);
    queue.add(BOB_C);

    transformer.expect(BOB_A).echo();
    transformer.expect(JIM_A).kill();
    transformer.expect(BOB_B).echo();
    transformer.expect(TOM_B).kill();
    transformer.expect(BOB_C).echo();

    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));
    transformer.checkDone();
    assertQueueSizeBetween(1, 3);
    assertEquals(list(BOB_A, BOB_B, BOB_C), copyList(queue.take()));
    assertQueueIsEmpty();
  }

  /**
   * Test we still compose operations after we've transformed.
   */
  public void testComposistionAfterTransform() throws TransformException {
    DeltaTestUtil util = new DeltaTestUtil(BOB);

    // 2 ops merged into one.
    queue.add(util.noOpDocOp("blipA"));
    queue.add(util.noOpDocOp("blipA"));

    // Get a server op
    transformer.expect(util.noOpDocOp("blipA")).echo();
    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));

    // Adding one more operation after that should not merge into previously
    // transformed client op
    queue.add(util.noOpDocOp("blipA"));

    // This operation should merge into the previous op as the previous op is not
    // yet transformed
    queue.add(util.noOpDocOp("blipA"));

    // Should get 1 op, the result of merging and composing the two deltas.
    assertQueueSizeBetween(1, 3);
    assertEquals(list(util.noOpDocOp("blipA")), copyList(queue.take()));

    assertQueueIsEmpty();
  }

  public void testCompositionBeforeTransform() throws TransformException {
    DeltaTestUtil util = new DeltaTestUtil(BOB);

    // 2 ops merged into one.
    queue.add(util.noOpDocOp("blipA"));
    queue.add(util.noOpDocOp("blipA"));

    // Get a server op
    transformer.expect(util.noOpDocOp("blipA")).echo();
    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));

    // Should get only 1 op
    assertQueueSize(1);
    assertEquals(list(util.noOpDocOp("blipA")), copyList(queue.take()));

    assertQueueIsEmpty();
  }

  /**
   * Test we compose operations after we've transformed. However, we should get several
   * transformed client operations out in the same delta if they don't compose.
   */
  public void testGettingSeveralOpsInOneDelta() throws TransformException {
    DeltaTestUtil util = new DeltaTestUtil(BOB);

    // Do 1 client op
    queue.add(util.noOpDocOp("blipA"));

    // Get a server op
    transformer.expect(util.noOpDocOp("blipA")).echo();
    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));

    // Do another client op
    queue.add(util.noOpDocOp("blipB"));

    // Get a server op
    transformer.expect(util.noOpDocOp("blipA"), util.noOpDocOp("blipB")).echo();
    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));

    // Should get 2 ops in the same delta
    assertQueueSizeBetween(1, 2);
    assertEquals(list(util.noOpDocOp("blipA"), util.noOpDocOp("blipB")), copyList(queue.take()));

    assertQueueIsEmpty();
  }

  /**
   * Since transform and compose doesn't commute, test we don't compose operations after
   * we've sent the operation. So when we push to head, we shouldn't merge sent deltas.
   */
  public void testNoCompositionWithInsertHead() throws TransformException {
    DeltaTestUtil util = new DeltaTestUtil(BOB);

    // Do 1 client op
    queue.add(util.noOpDocOp("blipA"));
    // Pushing an op to the head should never merge
    queue.insertHead(Arrays.asList(util.noOpDocOp("blipA")));

    // Get a server op
    transformer.expect(util.noOpDocOp("blipA")).echo();
    transformer.expect(util.noOpDocOp("blipA")).echo();
    assertEquals(transformer.getOutputServerDelta(), // \u2620
        queue.transform(transformer.getInputServerDelta()));


    // Should be 2 deltas a the first one is not mergable
    assertQueueSizeBetween(1, 2);
    assertEquals(list(util.noOpDocOp("blipA")), copyList(queue.take()));
    assertEquals(list(util.noOpDocOp("blipA")), copyList(queue.take()));

    assertQueueIsEmpty();
  }

  /**
   * Test operations are actually transformed.
   */
  public void testOpsAreTransformed() throws TransformException {
    queue = new OperationQueue();
    DeltaTestUtil bob = new DeltaTestUtil(BOB);
    DeltaTestUtil jim = new DeltaTestUtil(JIM);

    // Do bob client op 1
    queue.add(bob.insert(1, "a", 1, null));

    // Get delta 1 from jim
    queue.transform(Arrays.asList(jim.insert(1, "j", 1, null)));

    // Do bob client op 2
    queue.add(bob.insert(1, "b", 3, null));

    // Get delta 2 from jim
    queue.transform(Arrays.asList(jim.insert(1, "i", 2, null)));

    // check ops are transformed
    assertEquals(list(bob.insert(1, "ba", 3, null)), copyList(queue.take()));

    assertQueueIsEmpty();
  }

  /**
   * Tests that the queue's size estimate is at least one per un-mergable delta.
   * Since it's an underestimate it must be exactly correct if each delta
   * contains one op.
   */
  public void testQueueSizeEstimateIsAtLeastDeltaSize() {
    queue.add(BOB_A);
    queue.add(JIM_A);
    queue.add(BOB_B);
    queue.add(JIM_B);
    queue.add(BOB_C);
    queue.add(JIM_C);
    assertEquals(6, queue.estimateSize());
  }

  /** Asserts that the queue is empty and size estimate is zero. */
  private void assertQueueIsEmpty() {
    assertTrue("Expected empty queue", queue.isEmpty());
    assertEquals("Expected queue size zero", 0, queue.estimateSize());
  }

  /**
   * Asserts that the queue is not empty and the size estimate is as expected.
   */
  private void assertQueueSize(int size) {
    assertFalse("Expected non-empty queue", queue.isEmpty());
    int estimate = queue.estimateSize();
    assertEquals("Expected queue size " + size + ", was " + estimate, size, estimate);
  }

  /**
   * Asserts that the queue is not empty and the size estimate is within
   * expected bounds (inclusive).
   */
  private void assertQueueSizeBetween(int minSize, int maxSize) {
    assertFalse("Expected non-empty queue", queue.isEmpty());
    int estimate = queue.estimateSize();
    assertTrue("Expected queue size >= " + minSize + ", was " + estimate, estimate >= minSize);
    assertTrue("Expected queue size <= " + maxSize + ", was " + estimate, estimate <= maxSize);
  }

  @SuppressWarnings("unchecked")
  private static <T> List<T> list(T... es) {
    return Collections.unmodifiableList(CollectionUtils.newArrayList(es));
  }

  private static <T> List<T> copyList(Iterable<T> es) {
    return Collections.unmodifiableList(CollectionUtils.newArrayList(es));
  }
}
