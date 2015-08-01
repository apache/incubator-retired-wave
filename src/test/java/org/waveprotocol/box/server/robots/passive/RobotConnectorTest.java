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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.event.EventType;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.RobotConnection;
import com.google.wave.api.robot.RobotConnectionException;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit test for the {@link RobotConnector}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotConnectorTest extends TestCase {

  private static final String CAPABILITIES_HASH = "0x42ea590";
  private static final String CAPABILITIES_XML =
      "<w:robot xmlns:w=\"http://wave.google.com/extensions/robots/1.0\"> " + "<w:version>"
          + CAPABILITIES_HASH + "</w:version> " + "<w:protocolversion>0.22</w:protocolversion> "
          + "<w:capabilities> " + "<w:capability name=\"OPERATION_ERROR\"/> "
          + "<w:capability name=\"WAVELET_SELF_ADDED\"/> " + "</w:capabilities>" + "</w:robot> ";
  private static final ProtocolVersion PROTOCOL_VERSION = ProtocolVersion.DEFAULT;
  private static final String ROBOT_ACCOUNT_NAME = "test@example.com";
  private static final String TEST_URL = "www.example.com/robot";
  private static final String TEST_RPC_ENDPOINT = TEST_URL + Robot.RPC_URL;
  private static final String TEST_CAPABILITIES_ENDPOINT = TEST_URL + Robot.CAPABILITIES_URL;
  private static final RobotAccountData ROBOT_ACCOUNT =
      new RobotAccountDataImpl(ParticipantId.ofUnsafe(ROBOT_ACCOUNT_NAME), TEST_URL, "secret",
          new RobotCapabilities(
              Maps.<EventType, Capability> newHashMap(), "FakeHash", ProtocolVersion.DEFAULT),
          true);

  private static final EventMessageBundle BUNDLE =
      new EventMessageBundle(ROBOT_ACCOUNT_NAME, "www.example.com/rpc");
  private static final String SERIALIZED_BUNDLE = "BUNDLE";
  private static final String RETURNED_OPERATION = "OPERATION";

  private RobotConnection connection;
  private RobotSerializer serializer;
  private RobotConnector connector;
  private Robot robot;

  @Override
  protected void setUp() throws Exception {
    connection = mock(RobotConnection.class);
    serializer = mock(RobotSerializer.class);
    connector = new RobotConnector(connection, serializer);

    robot = mock(Robot.class);
    when(robot.getAccount()).thenReturn(ROBOT_ACCOUNT);
  }

  public void testSuccessfulSendMessageBundle() throws Exception {
    final List<OperationRequest> expectedOperations = Collections.unmodifiableList(
        Lists.newArrayList(new OperationRequest("wavelet.setTitle", "op1")));

    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_RPC_ENDPOINT, SERIALIZED_BUNDLE)).thenReturn(RETURNED_OPERATION);
    when(serializer.deserializeOperations(RETURNED_OPERATION)).thenReturn(expectedOperations);

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, robot, PROTOCOL_VERSION);
    assertEquals(expectedOperations, operations);
  }

  public void testConnectionFailsSafely() throws Exception {
    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_RPC_ENDPOINT, SERIALIZED_BUNDLE)).thenThrow(
        new RobotConnectionException("Connection Failed"));

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, robot, PROTOCOL_VERSION);
    assertTrue("Expected no operations to be returned", operations.isEmpty());
  }

  public void testDeserializationFailsSafely() throws Exception {
    when(serializer.serialize(BUNDLE, PROTOCOL_VERSION)).thenReturn(SERIALIZED_BUNDLE);
    when(connection.postJson(TEST_RPC_ENDPOINT, SERIALIZED_BUNDLE)).thenReturn(RETURNED_OPERATION);
    when(serializer.deserializeOperations(RETURNED_OPERATION)).thenThrow(
        new InvalidRequestException("Invalid Request"));

    List<OperationRequest> operations =
        connector.sendMessageBundle(BUNDLE, robot, PROTOCOL_VERSION);
    assertTrue("Expected no operations to be returned", operations.isEmpty());
  }

  public void testFetchCapabilities() throws Exception {
    when(connection.get(TEST_CAPABILITIES_ENDPOINT)).thenReturn(CAPABILITIES_XML);

    RobotAccountData accountData = connector.fetchCapabilities(ROBOT_ACCOUNT, "");

    RobotCapabilities capabilities = accountData.getCapabilities();
    assertEquals("Expected capabilities hash as specified in the xml", CAPABILITIES_HASH,
        capabilities.getCapabilitiesHash());
    assertEquals("Expected protocol version as specified in the xml", ProtocolVersion.V2_2,
        capabilities.getProtocolVersion());
    Map<EventType, Capability> capabilitiesMap = capabilities.getCapabilitiesMap();
    assertTrue("Expected capabilities as specified in the xml", capabilitiesMap.size() == 2);
    assertTrue("Expected capabilities as specified in the xml",
        capabilitiesMap.containsKey(EventType.WAVELET_SELF_ADDED));
    assertTrue("Expected capabilities as specified in the xml",
        capabilitiesMap.containsKey(EventType.OPERATION_ERROR));
    // Only one connection should be made
    verify(connection).get(TEST_CAPABILITIES_ENDPOINT);
  }
}
