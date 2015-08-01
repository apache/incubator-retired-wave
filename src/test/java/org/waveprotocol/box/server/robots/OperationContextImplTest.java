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

package org.waveprotocol.box.server.robots;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.OperationErrorEvent;
import com.google.wave.api.event.WaveletBlipCreatedEvent;

import junit.framework.TestCase;

import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Map;

/**
 * Unit tests for {@link OperationContextImpl}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationContextImplTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);
  private static final WaveId WAVE_ID = WaveId.of("example.com", "waveid");
  private static final WaveletId WAVELET_ID = WaveletId.of("example.com", "conv+root");
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final String ERROR_MESSAGE = "ERROR_MESSAGE";
  private static final String USERNAME = "test@example.com";
  private static final ParticipantId PARTICIPANT = ParticipantId.ofUnsafe(USERNAME);
  private static final String OPERATION_ID = "op1";

  private EventDataConverter converter;
  private WaveletProvider waveletProvider;
  private OperationRequest request;
  private OperationContextImpl operationContext;
  private RobotWaveletData wavelet;
  private OperationContextImpl boundOperationContext;
  private ConversationUtil conversationUtil;
  private ObservableWaveletData waveletData;

  @Override
  protected void setUp() throws Exception {
    converter = mock(EventDataConverter.class);
    waveletProvider = mock(WaveletProvider.class);
    conversationUtil = mock(ConversationUtil.class);

    request = new OperationRequest("wave.setTitle", OPERATION_ID);
    operationContext = new OperationContextImpl(waveletProvider, converter, conversationUtil);

    waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT,
        HASH_FACTORY.createVersionZero(WAVELET_NAME), 0L);
    waveletData.addParticipant(PARTICIPANT);
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    wavelet = new RobotWaveletData(waveletData, hashedVersionZero);

    when(waveletProvider.checkAccessPermission(WAVELET_NAME, PARTICIPANT)).thenReturn(true);
    CommittedWaveletSnapshot snapshotAndVersion = new CommittedWaveletSnapshot(
        waveletData, hashedVersionZero);
    when(waveletProvider.getSnapshot(WAVELET_NAME)).thenReturn(snapshotAndVersion);

    boundOperationContext =
        new OperationContextImpl(waveletProvider, converter, conversationUtil, wavelet);
  }

  public void testConstructResponse() {
    Map<ParamsProperty, Object> data = Maps.newHashMap();
    data.put(ParamsProperty.PARTICIPANT_ID, USERNAME);

    operationContext.constructResponse(request, data);
    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertFalse("Expected non-error response", response.isError());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
    assertEquals("Expected payload not to change", data, response.getData());
  }

  public void testConstructErrorResponse() {
    operationContext.constructErrorResponse(request, ERROR_MESSAGE);
    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertTrue("Expected error response", response.isError());
    assertEquals("Expected provided error message", ERROR_MESSAGE, response.getErrorMessage());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testProcessEvent() throws Exception {
    // A randomly selected non-error event
    Event event = new WaveletBlipCreatedEvent(null, null, USERNAME, 0L, "root", "newBlip");

    operationContext.processEvent(request, event);

    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertFalse("Expected non-error response", response.isError());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testProcessErrorEvent() throws Exception {
    // A randomly selected non-error event
    Event event = new OperationErrorEvent(null, null, USERNAME, 0L, OPERATION_ID, ERROR_MESSAGE);
    operationContext.processEvent(request, event);

    JsonRpcResponse response = operationContext.getResponse(request.getId());
    assertTrue("Expected error response", response.isError());
    assertEquals("Expected provided error message", ERROR_MESSAGE, response.getErrorMessage());
    assertEquals("Expected operation id not to change", OPERATION_ID, response.getId());
  }

  public void testContextIsBound() throws Exception {
    assertTrue("Bound contexts should return true", boundOperationContext.isBound());
    Map<WaveletName, RobotWaveletData> openWavelets = boundOperationContext.getOpenWavelets();
    assertEquals("Bound wavelet should be open", openWavelets.get(WAVELET_NAME), wavelet);

    assertFalse("Unbound contexts should return false", operationContext.isBound());
  }

  public void testPutNonTemporaryWavelet() throws Exception {
    OpBasedWavelet opBasedWavelet = wavelet.getOpBasedWavelet(PARTICIPANT);
    operationContext.putWavelet(WAVE_ID, WAVELET_ID, wavelet);
    assertEquals(opBasedWavelet, operationContext.openWavelet(WAVE_ID, WAVELET_ID, PARTICIPANT));
  }

  public void testPutTemporaryWavelet() throws Exception {
    OpBasedWavelet opBasedWavelet = wavelet.getOpBasedWavelet(PARTICIPANT);
    WaveId tempWaveId = WaveId.of("example.com", OperationContextImpl.TEMP_ID_MARKER + "random");
    WaveletId tempWaveletId = WaveletId.of("example.com", "conv+root");
    operationContext.putWavelet(tempWaveId, tempWaveletId, wavelet);
    assertEquals(
        opBasedWavelet, operationContext.openWavelet(tempWaveId, tempWaveletId, PARTICIPANT));
    assertEquals(opBasedWavelet, operationContext.openWavelet(WAVE_ID, WAVELET_ID, PARTICIPANT));
  }

  /**
   * Tests opening a wavelet that has to be retrieved using the
   * {@link WaveletProvider}.
   */
  public void testOpenWaveletFromWaveletProvider() throws Exception {
    OpBasedWavelet opBasedWavelet = wavelet.getOpBasedWavelet(PARTICIPANT);
    assertEquals(opBasedWavelet, operationContext.openWavelet(WAVE_ID, WAVELET_ID, PARTICIPANT));
  }

  public void testOpenNonExistingWaveletThrowsInvalidRequestException() throws Exception {
    try {
      operationContext.openWavelet(WAVE_ID, WaveletId.of("example.com", "unreal"), PARTICIPANT);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testOpenExistingWaveletForNonParticipantThrowsInvalidRequestException()
      throws Exception {
    ParticipantId nonExistingParticipant = ParticipantId.ofUnsafe("nonexisting@example.com");
    assertFalse("This participant should not exist",
        waveletData.getParticipants().contains(nonExistingParticipant));
    try {
      operationContext.openWavelet(WAVE_ID, WAVELET_ID, nonExistingParticipant);
      fail("Expected InvalidRequestException for a non-existing participant");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testPutNonTemporaryBlip() throws Exception {
    // Non temporary blip is ignored
    Conversation conversation = mock(Conversation.class);
    ConversationBlip blip = mock(ConversationBlip.class);
    String blipId = "b+1234";
    when(blip.getId()).thenReturn(blipId);
    when(conversation.getBlip(blipId)).thenReturn(blip);

    operationContext.putBlip(blip.getId(), blip);
    assertEquals(operationContext.getBlip(conversation, blipId), blip);
  }

  public void testPutTemporaryBlip() throws Exception {
    Conversation conversation = mock(Conversation.class);
    ConversationBlip blip = mock(ConversationBlip.class);
    String tempBlipId = OperationContextImpl.TEMP_ID_MARKER + "random";
    String blipId = "b+1234";
    when(blip.getId()).thenReturn(blipId);
    when(conversation.getBlip(blipId)).thenReturn(blip);

    operationContext.putBlip(tempBlipId, blip);
    assertEquals("Expected blip for the given tempId",
        operationContext.getBlip(conversation, tempBlipId), blip);
    assertEquals("Expected blip when its non temporary id is given",
        operationContext.getBlip(conversation, blipId), blip);
  }
}
