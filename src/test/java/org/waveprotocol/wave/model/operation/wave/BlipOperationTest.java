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

import org.waveprotocol.wave.model.operation.wave.BlipOperation.UpdateContributorMethod;
import org.waveprotocol.wave.model.testing.ModelTestUtils;
import org.waveprotocol.wave.model.wave.data.BlipData;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Tests for the shared behaviour in BlipOperation.
 *
 * @author anorth@google.com (Alex North)
 */

public class BlipOperationTest extends OperationTestBase {

  /**
   * A FakeBlipOperation doesn't actually modify the blip, just records
   * whether it's been applied.
   */
  static class FakeBlipOperation extends BlipOperation {

    public boolean applied = false;

    protected FakeBlipOperation(WaveletOperationContext context) {
      super(context);
    }

    @Override
    protected void doApply(BlipData target) {
      this.applied = true;
    }

    @Override
    protected void doUpdate(BlipData target) {
      // nothing
    }

    @Override
    protected boolean updatesBlipMetadata(String blipId) {
      return false;
    }

    /** Applies and returns another FakeBlipOperation as the reverse */
    @Override
    public List<? extends BlipOperation> applyAndReturnReverse(BlipData target) {
      WaveletOperationContext reverseContext = createReverseContext(target);
      doUpdate(target);
      doApply(target);
      return Collections.singletonList(new FakeBlipOperation(reverseContext));
    }

    public void acceptVisitor(BlipOperationVisitor visitor) { }
  }

  private WaveletOperationContext createJaneContext() {
    return new WaveletOperationContext(jane, 42L, 1L);
  }

  private BlipOperation createSampleContentOperation(WaveletOperationContext context,
      UpdateContributorMethod method) {
    // Some random op that doesn't actually update the document but still "isWorthy"
    DocOp op = new DocOpBuilder()
        .retain(1)
        .deleteElementStart("line", Attributes.EMPTY_MAP)
        .deleteElementEnd()
        .elementStart("line", Attributes.EMPTY_MAP)
        .elementEnd()
        .retain(1)
        .build();
    assertTrue(WorthyChangeChecker.isWorthy(op));
    return new BlipContentOperation(context, op, method);
  }

  private BlipOperation createSampleContentOperation(UpdateContributorMethod method) {
    return createSampleContentOperation(context, method);
  }

  private BlipOperation createSampleContentOperation() {
    return createSampleContentOperation(UpdateContributorMethod.ADD);
  }

  public void testGetContext() {
    BlipOperation op = new FakeBlipOperation(context);
    assertEquals(op.getContext(), context);
  }

  public void testApplyInvokesSubclassDoApply() throws Exception {
    BlipData data = createBlipData();
    FakeBlipOperation op = new FakeBlipOperation(context);

    op.apply(data);
    assertTrue(op.applied);
  }

  /**
   * Tests that apply() updates the blip timestamp to match the context
   */
  public void testApplyUpdatesTimestamp() throws Exception {
    BlipData data = createBlipData();
    BlipOperation op = createSampleContentOperation();

    assertFalse(context.getTimestamp() == data.getLastModifiedTime());
    op.apply(data);
    // The last modified time is now the context time
    assertEquals(context.getTimestamp(), data.getLastModifiedTime());
    // The blip version is updated to match the wavelet version + 1 (but the op
    // doesn't actually increment the wavelet/blip version until it's applied
    // to a wavelet).
    assertEquals(waveletData.getVersion() + 1,
        data.getLastModifiedVersion());
  }

  public void testNoneContributorMethodLeavesContributors() throws Exception {
    BlipData data = createBlipData();

    BlipOperation op = createSampleContentOperation(UpdateContributorMethod.NONE);
    op.apply(data);
    assertEquals(data.getContributors(), noParticipants);
  }

  public void testAddContributorMethodAddsNewContributor() throws Exception {
    BlipData data = createBlipData();

    BlipOperation op = createSampleContentOperation(UpdateContributorMethod.ADD);
    op.apply(data);
    assertTrue(data.getContributors().contains(fred));

    op = createSampleContentOperation(createJaneContext(), UpdateContributorMethod.ADD);
    op.apply(data);
    assertTrue(data.getContributors().contains(fred));
    assertTrue(data.getContributors().contains(jane));
  }

  public void testAddContributorMethodDoesntDuplicateContributors() throws Exception {
    BlipData data = createBlipData();

    BlipOperation op = createSampleContentOperation(UpdateContributorMethod.ADD);
    op.apply(data);

    op = createSampleContentOperation(UpdateContributorMethod.ADD);
    op.apply(data);

    assertEquals(Collections.singleton(fred), data.getContributors());
  }

  public void testReverseAddContributorRemovesContributor() throws Exception {
    BlipData data = createBlipData();

    BlipOperation op = createSampleContentOperation(UpdateContributorMethod.ADD);
    List<? extends BlipOperation> reverseOps = op.applyAndReturnReverse(data); // adds fred
    for (BlipOperation rop : reverseOps) {
      rop.apply(data);
    }

    assertEquals(noParticipants, data.getContributors());
  }

  private BlipData createBlipData() {
    return waveletData.createDocument("blipid", fred, noParticipants,
        ModelTestUtils.createContent(""), 0L, 0L);
  }
}
