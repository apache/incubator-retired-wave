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

package org.waveprotocol.box.server.authentication;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.security.Principal;

/**
 * A principal for a wave user who logged in using the AccountStoreLoginModule. 
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public final class ParticipantPrincipal implements Principal {
  private final ParticipantId id;
  
  /**
   * Create a WavePrincipal for the given wave user.
   * 
   * @param id The user's participant id.
   */
  public ParticipantPrincipal(ParticipantId id) {
    Preconditions.checkNotNull(id, "Participant id is null");
    this.id = id;
  }
  
  @Override
  public String getName() {
    return id.getAddress();
  }

  @Override
  public String toString() {
    return "[Principal " + id + "]";
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ParticipantPrincipal other = (ParticipantPrincipal) obj;
    return id.equals(other.id);
  }
}
