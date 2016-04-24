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

import org.waveprotocol.wave.concurrencycontrol.server.DeltaHistory;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.HashMap;

/**
 * Simple dumb implementation of delta history using hashtables.
 *
 * @author zdwang@google.com (David Wang)
 */
public class SimpleDeltaHistory implements DeltaHistory {

  private HashedVersion currentSignature;

  /**
   * maps version -> delta
   */
  private final HashMap<Long, TransformedWaveletDelta> history = CollectionUtils.newHashMap();

  /**
   * All the signatures we have. Knowing a signature does not mean we have a
   * delta at that signature.
   *
   * e.g. we don't have a delta for the most recent signature
   */
  private final HashMap<Long, HashedVersion> signatures =
      new HashMap<Long, HashedVersion>();

  /**
   * We always start at version 0
   */
  public SimpleDeltaHistory(HashedVersion v0) {
    Preconditions.checkArgument(v0.getVersion() == 0, "Delta history must start at zero");
    currentSignature = v0;
    // We always have a signature at version 0, but we have no operation for it.
    signatures.put(0L, currentSignature);
  }

  @Override
  public TransformedWaveletDelta getDeltaStartingAt(long version) {
    return history.get(version);
  }

  @Override
  public boolean hasSignature(HashedVersion signatureInformation) {
    HashedVersion found = signatures.get(signatureInformation.getVersion());
    if (found == null) {
      return false;
    }
    return found.equals(signatureInformation);
  }

  /**
   * @return last known signature.
   */
  public HashedVersion getCurrentSignature() {
    return currentSignature;
  }

  @Override
  public long getCurrentVersion() {
    return currentSignature.getVersion();
  }

  /**
   * Make the current signature the given one
   * @param currentSignature
   */
  public void setCurrentSignature(HashedVersion currentSignature) {
    assert currentSignature == signatures.get(currentSignature.getVersion());
    this.currentSignature = currentSignature;
  }

  /**
   * Add a delta to the hash tables.
   */
  public void addDelta(TransformedWaveletDelta delta) {
    history.put(delta.getAppliedAtVersion(), delta);
    signatures.put(delta.getResultingVersion().getVersion(), delta.getResultingVersion());
  }

  /**
   * Removes a delta from the hash tables.
   */
  public void removeDelta(TransformedWaveletDelta delta) {
    history.remove(delta.getAppliedAtVersion());
    signatures.remove(delta.getResultingVersion().getVersion());
  }

  /**
   * Gets the signature at the given version.
   */
  public HashedVersion getSignatureAt(long version) {
    return signatures.get(version);
  }


  /**
   * Truncate to the given version.
   */
  public void truncateAt(long version) {
    HashedVersion signature = getSignatureAt(version);
    setCurrentSignature(signature);

    TransformedWaveletDelta currentDelta = getDeltaStartingAt(version);
    while(currentDelta != null) {
      removeDelta(currentDelta);
      currentDelta = getDeltaStartingAt(currentDelta.getResultingVersion().getVersion());
    }
  }
}
