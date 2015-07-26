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

package org.waveprotocol.box.server.robots.passive;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.EventType;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.RobotName;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link Robot}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotTest extends TestCase {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private static final WaveletName WAVELET_NAME = WaveletName.of(
      "example.com", "waveid", "example.com", "waveletid");
  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");
  private static final RobotName ROBOT_NAME = RobotName.fromAddress("robot@example.com");
  private static final ParticipantId ROBOT = ParticipantId.ofUnsafe(ROBOT_NAME.toEmailAddress());
  private static final RobotAccountData ACCOUNT =
      new RobotAccountDataImpl(ROBOT, "www.example.com", "secret", null, true);
  private static final RobotAccountData INITIALIZED_ACCOUNT =
      new RobotAccountDataImpl(ROBOT, "www.example.com", "secret", new RobotCapabilities(
          Maps.<EventType, Capability> newHashMap(), "fake", ProtocolVersion.DEFAULT), true);

  private RobotsGateway gateway;
  private RobotConnector connector;
  private EventDataConverterManager converterManager;
  private WaveletProvider waveletProvider;
  private EventGenerator eventGenerator;
  private RobotOperationApplicator operationApplicator;
  private Robot robot;

  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    gateway = mock(RobotsGateway.class);
    connector = mock(RobotConnector.class);
    converterManager = mock(EventDataConverterManager.class);
    waveletProvider = mock(WaveletProvider.class);
    eventGenerator = mock(EventGenerator.class);
    operationApplicator = mock(RobotOperationApplicator.class);

    robot =
        new Robot(ROBOT_NAME, ACCOUNT, gateway, connector, converterManager, waveletProvider,
            eventGenerator, operationApplicator);
    // Set the initialized account when updateRobotAccount is called.
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        robot.setAccount(INITIALIZED_ACCOUNT);
        return null;
      }
    }).when(gateway).updateRobotAccount(robot);

    // Generate no events on default
    EventMessageBundle emptyMessageBundle = new EventMessageBundle(ROBOT_NAME.toEmailAddress(), "");
    when(eventGenerator.generateEvents(
        any(WaveletAndDeltas.class), anyMap(), any(EventDataConverter.class))).thenReturn(
        emptyMessageBundle);
  }

  public void testDequeueWavelet() throws Exception {
    assertNull("No wavelet to dequeue should return null", robot.dequeueWavelet());

    enqueueEmptyWavelet();
    assertNotNull("Wavelet should have been enqueued", robot.dequeueWavelet());
  }

  public void testDequeueWaveletReturnsNullIfEmpty() throws Exception {
    assertNull("On empty queue should return null", robot.dequeueWavelet());
  }

  public void testUpdateWaveletWithGap() throws Exception {
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    WaveletData waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX,
        hashedVersionZero, 0L);
    robot.waveletUpdate(waveletData, DeltaSequence.empty());

    // We are making an delta which applies to version 1, however the robot only
    // knows about version 0.
    ParticipantId bob = ParticipantId.of("bob@exmaple.com");
    HashedVersion v2 = HashedVersion.unsigned(2);
    WaveletOperation addBob = new AddParticipant(new WaveletOperationContext(ALEX, 0L, 1, v2), bob);
    addBob.apply(waveletData);
    waveletData.setHashedVersion(v2);
    waveletData.setVersion(2);

    TransformedWaveletDelta delta = new TransformedWaveletDelta(ALEX, v2,
        0L, Collections.singletonList(addBob));

    // Send the delta for version 1 to the robot, it should now enqueue a new
    // wavelet since it is missing deltas.
    robot.waveletUpdate(waveletData, DeltaSequence.of(delta));

    WaveletAndDeltas firstWavelet = robot.dequeueWavelet();
    assertNotNull("Expected a wavelet to be dequeued", firstWavelet);
    assertEquals("The wavelet with version zero should be first", hashedVersionZero,
        firstWavelet.getVersionAfterDeltas());
    WaveletAndDeltas secondWavelet = robot.dequeueWavelet();
    assertNotNull("Expected a wavelet to be dequeued", secondWavelet);
    assertEquals("The wavelet with version two should be second", v2,
        secondWavelet.getVersionAfterDeltas());
    assertNull("Only expected two wavelets to be dequeued", robot.dequeueWavelet());
  }

  public void testRunRequeuesRobotIfDoneWork() throws Exception {
    enqueueEmptyWavelet();
    robot.run();
    verify(gateway).doneRunning(robot);
    verify(gateway).ensureScheduled(robot);
  }

  public void testRunNotRequeingIfNoWork() {
    robot.run();
    verify(gateway).doneRunning(robot);
    verify(gateway, never()).ensureScheduled(robot);
  }

  public void testProcessUpdatesAccountIfNoCapabilities() throws Exception {
    // Enqueue a wavelet so that the robot actually runs.
    enqueueEmptyWavelet();
    robot.run();
    assertEquals("The robot should be initialized", INITIALIZED_ACCOUNT, robot.getAccount());
  }

  public void testProcessSendsNoBundleWhenNoEvents() throws Exception {
    enqueueEmptyWavelet();
    robot.run();
    // Verify that the robot was not called or any operations where processed
    verify(connector, never()).sendMessageBundle(
        any(EventMessageBundle.class), eq(robot), any(ProtocolVersion.class));
    verify(operationApplicator, never()).applyOperations(anyListOf(OperationRequest.class),
        any(ReadableWaveletData.class), any(HashedVersion.class), eq(ACCOUNT));
  }

  @SuppressWarnings("unchecked")
  public void testProcessSendsBundleAndCallsOperationsApplicator() throws Exception {
    EventMessageBundle messages = new EventMessageBundle(ROBOT_NAME.toEmailAddress(), "");
    messages.addEvent(new DocumentChangedEvent(null, null, ALEX.getAddress(), 0L, "b+1234"));
    when(eventGenerator.generateEvents(
        any(WaveletAndDeltas.class), anyMap(), any(EventDataConverter.class))).thenReturn(messages);

    OperationRequest op = new OperationRequest("wavelet.fetch", "op1");
    List<OperationRequest> ops = Collections.singletonList(op);
    when(connector.sendMessageBundle(
        any(EventMessageBundle.class), eq(robot), any(ProtocolVersion.class))).thenReturn(ops);

    enqueueEmptyWavelet();
    robot.run();

    verify(connector).sendMessageBundle(
        any(EventMessageBundle.class), eq(robot), any(ProtocolVersion.class));
    verify(operationApplicator).applyOperations(
        eq(ops), any(ReadableWaveletData.class), any(HashedVersion.class), eq(INITIALIZED_ACCOUNT));
  }

  /**
   * Enqueues an empty wavelet into the {@link Robot}.
   */
  private void enqueueEmptyWavelet() throws Exception {
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(WAVELET_NAME);
    WaveletData waveletData = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, ALEX,
        hashedVersionZero, 0L);
    robot.waveletUpdate(waveletData, DeltaSequence.empty());
  }
}
