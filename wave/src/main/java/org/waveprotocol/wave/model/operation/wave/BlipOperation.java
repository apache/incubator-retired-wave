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

import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.ReversibleOperation;
import org.waveprotocol.wave.model.operation.Visitable;
import org.waveprotocol.wave.model.wave.data.BlipData;

/**
 * Operation class for a particular kind of wavelet operation that does something to a blip within a
 * wavelet.
 *
 */
public abstract class BlipOperation implements
    ReversibleOperation<BlipOperation, BlipData>, Visitable<BlipOperationVisitor> {

  enum UpdateContributorMethod {
    ADD,     /** Will add the author if not already present (i.e., is a maybeAdd) */
    REMOVE,  /** Will remove the author if already present (i.e., is a maybeRemove) */
    NONE     /** Will not alter the list. */
  }

  /** Context in which this operation occurs. */
  protected final WaveletOperationContext context;

  /**
   * Constructs a blip operation.
   */
  protected BlipOperation(WaveletOperationContext context) {
    this(context, true);
  }

  /**
   * Constructs a blip operation.
   */
  protected BlipOperation(WaveletOperationContext context, boolean isWorthyOfAttribution) {
    this.context = context;
    this.isWorthyOfAttribution = isWorthyOfAttribution;
  }

  /**
   * Gets the operation context.
   *
   * @return the operation context.
   */
  public WaveletOperationContext getContext() {
    return context;
  }

  /**
   * Applies the logic in {@code #apply1(Blip)} to a blip, and updates its metadata.
   *
   * This should not be invoked directly by subclasses; use doApply instead.
   *
   * @param target   blip to modify
   * @throws OperationException if thrown by {@link #doApply(BlipData)}
   */
  public final void apply(BlipData target) throws OperationException {
    // Execute subtype logic first, because if the subtype logic throws an exception, we must
    // leave this wrapper untouched as though the operation never happened. The subtype is
    // responsible for making sure if they throw an exception they must leave themselves in a
    // state as those the op never happened.
    doApply(target);

    // Update metadata second. This means subtypes should assume that the
    // metadata of a blip will be at the old state if they look at it in their
    // operation logic.
    doUpdate(target);
  }

  /**
   * Mutates a blip.  Subclasses can arbitrarily override this to execute their logic.
   *
   * @param target    blip to modify
   * @throws OperationException if an error occurs in the application of this operation.
   */
  protected abstract void doApply(BlipData target) throws OperationException;

  /**
   * Updates the metadata of a blip.  An operation is free to choose whether or
   * not if affects the metadata of a blip.  If it does, it may find the
   * contributor-handling method
   * {@link #update(BlipData, UpdateContributorMethod)} useful.
   *
   * @param target  blip to update
   */
  protected abstract void doUpdate(BlipData target);

  /**
   * Whether this operation updates a blip's metadata: contributors,
   * last-modified version, etc.
   *
   * @param blipId id of the blip to be updated
   */
  protected abstract boolean updatesBlipMetadata(String blipId);

  /**
   * Creates the operation context for the reverse of an operation.
   *
   * @param target  blip from which to extract state to be restored by the
   *                reverse operation
   * @param versionDecrement Number of versions to decrement in reverse.
   * @return context for a reverse of this operation.
   */
  protected final WaveletOperationContext createReverseContext(BlipData target,
      long versionDecrement) {
    // For now, we don't care about version numbers with reverse context
    return new WaveletOperationContext(context.getCreator(), target.getLastModifiedTime(),
        -versionDecrement);
  }

  /**
   * Creates the operation context for the reverse of an operation.
   *
   * @param target  blip from which to extract state to be restored by the
   *                reverse operation
   * @return context for a reverse of this operation.
   */
  protected final WaveletOperationContext createReverseContext(BlipData target) {
    return createReverseContext(target, context.getVersionIncrement());
  }

  //
  // Helper methods for metadata updates, should subclasses choose to use them.
  //

  /**
   * Whether this mutation is the insertion of an inline blip anchor.
   * HACK(user): this is a temporary hack for preventing inline-replies
   *   from affecting the metadata of the parent blip.
   */
  private final boolean isWorthyOfAttribution;

  /**
   * Checks whether this blip operation is worthy of attribution.
   *
   * @param blipId The blip id this operation applies to.
   * @return whether this blip operation is worthy of attribution.
   */
  public boolean isWorthyOfAttribution(String blipId) {
    return isWorthyOfAttribution && WorthyChangeChecker.isBlipIdWorthy(blipId);
  }

  /**
   * Updates a blip's metadata with this operation's context. Updating meta data is a sign
   * that the content of the blip's document have changed.
   *
   * @param target  blip to update
   * @param method  method for updating the contributor list
   * @return the method to reverse any contributor change made by this update.
   */
  protected final UpdateContributorMethod update(BlipData target, UpdateContributorMethod method) {
    if (!updatesBlipMetadata(target.getId())) {
      return UpdateContributorMethod.NONE;
    }

    UpdateContributorMethod reverse;
    switch (method) {
      case ADD:
        if (!target.getContributors().contains(context.getCreator())) {
          target.addContributor(context.getCreator());
          reverse = UpdateContributorMethod.REMOVE;
        } else {
          reverse = UpdateContributorMethod.NONE;
        }
        break;
      case REMOVE:
        if (target.getContributors().contains(context.getCreator())) {
          target.removeContributor(context.getCreator());
          reverse = UpdateContributorMethod.ADD;
        } else {
          reverse = UpdateContributorMethod.NONE;
        }
        break;
      case NONE:
      default:
        reverse = UpdateContributorMethod.NONE;
        break;
    }

    target.setLastModifiedVersion(target.getWavelet().getVersion() + context.getVersionIncrement());
    if (context.hasTimestamp()) {
      target.setLastModifiedTime(context.getTimestamp());
    }

    return reverse;
  }
}
