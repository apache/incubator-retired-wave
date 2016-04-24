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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import org.waveprotocol.wave.concurrencycontrol.common.DeltaPair;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Mock for describing interactions with a {@link OperationQueue.Transformer},
 * including specifying what deltas should result from those interactions.
 *
 */
class MockDeltaTransformer implements OperationQueue.Transformer {
  /** Representation of an expected call to {@link #transform()}. */
  static class Expectation {
    private final WaveletOperation[] inputClientOps;
    private List<WaveletOperation> outputClientDelta;
    private final List<WaveletOperation> outputServerDelta;

    /**
     * Sets up an expected transform that will accept an input client delta
     * which has the given op list. It will accept any input server delta and
     * output its own server delta.
     */
    Expectation(WaveletOperation... inputClientOps) {
      this.inputClientOps = inputClientOps;
      this.outputServerDelta = new MergingSequence();
    }

    /**
     * Asserts that the given delta meets the requirements to be the input
     * client delta for this expectation.
     */
    void assertClientInputValid(Iterable<WaveletOperation> candidate) {
      assertEquals(Arrays.asList(inputClientOps), CollectionUtils.newArrayList(candidate));
    }

    /**
     * Specifies that the output client delta should be the same as the input
     * client delta.
     */
    public void echo() {
      transformTo(inputClientOps);
    }

    /**
     * Gets the client/server delta pair that will be output by an appropriately
     * timed and parameterised call to this expected transform.
     */
    DeltaPair getOutput() {
      assertNotNull("No output client delta specified", outputClientDelta);
      return new DeltaPair(outputClientDelta, outputServerDelta);
    }

    public void kill() {
      transformTo();
    }

    /**
     * Specifies that the output client delta should consist of the given
     * operations.
     */
    public void transformTo(WaveletOperation... outputClientOps) {
      assertNull("Output client delta already specified", outputClientDelta);
      outputClientDelta = new MergingSequence(Arrays.asList(outputClientOps));
    }
  }

  private final LinkedList<Expectation> expectations;
  private final List<WaveletOperation> firstServerInputDelta;

  /**
   * Creates a transformer that initially has no expectations, and only allows
   * calls to {@link #transform()} that have been expected via
   * {@link #expect(WaveletOperation...)}.
   */
  public MockDeltaTransformer() {
    expectations = CollectionUtils.newLinkedList();
    firstServerInputDelta = new MergingSequence();
  }

  /** Checks that all expected calls to transform have been made. */
  public void checkDone() {
    assertEquals(Collections.emptyList(), expectations);
  }

  /**
   * Sets up that the transform call after all existing expectations must be one
   * with a client input delta consisting of the given ops.
   *
   * @return the expectation which should be set to either
   *         {@link Expectation#echo()} the client input delta or return some
   *         transformed delta using
   *         {@link Expectation#transformTo(WaveletOperation...)}.
   */
  public Expectation expect(WaveletOperation... ops) {
    Expectation expectation = new Expectation(ops);
    expectations.addLast(expectation);
    return expectation;
  }

  /**
   * Gets the ops that should be given to the first
   * {@link #transform()} call.
   */
  public List<WaveletOperation> getInputServerDelta() {
    return firstServerInputDelta;
  }

  /**
   * Gets the delta that should be produced if all expected transforms are
   * performed in order on the {@link #getInputServerDelta()}.
   */
  public List<WaveletOperation> getOutputServerDelta() {
    List<WaveletOperation> delta;
    if (expectations.isEmpty()) {
      delta = firstServerInputDelta;
    } else {
      delta = expectations.getLast().getOutput().getServer();
    }
    return delta;
  }

  @Override
  public DeltaPair transform(Iterable<WaveletOperation> client, Iterable<WaveletOperation> server) {
    Expectation expectation = expectations.removeFirst();
    expectation.assertClientInputValid(client);
    DeltaPair result = expectation.getOutput();
    return result;
  }
}
