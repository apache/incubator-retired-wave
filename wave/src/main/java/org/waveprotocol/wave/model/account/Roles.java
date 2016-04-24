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

package org.waveprotocol.wave.model.account;


import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Describes permissions that participants have to a wavelet.
 *
 * Being on the participant list currently grants you full access to a wavelet,
 * you can explicitly deny capabilities (expressed by
 * {@link org.waveprotocol.wave.model.account.Capability}) for participants.
 *
 */
public interface Roles {
  /**
   * Checks whether the participant is allowed to perform the specified
   * capability.
   *
   * NOTE: This check is only valid if the participant is on the participant
   * list. This is not verified by this class.
   * 
   * TODO(user): Move this out to be a static helper method,
   * isPermitted(Roles, ParticipantId, Capability)
   *
   * @param participant
   * @param capability the capability to check for.
   * @return false if the capability has been denied.
   */
  boolean isPermitted(ParticipantId participant, Capability capability);

  /**
   * Assign role to participant.
   *
   * @param participant the participant to assign role to.
   * @param role the role to assign.
   */
  void assign(ParticipantId participant, Role role);
  
  /**
   * The current role assigned to the participant. If there are several
   * assignments the last one is used. If there is no assignment
   * {@link Role#FULL} is assumed.
   */
  Role getRole(ParticipantId participant);
 
  /**
   * Get all the assignments explicitly stored. There may be more than one
   * Assignment for a single ParticipantId, and there may be no Assignment
   * for a ParticipantId which is a participant on the wavelet.
   * 
   * TODO(user): Change this contract such that there is a unique
   * assignment per participant.
   */
  Iterable<? extends Assignment> getAssignments();
}