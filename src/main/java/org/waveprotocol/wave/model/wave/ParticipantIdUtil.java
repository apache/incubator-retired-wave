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

import org.waveprotocol.wave.model.id.WaveIdentifiers;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Utility methods for participant IDs.
 *
 */
public final class ParticipantIdUtil {

  /** Unknown participant ID */
  public static final ParticipantId UNKNOWN = new ParticipantId("unknown");

  private ParticipantIdUtil() {}

  /**
   * Normalises an address.
   *
   * @param address  address to normalise; may be null
   * @return normal form of {@code address} if non-null; null otherwise.
   */
  // TODO(ohler): Make this not nullable.
  public static String normalize(String address) {
    if (address == null) {
      return null;
    }
    return address.toLowerCase();
  }

  /**
   * @param x a String
   * @return true if x is a wave address and normalized
   */
  // NOTE(ohler), 2009-10-27:
  // Unfortunately, not all addresses in our database are normalized:
  // Some deltas contain authors of "<nobody>" and similar (not wave
  // addresses), or may contain upper-case characters (not
  // normalized).  For future deltas, however, the wave server
  // enforces that they have normalized addresses as their authors and
  // as participant IDs in add/remove participant ops.
  //
  // If the federation code that serves outgoing deltas encounters a
  // (historic) delta that does not satisfy this constraint, it will
  // try to normalize the addresses in case the problem is just
  // upper-case characters, but if there's a junk address like
  // "<nobody>" that normalization doesn't fix, the entire wavelet
  // will become non-federable.
  public static boolean isNormalizedAddress(String x) {
    Preconditions.checkNotNull(x, "Null address");
    // TODO(ohler): Define what wave addresses really are, and add proper checks here.
    if (!x.equals(normalize(x))) {
      return false;
    }
    int at = x.indexOf('@');
    return at > 0 && WaveIdentifiers.isValidDomain(at + 1, x);
  }

  /**
   * Check if the address is a domain address in the form "@domain.com".
   */
  public static boolean isDomainAddress(String address) {
    int sepIndex = address.indexOf(ParticipantId.DOMAIN_PREFIX);
    return (sepIndex == 0 && WaveIdentifiers.isValidDomain(1, address));
  }

  public static String makeDomainAddress(String domain) {
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, domain), "Invalid domain: %s",
        domain);
    return ParticipantId.DOMAIN_PREFIX + domain;
  }
  
  /**
   * Makes the shared domain participant id.
   * 
   * @param domain the wave domain. The wave domain should be validated by
   *        {@link #isDomainAddress(String)} before calling this method. Failure
   *        to pass a validated wave domain may result in a run time exception.
   * @return the shared domain participant id of the form: "@example.com".
   */
  public static ParticipantId makeUnsafeSharedDomainParticipantId(String domain) {
    return ParticipantId.ofUnsafe(makeDomainAddress(domain));
  }

}
