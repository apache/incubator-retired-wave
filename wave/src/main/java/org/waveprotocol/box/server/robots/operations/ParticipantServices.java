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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.collect.Lists;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.WaveletParticipantsChangedEvent;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.List;

/**
 * {@link OperationService} for operations that add or remove a participant.
 *
 * <p>
 * These operations are:
 * <li>{@link OperationType#WAVELET_ADD_PARTICIPANT_NEWSYNTAX}</li>
 * <li>{@link OperationType#WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX}</li>.
 *
 * @author anthony dot watkins at sesi dot com (Anthony Watkins)
 */
public class ParticipantServices implements OperationService {

  private static final Log LOG = Log.get(ParticipantServices.class);

  private ParticipantServices() {
  }

  /**
   * Adds or Removes a Participant on a Wavelet.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @throws InvalidRequestException if the operation fails to perform.
   */
  @Override
  public void execute(OperationRequest operation, OperationContext context,
      ParticipantId participant) throws InvalidRequestException {

    // Get the conversation wavelet. If participant performing operation is not
    // a member of wavelet, InvalidRequestException is thrown by this method.
    ObservableConversation conversation =
        context.openConversation(operation, participant).getRoot();

    // Get participant operation is being performed on.
    String paramParticipant =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.PARTICIPANT_ID);

    ParticipantId targetParticipant;
    try {
      targetParticipant = ParticipantId.of(paramParticipant);
    } catch (InvalidParticipantAddress e) {
      String message = "Target ParticipantId " + paramParticipant + " is not " + "valid";
      LOG.info(message);
      throw new InvalidRequestException(message);
    }

    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    // Create generic event (defined by operation type) that will be processed
    // by the context.
    Event event;

    // Set up participant containers.
    List<String> participantsAdded = Lists.newArrayList();
    List<String> participantsRemoved = Lists.newArrayList();

    OperationType type = OperationUtil.getOperationType(operation);
    switch (type) {
      case WAVELET_ADD_PARTICIPANT_NEWSYNTAX:
        // Make sure targetParticipant is not already member.
        if (conversation.getParticipantIds().contains(targetParticipant)) {
          String message = targetParticipant.getAddress() + " is already a " + "member of wavelet";
          LOG.info(message);
          throw new InvalidRequestException(message, operation);
        }

        // Add participant to conversation and send event.
        conversation.addParticipant(targetParticipant);
        participantsAdded.add(targetParticipant.getAddress());
        event =
            new WaveletParticipantsChangedEvent(null, null, participant.getAddress(),
                System.currentTimeMillis(), rootBlipId, participantsAdded, participantsRemoved);
        break;
      case WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX:
        // Make sure targetParticipant is already member.
        if (!conversation.getParticipantIds().contains(targetParticipant)) {
          // Not a member, throw invalid request.
          String message = targetParticipant.getAddress() + " is not a " + "member of wavelet";
          LOG.info(message);
          throw new InvalidRequestException(message, operation);
        }

        // Remove participant and send event.
        conversation.removeParticipant(targetParticipant);
        participantsRemoved.add(targetParticipant.getAddress());

        event =
            new WaveletParticipantsChangedEvent(null, null, participant.getAddress(),
                System.currentTimeMillis(), rootBlipId, participantsAdded, participantsRemoved);
        break;
      default:
        throw new UnsupportedOperationException(
            "This OperationService does not implement operation of type " + type.method());
    }

    // Process the participant event.
    context.processEvent(operation, event);
  }

  public static ParticipantServices create() {
    return new ParticipantServices();
  }
}
