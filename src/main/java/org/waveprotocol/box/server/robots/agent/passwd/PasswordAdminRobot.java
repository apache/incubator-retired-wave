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

package org.waveprotocol.box.server.robots.agent.passwd;

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

import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.CANNOT_CHANGE_PASSWORD_FOR_USER;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.changeUserPassword;

/**
 * Robot agent that handles the password reset for users by admin.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
@Singleton
public final class PasswordAdminRobot extends AbstractCliRobotAgent {

  private static final Logger LOG = Logger.getLogger(PasswordAdminRobot.class.getName());
  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/passwd/admin";

  /** The address of the admin user as defined in the server configuration. */
  private final String serverAdminId;

  /** Account store with user and robot accounts. */
  private final AccountStore accountStore;

  @Inject
  public PasswordAdminRobot(Injector injector) {
    super(injector);
    serverAdminId =
        injector.getInstance(Config.class).getString("administration.admin_user");
    accountStore = injector.getInstance(AccountStore.class);
  }

  @Override
  protected String maybeExecuteCommand(CommandLine commandLine, String modifiedBy) {
    String robotMessage;
    // Verify that the user that attempts to change the password has admin privileges.
    if (!modifiedBy.equals(serverAdminId)) {
      robotMessage =
          "User " + modifiedBy + " is not authorized to use " + getCommandName() + " command.";
    } else {
      String userId = null;
      try {
        String[] args = commandLine.getArgs();
        userId = args[1];
        String newPassword = args[2];
        // Add domain to the user id if needed.
        userId = userId + (userId.contains("@") ? "" : "@" + getWaveDomain());
        ParticipantId participantId = ParticipantId.of(userId);
        changeUserPassword(newPassword, participantId, accountStore);
        robotMessage =
            String.format("Changed password for user %s, the new password is: %s\n", userId,
                newPassword);
        LOG.log(Level.INFO, "Password changed for user " + userId + " by " + modifiedBy);
      } catch (IllegalArgumentException e) {
        LOG.log(Level.SEVERE, userId, e);
        robotMessage = e.getMessage();
      } catch (PersistenceException | InvalidParticipantAddress e) {
        robotMessage = CANNOT_CHANGE_PASSWORD_FOR_USER + userId;
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
    return "passwdadmin";
  }

  @Override
  public String getFullDescription() {
    return getShortDescription() + "\n" + getUsage() + "\nExample: " + getCommandName() + " "
        + getExample();
  }

  @Override
  public String getCmdLineSyntax() {
    return "[OPTIONS] [USERNAME] [NEW_PASSWORD]";
  }

  @Override
  public String getExample() {
    return "user_id new_password";
  }

  @Override
  public String getShortDescription() {
    return "The command allows the admin to change the password of other user. "
    + "Please make sure to use it in a wave without other participants. "
    + "It is also advised to remove yourself from the wave "
    + "when you finished changing the password.";
  }

  @Override
  public String getRobotName() {
    return "PasswdAdmin-Bot";
  }

  @Override
  public String getRobotUri() {
    return ROBOT_URI;
  }

  @Override
  public String getRobotId() {
    return "passwdadmin-bot";
  }
}
