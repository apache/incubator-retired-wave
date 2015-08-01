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
 * Operation class for the add-participant operation.
 *
 */
public final class AddParticipant extends WaveletOperation {
  private static final int END_POSITION = -1;

  /** Participant to add. */
  private final ParticipantId participant;
  private final int position;

  /**
   * Creates an add-participant operation.
   *
   * @param context      context of this operation
   * @param participant  participant to add
   */
  public AddParticipant(WaveletOperationContext context, ParticipantId participant) {
    super(context);
    Preconditions.checkNotNull(participant, "Null participant ID");
    this.participant = participant;
    this.position = END_POSITION;
  }

  /**
   * Creates an add-participant operation which inserts the added participant at
   * a specified position in the participant list.
   * This constructor is package private and only used to create the reverse of
   * a remove-participant operation. See {@link RemoveParticipant}.
   *
   * @param context      context of this operation
   * @param participant  participant to add
   * @param position     position in participant list where to add the participant
   */
  AddParticipant(WaveletOperationContext context, ParticipantId participant, int position) {
    super(context);
    Preconditions.checkNotNull(participant, "Null participant ID");
    Preconditions.checkPositionIndex(position, Integer.MAX_VALUE);
    this.participant = participant;
    this.position = position;
  }

  /**
   * Gets the participant to add.
   *
   * @return the participant to add.
   */
  public ParticipantId getParticipantId() {
    return participant;
  }

  @Override
  public void doApply(WaveletData target) throws OperationException {
    if (!(position == END_POSITION
          ? target.addParticipant(participant)
          : target.addParticipant(participant, position))) {
      throw new OperationException("Attempt to add a duplicate participant " + participant);
    }
  }

  public void acceptVisitor(WaveletOperationVisitor visitor) {
    visitor.visitAddParticipant(this);
  }

  @Override
  public String toString() {
    return "add participant " + participant
        + (position != END_POSITION ? " at position " + position : "")
        + " " + suffixForToString();
  }

  @Override
  public List<? extends WaveletOperation> applyAndReturnReverse(WaveletData target)
      throws OperationException {
    WaveletOperationContext reverseContext = createReverseContext(target);
    doApply(target);
    update(target);
    return Collections.singletonList(new RemoveParticipant(reverseContext, participant));
  }

  @Override
  public int hashCode() {
    return participant.hashCode() + 31 * position;
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(user): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    if (!(obj instanceof AddParticipant)) {
      return false;
    }
    AddParticipant other = (AddParticipant) obj;
    return participant.equals(other.participant) && position == other.position;
  }

}
