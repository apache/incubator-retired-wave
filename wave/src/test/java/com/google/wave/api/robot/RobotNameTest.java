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

package com.google.wave.api.robot;


import junit.framework.TestCase;

/*
 * Test case for parsing robot addresses.
 */

public class RobotNameTest extends TestCase {

  public void testWellFormedAddress() {
    assertFalse(RobotName.isWellFormedAddress("foo"));
    assertFalse(RobotName.isWellFormedAddress("bar.com"));
    assertFalse(RobotName.isWellFormedAddress("@bar.com"));
    assertFalse(RobotName.isWellFormedAddress("foo@"));
    assertTrue(RobotName.isWellFormedAddress("foo@bar.com"));
    assertTrue(RobotName.isWellFormedAddress("foo#1@bar.com"));
    assertTrue(RobotName.isWellFormedAddress("foo+wave#1@bar.com"));
    assertTrue(RobotName.isWellFormedAddress("foo+wave+foo#1@bar.com"));
  }

  public void testBasicParsing() {
    RobotName address = RobotName.fromAddress("robot@appspot.com");
    assertEquals("appspot.com", address.getDomain());
    assertEquals("robot", address.getId());
  }

  public void testComplexParsing() {
    RobotName address = RobotName.fromAddress("robot+proxy#version@appspot.com");
    assertEquals("appspot.com", address.getDomain());
    assertEquals("robot", address.getId());
    assertEquals("proxy", address.getProxyFor());
    assertEquals("version", address.getVersion());
  }

  public void testToAddress() {
    assertEquals("robot@appspot.com",
        RobotName.fromAddress("robot@appspot.com").toParticipantAddress());
    assertEquals("robot+id@appspot.com",
        RobotName.fromAddress("robot+id@appspot.com").toParticipantAddress());
    assertEquals("robot+id#1@appspot.com",
        RobotName.fromAddress("robot+id#1@appspot.com").toParticipantAddress());

    assertEquals("robot@appspot.com",
        RobotName.fromAddress("robot#1@appspot.com").toEmailAddress());
    assertEquals("robot#1@appspot.com",
        RobotName.fromAddress("robot#1@appspot.com").toEmailAddressWithVersion());
    assertEquals("robot@appspot.com",
        RobotName.fromAddress("robot+proxy#1@appspot.com").toEmailAddress());
    assertEquals("robot#1@appspot.com",
        RobotName.fromAddress("robot+proxy#1@appspot.com").toEmailAddressWithVersion());
  }

  public void testBadAddressReturnsNull() {
    assertNull(RobotName.fromAddress("foo"));
  }
}
