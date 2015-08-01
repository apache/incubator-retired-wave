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

package org.waveprotocol.wave.model.operation.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.RemovedAuthorException;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Tests for the transformations of operations that remove or add participants.
 *
 */

public class ParticipantTransformTest extends TestCase {

  private static final RemoveParticipant remove1a;
  private static final RemoveParticipant remove2a;
  private static final RemoveParticipant remove2b;
  private static final AddParticipant add1a;
  private static final AddParticipant add2a;
  private static final AddParticipant add2b;
  private static final NoOp noop1;
  private static final NoOp noop2;
  private static final WaveletBlipOperation mutation;

  static {
    WaveletOperationContext context1 =
        new WaveletOperationContext(new ParticipantId("p1@google.com"), 1L, 1L);
    WaveletOperationContext context2 =
        new WaveletOperationContext(new ParticipantId("p2@google.com"), 1L, 1L);
    WaveletOperationContext contextA =
      new WaveletOperationContext(new ParticipantId("a@google.com"), 1L, 1L);
    remove1a = new RemoveParticipant(context1, new ParticipantId("a@google.com"));
    remove2a = new RemoveParticipant(context2, new ParticipantId("a@google.com"));
    remove2b = new RemoveParticipant(context2, new ParticipantId("b@google.com"));
    add1a = new AddParticipant(context1, new ParticipantId("a@google.com"));
    add2a = new AddParticipant(context2, new ParticipantId("a@google.com"));
    add2b = new AddParticipant(context2, new ParticipantId("b@google.com"));
    noop1 = new NoOp(context1);
    noop2 = new NoOp(context2);
    mutation = new WaveletBlipOperation("dummy",
        new BlipContentOperation(contextA, (new DocOpBuilder()).characters("x").build()));
  }

  /**
   * Tests that the correct exception is thrown when a removed participant
   * issues an operation.
   */
  public void testRemovedAuthorException() {
    checkTransformThrowsException(mutation, remove2a, RemovedAuthorException.class);
  }

  /**
   * Tests that no exception is thrown in various cases.
   */
  public void testNoException() {
    checkIdentityTransform(remove2a, mutation);
    checkIdentityTransform(mutation, remove2b);
    checkIdentityTransform(remove2b, mutation);
  }

  /**
   * Tests the transformation of two participant addition operations.
   */
  public void testAdditionVsAddition() {
    checkTransform(add1a, add2a, noop1, noop2);
    checkIdentityTransform(add1a, add2b);
  }

  /**
   * Tests the transformation of a participant addition with a participant
   * removal.
   */
  public void testAdditionVsRemoval() {
    checkTransformThrowsException(add1a, remove2a, TransformException.class);
    checkTransformThrowsException(remove2a, add1a, TransformException.class);
    checkIdentityTransform(add1a, remove2b);
    checkIdentityTransform(remove2b, add1a);
  }

  /**
   * Tests the transformation of two participant removal operations.
   */
  public void testRemovalVsRemoval() {
    checkTransform(remove1a, remove2a, noop1, noop2);
    checkIdentityTransform(remove1a, remove2b);
  }

  private static void checkTransform(
      WaveletOperation clientOperation,
      WaveletOperation serverOperation,
      WaveletOperation transformedClientOperation,
      WaveletOperation transformedServerOperation) {
    try {
      OperationPair<WaveletOperation> operationPair =
          Transform.transform(clientOperation, serverOperation);
      assertTrue(transformedClientOperation.equals(operationPair.clientOp()));
      assertTrue(transformedServerOperation.equals(operationPair.serverOp()));
    } catch (TransformException e) {
      fail("Unexpected exception thrown");
    }
  }

  private static void checkTransformThrowsException(
      WaveletOperation clientOperation,
      WaveletOperation serverOperation,
      Class<? extends TransformException> exceptionClass) {
    try {
      OperationPair<WaveletOperation> operationPair =
          Transform.transform(clientOperation, serverOperation);
      fail("Expected exception not thrown.");
    } catch (TransformException e) {
      assertEquals(exceptionClass, e.getClass());
    }
  }

  private static void checkIdentityTransform(
      WaveletOperation clientOperation,
      WaveletOperation serverOperation) {
    checkTransform(clientOperation, serverOperation, clientOperation, serverOperation);
  }

}
