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

package org.waveprotocol.box.server.account;

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * Represents a basic immutable account, consists solely out of a username. It
 * has methods to check and convert to other type of accounts.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface AccountData {

  /**
   * Gets the participant id of the user who owns this account. This is the
   * primary identifier for accounts.
   *
   * @return returns a non-null participant id.
   */
  ParticipantId getId();

  /**
   * @return true iff this account is a {@link HumanAccountData}.
   */
  boolean isHuman();

  /**
   * Returns this account as a {@link HumanAccountData}.
   *
   * @precondition isHuman()
   */
  HumanAccountData asHuman();

  /**
   * @return true iff this account is a {@link RobotAccountData}.
   */
  boolean isRobot();

  /**
   * Returns this account as a {@link RobotAccountData}.
   *
   * @precondition isRobot()
   */
  RobotAccountData asRobot();
}
