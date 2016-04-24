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

import org.waveprotocol.wave.model.wave.data.WaveletData;

import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;

/**
 * Operation class for the remove-participant operation.
 *
 */
public final class RemoveParticipant extends WaveletOperation {
  /** Participant to remove. */
  private final ParticipantId participant;

  /**
   * Creates an remove-participant operation.
   *
   * @param context      context of this operation
   * @param participant  participant to remove
   */
  public RemoveParticipant(WaveletOperationContext context, ParticipantId participant) {
    super(context);
    Preconditions.checkNotNull(participant, "Null participant ID");
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
  public void doApply(WaveletData target) throws OperationException {
    if (!target.removeParticipant(participant)) {
      throw new OperationException("Attempt to remove non-existent participant " + participant);
    }
  }

  @Override
  public void acceptVisitor(WaveletOperationVisitor visitor) {
    visitor.visitRemoveParticipant(this);
  }

  @Override
  public String toString() {
    return "remove participant " + participant + " " + suffixForToString();
  }

  @Override
  public List<? extends WaveletOperation> applyAndReturnReverse(WaveletData target)
      throws OperationException {
    WaveletOperationContext reverseContext = createReverseContext(target);
    int position = participantPosition(target);
    doApply(target);
    update(target);
    return Collections.singletonList(new AddParticipant(reverseContext, participant, position));
  }

  @Override
  public int hashCode() {
    return participant.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    if (!(obj instanceof RemoveParticipant)) {
      return false;
    }
    RemoveParticipant other = (RemoveParticipant) obj;
    return participant.equals(other.participant);
  }

  /**
   * @return The position of the removed participant in the wavelet participant
   *   list.
   * @throws OperationException if the removed participant is not in the
   *   participant list.
   */
  private int participantPosition(WaveletData target) throws OperationException {
    int position = 0;
    for (ParticipantId next : target.getParticipants()) {
      if (participant.equals(next)) {
        return position;
      }
      position++;
    }
    throw new OperationException("Attempt to remove non-existent participant " + participant);
  }
}
