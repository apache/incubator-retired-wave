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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.robot.RobotName;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.operations.NotifyOperationService;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.testing.DeferredExecutor;

/**
 * Unit tests for {@link RobotsGateway}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotsGatewayTest extends TestCase {

  private WaveletProvider waveletProvider;
  private RobotConnector robotConnector;
  private AccountStore accountStore;
  private RobotSerializer serializer;
  private EventDataConverterManager converterManager;
  private RobotsGateway gateway;
  private DeferredExecutor executor;
  private ConversationUtil conversationUtil;
  private NotifyOperationService notifyOpService;

  @Override
  protected void setUp() {
    waveletProvider = mock(WaveletProvider.class);
    robotConnector = mock(RobotConnector.class);
    accountStore = mock(AccountStore.class);
    serializer = mock(RobotSerializer.class);
    converterManager = mock(EventDataConverterManager.class);
    executor = new DeferredExecutor();
    conversationUtil = mock(ConversationUtil.class);
    notifyOpService = mock(NotifyOperationService.class);

    gateway =
        new RobotsGateway(waveletProvider, robotConnector, accountStore, serializer,
            converterManager, executor, conversationUtil, notifyOpService);
  }

  public void testWaveletUpdate() throws Exception {
    // TODO(ljvderijk): Update this test once this method properly sends updates
    // to robots.
    // Test: gateway.waveletUpdate(wavelet, resultingVersion, deltas);
  }

  public void testEnsureRunnableCallsRobot() throws Exception {
    Robot robot = mock(Robot.class);
    when(robot.getRobotName()).thenReturn(RobotName.fromAddress("robot@example.com"));

    gateway.ensureScheduled(robot);
    executor.runAllCommands();

    verify(robot).run();
  }

  public void testUpdateRobotAccount() throws Exception {
    Robot robot = mock(Robot.class);
    RobotAccountData account = mock(RobotAccountData.class);
    when(robot.getAccount()).thenReturn(account);

    // Return newAccount when updated
    RobotAccountData newAccount = mock(RobotAccountData.class);
    when(robotConnector.fetchCapabilities(eq(account), any(String.class))).thenReturn(newAccount);

    gateway.updateRobotAccount(robot);

    verify(accountStore).putAccount(newAccount);
  }
}
