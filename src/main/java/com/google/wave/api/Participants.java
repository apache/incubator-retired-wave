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

package com.google.wave.api;


import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that represents wavelet's participants. This class supports various
 * participant related operations, such as, adding participant to a wavelet.
 */
public class Participants implements Set<String>, Serializable {

  /**
   * Roles to use for the participants
   */
  public enum Role {
    /** Full member. */
    FULL,
    /** Can only view the wave. */
    READ_ONLY,
    /** Not recognized. Probably a newer server version. */
    UNKNOWN;
  }

  /** A set of participant id that represents wavelet participants. */
  private final Set<String> participants;

  /** The wavelet that this participant set represents. */
  private final Wavelet wavelet;

  /** The operation queue to queue operation to the robot proxy. */
  private final OperationQueue operationQueue;

  /** The roles of the participants. The values are strings to match the wire
   * protocol.
   */
  private final Map<String, String> roles;

  /**
   * Constructor.
   *
   * @param participants a collection of initial participants of the wavelet.
   * @param wavelet the wavelet that this participants list represents.
   * @param operationQueue the operation queue to queue operation to the robot
   *     proxy.
   */
  public Participants(Collection<String> participants, Map<String, String> roles,
      Wavelet wavelet, OperationQueue operationQueue) {
    this.participants = new LinkedHashSet<String>(participants);
    this.roles = roles;
    this.wavelet = wavelet;
    this.operationQueue = operationQueue;
  }

  /**
   * Add the given participant id if it doesn't exist.
   *
   * @param participantId the id of the participant that will be added.
   * @return {@code true} if the given participant id does not exist yet
   *     in the set of participants, which means that a new
   *     {@code wavelet.addParticipant()} has been queued. Otherwise, returns
   *     {@code false}.
   */
  @Override
  public boolean add(String participantId) {
    if (participants.contains(participantId)) {
      return false;
    }

    operationQueue.addParticipantToWavelet(wavelet, participantId);
    participants.add(participantId);
    return true;
  }

  /**
   * Checks whether the given participant id exists in the set or not.
   *
   * @param participantId the participant id to check.
   * @return {@code true} if the set contains the given participant id.
   *     Otherwise, returns {@code false}.
   */
  @Override
  public boolean contains(Object participantId) {
    return participants.contains(participantId);
  }

  /**
   * Returns the number of participants of the wavelet that owns this
   * participant set.
   *
   * @return the number of participants.
   */
  @Override
  public int size() {
    return participants.size();
  }

  /**
   * Checks whether this participant set is empty or not.
   *
   * @return {@code true} if the participant set is empty. Otherwise, returns
   *     {@code false}.
   */
  @Override
  public boolean isEmpty() {
    return participants.isEmpty();
  }

  @Override
  public Iterator<String> iterator() {
    return participants.iterator();
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    boolean retval = false;
    for (String participant : c) {
      retval = retval || add(participant);
    }
    return retval;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    boolean retval = true;
    for (Object participant : c) {
      retval = retval && contains(participant);
    }
    return retval;
  }

  /**
   * Remove the given participant id if it exist.
   *
   * @param participantId the id of the participant that will be removed.
   * @return {@code true} if the given participant id does exist in the set
   *     of participants, which means that the
   *     {@code wavelet.removeParticipant()} has been dequeued. Otherwise, returns
   *     {@code false}.
   */
  @Override
  public boolean remove(Object oParticipantId) {
    
    if(!(oParticipantId instanceof String)) {
      throw new IllegalArgumentException("ParticipantId must be a string.");
    }
    String participantId = (String) oParticipantId;
    
    if (!participants.contains(participantId)) {
      return false;
    }

    operationQueue.removeParticipantFromWavelet(wavelet, participantId);
    participants.remove(participantId);
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] toArray() {
    return participants.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return participants.toArray(a);
  }

  public void setParticipantRole(String participant, Role role) {
    operationQueue.modifyParticipantRoleOfWavelet(wavelet, participant, role.name());
    roles.put(participant, role.name());
  }
  
  public Role getParticipantRole(String participant) {
    String stringRole = roles.get(participant);
    if (stringRole == null) {
      return Role.FULL;
    }
    try {
      return Role.valueOf(stringRole);
    } catch (IllegalArgumentException e) {
      return Role.UNKNOWN;
    }
  }
}
