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

package org.waveprotocol.box.server.robots.agent;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link RobotAgentUtil}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotAgentUtilTest extends TestCase {

  public void testLastEnteredLineOf() throws Exception {
    String test1 = "Hello World!\n";
    String test2 = "\n\nHello World!\n";
    String test3 = "\n\nHello World!\nHello World without line end.";
    String test4 = "\n\nHello World!\nHello World with line end.\n";
    String test5 = "\n";
    assertEquals("Hello World!", RobotAgentUtil.lastEnteredLineOf(test1));
    assertEquals("Hello World!", RobotAgentUtil.lastEnteredLineOf(test2));
    assertNull(RobotAgentUtil.lastEnteredLineOf(test3));
    assertEquals("Hello World with line end.", RobotAgentUtil.lastEnteredLineOf(test4));
    assertEquals("", RobotAgentUtil.lastEnteredLineOf(test5));
  }
}
