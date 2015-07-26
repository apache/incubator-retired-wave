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

package org.waveprotocol.box.server.robots.register;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Unit tests for {@link RobotRegistrarImpl}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotRegistrarImplTest extends TestCase {

  private final static String LOCATION = "https://example.com:9898/robot/";
  private final static String OTHER_LOCATION = "http://foo.com:9898/robot/";
  private final static ParticipantId ROBOT_ID = ParticipantId.ofUnsafe("robot@example.com");
  private final static ParticipantId HUMAN_ID = ParticipantId.ofUnsafe("human@example.com");
  private final static String CONSUMER_TOKEN = "sometoken";

  private AccountStore accountStore;
  private TokenGenerator tokenGenerator;
  private RobotAccountData accountData;
  private RobotRegistrar registrar;

  @Override
  protected void setUp() throws Exception {
    accountStore = mock(AccountStore.class);
    tokenGenerator = mock(TokenGenerator.class);
    accountData = mock(RobotAccountData.class);
    when(accountData.isRobot()).thenReturn(true);
    when(accountData.asRobot()).thenReturn(accountData);
    when(accountData.getUrl()).thenReturn(LOCATION);
    when(accountData.getId()).thenReturn(ROBOT_ID);
    when(tokenGenerator.generateToken(anyInt())).thenReturn(CONSUMER_TOKEN);
    registrar = new RobotRegistrarImpl(accountStore, tokenGenerator);
  }

  public void testRegisterNewSucceeds() throws PersistenceException, RobotRegistrationException {
    RobotAccountData resultAccountData = registrar.registerNew(ROBOT_ID, LOCATION);
    verify(accountStore, atLeastOnce()).getAccount(ROBOT_ID);
    verify(accountStore).putAccount(any(RobotAccountData.class));
    verify(tokenGenerator).generateToken(anyInt());
    assertTrue(resultAccountData.isRobot());
    RobotAccountData robotAccountData = resultAccountData.asRobot();
    // Remove the last '/'.
    assertEquals(LOCATION.substring(0, LOCATION.length() - 1), robotAccountData.getUrl());
    assertEquals(ROBOT_ID, robotAccountData.getId());
    assertEquals(CONSUMER_TOKEN, robotAccountData.getConsumerSecret());
  }

  public void testRegisterNewFailsOnInvalidLocation() throws PersistenceException {
    String invalidLocation = "ftp://some$$$&&&###.com";
    try {
      registrar.registerNew(ROBOT_ID, invalidLocation);
      fail("Location " + invalidLocation + " is invalid, exception is expected.");
    } catch (RobotRegistrationException e) {
      // Expected.
    }
  }

  public void testRegisterNewFailsOnExistingAccount() throws PersistenceException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    try {
      registrar.registerNew(ROBOT_ID, LOCATION);
      fail();
    } catch (RobotRegistrationException e) {
      // Expected.
    }
  }

  public void testUnregisterSucceeds() throws PersistenceException, RobotRegistrationException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    AccountData unregisteredAccountData = registrar.unregister(ROBOT_ID);
    assertTrue(unregisteredAccountData.equals(accountData));
    verify(accountData).isRobot();
    verify(accountStore).removeAccount(ROBOT_ID);
  }

  public void testUnregisterFailsOnHumanAccount() throws PersistenceException {
    when(accountStore.getAccount(HUMAN_ID)).thenReturn(
        new HumanAccountDataImpl(ParticipantId.ofUnsafe(HUMAN_ID.getAddress())));
    try {
      registrar.unregister(HUMAN_ID);
      fail();
    } catch (RobotRegistrationException e) {
      // Expected.
    }
  }

  public void testUnregisterNonExistingRobot() throws PersistenceException,
      RobotRegistrationException {
    AccountData unregisteredAccountData = registrar.unregister(ROBOT_ID);
    assertNull(unregisteredAccountData);
  }

  public void testReRegisterSucceedsOnExistingRobotAccount() throws PersistenceException,
      RobotRegistrationException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    AccountData unregisteredAccountData = registrar.registerOrUpdate(ROBOT_ID, OTHER_LOCATION);
    verify(accountStore).removeAccount(ROBOT_ID);
    verify(accountStore).putAccount(any(RobotAccountData.class));
    assertTrue(unregisteredAccountData.isRobot());
    RobotAccountData robotAccountData = unregisteredAccountData.asRobot();
    // Remove the last '/'.
    assertEquals(OTHER_LOCATION.substring(0, OTHER_LOCATION.length() - 1),
        robotAccountData.getUrl());
    assertEquals(ROBOT_ID, robotAccountData.getId());
    assertEquals(CONSUMER_TOKEN, robotAccountData.getConsumerSecret());
  }

  public void testReRegisterFailsOnExistingHumanAccount() throws PersistenceException {
    when(accountStore.getAccount(HUMAN_ID)).thenReturn(
        new HumanAccountDataImpl(ParticipantId.ofUnsafe(HUMAN_ID.getAddress())));
    try {
      registrar.registerOrUpdate(HUMAN_ID, OTHER_LOCATION);
      fail();
    } catch (RobotRegistrationException e) {
      // Expected.
    }
  }

  public void testReRegisterSucceedsOnNonExistingAccount() throws PersistenceException,
      RobotRegistrationException {
    registrar.registerOrUpdate(ROBOT_ID, OTHER_LOCATION);
    verify(accountStore).putAccount(any(RobotAccountData.class));
  }
}
