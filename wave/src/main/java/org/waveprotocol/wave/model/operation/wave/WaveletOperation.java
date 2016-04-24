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
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Date;

/**
 * Superclass of all wavelet operations.  A wavelet operation occurs in a
 * {@link WaveletOperationContext}, and applies to a {@link WaveletData}.
 *
 * wavelet operations are also {@link Visitable} for the purpose of extracting serialization
 * and merge logic into separate classes.
 *
 * Operations are partitioned into wavelet operations, blip operations, and document operations, in
 * order to reduce the number of operation pairs that must be considered for transform and merge
 * (it only makes sense to transform or merge operations that apply to the same type).  The boxing
 * relationships where document ops are boxed as a blip operation, which in turn is boxed as a
 * wavelet operation, are as follows:
 *
 *                                        Operation<T>
 *                                             A
 *         ____________________________________|______________________________________
 *        |                                    |                                      |
 *   WaveletOperation                 --- BlipOperation                    --- DocumentOperation
 *        A                           |        A                           |          A
 *    ____|__________                 |    ____|___________                |    ______|______
 *   |    |          |                |   |    |           |               |   |             |
 *  ...  ...  WaveletBlipOperation <>--  ...  ...  BlipContentOperation <>--  ... (XML ops) ...
 *
 */
public abstract class WaveletOperation implements
    ReversibleOperation<WaveletOperation, WaveletData>, Visitable<WaveletOperationVisitor> {

  /**
   * Clones a wavelet operation, attaching a new context.
   */
  // This might be better as a member method, but is less code this way.
  public static WaveletOperation cloneOp(WaveletOperation op, WaveletOperationContext newContext) {
    if (op instanceof NoOp) {
      return new NoOp(newContext);
    } else if (op instanceof AddParticipant) {
      return new AddParticipant(newContext, ((AddParticipant) op).getParticipantId());
    } else if (op instanceof RemoveParticipant) {
      return new RemoveParticipant(newContext, ((RemoveParticipant) op).getParticipantId());
    } else if (op instanceof WaveletBlipOperation) {
      String docId = ((WaveletBlipOperation) op).getBlipId();
      // BlipContentOperation is the only expected blip op type.
      BlipContentOperation blipOp = (BlipContentOperation) ((WaveletBlipOperation) op).getBlipOp();
      return new WaveletBlipOperation(docId, new BlipContentOperation(newContext,
          blipOp.getContentOp()));
    } else {
      throw new IllegalArgumentException("Un-cloneable operation: " + op);
    }
  }

  /** Context/metadata which does not affect the logic of an operation. */
  protected final WaveletOperationContext context;

  /**
   * Constructs a wavelet operation in a given context.
   *
   * @param context  context in which this operation is occuring
   */
  protected WaveletOperation(WaveletOperationContext context) {
    this.context = context;
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
   * {@inheritDoc}
   *
   * This method delegates the operation logic to {@link #doApply(WaveletData)}, and then
   * updates the wave's timestamp and version.
   */
  public final void apply(WaveletData wavelet) throws OperationException {
    // Execute subtype logic first, because if the subtype logic throws an exception, we must
    // leave this wrapper untouched as though the operation never happened. The subtype is
    // responsible for making sure if they throw an exception they must leave themselves in a
    // state as those the op never happened.
    doApply(wavelet);

    // Update metadata second. This means subtype subtypes should assume that the
    // metadata of a wavelet will be at the old state if they look at it in their
    // operation logic.
    update(wavelet);
  }

  /**
   * Updates the metadata of a wave, according to the operation context.
   *
   * @param wavelet  wavelet to update
   */
  public final void update(WaveletData wavelet) {
    if (context.hasTimestamp()) {
      wavelet.setLastModifiedTime(context.getTimestamp());
    }

    if (context.getVersionIncrement() != 0L) {
      wavelet.setVersion(wavelet.getVersion() + context.getVersionIncrement());
    }

    if (context.hasHashedVersion()) {
      wavelet.setHashedVersion(context.getHashedVersion());
    }
  }

  /**
   * Applies this operation's logic to a given wavelet. This method can be
   * arbitrarily overridden by subclasses.
   *
   * @param wavelet wavelet on which this operation is to apply itself
   * @throws OperationException
   */
  protected abstract void doApply(WaveletData wavelet) throws OperationException;

  /**
   * Creates a no-op operation that updates server meta data. i.e.
   * version numbers and distinct version. Subclasses may override this.
   *
   * @param versionIncrement the version increment for the created op
   * @param hashedVersion the wavelet distinct version for the created op (or null)
   */
  public VersionUpdateOp createVersionUpdateOp(long versionIncrement,
      HashedVersion hashedVersion) {
    return new VersionUpdateOp(context.getCreator(), versionIncrement, hashedVersion);
  }

  /**
   * Creates the operation context for the reverse of an operation.
   *
   * @param target wavelet from which to extract state to be restored by the
   *        reverse operation
   * @param versionDecrement Number of versions to decrement in reverse.
   * @return context for a reverse of this operation.
   */
  protected final WaveletOperationContext createReverseContext(WaveletData target,
      long versionDecrement) {
    return new WaveletOperationContext(context.getCreator(), target.getLastModifiedTime(),
        -versionDecrement, target.getHashedVersion());
  }

  /**
   * Creates the operation context for the reverse of an operation.
   *
   * @param target  wavelet from which to extract state to be restored by the
   *                reverse operation
   * @return context for a reverse of this operation.
   */
  protected final WaveletOperationContext createReverseContext(WaveletData target) {
    return createReverseContext(target, context.getVersionIncrement());
  }

  /**
   * Return a suffix message from the wavelet operation context; the idea is that subclasses
   * will override toString() to call this method and prepend some useful prefix.
   */
  protected String suffixForToString() {
    return "by " + context.getCreator() + " at " + new Date(context.getTimestamp())
        + " version " + context.getHashedVersion() ;
  }

  /**
   * Whether this operation is worthy of attribution. Subclasses may override this.
   * This default implementation always returns true.
   */
  public boolean isWorthyOfAttribution() {
    return true;
  }
}
