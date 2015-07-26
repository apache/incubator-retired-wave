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

import org.waveprotocol.wave.model.wave.data.BlipData;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Operation class for boxing a document operation as a wave/blip operation.
 * A {@code BlipContentOperation} applies to a blip by passing its contained document operation to
 * the blip's document-operation sink.  Note that this is slightly different from what one might
 * expect (applying to a blip by applying the contained document-operation to the blip's content),
 * but it leverages the weaker contract of a sink in order to hide the document structure from the
 * blip interface such that a wider range of implementation options are possible for the reception
 * of document operations.
 *
 */
public final class BlipContentOperation extends BlipOperation {

  /** Document operation to apply to the target blip's content. */
  private final DocOp contentOp;

  private final UpdateContributorMethod method;

  /**
   * Constructs a blip-content operation.
   *
   * @param context     operation context
   * @param contentOp   document operation to apply to the target blip
   */
  public BlipContentOperation(WaveletOperationContext context, DocOp contentOp) {
    this(context, contentOp, UpdateContributorMethod.ADD);
  }

  /**
   * Constructs a blip-content operation.
   *
   * @param context     operation context
   * @param contentOp   document operation to apply to the target blip
   * @param update      contributor update method
   */
  BlipContentOperation(WaveletOperationContext context, DocOp contentOp,
      UpdateContributorMethod update) {
    super(context, WorthyChangeChecker.isWorthy(contentOp));
    Preconditions.checkNotNull(contentOp, "Null document mutation");
    this.contentOp = contentOp;
    this.method = update;
  }

  /**
   * Applies this operation to a blip by applying its document operation to the blip's content.
   */
  @Override
  protected void doApply(BlipData blip) throws OperationException {
    // Apply document mutation
    blip.getContent().consume(contentOp);

    if (isWorthyOfAttribution(blip.getId())) {
      blip.onRemoteContentModified();
    }
  }

  @Override
  protected void doUpdate(BlipData target) {
    update(target, method);
  }

  @Override
  public List<? extends BlipOperation> applyAndReturnReverse(final BlipData blip)
      throws OperationException {
    WaveletOperationContext reverseContext = createReverseContext(blip);

    // Update metadata
    UpdateContributorMethod reverseMethod = update(blip, method);

    blip.getContent().consume(contentOp);

    DocOp reverseContentOp = DocOpInverter.invert(contentOp);

    BlipContentOperation reverseOp =
        new BlipContentOperation(reverseContext, reverseContentOp, reverseMethod);
    return Collections.singletonList(reverseOp);
  }

  @Override
  public void acceptVisitor(BlipOperationVisitor visitor) {
    visitor.visitBlipContentOperation(this);
  }

  /**
   * Updates only the metadata of a blip.
   *
   * @param target  blip to update
   */
  public void update(BlipData target) {
    doUpdate(target);
  }

  /**
   * Gets the contained document operation.
   *
   * @return the contained document operation.
   */
  public DocOp getContentOp() {
    return contentOp;
  }

  @Override
  public boolean updatesBlipMetadata(String blipId) {
    return isWorthyOfAttribution(blipId);
  }

  @Override
  public String toString() {
    return "document op: " + contentOp;
  }

  @Override
  public int hashCode() {
    // Note that we don't have an implementation of contentOp.hashCode()
    // which is compatible with OpComparators.SYNTACTIC_IDENTITY.equal().
    // Therefore we ignore contentOp in the hash code computation here
    // so that it's compatible with equals().
    // TODO: Implement contentOp.hashCode(), compatible with
    // OpComparators.SYNTACTIC_IDENTITY.equal(), and return that here.
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context and update method in equality
     * comparison. The plan is to remove context from all operations in the
     * future.
     */
    if (!(obj instanceof BlipContentOperation)) {
      return false;
    }
    BlipContentOperation other = (BlipContentOperation) obj;
    return OpComparators.SYNTACTIC_IDENTITY.equal(contentOp, other.contentOp);
  }
}
