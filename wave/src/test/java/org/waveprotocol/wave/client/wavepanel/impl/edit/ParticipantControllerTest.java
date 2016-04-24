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

package org.waveprotocol.wave.client.wavepanel.impl.edit;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Tests for the {@link ParticipantController} class.
 *
 */
public class ParticipantControllerTest extends TestCase {

  // Valid
  private static final String DOMAIN_ONLY_ADDRESS = "@example.com";
  private static final String TYPICAL_ADDRESS = "test@example.com";

  // Invalid.
  private static final String EMPTY_ADDRESS = "";
  private static final String NO_DOMAIN_PREFIX = "test";
  private static final String NO_DOMAIN_ADDRESS = "test@";
  private static final String PREFIX_ONLY_ADDRESS = "@";

  /**
  * Tests for ParticipantController.buildParticipantList() method that creates a participant
  * list from a comma separated string
  */
  public void testOneValidAddressInAddressList() throws Exception {
    assertAddressListValid(TYPICAL_ADDRESS);
    assertAddressListValid(DOMAIN_ONLY_ADDRESS);
    assertLocalAddressListValid(NO_DOMAIN_PREFIX);
  }

  public void testOneInvalidAddressInAddressList() throws Exception {
    assertAddressListInvalid(EMPTY_ADDRESS);
    assertAddressListInvalid(NO_DOMAIN_PREFIX);
    assertAddressListInvalid(NO_DOMAIN_ADDRESS);
    assertAddressListInvalid(PREFIX_ONLY_ADDRESS);

    try {
      ParticipantController.buildParticipantList(null, null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // Expected.
    }
  }

  public void testMultipleVaildAddresses()  throws Exception {
    String stringOne = "test1@test.com";
    String stringTwo = "test2@example.com";
    String stringThree = "test3@example.com";
    ParticipantId idOne = ParticipantId.ofUnsafe(stringOne);
    ParticipantId idTwo = ParticipantId.ofUnsafe(stringTwo);
    ParticipantId idThree = ParticipantId.ofUnsafe(stringThree);

    ParticipantId[] participants = ParticipantController.buildParticipantList(
        null, stringOne + "," + stringTwo + "," + stringThree);

    assertEquals("Wrong number of participants created", 3, participants.length);
    assertEquals("Participant one not the same", idOne, participants[0]);
    assertEquals("Participant two not the same", idTwo, participants[1]);
    assertEquals("Participant three not the same", idThree, participants[2]);
  }

  public void testMultipleLocalVaildAddresses() throws Exception {
    String stringOne = "test1";
    String stringTwo = "test2";
    String stringThree = "test3";
    ParticipantId idOne = ParticipantId.ofUnsafe(stringOne + "@localhost");
    ParticipantId idTwo = ParticipantId.ofUnsafe(stringTwo + "@localhost");
    ParticipantId idThree = ParticipantId.ofUnsafe(stringThree + "@localhost");

    ParticipantId[] participants = ParticipantController.buildParticipantList(
        "localhost", stringOne + "," + stringTwo + "," + stringThree);

    assertEquals("Wrong number of participants created", 3, participants.length);
    assertEquals("Participant one not the same", idOne, participants[0]);
    assertEquals("Participant two not the same", idTwo, participants[1]);
    assertEquals("Participant three not the same", idThree, participants[2]);
  }

  public void testMultipleAddressesWithOneInvalid() {
    assertAddressListInvalid(NO_DOMAIN_ADDRESS + "," + TYPICAL_ADDRESS);
    assertAddressListInvalid(NO_DOMAIN_PREFIX + "," + TYPICAL_ADDRESS);
    assertAddressListInvalid(TYPICAL_ADDRESS + "," + NO_DOMAIN_PREFIX + "," + TYPICAL_ADDRESS);
    assertLocalAddressListInvalid(TYPICAL_ADDRESS + "," + NO_DOMAIN_PREFIX + PREFIX_ONLY_ADDRESS);
  }

  /**
   * Checks that a comma separated address list is valid (by throwing an exception if it is not)
   */
  private static void assertAddressListValid(String addresses) throws InvalidParticipantAddress {
    ParticipantController.buildParticipantList(null, addresses);
  }

  /**
   * Checks that a comma separated address list is valid (by throwing an exception if it is not)
   * when local domain is provided
   */
  private static void assertLocalAddressListValid(String addresses) throws InvalidParticipantAddress {
    ParticipantController.buildParticipantList("localhost", addresses);
  }

  /**
   * Checks that an comma separated address list is not valid.
   */
  private static void assertAddressListInvalid(String addresses) {
    try {
      ParticipantController.buildParticipantList(null, addresses);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Expected.
    }
  }

  /**
   * Checks that an comma separated address list is not valid when local domain
   * is provided
   */
  private static void assertLocalAddressListInvalid(String addresses) {
    try {
      ParticipantController.buildParticipantList("localhost", addresses);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Expected.
    }
  }
}
