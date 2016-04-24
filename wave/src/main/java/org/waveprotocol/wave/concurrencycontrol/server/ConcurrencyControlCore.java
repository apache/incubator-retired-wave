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

package org.waveprotocol.wave.concurrencycontrol.server;

import org.waveprotocol.wave.concurrencycontrol.common.DeltaPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;

/**
 * The core of the concurrency control that lives on the server. It manages all
 * of the transforms for a single wavelet that needs CC.
 *
 * @author zdwang@google.com (David Wang)
 */
public class ConcurrencyControlCore {
  /** This is a list of delta history which we can use for transformation */
  private final DeltaHistory deltaHistory;

  /**
   * This is the information needed to resend to the client.
   *
   * @author zdwang@google.com (David Wang)
   */
  public class ReOpenInfo {
    private final List<TransformedWaveletDelta> deltas;
    private final HashedVersion startSignature;

    ReOpenInfo(HashedVersion startSignature, List<TransformedWaveletDelta> toSend) {
      this.deltas = toSend;
      this.startSignature = startSignature;
    }

    /**
     * @return The starting signature for the client.
     */
    public HashedVersion getStartSignature() {
      return startSignature;
    }

    /**
     * @return List of delta we need to give to the client intially.
     */
    public List<TransformedWaveletDelta> getDeltas() {
      return deltas;
    }
  }

  /**
   * @param deltaHistory Cannot be null.
   */
  public ConcurrencyControlCore(DeltaHistory deltaHistory) {
    this.deltaHistory = deltaHistory;
  }

  /**
   * Transform the given client delta against the known delta history.
   *
   * @param delta The received delta
   * @return The transformed client operation and it starts off from the latest version.
   * @throws TransformException
   */
  public WaveletDelta onClientDelta(WaveletDelta delta) throws TransformException {
    if (delta.getTargetVersion().getVersion() > deltaHistory.getCurrentVersion()) {
      throw new TransformException("Client has a newer version than server knows. client: "
                                   + delta.getTargetVersion() + ", server: "
                                   + deltaHistory.getCurrentVersion());
    }
    WaveletDelta result = delta;
    while (result.getTargetVersion().getVersion() < deltaHistory.getCurrentVersion()) {
      TransformedWaveletDelta serverDelta =
          deltaHistory.getDeltaStartingAt(result.getTargetVersion().getVersion());
      if (serverDelta == null) {
        // Note that this will trigger if the available history changes out from
        // under us. This should not happen as the caller of this method should
        // control changes to the underlying set via locks, e.g. writeLock in
        // the WS's WaveletContext.
        throw new IllegalStateException("No delta at version: " + result.getTargetVersion());
      }
      DeltaPair pair = new DeltaPair(result, serverDelta).transform();
      result = new WaveletDelta(delta.getAuthor(), serverDelta.getResultingVersion(),
          pair.getClient());
    }
    return result;
  }

  /**
   * A client wants to reopen a wave. They'll send us a list of signature that they
   * know of. We'll return a list of Deltas from the last signature we know of.
   */
  public ReOpenInfo reopen(List<HashedVersion> clientKnownSignatures) {
    List<TransformedWaveletDelta> deltas = CollectionUtils.newArrayList();
    // Find the most recent delta.
    for (int i = clientKnownSignatures.size() - 1; i >= 0; i--) {
      if (deltaHistory.hasSignature(clientKnownSignatures.get(i))) {
        TransformedWaveletDelta old =
            deltaHistory.getDeltaStartingAt(clientKnownSignatures.get(i).getVersion());
        while (old != null) {
          deltas.add(old);
          old = deltaHistory.getDeltaStartingAt(old.getResultingVersion().getVersion());
        }
        return new ReOpenInfo(clientKnownSignatures.get(i), deltas);
      }
    }
    return null;
  }
}
