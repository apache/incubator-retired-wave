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

package org.waveprotocol.wave.model.document.operation.util;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class ImmutableStateMapTest extends TestCase {

  public void testRemovalInUpdateWith() {
    assertFalse(new AttributesImpl("a", "1").updateWith(
        new AttributesUpdateImpl("a", "1", null)).containsKey("a"));
  }

  public void testVarargsConstructor() {
    Attributes a = new AttributesImpl("c", "0", "a", "1", "b", "2");
    Map<String, String> m = new HashMap<String, String>();
    m.put("a", "1");
    m.put("b", "2");
    m.put("c", "0");
    assertEquals(new AttributesImpl(m), a);
  }

  public void testUpdate() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("a", "0");
    AttributesImpl a = new AttributesImpl(m).updateWith(new AttributesUpdateImpl("b", null, "1"));
    assertEquals("0", a.get("a"));
    assertEquals("1", a.get("b"));
    assertEquals(2, a.size());
  }

  public void testCheckAttributesSorted() {
    // see also the corresponding tests in ImmutableUpdateMapTest.
    ImmutableStateMap.checkAttributesSorted(Arrays.asList(new Attribute[] {}));
    ImmutableStateMap.checkAttributesSorted(Arrays.asList(new Attribute("a", "1")));
    ImmutableStateMap.checkAttributesSorted(Arrays.asList(
        new Attribute("aa", "1"), new Attribute("ab", "1")));
    ImmutableStateMap.checkAttributesSorted(Arrays.asList(
        new Attribute("a", "1"), new Attribute("b", "2"), new Attribute("c", "1")));
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("asdfa", "1"), new Attribute("asdfb", "2"), new Attribute("asdfb", "2")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("rar", "1"), new Attribute("rar", "2"), new Attribute("rbr", "1")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          null, new Attribute("a", "2")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "2"), null));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "1"), new Attribute("a", "1")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "1"), null, new Attribute("c", "1")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          null, new Attribute("a", "1"), new Attribute("c", "1")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "1"), new Attribute("c", "1"), null));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "1"), new Attribute("a", "1"), null));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableStateMap.checkAttributesSorted(Arrays.asList(
          new Attribute("a", "1"), new Attribute("c", "2"), new Attribute("b", "3")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
  }
}
