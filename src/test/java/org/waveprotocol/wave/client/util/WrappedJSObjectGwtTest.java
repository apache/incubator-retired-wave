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

package org.waveprotocol.wave.client.util;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for WrappedJSObject
 *
 */

public class WrappedJSObjectGwtTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.util.ClientFlags";
  }

  private static final native ExtendedJSObject getExtendedJSO() /*-{
    return {booleanValue:true, stringValue:'hello'};
  }-*/;

  private static final WrappedJSObject getWrappedJSO() {
    return new WrappedJSObject(getExtendedJSO());
  }

  public void testSafeGetters() {
    WrappedJSObject jsObj = getWrappedJSO();
    assertEquals(Boolean.valueOf(true), jsObj.getBoolean("booleanValue"));
    assertEquals("hello", jsObj.getString("stringValue"));
  }

  public void testNonExistent() {
    WrappedJSObject jsObj = getWrappedJSO();
    assertEquals(jsObj.getBoolean("nonexistent"), null);
  }

  public void testTypechecking() {
    WrappedJSObject jsObj = getWrappedJSO();
    assertEquals(null, jsObj.getBoolean("stringValue"));
    assertEquals(null, jsObj.getInteger("stringValue"));
  }

}
