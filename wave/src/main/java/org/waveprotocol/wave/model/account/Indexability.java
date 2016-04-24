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

import java.util.Set;

/**
 * Indexability represents whether to index a wavelet for a participant or not.
 *
 * There is no contract that the participants listed in an Indexability bear
 * any relationship to the participants in the wavelet to which it will (usually) belong.
 *
 */
public interface Indexability {
  /**
   * Get the indexability for this participant. Returns null for participants
   * without explicit assignment.
   */
  IndexDecision getIndexability(ParticipantId participant);

  /**
   * All assigned indexability. Note that participants may not appear here (and
   * thus will have default indexability) and that participants which are not
   * participants in the wavelet may appear here.
   *
   * @return all participants with assigned indexability
   */
  Set<ParticipantId> getIndexDecisions();
}
