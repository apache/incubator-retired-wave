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

package org.waveprotocol.wave.concurrencycontrol.common;


import junit.framework.TestCase;


/**
 * Tests for the SystemClock implementation.
 *
 * @author anorth@google.com (Alex North)
 */

public class SystemClockTest extends TestCase {

  Clock clock;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clock = new SystemClock();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Tests that the system clock returns a timestamp approximately equal to
   * the current system time.
   */
  public void testGetCurrentTime() {
    assertEquals(System.currentTimeMillis(), clock.getCurrentTime(), 100);
  }
}
