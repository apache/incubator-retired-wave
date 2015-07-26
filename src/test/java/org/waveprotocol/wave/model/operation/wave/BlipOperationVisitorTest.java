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

import org.waveprotocol.wave.model.testing.ModelTestUtils;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Tests for the {@code acceptVisitor} method on all blip operations
 *
 * @author anorth@google.com (Alex North)
 */

public class BlipOperationVisitorTest extends TestCase {

  private static class MockBlipOperationVisitor implements BlipOperationVisitor {

    private BlipContentOperation visitedBlipContentOperation;
    private SubmitBlip visitedSubmitBlip;

    @Override
    public void visitBlipContentOperation(BlipContentOperation op) {
      visitedBlipContentOperation = op;
    }

    @Override
    public void visitSubmitBlip(SubmitBlip op) {
      visitedSubmitBlip = op;
    }
  }

  private final ParticipantId fred = new ParticipantId("fred@gwave.com");
  private final WaveletOperationContext context = new WaveletOperationContext(fred, 0L, 0L);
  private MockBlipOperationVisitor visitor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    visitor = new MockBlipOperationVisitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testBlipContentOperationVisitor() {
    DocOp docOp = ModelTestUtils.createContent("Hello");
    BlipContentOperation op = new BlipContentOperation(context, docOp);
    op.acceptVisitor(visitor);
    assertEquals(op, visitor.visitedBlipContentOperation);
  }

  public void testSubmitBlipVisitor() {
    SubmitBlip op = new SubmitBlip(context);
    op.acceptVisitor(visitor);
    assertEquals(op, visitor.visitedSubmitBlip);
  }
}
