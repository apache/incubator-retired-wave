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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * Implements {@link RobotRegistrar}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotRegistrarImpl implements RobotRegistrar {

  private static final Listener REGISTRATION_EVENTS_LOGGER = new Listener() {

    final Logger log = Logger.getLogger(RobotRegistrarImpl.class.getName());

    @Override
    public void onRegistrationSuccess(RobotAccountData account) {
      log.info("Registered robot: " + account.getId().getAddress() + " at " + account.getUrl());
    }

    @Override
    public void onUnregistrationSuccess(RobotAccountData account) {
      log.info("Unregistered robot: " + account.getId().getAddress() + " at " + account.getUrl());
    }
  };

  /** The length of the verification token (token secret). */
  private static final int TOKEN_LENGTH = 48;

  /** The account store. */
  private final AccountStore accountStore;

  /** The verification token generator. */
  private final TokenGenerator tokenGenerator;

  /** The list of listeners on robot un/registration events. */
  private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<Listener>();

  /**
   * Computes and validates the robot URL.
   *
   * @param location the robot location.
   * @return the validated robot URL in the form:
   *         [http|https]://[domain]:[port]/[path], for example:
   *         http://example.com:80/myrobot
   * @throws RobotRegistrationException if the specified URI is invalid.
   */
  private static String computeValidateRobotUrl(String location)
      throws RobotRegistrationException {
    URI uri;
    try {
      uri = URI.create(location);
    } catch (IllegalArgumentException e) {
      String errorMessage = "Invalid Location specified, please specify a location in URI format.";
      throw new RobotRegistrationException(errorMessage + " " + e.getLocalizedMessage(), e);
    }
    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
      scheme = "http";
    }
    String robotLocation;
    if (uri.getPort() != -1) {
      robotLocation = scheme + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
    } else {
      robotLocation = scheme + "://" + uri.getHost() + uri.getPath();
    }

    if (robotLocation.endsWith("/")) {
      robotLocation = robotLocation.substring(0, robotLocation.length() - 1);
    }
    return robotLocation;
  }

  @Inject
  public RobotRegistrarImpl(AccountStore accountStore, TokenGenerator tokenGenerator) {
    this.accountStore = accountStore;
    this.tokenGenerator = tokenGenerator;
    addRegistrationListener(REGISTRATION_EVENTS_LOGGER);
  }

  @Override
  public RobotAccountData registerNew(ParticipantId robotId, String location)
      throws RobotRegistrationException, PersistenceException {
    Preconditions.checkNotNull(robotId);
    Preconditions.checkNotNull(location);
    Preconditions.checkArgument(!location.isEmpty());

    if (accountStore.getAccount(robotId) != null) {
      throw new RobotRegistrationException(robotId.getAddress()
          + " is already in use, please choose another one.");
    }
    return registerRobot(robotId, location);
  }

  @Override
  public RobotAccountData unregister(ParticipantId robotId) throws RobotRegistrationException,
      PersistenceException {
    Preconditions.checkNotNull(robotId);
    AccountData accountData = accountStore.getAccount(robotId);
    if (accountData == null) {
      return null;
    }
    throwExceptionIfNotRobot(accountData);
    RobotAccountData robotAccount = accountData.asRobot();
    removeRobotAccount(robotAccount);
    return robotAccount;
  }

  @Override
  public RobotAccountData registerOrUpdate(ParticipantId robotId, String location)
      throws RobotRegistrationException, PersistenceException {
    Preconditions.checkNotNull(robotId);
    Preconditions.checkNotNull(location);
    Preconditions.checkArgument(!location.isEmpty());

    AccountData account = accountStore.getAccount(robotId);
    if (account != null) {
      throwExceptionIfNotRobot(account);
      RobotAccountData robotAccount = account.asRobot();
      if (robotAccount.getUrl().equals(location)) {
        return robotAccount;
      } else {
        removeRobotAccount(robotAccount);
      }
    }
    return registerRobot(robotId, location);
  }

  /**
   *  Adds the robot to the account store and notifies the listeners.
   */
  private RobotAccountData registerRobot(ParticipantId robotId, String location)
      throws RobotRegistrationException, PersistenceException {
    String robotLocation = computeValidateRobotUrl(location);

    RobotAccountData robotAccount =
        new RobotAccountDataImpl(robotId, robotLocation,
            tokenGenerator.generateToken(TOKEN_LENGTH), null, true);
    accountStore.putAccount(robotAccount);
    for (Listener listener : listeners) {
      listener.onRegistrationSuccess(robotAccount);
    }
    return robotAccount;
  }

  /**
   * Removes the robot account and notifies the listeners.
   * @param existingAccount the account to remove
   * @throws PersistenceException if the persistence layer reports an error.
   */
  private void removeRobotAccount(RobotAccountData existingAccount)
      throws PersistenceException {
    accountStore.removeAccount(existingAccount.getId());
    for (Listener listener : listeners) {
      listener.onUnregistrationSuccess(existingAccount);
    }
  }

  /**
   * Ensures that the account belongs to a robot.
   *
   * @param existingAccount the account to check.
   * @throws RobotRegistrationException if the account is not robot.
   */
  private void throwExceptionIfNotRobot(AccountData existingAccount)
      throws RobotRegistrationException {
    if (!existingAccount.isRobot()) {
      throw new RobotRegistrationException(existingAccount.getId().getAddress()
          + " is not a robot account!");
    }
  }

  // Handle listeners.
  @Override
  public void addRegistrationListener(Listener listener) {
    Preconditions.checkNotNull(listener);
    listeners.add(listener);
  }

  @Override
  public void removeRegistrationListener(Listener listener) {
    Preconditions.checkNotNull(listener);
    listeners.remove(listener);
  }
}
