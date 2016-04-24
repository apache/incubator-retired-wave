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

import com.google.wave.api.BlipData;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;

import org.waveprotocol.box.server.robots.RobotsTestBase;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

/**
 * Unit tests for {@link ParticipantServices}.
 *
 * @author anthony dot watkins at sesi dot com (Anthony Watkins)
 */
public class ParticipantServicesTest extends RobotsTestBase {

  private static final String TEMP_BLIP_ID = OperationContext.TEMP_ID_MARKER + "blip1";
  private static final String NEW_BLIP_CONTENT = "Hello World";
  private static final ParticipantId ROBOT = ParticipantId.ofUnsafe("robot@example.com");
  private static final String MALFORMED_ADDRESS = "malformed!@@#$%(*)^_^@@.com";

  private ParticipantServices service;
  private OperationServiceHelper helper;
  private BlipData blipData;

  @Override
  protected void setUp() {
    service = ParticipantServices.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ROBOT);
    // BlipData constructor is broken, it doesn't set the blipId passed in the
    // constructor
    blipData = new BlipData(s(WAVE_ID), s(WAVELET_ID), TEMP_BLIP_ID, NEW_BLIP_CONTENT);
    blipData.setBlipId(TEMP_BLIP_ID);
  }

  public void testAddParticipant() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ROBOT).getRoot();
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    // Confirm alex is not on wave prior to operation.
    Set<ParticipantId> participants = conversation.getParticipantIds();
    assertFalse("Alex should not be a participant on wavelet prior to operation to add him.",
        participants.contains(ALEX));

    OperationRequest operation =
        operationRequest(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX, rootBlipId,
            Parameter.of(ParamsProperty.PARTICIPANT_ID,ALEX.getAddress()));

    service.execute(operation, context, ROBOT);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse("Add Participant generated error in service execution.", response.isError());

    // Verify Alex is now a participant on the wave.
    participants = conversation.getParticipantIds();
    assertTrue("Alex should now be a participant on the wavelet.", participants.contains(ALEX));
  }

  public void testAddThrowsOnDuplicateParticipant() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ROBOT).getRoot();
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    // Confirm robot is on wave prior to operation to re-add it.
    Set<ParticipantId> participants = conversation.getParticipantIds();
    assertTrue("Robot should be a participant on wavelet prior to test operation to add it.",
        participants.contains(ROBOT));

    OperationRequest operation =
        operationRequest(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX, rootBlipId,
            Parameter.of(ParamsProperty.PARTICIPANT_ID, ROBOT.getAddress()));

    try {
      service.execute(operation, context, ROBOT);

      fail("Duplicate add of participant should have generated error in service execution.");
    } catch(InvalidRequestException e) {
      // Good.
    }
  }

  public void testRemoveParticipant() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ROBOT).getRoot();
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    // Confirm alex is not on wave prior to operation.
    Set<ParticipantId> participants = conversation.getParticipantIds();
    assertFalse("Alex should not be a participant on wavelet prior to operation to add him.",
        participants.contains(ALEX));

    OperationRequest operation =
        new OperationRequest(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX.method(), OPERATION_ID,
            s(WAVE_ID), s(WAVELET_ID), rootBlipId, Parameter.of(ParamsProperty.PARTICIPANT_ID,
                ALEX.getAddress()));

    service.execute(operation, context, ROBOT);

    // Verify Alex is now a participant on the wave.
    participants = conversation.getParticipantIds();
    assertTrue("Alex should now be a participant on the wavelet.", participants.contains(ALEX));

    // Attempt to remove Alex.
    OperationRequest operation2 =
        operationRequest(OperationType.WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX, OPERATION2_ID,
            rootBlipId, Parameter.of(ParamsProperty.PARTICIPANT_ID, ALEX.getAddress()));

    service.execute(operation2, context, ROBOT);

    // Verify Alex is no longer a participant on the wave.
    participants = conversation.getParticipantIds();
    assertFalse("Alex should no longer be a participant on the wavelet.",
        participants.contains(ALEX));
  }

  public void testRemoveThrowsOnNonWaveletParticipant() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ROBOT).getRoot();
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    // Confirm alex is not on wave prior to operation.
    Set<ParticipantId> participants = conversation.getParticipantIds();
    assertFalse("Alex should not be a participant on wavelet prior to operation to add him.",
        participants.contains(ALEX));

    OperationRequest operation =
        operationRequest(OperationType.WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX, rootBlipId,
            Parameter.of(ParamsProperty.PARTICIPANT_ID, ALEX.getAddress()));

    try {
      service.execute(operation, context, ROBOT);

      fail("Removal of non-participant should have generated error in service execution.");
    } catch(InvalidRequestException e) {
      // Good.
    }
  }

  public void testInvalidParticipantAddress() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ROBOT).getRoot();
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);

    OperationRequest operation =
        operationRequest(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX, rootBlipId,
            Parameter.of(ParamsProperty.PARTICIPANT_ID, MALFORMED_ADDRESS));

    try {
      service.execute(operation, context, ROBOT);

      fail("Addition of invalid particpant address should have generated error in service " +
          "execution.");
    } catch(InvalidRequestException e) {
      // Good.
    }
  }
}
