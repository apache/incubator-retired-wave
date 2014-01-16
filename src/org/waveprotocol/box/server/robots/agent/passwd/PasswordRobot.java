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

import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.CANNOT_CHANGE_PASSWORD_FOR_USER;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.changeUserPassword;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.apache.commons.cli.CommandLine;
import org.eclipse.jetty.util.MultiMap;
import org.waveprotocol.box.server.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.agent.AbstractCliRobotAgent;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Robot agent that allows a user to change her own password. The userId should
 * be from this wave domain.
 * 
 * When the robot is added to a wave, it prints basic description and then
 * inspects text entered by the user. When a new line char is entered it scans
 * the last line of the text in the blip and parses it using Apache CLI command
 * line interpreter. If the command found to be valid, the robot validates user
 * credentials and then changes the password of the user to a new one.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
@Singleton
public final class PasswordRobot extends AbstractCliRobotAgent {

  private static final Logger LOG = Logger.getLogger(PasswordRobot.class.getName());
  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/passwd/user";

  /** Configuration for the LoginContext. */
  private final Configuration configuration;
  
  @Inject
  public PasswordRobot(Injector injector) {
    super(injector);
    configuration = injector.getInstance(Configuration.class);
  }

  @Override
  protected String maybeExecuteCommand(CommandLine commandLine, String modifiedBy) {
    String robotMessage = null;
    // Get the user that wants to change her own password.
    if (!modifiedBy.endsWith("@" + getWaveDomain())) {
      // Can change passwords only for users on this wave domain.
      robotMessage =
          String.format("User %s does not belong to the @%s domain\n", modifiedBy,
              getWaveDomain());
    } else {
      String[] args = commandLine.getArgs();
      try {
        ParticipantId participantId = ParticipantId.of(modifiedBy);
        if (args.length == 2) {
          // If current password is empty, i.e. "", then user should pass
          // only the new password.
          args = Arrays.copyOf(args, 3);
          args[2] = args[1];
          args[1] = "";
        }
        String oldPassword = args[1];
        String newPassword = args[2];
        verifyCredentials(oldPassword, participantId);
        changeUserPassword(newPassword, participantId, getAccountStore());
        robotMessage =
            String.format("Changed password for user %s, the new password is: %s", modifiedBy,
                newPassword);
        LOG.info(modifiedBy + " changed  password for user: " + modifiedBy);
      } catch (IllegalArgumentException e) {
        robotMessage = e.getMessage();
        LOG.log(Level.SEVERE, "userId: " + modifiedBy, e);
      } catch (PersistenceException e) {
        robotMessage = CANNOT_CHANGE_PASSWORD_FOR_USER + modifiedBy;
        LOG.log(Level.SEVERE, "userId: " + modifiedBy, e);
      } catch (InvalidParticipantAddress e) {
        robotMessage = CANNOT_CHANGE_PASSWORD_FOR_USER + modifiedBy;
        LOG.log(Level.SEVERE, "userId: " + modifiedBy, e);
      } catch (LoginException e) {
        robotMessage =
            CANNOT_CHANGE_PASSWORD_FOR_USER + modifiedBy
                + ". Please verify your old password";
        LOG.log(Level.SEVERE, "userId: " + modifiedBy, e);
      }
    }
    return robotMessage;
  }

  /**
   * Verifies user credentials.
   * 
   * @param oldPassword the password to verify.
   * @param participantId the participantId of the user.
   * @throws LoginException if the user provided incorrect password.
   */
  private void verifyCredentials(String password, ParticipantId participantId)
      throws LoginException {
    MultiMap<String> parameters = new MultiMap<String>();
    parameters.putAllValues(ImmutableMap.of("password", password, "address", participantId.getAddress()));
    CallbackHandler callbackHandler = new HttpRequestBasedCallbackHandler(parameters);
    LoginContext context = new LoginContext("Wave", new Subject(), callbackHandler, configuration);
    // If authentication fails, login() will throw a LoginException.
    context.login();
  }

  @Override
  public String getFullDescription() {
    return getShortDescription() + " If your password is empty - enter only the new password.\n"
        + getUsage() + "\nExample: " + getCommandName() + " " + getExample();
  }

  @Override
  public String getCmdLineSyntax() {
    return "[OPTIONS] [OLD_PASSWORD] [NEW_PASSWORD]";
  }

  @Override
  public String getExample() {
    return "old_password new_password";
  }

  @Override
  public String getShortDescription() {
    return "The command allows users to change their own password. "
    + "Please make sure to use it in a wave without other participants. "
    + "It is also advised to remove yourself from the wave "
    + "when you finished changing the password.";
  }

  @Override
  public String getCommandName() {
    return "passwd";
  }

  @Override
  public String getRobotName() {
    return "Passwd-Bot";
  }

  @Override
  public int getMinNumOfArguments() {
    return 1;
  }
  
  @Override
  public int getMaxNumOfArguments() {
    return 2;
  }
  
  @Override
  public String getRobotUri() {
    return ROBOT_URI;
  }
  
  @Override
  public String getRobotId() {
    return "passwd-bot";
  }
}
