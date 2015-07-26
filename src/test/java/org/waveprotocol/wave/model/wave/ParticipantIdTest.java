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


import junit.framework.TestCase;

/**
 * Tests for the {@link ParticipantId} class.
 *
 */

public class ParticipantIdTest extends TestCase {

  // Valid
  private static final String DOMAIN_ONLY_ADDRESS = "@example.com";
  private static final String TYPICAL_ADDRESS = "test@example.com";

  // Invalid.
  private static final String EMPTY_ADDRESS = "";
  private static final String NO_DOMAIN_PREFIX = "test";
  private static final String NO_DOMAIN_ADDRESS = "test@";
  private static final String PREFIX_ONLY_ADDRESS = "@";

  public void testTypicalAddressIsValid() throws Exception {
    assertAddressValid(TYPICAL_ADDRESS);
  }

  public void testDomainOnlyIsValid() throws Exception {
    assertAddressValid(DOMAIN_ONLY_ADDRESS);
  }

  public void testEmptyAddressIsNotValid() {
    assertAddressInvalid(EMPTY_ADDRESS);
  }

  public void testNoDomainPrefixIsNotValid() {
    assertAddressInvalid(NO_DOMAIN_PREFIX);
  }

  public void testNoDomainAddressIsNotValid() {
    assertAddressInvalid(NO_DOMAIN_ADDRESS);
  }

  public void testPrefixOnlyAddressIsNotValid() {
    assertAddressInvalid(PREFIX_ONLY_ADDRESS);
  }

  public void testNullAddressIsNotValid() throws Exception {
    try {
      ParticipantId.of(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // Expected.
    }
    try {
      ParticipantId.ofUnsafe(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // Expected.
    }
  }

  public void testComparable() {
    ParticipantId idOne = ParticipantId.ofUnsafe("test@test.com");
    ParticipantId idOneAgain = ParticipantId.ofUnsafe("test@test.com");
    ParticipantId idTwo = ParticipantId.ofUnsafe("test@example.com");
    ParticipantId idThree = ParticipantId.ofUnsafe("zzz@test.com");
    ParticipantId idNoDomain = new ParticipantId("test@");
    ParticipantId idNoName = new ParticipantId("@test.com");
    ParticipantId idInvalid = new ParticipantId("test");
    
    // Compare same-name, same-domain
    assertTrue(idOne.compareTo(idOneAgain) == 0);
    assertTrue(idOneAgain.compareTo(idOne) == 0);

    // Compare different-name, same domain
    assertTrue(idOne.compareTo(idThree) < 0);
    assertTrue(idThree.compareTo(idOne) > 0);
    
    // Compare same-name, different-domain
    assertTrue(idOne.compareTo(idTwo) > 0);
    assertTrue(idTwo.compareTo(idOne) < 0);

    // Compare no-name to no-domain (but both with domain prefix.
    assertTrue(idNoDomain.compareTo(idNoName) > 0);
    assertTrue(idNoName.compareTo(idNoDomain) < 0);

    // Compare two ids one with domain prefix, one without.
    assertTrue(idOne.compareTo(idInvalid) > 0);
    assertTrue(idInvalid.compareTo(idOne) < 0);
  }
  
  /**
   * Checks that an address is valid (by throwing an exception if it is not).
   */
  private static void assertAddressValid(String address) throws InvalidParticipantAddress {
    ParticipantId.of(address);
    ParticipantId.ofUnsafe(address);
  }

  /**
   * Checks that an address is not valid.
   */
  private static void assertAddressInvalid(String address) {
    try {
      ParticipantId.of(address);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Expected.
    }

    try {
      ParticipantId.ofUnsafe(address);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
}
