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

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;
import java.util.List;

/**
 * This is pretty much like a no-op except it updates the version information.
 * It also contains a doc id when it wants to update the meta data of a
 * document.
 *
 * This operation has simple identity transformation like no-op.
 *
 * The constructors are purposely package private.
 *
 * @author zdwang@google.com (David Wang)
 */
final public class VersionUpdateOp extends WaveletOperation {

  /**
   * The document that should also have it's version information updated.
   * This field is optional.
   */
  private final String docId;

  /**
   * Decides whether to use the blipXXX fields to update the blips. This is used for
   * reversing this op.
   */
  private final boolean useFixedDocInfo;

  /**
   * If there is a document to update, set the last modified version of the
   * document to this value. This is needed for reverse op to work. Only
   * meaningful when docId is non-null.
   */
  private final long docVersion;

  /**
   * Constructs a VersionUpdateOp that does not update any blip
   *
   * @param versionIncrement the version increment when the operation is applied
   */
  VersionUpdateOp(ParticipantId creator, long versionIncrement, HashedVersion hashedVersion) {
    this(creator, versionIncrement, hashedVersion, null);
  }

  /**
   * Constructs a VersionUpdateOp that also updates the given blip
   *
   * @param increment the version increment when the operation is applied
   * @param docId the doc also to update the version; can be null to mean no
   *        documents to update
   */
  VersionUpdateOp(ParticipantId creator, long increment, HashedVersion hashedVersion,
      String docId) {
    this(creator, increment, hashedVersion, docId, -1L, false);
  }

  /**
   * Constructs a VersionUpdateOp that also updates the given blip
   *
   * @param creator           creator to add or remove as a contributor
   * @param versionIncrement  version increment when the operation is applied
   * @param hashedVersion     distinct version after the operation is applied
   * @param docId             optional document whose version is to be updated
   * @param useFixedBlipInfo  whether to use the remaining two fields
   * @param docVersion        if {@code useFixedBlipInfo}, the last modified
   *                          version to apply to blip being updated
   */
  VersionUpdateOp(ParticipantId creator, long versionIncrement, HashedVersion hashedVersion,
      String docId, long docVersion, boolean useFixedBlipInfo) {
    super(new WaveletOperationContext(creator, Constants.NO_TIMESTAMP, versionIncrement,
        hashedVersion));
    Preconditions.checkNotNull(creator, "Null participant ID");
    this.docId = docId;
    this.docVersion = docVersion;
    this.useFixedDocInfo = useFixedBlipInfo;
  }

  /**
   * Updates the blips metadata/version.
   *
   * Wavelet version and timestamp are expected to be updated by the universal
   * application logic in {@link WaveletBlipOperation#apply(WaveletData)}
   */
  @Override
  protected void doApply(WaveletData wave) {
    doInternalApply(wave);
  }

  private VersionUpdateOp doInternalApply(WaveletData wavelet) {
    HashedVersion oldHashedVersion = wavelet.getHashedVersion();
    if (docId != null) {
      // Update blip version.
      BlipData blip = wavelet.getDocument(docId);
      long newWaveletVersion = wavelet.getVersion() + context.getVersionIncrement();
      long newDocVersion = useFixedDocInfo ? docVersion : newWaveletVersion;
      long oldDocVersion = blip.setLastModifiedVersion(newDocVersion);

      return new VersionUpdateOp(context.getCreator(), -context.getVersionIncrement(),
          oldHashedVersion, docId, oldDocVersion, true);
    } else {
      return new VersionUpdateOp(context.getCreator(), -context.getVersionIncrement(),
          oldHashedVersion);
    }
  }

  @Override
  public void acceptVisitor(WaveletOperationVisitor visitor) {
    visitor.visitVersionUpdateOp(this);
  }

  @Override
  public String toString() {
    return "version update op, blip id " + docId + " blipVersion " + docVersion;
  }

  @Override
  public List<? extends WaveletOperation> applyAndReturnReverse(WaveletData target) {
    List<? extends WaveletOperation> ret = Collections.singletonList(doInternalApply(target));
    update(target);
    return ret;
  }

  @Override
  public int hashCode() {
    return ((docId == null) ? 0 : docId.hashCode())
        ^ (useFixedDocInfo ? 0 : 1)
        ^ (int) (docVersion ^ (docVersion >>> 32));
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    if (!(obj instanceof VersionUpdateOp)) {
      return false;
    }
    VersionUpdateOp other = (VersionUpdateOp) obj;
    return docId.equals(other.docId)
        && useFixedDocInfo == other.useFixedDocInfo
        && docVersion == other.docVersion;
  }
}
