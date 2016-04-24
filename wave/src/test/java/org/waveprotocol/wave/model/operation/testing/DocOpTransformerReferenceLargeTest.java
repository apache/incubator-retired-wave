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

package org.waveprotocol.wave.model.operation.testing;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.reference.ReferenceTransformer;

import java.util.Random;

/**
 * Tests for verifying that the behavior of the optimized document operation
 * transformer matches the behavior of the reference transformer.
 *
 * @author Alexandre Mah
 */
public class DocOpTransformerReferenceLargeTest extends TestCase {

  // NOTE: Ideally, the optimised transform implementation in class Transformer
  // should match the reference implementation. It currently doesn't,
  // which becomes apparent if you run much more than 100 iterations.
  // See the comment in the Transformer class.
  private final int NUM_ITERATIONS = 100; // NOTE: increase if you test changes to doc op transform
  private final int INITIAL_MUTATION_COUNT = 3;
  private final int FEATURE_ITERATION_COUNT = 20;

  public void testEquivalence() throws OperationException, TransformException {
    Random r = new Random(0);
    DocOpGenerator generator = new DocOpGenerator();
    for (int iteration = 0; iteration < NUM_ITERATIONS; ++iteration) {
      System.out.println("Iteration: " + iteration);
      BootstrapDocument document= new BootstrapDocument();
      for (int i = 0; i < INITIAL_MUTATION_COUNT; ++i) {
        document.consume(generator.randomOperation(document, r));
      }
      for (int i = 0; i < FEATURE_ITERATION_COUNT; ++i) {
        DocOp clientOp = generator.randomOperation(document, r);
        DocOp serverOp = generator.randomOperation(document, r);
        OperationPair<DocOp> pair = Transformer.transform(clientOp, serverOp);
        OperationPair<DocOp> referencePair =
            ReferenceTransformer.transform(clientOp, serverOp);
        assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(
            pair.clientOp(), referencePair.clientOp()));
        assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(
            pair.serverOp(), referencePair.serverOp()));
        document.consume(clientOp);
        document.consume(pair.serverOp());
      }
    }
  }

}
