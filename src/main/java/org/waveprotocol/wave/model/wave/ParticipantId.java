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

package org.waveprotocol.wave.model.wave;

import org.waveprotocol.wave.model.util.Preconditions;

import java.io.Serializable;

/**
 * A ParticipantId uniquely identifies a participant. It looks like an email
 * address, e.g. 'joe@example.com'
 */
public final class ParticipantId implements Comparable<ParticipantId>, Serializable {

  private static final long serialVersionUID = -1465142562958113670L;

  /** The prefix of a domain in the ParticpantId */
  public static final String DOMAIN_PREFIX = "@";

  /** The participant's address */
  private final String address;

  /**
   * Constructs an id.
   *
   * This constructor will be deprecated soon. Use the static of() methods
   * instead.
   *
   * @param address a non-null address string
   */
  public ParticipantId(String address) {
    Preconditions.checkNotNull(address, "Non-null address expected");

    address = normalize(address);
    this.address = address;
  }

  /**
   * Normalizes an address.
   *
   * @param address address to normalize; may be null
   * @return normal form of {@code address} if non-null; null otherwise.
   */
  private static String normalize(String address) {
    return address.toLowerCase();
  }

  /**
   * Validates the given address. Validation currently only checks whether one
   * and only one @ symbol is present.
   *
   * @param address the non-null address to validate
   * @throws InvalidParticipantAddress if the validation fails.
   */
  private static void validate(String address) throws InvalidParticipantAddress {
    Preconditions.checkNotNull(address, "Expected non-null address");
    int sepIndex = address.indexOf(DOMAIN_PREFIX);
    // The domain separator may be the first char, but must not be the last.
    if (sepIndex < 0) {
      throw new InvalidParticipantAddress(address, "Missing domain prefix: " + DOMAIN_PREFIX);
    } else if (sepIndex >= (address.length() - 1)) {
      throw new InvalidParticipantAddress(address, "Missing domain");
    } else if (sepIndex != address.lastIndexOf(DOMAIN_PREFIX)) {
      throw new InvalidParticipantAddress(address, "Multiple domain prefixes: " + DOMAIN_PREFIX);
    }
    // TODO: Check the validity of the username and domain part
  }

  /**
   * @return the participant's address
   */
  public String getAddress() {
    return address;
  }

  /**
   * @return the name in the address. If no "@" occurs, it will be the
   *         whole string, if more than one occurs, it will be the part before
   *         the last "@".
   */
  public String getName() {
    String[] parts = address.split(DOMAIN_PREFIX);
    return parts[0];
  }

  /**
   * @return the domain name in the address. If no "@" occurs, it will be the
   *         whole string, if more than one occurs, it will be the part after
   *         the last "@".
   */
  public String getDomain() {
    String[] parts = address.split(DOMAIN_PREFIX);
    return parts[parts.length - 1];
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof ParticipantId) {
      ParticipantId p = (ParticipantId) o;
      return address.equals(p.address);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public String toString() {
    return getAddress();
  }

  /**
   * Constructs a {@link ParticipantId} with the supplied name and domain.
   *
   * @param name the name of participant.
   * @param domain the domain of participant.
   * @return an instance of {@link ParticipantId} constructed using the given
   *         address.
   * @throws InvalidParticipantAddress if the validation on the address fails.
   */
  public static ParticipantId of(String name, String domain) throws InvalidParticipantAddress {
    return ParticipantId.of(name + ParticipantId.DOMAIN_PREFIX + domain);
  }

  /**
   * Constructs a {@link ParticipantId} with the supplied address. The given
   * address will be validated.
   *
   * @param address the non-null address to construct a {@link ParticipantId} for
   * @return an instance of {@link ParticipantId} constructed using the given
   *         address.
   * @throws InvalidParticipantAddress if the validation on the address fails.
   */
  public static ParticipantId of(String address) throws InvalidParticipantAddress {
    validate(address);
    return new ParticipantId(address);
  }

  /**
   * Constructs a {@link ParticipantId} with the given address. It will validate
   * the given address. It is unsafe because it will throw an unchecked
   * exception if the validation fails.
   *
   * @param address the non-null address of the Participant
   * @return an instance of {@link ParticipantId} constructed using the given
   *         address.
   * @throws IllegalArgumentException if the validation on the address fails
   */
  public static ParticipantId ofUnsafe(String address) {
    try {
      return of(address);
    } catch (InvalidParticipantAddress e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Compare two {@link ParticipantId}s, name first, then domain.
   */
  @Override
  public int compareTo(ParticipantId other) {
    /*
     *  Because it's still possible to create invalid ParticipantId instances
     *  we must deal with the case where an address has other than two (2) parts.
     */
    String[] parts = address.split(DOMAIN_PREFIX);
    String[] otherParts = other.address.split(DOMAIN_PREFIX);
    int minLen = Math.min(parts.length, otherParts.length);
    for (int i = 0; i < minLen; i++) {
      int diff = parts[i].compareTo(otherParts[i]);
      if (diff != 0) {
        return diff;
      }
    }
    return parts.length - otherParts.length;
  }
}
