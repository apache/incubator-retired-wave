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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.CapabilityFetchException;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.box.server.robots.passive.RobotConnector;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Map;

public class NotifyOperationServiceTest extends TestCase {

  private static final Map<EventType, Capability> EMPTY_CAPABILITIES = 
    Collections.<EventType, Capability> emptyMap();
  private static final String OP_ID = "op1";
  private static final ParticipantId ROBOT = ParticipantId.ofUnsafe("robot@example.com");
  private static final String OLD_HASH = "oldHash";
  private static final RobotCapabilities OLD_CAPABILITIES = new RobotCapabilities(
      EMPTY_CAPABILITIES, OLD_HASH, ProtocolVersion.V2_2);
  private static final RobotAccountData ROBOT_ACCOUNT = new RobotAccountDataImpl(ROBOT,
      "http://www.example.com", "fake", OLD_CAPABILITIES, true);
  private static final String NEW_HASH = "newHash";
  private static final RobotCapabilities NEW_CAPABILITIES = new RobotCapabilities(
      EMPTY_CAPABILITIES, NEW_HASH, ProtocolVersion.V2_2);
  private static final RobotAccountData NEW_ROBOT_ACCOUNT = new RobotAccountDataImpl(ROBOT,
      "http://www.example.com", "fake", NEW_CAPABILITIES, true);

  private AccountStore accountStore;
  private RobotConnector connector;
  private NotifyOperationService operationService;
  private OperationContext context;

  @Override
  protected void setUp() throws Exception {
    accountStore = mock(AccountStore.class);
    connector = mock(RobotConnector.class);
    context = mock(OperationContext.class);
    operationService = new NotifyOperationService(accountStore, connector);

    when(accountStore.getAccount(ROBOT)).thenReturn(ROBOT_ACCOUNT);
    when(connector.fetchCapabilities(eq(ROBOT_ACCOUNT), anyString())).thenReturn(NEW_ROBOT_ACCOUNT);
  }

  public void testUpdateCapabilites() throws Exception {
    OperationRequest operation = new OperationRequest(
        "robot.notify", OP_ID, Parameter.of(ParamsProperty.CAPABILITIES_HASH, NEW_HASH));

    operationService.execute(operation, context, ROBOT);

    verify(connector).fetchCapabilities(eq(ROBOT_ACCOUNT), anyString());
    verify(accountStore).putAccount(NEW_ROBOT_ACCOUNT);
  }

  public void testDontUpdateIfHashesMatch() throws Exception {
    OperationRequest operation =
        new OperationRequest("robot.notify", OP_ID, Parameter.of(ParamsProperty.CAPABILITIES_HASH,
            OLD_HASH));

    operationService.execute(operation, context, ROBOT);

    verifyZeroInteractions(connector);
    verify(accountStore, never()).putAccount(any(AccountData.class));
  }

  public void testErrorOnFailingConnection() throws Exception {
    OperationRequest operation =
        new OperationRequest("robot.notify", OP_ID, Parameter.of(ParamsProperty.CAPABILITIES_HASH,
            NEW_HASH));

    when(connector.fetchCapabilities(any(RobotAccountData.class), anyString())).thenThrow(
        new CapabilityFetchException(""));

    operationService.execute(operation, context, ROBOT);

    verify(accountStore, never()).putAccount(any(AccountData.class));
    verify(context).constructErrorResponse(eq(operation), anyString());
  }

  public void testExceptionOnUnknownAccount() throws Exception {
    OperationRequest operation =
        new OperationRequest("robot.notify", OP_ID, Parameter.of(ParamsProperty.CAPABILITIES_HASH,
            NEW_HASH));

    when(accountStore.getAccount(ROBOT)).thenReturn(null);

    try {
      operationService.execute(operation, context, ROBOT);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }

    verifyZeroInteractions(connector);
    verify(accountStore, never()).putAccount(any(AccountData.class));
  }

  public void testExceptionOnMissingHash() throws Exception {
    OperationRequest operation = new OperationRequest("robot.notify", OP_ID);

    try {
      operationService.execute(operation, context, ROBOT);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }

    verifyZeroInteractions(connector);
    verify(accountStore, never()).putAccount(any(AccountData.class));
  }
}
