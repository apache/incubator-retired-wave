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

import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Provides robot un/registration services.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public interface RobotRegistrar {

  /** Receives robot un/registration events. */
  public interface Listener {

    /** Invoked after the robot account was created and added to the account store. */
    public void onRegistrationSuccess(RobotAccountData account);

    /** Invoked after the robot account was removed from the account store. */
    public void onUnregistrationSuccess(RobotAccountData account);
  }

  /**
   * Registers a new robot account.
   *
   * @param robotId the robotId to register.
   * @param location the location of the robot (URI).
   * @return the newly registered robot account.
   * @throws RobotRegistrationException if account for this id already exist.
   * @throws PersistenceException if the persistence layer reports an error.
   */
  public RobotAccountData registerNew(ParticipantId robotId, String location)
      throws RobotRegistrationException, PersistenceException;

  /**
   * Unregisters a robot by removing it from the account store.
   *
   * @param robotId the id to remove.
   * @return the account data of the removed robot or
   *         <code>null</null> if no such robot account exist.
   * @throws RobotRegistrationException if the id to remove exist but is not a
   *         robot.
   * @throws PersistenceException if the persistence layer reports an error.
   */
  public RobotAccountData unregister(ParticipantId robotId) throws RobotRegistrationException,
      PersistenceException;

  /**
   * Registers a new robot or re-registers an existing robot in order to update the robot location.
   *
   * @param robotId the robot id.
   * @param location the new location of the robot (URI).
   * @return the updated robot account.
   * @throws RobotRegistrationException if the id to re-register exist but is not a robot.
   * @throws PersistenceException if the persistence layer reports an error.
   */
  public RobotAccountData registerOrUpdate(ParticipantId robotId, String location)
      throws RobotRegistrationException, PersistenceException;

  /** Adds listener. */
  public void addRegistrationListener(Listener listener);

  /** Removes listener. */
  public void removeRegistrationListener(Listener listener);
}
