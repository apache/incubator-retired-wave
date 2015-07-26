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

package org.waveprotocol.box.server.robots.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.data.converter.EventDataConverter;

import org.mockito.internal.stubbing.answers.ThrowsException;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.OperationResults;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.RobotsTestBase;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Collections;

/**
 * Unit tests for {@link OperationUtil}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationUtilTest extends RobotsTestBase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);
  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");

  private OperationRequest operation;
  private OperationServiceRegistry operationRegistry;
  private OperationContextImpl context;
  private WaveletProvider waveletProvider;
  private EventDataConverter converter;

  @Override
  protected void setUp() throws Exception {
    waveletProvider = mock(WaveletProvider.class);
    operationRegistry = mock(OperationServiceRegistry.class);
    ConversationUtil conversationUtil = mock(ConversationUtil.class);
    converter = mock(EventDataConverter.class);

    operation = new OperationRequest("wavelet.fetch", "op1", s(WAVE_ID), s(WAVELET_ID));
    context = new OperationContextImpl(waveletProvider, converter, conversationUtil);
  }

  public void testGetRequiredParameter() throws Exception {
    String waveId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVE_ID);
    assertEquals(s(WAVE_ID), waveId);
  }

  public void testGetRequiredParameterThrowsInvalidRequestException() throws Exception {
    try {
      OperationUtil.getRequiredParameter(operation, ParamsProperty.ANNOTATION);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testGetOptionalParameter() throws Exception {
    String waveId = OperationUtil.getOptionalParameter(operation, ParamsProperty.WAVE_ID);
    assertEquals(s(WAVE_ID), waveId);

    assertNull("Non existing properties should return null when optional",
        OperationUtil.getOptionalParameter(operation, ParamsProperty.ANNOTATION));

    String defaultValue = "b+1234";
    String blipId =
        OperationUtil.getOptionalParameter(operation, ParamsProperty.BLIP_ID, defaultValue);
    assertSame("Default value should be returned when object does not exist", defaultValue, blipId);
  }

  public void testGetProtocolVersion() throws Exception {
    ProtocolVersion protocolVersion =
        OperationUtil.getProtocolVersion(Collections.<OperationRequest> emptyList());
    assertEquals(
        "Empty list should return default version", ProtocolVersion.DEFAULT, protocolVersion);

    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(operation));
    assertEquals("Non notify op as first op should return default", ProtocolVersion.DEFAULT,
        protocolVersion);

    OperationRequest notifyOp = new OperationRequest(OperationType.ROBOT_NOTIFY.method(), "op1");
    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(notifyOp));
    assertEquals("Notify op as first op without version parameter should return default",
        ProtocolVersion.DEFAULT, protocolVersion);

    Parameter versionParameter =
        Parameter.of(ParamsProperty.PROTOCOL_VERSION, ProtocolVersion.V2_1.getVersionString());
    notifyOp = new OperationRequest(OperationType.ROBOT_NOTIFY.method(), "op1", versionParameter);
    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(notifyOp));
    assertEquals(
        "Notify op as first op should return its version", ProtocolVersion.V2_1, protocolVersion);
  }

  public void testExecuteOperationCallsExecute() throws Exception {
    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);

    OperationService service = mock(OperationService.class);
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    OperationUtil.executeOperation(operation, operationRegistry, context, ALEX);

    verify(service).execute(operation, context, ALEX);
  }

  public void testExecuteOperationsSetsErrorOnInvalidRequestException() throws Exception {
    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);

    OperationService service =
        mock(OperationService.class, new ThrowsException(new InvalidRequestException("")));
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    OperationUtil.executeOperation(operation, operationRegistry, context, ALEX);

    assertTrue("Expected one response", context.getResponses().size() == 1);
    assertTrue("Expected an error response", context.getResponse(operationId).isError());
  }

  public void testSubmitDeltas() {
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    ObservableWaveletData waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX,
        hashedVersionZero, 0L);
    DocInitialization content = new DocInitializationBuilder().build();
    waveletData.createDocument("b+example", BOB, Collections.singletonList(BOB), content, 0L, 0);

    RobotWaveletData wavelet = new RobotWaveletData(waveletData, hashedVersionZero);

    // Perform an operation that will be put into a delta
    wavelet.getOpBasedWavelet(BOB).addParticipant(ALEX);

    OperationResults results = mock(OperationResults.class);
    when(results.getOpenWavelets()).thenReturn(Collections.singletonMap(WAVELET_NAME, wavelet));

    SubmitRequestListener requestListener = mock(SubmitRequestListener.class);
    OperationUtil.submitDeltas(results, waveletProvider, requestListener);

    verify(waveletProvider).submitRequest(
        eq(WAVELET_NAME), any(ProtocolWaveletDelta.class), eq(requestListener));
  }
  
  public void testToProxyParticipant() throws Exception {
    ParticipantId participant = ParticipantId.of("foo@example.com");
    ParticipantId proxyParticipant = ParticipantId.of("foo+proxyFor@example.com");
    assertEquals(proxyParticipant, OperationUtil.toProxyParticipant(participant, "proxyFor"));
    // If participant is already a proxy - return without a change.
    assertEquals(proxyParticipant, OperationUtil.toProxyParticipant(proxyParticipant, "proxyFor"));
  }
  
  public void testComputeParticipantWithInvalidProxyFor() throws Exception {
    String invalidProxyFor = "~@#+^+";
    String participantAddress = "foo@bar.com";
    OperationRequest operation = mock(OperationRequest.class);
    when(operation.getParameter(ParamsProperty.PROXYING_FOR)).thenReturn(invalidProxyFor);
    try {
      OperationUtil.computeParticipant(operation, ParticipantId.of(participantAddress));
      fail("InvalidRequestException should be thrown.");
    } catch (InvalidRequestException e) {
      // Pass.
    }
    verify(operation).getParameter(ParamsProperty.PROXYING_FOR);
  }
  
  public void testComputeParticipantWithValidProxyFor() throws Exception {
    String validProxyFor = "foo";
    String participantAddress = "foo@bar.com";
    OperationRequest operation = mock(OperationRequest.class);
    when(operation.getParameter(ParamsProperty.PROXYING_FOR)).thenReturn(validProxyFor);
    try {
      OperationUtil.computeParticipant(operation, ParticipantId.of(participantAddress));
    } catch (InvalidRequestException e) {
      fail("Exception is thrown for a valid address.");
    }
    verify(operation).getParameter(ParamsProperty.PROXYING_FOR);
  }
}
