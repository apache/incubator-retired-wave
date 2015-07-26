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

import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Operation class for a particular kind of wavelet operation that does something to a blip within a
 * wavelet.
 *
 */
public final class WaveletBlipOperation extends WaveletOperation {
  /** Identifier of the blip within the target wavelet to mutate. */
  private final String blipId;

  /** The operation to apply to the identified blip. */
  private final BlipOperation blipOp;

  /**
   * Constructs a wave-blip operation.
   *
   * NOTE(user): This is a dangerous constructor. BlipOperation is currently
   * subclassable outside this package. Perhaps BlipOperation should be more
   * restricted.
   *
   * @param blipId identifier of the blip within the target wavelet to mutate
   * @param blipOp operation to apply to the identified blip
   */
  public WaveletBlipOperation(String blipId, BlipOperation blipOp) {
    super(blipOp.getContext());
    Preconditions.checkNotNull(blipId, "Null blip ID");
    Preconditions.checkNotNull(blipOp, "Null blip operation");
    this.blipId = blipId;
    this.blipOp = blipOp;
  }

  /**
   * Gets the identifier of the blip  within the target wavelet to mutate.
   *
   * @return the identifier of the blip  within the target wavelet to mutate.
   */
  public String getBlipId() {
    return blipId;
  }

  /**
   * Gets the boxed blip operation.
   *
   * @return the boxed blip operation.
   */
  public BlipOperation getBlipOp() {
    return blipOp;
  }

  /**
   * Applies this operation to a wavelet by applying its contained blip operation to the
   * identified blip.
   */
  @Override
  protected void doApply(WaveletData target) throws OperationException {
    BlipData blip = getTargetBlip(target);
    blipOp.apply(blip);
  }

  @Override
  public VersionUpdateOp createVersionUpdateOp(long versionIncrement,
      HashedVersion hashedVersion) {
    return new VersionUpdateOp(context.getCreator(), versionIncrement, hashedVersion,
        blipOp.updatesBlipMetadata(blipId) ? blipId : null);
  }

  @Override
  public void acceptVisitor(WaveletOperationVisitor visitor) {
    visitor.visitWaveletBlipOperation(this);
  }

  @Override
  public String toString() {
    return blipOp + " on blip " + blipId + " " + suffixForToString() +
        " isWorthy " + isWorthyOfAttribution();
  }

  @Override
  public List<WaveletBlipOperation> applyAndReturnReverse(
      WaveletData target) throws OperationException {
    BlipData blip = getTargetBlip(target);
    List<? extends BlipOperation> operations = blipOp.applyAndReturnReverse(blip);
    List<WaveletBlipOperation> ret = new ArrayList<WaveletBlipOperation>();
    for (BlipOperation op : operations) {
      ret.add(new WaveletBlipOperation(blipId, op));
    }
    update(target);
    return ret;
  }

  /**
   * Gets the blip targeted by this operation, creating a data document if it
   * does not exist.
   *
   * @throws OperationException if the blip does not exist and is not a data
   *         document
   * @return blip targeted by this operation, never null
   */
  private BlipData getTargetBlip(WaveletData target) throws OperationException {
    BlipData blip = target.getDocument(blipId);
    if (blip == null) {
      // Implicitly create blips.
      blip = target.createDocument(blipId, context.getCreator(),
          Collections.singleton(context.getCreator()), EmptyDocument.EMPTY_DOCUMENT,
          context.getTimestamp(), target.getVersion() + context.getVersionIncrement());
    }
    assert blip != null;
    return blip;
  }

  @Override
  public boolean isWorthyOfAttribution() {
    return blipOp.isWorthyOfAttribution(blipId);
  }

  @Override
  public int hashCode() {
    return blipId.hashCode() ^ blipOp.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    if (!(obj instanceof WaveletBlipOperation)) {
      return false;
    }
    WaveletBlipOperation other = (WaveletBlipOperation) obj;
    return blipId.equals(other.blipId) && blipOp.equals(other.blipOp);
  }
}
