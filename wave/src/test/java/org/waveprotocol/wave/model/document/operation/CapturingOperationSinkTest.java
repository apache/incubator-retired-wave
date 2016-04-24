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

package org.waveprotocol.wave.model.document.operation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.operation.CapturingOperationSink;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link CapturingOperationSink}.
 *
 */

public class CapturingOperationSinkTest extends TestCase {
  private static final ParticipantId BOB = new ParticipantId("bob@google.com");
  private static final WaveletOperation ONE = new NoOp(createContext());
  private static final WaveletOperation TWO = new NoOp(createContext());

  private static WaveletOperationContext createContext() {
    return new WaveletOperationContext(BOB, 1L, 1L);
  }

  private CapturingOperationSink<WaveletOperation> sink;

  @Override
  protected void setUp() {
    sink = new CapturingOperationSink<WaveletOperation>();
  }

  /**
   * Tests that the list of operations from a capturing sink is empty if no
   * operations are consumed by it.
   */
  public void testCaptureZeroOps() {
    assertTrue(sink.getOps().isEmpty());
  }

  /**
   * Tests that the operations captured by a capturing sink are returned in
   * order from first to last captured.
   */
  public void testCapturedOpsAreOrdered() {
    sink.consume(ONE);
    sink.consume(TWO);
    List<WaveletOperation> ops = sink.getOps();
    assertEquals(Arrays.asList(ONE, TWO), ops);
    assertSame(ONE, ops.get(0));
    assertSame(TWO, ops.get(1));
  }

  /**
   * Tests that the list of operations a sink returns is kept up to date as more
   * operations arrive.
   */
  public void testOpListIsALiveView() {
    sink.consume(ONE);
    List<WaveletOperation> ops = sink.getOps();
    assertEquals(Arrays.asList(ONE), ops);
    sink.consume(TWO);
    assertEquals(Arrays.asList(ONE, TWO), ops);
  }

  /**
   * Test that the list of operations is properly cleared and kept as a live
   * view.
   */
  public void testClearIsALiveView() {
    sink.consume(ONE);
    List<WaveletOperation> ops = sink.getOps();
    assertEquals(Arrays.asList(ONE), ops);
    sink.clear();
    assertTrue(sink.getOps().isEmpty());
    sink.consume(TWO);
    assertEquals(Arrays.asList(TWO), ops);
  }
}
