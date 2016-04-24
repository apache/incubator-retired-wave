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

package org.waveprotocol.box.server.robots.agent.registration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.commons.cli.CommandLine;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.agent.AbstractCliRobotAgent;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.CANNOT_CREATE_USER;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.createUser;

/**
 * Robot agent that handles the user registration by an admin
 *
 * @author ali@lown.me.uk (Ali Lown)
 */
@SuppressWarnings("serial")
@Singleton
public final class RegistrationRobot extends AbstractCliRobotAgent {

  private static final Logger LOG = Logger.getLogger(RegistrationRobot.class.getName());
  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/registration/admin";

  /** The address of the admin user as defined in the server configuration. */
  private final String serverAdminId;

  /** Account store with user and robot accounts. */
  private final AccountStore accountStore;

  @Inject
  public RegistrationRobot(Injector injector) {
    super(injector);
    serverAdminId =
        injector.getInstance(Config.class).getString("administration.admin_user");
    accountStore = injector.getInstance(AccountStore.class);
  }

  @Override
  protected String maybeExecuteCommand(CommandLine commandLine, String modifiedBy) {
    String robotMessage;
    // Verify that the user that attempts to create a user has admin privileges.
    if (!modifiedBy.equals(serverAdminId)) {
      robotMessage =
          "User " + modifiedBy + " is not authorized to use " + getCommandName() + " command.";
    } else {
      String userId = null;
      try {
        String[] args = commandLine.getArgs();
        userId = args[1];
        String password = args[2];
        // Add domain to the user id if needed.
        userId = userId + (userId.contains("@") ? "" : "@" + getWaveDomain());
        ParticipantId participantId = ParticipantId.of(userId);
        createUser(accountStore, participantId, password);
        robotMessage = String.format("Created user %s, the password is: %s\n", userId, password);
        LOG.log(Level.INFO, "Created user " + userId + " by " + modifiedBy);
      } catch (IllegalArgumentException e) {
        LOG.log(Level.SEVERE, userId, e);
        robotMessage = e.getMessage();
      } catch (PersistenceException | InvalidParticipantAddress e) {
        robotMessage = CANNOT_CREATE_USER + userId;
        LOG.log(Level.SEVERE, "userId: " + userId, e);
      }
    }

    return robotMessage;
  }

  @Override
  public int getMinNumOfArguments() {
    return 2;
  }

  @Override
  public int getMaxNumOfArguments() {
    return 2;
  }

  @Override
  public String getCommandName() {
    return "register";
  }

  @Override
  public String getFullDescription() {
    return getShortDescription() + "\n" + getUsage() + "\nExample: " + getCommandName() + " "
        + getExample();
  }

  @Override
  public String getCmdLineSyntax() {
    return "[OPTIONS] [USERNAME] [PASSWORD]";
  }

  @Override
  public String getExample() {
    return "user_id password";
  }

  @Override
  public String getShortDescription() {
    return "The command allows the admin to register other users. "
    + "Please make sure to use it in a wave without other participants. "
    + "It is also advised to remove yourself from the wave "
    + "when you finished creating users.";
  }

  @Override
  public String getRobotName() {
    return "Registration-Bot";
  }

  @Override
  public String getRobotUri() {
    return ROBOT_URI;
  }

  @Override
  public String getRobotId() {
    return "registration-bot";
  }
}
