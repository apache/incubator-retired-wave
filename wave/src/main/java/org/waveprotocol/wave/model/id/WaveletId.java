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

package org.waveprotocol.wave.model.id;

import org.waveprotocol.wave.model.util.Preconditions;

import java.io.Serializable;

/**
 * In the context of a single wave, a wavelet is identified by a tuple of a
 * wave provider domain and a local identifier which is unique within the domain
 * and the wave.
 *
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */
public final class WaveletId implements Comparable<WaveletId>, Serializable {

  /**
   * The implementation of this class is subject to change, so {@code java.io}
   * serialization should only be used for short-term storage.
   */
  private static final long serialVersionUID = 0;

  private final String domain;
  private final String id;

  private transient String cachedSerialisation = null;

  /**
   * Deserialises a wavelet identifier from a string, throwing a checked exception
   * if deserialisation fails.
   *
   * @param waveletIdString a serialised wave id
   * @return a wave id
   * @throws InvalidIdException if the serialised form is invalid
   */
  public static WaveletId checkedDeserialise(String waveletIdString) throws InvalidIdException {
    return LegacyIdSerialiser.INSTANCE.deserialiseWaveletId(waveletIdString);
  }

  /**
   * Deserialises a wave identifier from a string.
   *
   * @param waveletIdString a serialised wave id
   * @return a wavelet id
   * @throws IllegalArgumentException if the serialised form is invalid
   */
  public static WaveletId deserialise(String waveletIdString) {
    try {
      return checkedDeserialise(waveletIdString);
    } catch (InvalidIdException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Creates a wavelet id from a domain and local identifier.
   *
   * @throws IllegalArgumentException if the domain or id are not valid
   */
  public static WaveletId of(String domain, String id) {
    Preconditions.checkNotNull(domain, "Null domain");
    Preconditions.checkNotNull(id, "Null id");
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, domain), "Invalid domain %s",
        domain);
    Preconditions.checkArgument(WaveIdentifiers.isValidIdentifier(id), "Invalid id %s", id);
    return new WaveletId(domain, id);
  }

  /**
   * Creates a wavelet id from a domain and local identifier.
   *
   * @throws InvalidIdException if the domain or id are not valid
   */
  public static WaveletId ofChecked(String domain, String id) throws InvalidIdException {
    Preconditions.checkNotNull(domain, "Null domain");
    Preconditions.checkNotNull(id, "Null id");
    if (!WaveIdentifiers.isValidDomain(0, domain)) {
      throw new InvalidIdException(domain, "Invalid domain");
    }
    if (!WaveIdentifiers.isValidIdentifier(id)) {
      throw new InvalidIdException(id, "Invalid id");
    }
    return new WaveletId(domain, id);
  }

  /**
   * Creates a wavelet id without doing validity checking, except for a
   * deprecated serialization scheme.
   *
   * @param domain must not be null. This is assumed to be of a valid canonical
   *        domain format.
   * @param id must not be null. This is assumed to be escaped with
   *        SimplePrefixEscaper.DEFAULT_ESCAPER.
   * @throws IllegalArgumentException if domain or id is invalid.
   */
  public static WaveletId ofLegacy(String domain, String id) {
    Preconditions.checkNotNull(domain, "Null domain");
    Preconditions.checkNotNull(id, "Null id");
    Preconditions.checkArgument(!domain.isEmpty(), "Empty domain");
    Preconditions.checkArgument(!id.isEmpty(), "Empty id");

    if (SimplePrefixEscaper.DEFAULT_ESCAPER.hasEscapeCharacters(domain)) {
      Preconditions.illegalArgument(
          "Domain cannot contain characters that requires escaping: " + domain);
    }

    if (!SimplePrefixEscaper.DEFAULT_ESCAPER.isEscapedProperly(IdConstants.TOKEN_SEPARATOR, id)) {
      Preconditions.illegalArgument("Id is not properly escaped: " + id);
    }

    return new WaveletId(domain, id);
  }

  private WaveletId(String domain, String id) {
    // Intern domain string for memory efficiency.
    this.domain = domain.intern();
    this.id = id;
  }

  /**
   * @return the domain
   */
  public String getDomain() {
    return domain;
  }

  /**
   * @return the local id
   */
  public String getId() {
    return id;
  }

  /**
   * Serialises this waveletId into a unique string. For any two wavelet ids,
   * waveletId1.serialise().equals(waveletId2.serialise()) iff waveId1.equals(waveId2).
   */
  public String serialise() {
    if (cachedSerialisation == null) {
      cachedSerialisation = LegacyIdSerialiser.INSTANCE.serialiseWaveletId(this);
    }
    return cachedSerialisation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + domain.hashCode();
    result = prime * result + id.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WaveletId)) {
      return false;
    }
    WaveletId other = (WaveletId) obj;
    // Equals method even though domains are interned since deserialized
    // instances might not be.
    return domain.equals(other.domain) && id.equals(other.id);
  }

  @Override
  public String toString() {
    return "[WaveletId " + ModernIdSerialiser.INSTANCE.serialiseWaveletId(this) + "]";
  }

  @Override
  public int compareTo(WaveletId other) {
    int domainCompare = domain.compareTo(other.domain);
    if (domainCompare == 0) {
      return id.compareTo(other.id);
    } else {
      return domainCompare;
    }
  }
}
