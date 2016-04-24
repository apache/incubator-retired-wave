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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.robot.CapabilityFetchException;
import com.google.wave.api.robot.RobotName;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.box.server.robots.passive.RobotConnector;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

/**
 * Implementation of the robot.notify operation which might update the robot's
 * capabilties.
 * 
 * @author ljvderijk@gmail.com (Lennard de Rijk)
 */
public class NotifyOperationService implements OperationService {

  private static final Log LOG = Log.get(NotifyOperationService.class);

  private final AccountStore accountStore;
  private final RobotConnector connector;

  @Inject
  public NotifyOperationService(AccountStore accountStore, RobotConnector connector) {
    this.accountStore = accountStore;
    this.connector = connector;
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context,
      ParticipantId participant) throws InvalidRequestException {
    String capabilitiesHash =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.CAPABILITIES_HASH);

    RobotName robotName = RobotName.fromAddress(participant.getAddress());

    ParticipantId robotAccountId = ParticipantId.ofUnsafe(robotName.toEmailAddress());
    AccountData account;
    try {
      account = accountStore.getAccount(robotAccountId);
    } catch (PersistenceException e) {
      LOG.severe("Failed to retreive account data for " + robotAccountId, e);
      context.constructErrorResponse(operation, "Unable to retrieve account data");
      return;
    }

    if (account == null || !account.isRobot()) {
      throw new InvalidRequestException("Can't exectute robot.notify for unknown robot "
          + robotAccountId);
    }

    RobotAccountData robotAccountData = account.asRobot();
    RobotCapabilities capabilities = robotAccountData.getCapabilities();
    if (capabilities != null && capabilitiesHash.equals(capabilities.getCapabilitiesHash())) {
      // No change in capabilities indicated
      context.constructResponse(operation, Maps.<ParamsProperty, Object> newHashMap());
      return;
    }

    try {
      robotAccountData = connector.fetchCapabilities(robotAccountData, "");
    } catch (CapabilityFetchException e) {
      LOG.fine("Unable to retrieve capabilities for " + account.getId(), e);
      context.constructErrorResponse(operation, "Unable to retrieve new capabilities");
      return;
    }

    try {
      accountStore.putAccount(robotAccountData);
    } catch (PersistenceException e) {
      LOG.severe("Failed to update account data for " + robotAccountId, e);
      context.constructErrorResponse(operation, "Unable to update account data");
      return;
    }

    // Set empty response to indicate success
    context.constructResponse(operation, Maps.<ParamsProperty, Object> newHashMap());
  }
}
