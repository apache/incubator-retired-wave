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

import com.google.common.base.Charsets;

import junit.framework.TestCase;

/*
 * Test cases for {@link RobotConnectionUtil}.
 */

public class RobotConnectionUtilTest extends TestCase {

  public void testValidateAndReadResponse() throws Exception {
    String url = "http://foo.appspot.com/_wave/robot/jsonrpc";
    int statusCode = 200;
    String response = "{'foo':'bar'}";
    assertEquals(response, RobotConnectionUtil.validateAndReadResponse(url, statusCode,
        response.getBytes(Charsets.UTF_8)));
  }

  public void testValidateAndReadResponseOnNonOkResponse() throws Exception {
    String url = "http://foo.appspot.com/_wave/robot/jsonrpc";
    int statusCode = 404;
    try {
      RobotConnectionUtil.validateAndReadResponse(url, statusCode, (byte[]) null);
    } catch (RobotConnectionException e) {
      assertEquals(statusCode, e.getStatusCode());
      assertTrue(e.getMessage().contains(url));
    }
  }
}
