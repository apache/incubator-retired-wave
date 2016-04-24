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

package org.waveprotocol.wave.concurrencycontrol;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * The base class for client concurrencycontrol tests.
 *
 */
public abstract class TestBase extends GWTTestCase {

  /**
   * The default constructor.
   */
  public TestBase() {
  }

  /**
   * Specifies a module to use when running this test case. The returned module
   * must cause the source for this class to be included.
   *
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.concurrencycontrol.Tests";
  }

}
