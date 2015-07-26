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

import junit.framework.TestCase;

import java.net.URLEncoder;

/**
 * Test cases for {@link Util}.
 */
public class UtilRobotTest extends TestCase {

  public void testIsValidProxyForId() throws Exception {
    assertTrue(Util.isValidProxyForId(""));
    assertTrue(Util.isValidProxyForId("proxyid"));
    assertTrue(Util.isValidProxyForId("proxy-id1+gmail.com"));
    assertTrue(Util.isValidProxyForId("proxy-id1_at_gmail.com"));
    assertTrue(Util.isValidProxyForId("proxy-id%201_at_gmail.com"));
    assertTrue(Util.isValidProxyForId(URLEncoder.encode("proxyid@bar.com", "UTF-8")));

    assertFalse(Util.isValidProxyForId("proxy id1"));
    assertFalse(Util.isValidProxyForId("proxy\u0000id1"));
    assertFalse(Util.isValidProxyForId("proxy\u0009id1"));
    assertFalse(Util.isValidProxyForId("proxy\u001Fid1"));
    assertFalse(Util.isValidProxyForId("proxy@id"));
    assertFalse(Util.isValidProxyForId("proxy,id"));
    assertFalse(Util.isValidProxyForId("proxy:id"));
    assertFalse(Util.isValidProxyForId("proxy<id"));
    assertFalse(Util.isValidProxyForId("proxy>id"));
    assertFalse(Util.isValidProxyForId("proxy\u007Fid"));
  }

  public void testCheckIsValidProxyForId() throws Exception {
    try {
      Util.checkIsValidProxyForId("foo@bar.com");
      fail("Should have failed since input was not encoded.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      Util.checkIsValidProxyForId(null);
    } catch (IllegalArgumentException e) {
      fail("Null is valid proxy id.");
    }

    try {
      Util.checkIsValidProxyForId("");
    } catch (IllegalArgumentException e) {
      fail("Empty string is a valid proxy id.");
    }

    try {
      Util.checkIsValidProxyForId("foo+bar.com");
    } catch (IllegalArgumentException e) {
      fail("Input was encoded.");
    }

    try {
      Util.checkIsValidProxyForId(URLEncoder.encode("foo@bar.com", "UTF-8"));
    } catch (IllegalArgumentException e) {
      fail("Input was encoded.");
    }

    try {
      Util.checkIsValidProxyForId(null);
    } catch (IllegalArgumentException e) {
      fail("Input was encoded.");
    }
  }
}
