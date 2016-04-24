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

package org.waveprotocol.box.server.util;

import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

/**
 * User Registration utility methods.
 *
 * @author ali@lown.me.uk (Ali Lown)
 */
public class RegistrationUtil {

  private static final Log LOG = Log.get(RegistrationUtil.class);

  private RegistrationUtil() {
  }

  /**
   * Checks if a new username is correct and generates its ParticipantId. On error returns
   * exception containing an error message. On success, returns the id.
   *
   * @param domain the domain
   * @param username the new username
   * @return the new participant id
   * @throws InvalidParticipantAddress if the new username is an invalid participant address
   */
  public static ParticipantId checkNewUsername(String domain, String username) throws InvalidParticipantAddress {
    ParticipantId id = null;

      // First, some cleanup on the parameters.
      if (username == null) {
        throw new InvalidParticipantAddress(username, "Username portion of address cannot be empty");
      }
      username = username.trim().toLowerCase();
      if (username.contains(ParticipantId.DOMAIN_PREFIX)) {
        id = ParticipantId.of(username);
      } else {
        id = ParticipantId.of(username + ParticipantId.DOMAIN_PREFIX + domain);
      }
      if (id.getAddress().indexOf("@") < 1) {
        throw new InvalidParticipantAddress(username, "Username portion of address cannot be empty");
      }
      String[] usernameSplit = id.getAddress().split("@");
      if (usernameSplit.length != 2 || !usernameSplit[0].matches("[\\w\\.]+")) {
        throw new InvalidParticipantAddress(username, "Only letters (a-z), numbers (0-9), and periods (.) are allowed in Username");
      }
      if (!id.getDomain().equals(domain)) {
        throw new InvalidParticipantAddress(username,"You can only create users at the " + domain + " domain");
      }
    return id;
  }

  public static boolean createAccountIfMissing(AccountStore accountStore, ParticipantId id,
      PasswordDigest password, WelcomeRobot welcomeBot) {
    HumanAccountDataImpl account = new HumanAccountDataImpl(id, password);
    try {
      LOG.info("Registering new account for" + id);
      accountStore.putAccount(account);
    } catch (PersistenceException e) {
      LOG.severe("Failed to cretaea new account for " + id, e);
      return false;
    }
    try {
      welcomeBot.greet(account.getId());
    } catch (IOException e) {
      LOG.warning("Failed to create a welcome wavelet for " + id, e);;
    }
    return true;
  }

  public static boolean doesAccountExist(AccountStore accountStore, ParticipantId id) {
    try {
      if (accountStore.getAccount(id) != null) {
        return true;
      }
    }
    catch (PersistenceException e) {
      LOG.severe("Failed to retrieve account data for " + id, e);
      throw new RuntimeException("An unexpected error occurred trying to retrieve account status");
    }
    return false;
  }
}
