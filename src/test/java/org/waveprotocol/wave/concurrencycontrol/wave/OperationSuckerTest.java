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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.concurrencycontrol.testing.MockOperationChannel;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Queue;

/**
 * Tests for operation sucker operation.
 *
 * @author anorth@google.com (Alex North)
 */

public class OperationSuckerTest extends TestCase {
  /**
   * A mock flushing operation sink.
   */
  private static final class MockFlushingOperationSink implements
      FlushingOperationSink<WaveletOperation> {

    private static enum Method {
      CONSUME, FLUSH
    }

    private final Queue<Object[]> expectations = CollectionUtils.newLinkedList();
    private Runnable resumeCommand;

    /**
     * Expects a call to consume an op. Optionally performs an action
     * when the consume() call is made.
     */
    void expectConsume(WaveletOperation op, Runnable action) {
      expectations.add(new Object[] {Method.CONSUME, op, action});
    }

    /**
     * Expects a call to flush and operation with a resume command. If {@code}
     * succeed is true then the call will return true, else it will return false
     * (and the caller will expect the resume command to be later invoked).
     */
    void expectFlush(WaveletOperation operation, boolean succeed) {
      expectations.add(new Object[] {Method.FLUSH, operation, succeed});
    }

    void checkExpectationsSatisfied() {
      assertTrue(expectations.isEmpty());
    }

    /**
     * @return the last command passed to flush()
     */
    Runnable getLastResumeCommand() {
      return resumeCommand;
    }

    @Override
    public void consume(WaveletOperation op) {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.CONSUME);
      assertSame(expected[1], op);
      if (expected[2] != null) {
        ((Runnable) expected[2]).run();
      }
    }

    @Override
    public boolean flush(WaveletOperation operation, Runnable resume) {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.FLUSH);
      assertSame(expected[1], operation);
      resumeCommand = resume;
      return (Boolean) expected[2];
    }
  }

  private final WaveletOperationContext.Factory contextFactory =
      new BasicWaveletOperationContextFactory(new ParticipantId("bob@example.com"));

  private MockOperationChannel channel;
  private MockFlushingOperationSink sink;
  private OperationSucker sucker;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    channel = new MockOperationChannel();
    sink = new MockFlushingOperationSink();
    sucker = new OperationSucker(channel, sink);
  }

  @Override
  protected void tearDown() throws Exception {
    channel.checkExpectationsSatisfied();
    sink.checkExpectationsSatisfied();
    super.tearDown();
  }

  /**
   * Tests that the sucker keeps sucking while ops are available and the
   * sink doesn't need to flush.
   */
  public void testSucksWhileNoFlushNeeded() {
    // Set expectations.
    WaveletOperation[] ops = makeOps(3);
    for (int i = 0; i < 3; ++i) {
      WaveletOperation op = ops[0];
      channel.expectPeek(op);
      sink.expectFlush(op, true);
      // The sucker peeks again after flush in case the op has changed.
      channel.expectPeek(op);
      channel.expectReceive(op);
      sink.expectConsume(op, null);
    }
    channel.expectPeek(null);

    // Go!
    sucker.onOperationReceived();
  }

  /**
   * Tests that the sucker stops sucking if a flush is required, and
   * resumes when the flush is done.
   */
  public void testFlushPausesSucking() {
    // Set expectations.
    WaveletOperation op = makeOp();
    channel.expectPeek(op);
    sink.expectFlush(op, false);

    // Go!
    sucker.onOperationReceived();

    channel.checkExpectationsSatisfied();
    sink.checkExpectationsSatisfied();
    Runnable resume = sink.getLastResumeCommand();
    assertNotNull(resume);

    // Another op received should not cause any action
    sucker.onOperationReceived();
    channel.checkExpectationsSatisfied();
    sink.checkExpectationsSatisfied();

    // Set expectations for the resume command.
    channel.expectPeek(op);
    sink.expectFlush(op, true);
    channel.expectPeek(op);
    channel.expectReceive(op);
    sink.expectConsume(op, null);

    channel.expectPeek(null);

    // Go!
    resume.run();
  }

  public void testNoSuckingAfterShutdown() {
    sucker.shutdown();
    sucker.onOperationReceived();
    // Expect no interactions with the channel.
  }

  /**
   * Tests that the sucker doesn't touch the operation channel after being
   * shut down while consuming ops.
   */
  public void testStopsSuckingAfterShutdown() {
    // Set expectations.
    WaveletOperation op = makeOp();
    channel.expectPeek(op);
    sink.expectFlush(op, true);

    channel.expectPeek(op);
    channel.expectReceive(op);
    // Shut down the sucker when consuming the op.
    sink.expectConsume(op, new Runnable() {
      @Override
      public void run() {
        sucker.shutdown();
      }
    });
    // No more expectations of peek() or receive().

    sucker.onOperationReceived();
  }

  private WaveletOperation[] makeOps(int howMany) {
    WaveletOperation[] ops = new WaveletOperation[howMany];
    for (int i = 0; i < howMany; ++i) {
      ops[i] = makeOp();
    }
    return ops;
  }

  private WaveletOperation makeOp() {
    return new NoOp(contextFactory.createContext());
  }
}
