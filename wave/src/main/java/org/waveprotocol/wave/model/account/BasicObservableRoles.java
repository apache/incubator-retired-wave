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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map;

/**
 * A Map-backed local version of ObservableRoles.
 *
 *
 */
public class BasicObservableRoles implements ObservableRoles {
  private final Map<String, BasicAssignment> assignments = CollectionUtils.newHashMap();
  private final CopyOnWriteSet<ObservableRoles.Listener> listeners = CopyOnWriteSet.create();

  BasicObservableRoles(Iterable<? extends Assignment> assignments) {
    // TODO(user): make it a precondition that assignments are for unique participants.
    for (Assignment a : assignments) {
      this.assignments.put(a.getParticipant().getAddress(), new BasicAssignment(a));
    }
  }

  public BasicObservableRoles() {
  }

  @Override
  public Iterable<? extends Assignment> getAssignments() {
    return assignments.values();
  }

  @Override
  public Role getRole(ParticipantId participant) {
    Assignment a = assignments.get(participant.getAddress());
    if (a != null && a.getRole() != null) {
      return a.getRole();
    }
    return Policies.DEFAULT_ROLE;
  }

  @Override
  public boolean isPermitted(ParticipantId participant, Capability capability) {
    Role role = getRole(participant);
    return role.isPermitted(capability);
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnChanged() {
    for (ObservableRoles.Listener l : listeners) {
      l.onChanged();
    }
  }

  @Override
  public void assign(ParticipantId participant, Role role) {
    Preconditions.checkNotNull(role, "Can't assign null");
    Role currentRole = getRole(participant);
    if (!currentRole.equals(role)) {
      BasicAssignment assignment = assignments.get(participant.getAddress());
      if (assignment == null) {
        assignment = new BasicAssignment(participant, role);
        assignments.put(participant.getAddress(), assignment);
      } else {
        assignment.setRole(role);
      }
      fireOnChanged();
    }
  }
}
