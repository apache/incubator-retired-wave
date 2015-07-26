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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */

public class UrlParametersGwtTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.util.ClientFlags";
  }

  public void testSomeQueries() {
    UrlParameters u = new UrlParameters("?act=new");
    assertEquals("test1", "new", u.getParameter("act"));
    assertEquals("test2", null, u.getParameter("cnv"));

    u = new UrlParameters("?act=new&cnv=123");
    assertEquals("test3", "new", u.getParameter("act"));
    assertEquals("test4", "123", u.getParameter("cnv"));
  }

  public void testNonExistentQuery() {
    UrlParameters u = new UrlParameters("?");
    assertEquals("test5", null, u.getParameter("act"));
    assertEquals("test6", null, u.getParameter("cnv"));

    u = new UrlParameters("");
    assertEquals("test7", null, u.getParameter("nonexistent_key"));
  }

  public void testSafeGetters() {
    UrlParameters u = new UrlParameters("?booleanValue=true&stringValue=hello");
    assertEquals(Boolean.valueOf(true), u.getBoolean("booleanValue"));
    assertEquals("hello", u.getString("stringValue"));
    assertEquals(null, u.getDouble("booleanValue"));
  }

  public void testInvalidQueryStrings() {
    UrlParameters u = new UrlParameters("?act=");
    assertEquals("", u.getParameter("act"));

    u = new UrlParameters("?act==&");
    assertEquals("", u.getParameter("act"));


    u = new UrlParameters("?act=&cnv=3");
    assertEquals("", u.getParameter("act"));
    assertEquals("3", u.getParameter("cnv"));
  }

  public void testParamsDecoded() {
    UrlParameters u = new UrlParameters("?a+b=c+d");
    assertEquals("c d", u.getParameter("a b"));
  }

  public void testNonExistent() {
    UrlParameters u = new UrlParameters("?booleanValue=true&stringValue=hello");
    assertEquals(null, u.getBoolean("nonexistent"));
  }

  public void testBuildQueryString() {
    // Test empty map
    assertEquals("", UrlParameters.buildQueryString(Collections.<String, String> emptyMap()));

    // Test one item
    assertEquals("?item=1", UrlParameters.buildQueryString(Collections
        .<String, String> singletonMap("item", "1")));

    // Test that characters are urlencoded
    assertEquals("?one+one=one+two", UrlParameters.buildQueryString(Collections
        .<String, String> singletonMap("one one", "one two")));

    // Test multiple items
    Map<String, String> queryMap = new HashMap<String, String>();
    queryMap.put("a", "b");
    queryMap.put("c", "d");
    queryMap.put("e", "f");
    String queryString = UrlParameters.buildQueryString(queryMap);

    UrlParameters u = new UrlParameters(queryString);
    assertEquals("b", u.getParameter("a"));
    assertEquals("d", u.getParameter("c"));
    assertEquals("f", u.getParameter("e"));
  }
}
