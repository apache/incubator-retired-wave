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

package org.waveprotocol.wave.client.account;

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Manages profiles for participants.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface ProfileManager extends SourcesEvents<ProfileListener> {

  /**
   * Gets the profile for a participant.
   *
   * @param participantId id of the participant
   * @return the profile for a participant
   */
  Profile getProfile(ParticipantId participantId);

  /**
   * Returns whether the participant should be ignored in the context of
   * accounts.
   *
   * @param participantId the participant id to check
   * @return true if the participant should be ignored, false if not
   */
  boolean shouldIgnore(ParticipantId participantId);
}
