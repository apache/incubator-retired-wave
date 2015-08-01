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

package org.waveprotocol.box.server.robots.agent;

import com.google.wave.api.Blip;

import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.annotation.Nullable;

/**
 * Util methods for the robot agents.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotAgentUtil {

  public static final String CANNOT_CHANGE_PASSWORD_FOR_USER = "Cannot change password for user: ";
  public static final String CANNOT_CREATE_USER = "Cannot create user: ";

  private RobotAgentUtil() {

  }

  /**
   * Appends a message followed by a new line to the end of the blip.
   *
   * @param blip the blip.
   * @param msg the message.
   */
  public static void appendLine(Blip blip, String msg) {
    blip.at(blip.getContent().length()).insert(msg + "\n");
  }

  /**
   * Returns the last line of the content if it ends with "\n" or null
   * otherwise.
   */
  public static String lastEnteredLineOf(@Nullable String blipContent) {
    if (blipContent == null || blipContent.isEmpty() || !blipContent.endsWith("\n")) {
      return null;
    }
    blipContent = blipContent.trim();
    String[] split = blipContent.split("\n");
    return split[split.length - 1];
  }

  /**
   * Changes the user password.
   *
   * @param newPassword the new password of the user.
   * @param participantId the user wave address.
   * @param accountStore the account store with user accounts.
   * @throws PersistenceException if the persistence layer fails.
   * @throws IllegalArgumentException if the user doesn't exist.
   */
  public static void changeUserPassword(String newPassword, ParticipantId participantId,
      AccountStore accountStore) throws PersistenceException, IllegalArgumentException {
    PasswordDigest newPasswordDigest = new PasswordDigest(newPassword.toCharArray());
    HumanAccountDataImpl account = new HumanAccountDataImpl(participantId, newPasswordDigest);
    if (accountStore.getAccount(participantId) != null) {
      accountStore.removeAccount(participantId);
      accountStore.putAccount(account);
    } else {
      throw new IllegalArgumentException(String.format("User %s does not exist on this domain.",
          participantId.getAddress()));
    }
  }

  /**
   * Creates a new user.
   *
   * @param accountStore the account store with user accounts.
   * @param participantId requested user wave address.
   * @param password requested user password
   * @throws PersistenceException if the persistence layer fails.
   * @throws IllegalArgumentException if the userId is already in use.
   */
  public static void createUser(AccountStore accountStore, ParticipantId participantId, String password)
    throws PersistenceException, IllegalArgumentException {
    if (accountStore.getAccount(participantId) != null) {
      throw new IllegalArgumentException(String.format("User %s already exists on this domain.", participantId.getAddress()));
    }

    HumanAccountDataImpl account = new HumanAccountDataImpl(participantId, new PasswordDigest(password.toCharArray()));
    accountStore.putAccount(account);
  }
}
