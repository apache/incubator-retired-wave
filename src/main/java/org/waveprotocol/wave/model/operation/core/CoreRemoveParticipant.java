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

package org.waveprotocol.wave.model.operation.core;

import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

/**
 * Operation class for the remove-participant operation.
 */
public final class CoreRemoveParticipant extends CoreWaveletOperation {
  /** Participant to remove. */
  private final ParticipantId participant;

  /**
   * Creates an remove-participant operation.
   *
   * @param participant  participant to remove
   */
  public CoreRemoveParticipant(ParticipantId participant) {
    if (participant == null) {
      throw new NullPointerException("Null participant ID");
    }
    this.participant = participant;
  }

  /**
   * Gets the participant to remove.
   *
   * @return the participant to remove.
   */
  public ParticipantId getParticipantId() {
    return participant;
  }

  /**
   * Removes a participant from the given wavelet.
   */
  @Override
  public void doApply(CoreWaveletData target) throws OperationException {
    if (!target.removeParticipant(participant)) {
      throw new OperationException("Attempt to delete non-existent participant.");
    }
  }

  @Override
  public CoreWaveletOperation getInverse() {
    return new CoreAddParticipant(participant);
  }


  @Override
  public String toString() {
    return "RemoveParticipant(" + participant + ")";
  }

  @Override
  public int hashCode() {
    return participant.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CoreRemoveParticipant)) {
      return false;
    }
    CoreRemoveParticipant other = (CoreRemoveParticipant) obj;
    return participant.equals(other.participant);
  }
}
