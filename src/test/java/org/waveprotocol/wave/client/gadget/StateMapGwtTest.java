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

package org.waveprotocol.wave.client.gadget;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the StateMap class.
 *
 */

public class StateMapGwtTest extends GWTTestCase {
  /**
   * Sample JSON string to convert to StateMap object.
   */
  private final static String TEST_JSON_STRING = "{" +
      "\"testKey\":\"testValue\"," +
      "\"hasOwnProperty\":\"oddCase\"," +
      "\"nullKey\":null," +
      "\"nullString\":\"null\"" +
    "}";

  /**
   * String with characters that must be escaped in JS strings.
   */
  private final String ESCAPED_CHARS = "' \" \\ \b \t \n \f \r \u200f";

  /**
   * Tests the basic map operations method.
   */
  public void testHasPutGetRemove() {
    StateMap ab = createTestMapAB();
    assertTrue(ab.has("a"));
    assertTrue(ab.has("b"));
    assertFalse(ab.has("c"));
    assertEquals("a value", ab.get("a"));
    assertEquals("b value", ab.get("b"));
    assertEquals(null, ab.get("c"));
    ab.put("a", null);
    ab.put("c", "c value");
    assertTrue(ab.has("a"));
    assertTrue(ab.has("b"));
    assertTrue(ab.has("c"));
    assertEquals(null, ab.get("a"));
    assertEquals("b value", ab.get("b"));
    assertEquals("c value", ab.get("c"));
    ab.remove("a");
    ab.remove("b");
    assertFalse(ab.has("a"));
    assertFalse(ab.has("b"));
    assertTrue(ab.has("c"));
    assertEquals(null, ab.get("a"));
    assertEquals(null, ab.get("b"));
    assertEquals("c value", ab.get("c"));
  }

  /**
   * Tests the comparison method.
   */
  public void testCompare() {
    StateMap ab = createTestMapAB();
    StateMap ba = createTestMapBA();
    assertFalse(ab.compare(null));
    assertTrue(ab.compare(ab));
    assertTrue(ab.compare(ba));
    assertTrue(ba.compare(ab));
    ab.remove("a");
    assertFalse(ab.compare(ba));
    assertFalse(ba.compare(ab));
  }

  /**
   * Tests the copy method.
   */
  public void testCopyFrom() {
    StateMap ab = createTestMapAB();
    StateMap cd = createTestMapCD();
    ab.copyFrom(cd);
    assertTrue(ab.compare(cd));
  }

  /**
   * Tests the toJson method.
   */
  public void testToJson() {
    StateMap ab = createTestMapAB();
    assertEquals("{\"a\":\"a value\",\"b\":\"b value\"}", ab.toJson());
  }

  /**
   * Tests the escaped character handling.
   */
  public void testEscapedChars() {
    StateMap map = StateMap.create();
    map.put("funnyChars", ESCAPED_CHARS);
    String json = map.toJson();
    assertEquals("{\"funnyChars\":\"' \\\" \\\\ \\b \\t \\n \\f \\r \\u200f\"}", json);
    StateMap anotherMap = StateMap.create();
    anotherMap.fromJson(json);
    assertEquals(ESCAPED_CHARS, anotherMap.get("funnyChars"));
  }

  /**
   * Tests the fromJson method.
   */
  public void testFromJson() {
    StateMap map = StateMap.create();
    map.fromJson(TEST_JSON_STRING);
    assertEquals("testValue", map.get("testKey"));
    assertEquals("oddCase", map.get("hasOwnProperty"));
    assertTrue(map.has("nullKey"));
    assertEquals(null, map.get("nullKey"));
    assertEquals("null", map.get("nullString"));
  }

  /**
   * Tests the applyDelta method.
   */
  public void testApplyDelta() {
    StateMap delta = StateMap.create();
    delta.fromJson(TEST_JSON_STRING);
    StateMap mapToModify = StateMap.create();
    mapToModify.put("nullKey", "notNull");
    assertEquals("notNull", mapToModify.get("nullKey"));
    mapToModify.applyDelta(delta);
    assertEquals("testValue", mapToModify.get("testKey"));
    assertEquals("oddCase", mapToModify.get("hasOwnProperty"));
    assertFalse(mapToModify.has("nullKey"));
    assertEquals("null", mapToModify.get("nullString"));
  }

  /**
   * Tests the getDelta method.
   */
  public void testGetDelta() {
    StateMap ab = createTestMapAB();
    StateMap cd = createTestMapCD();
    StateMap delta = ab.getDelta(cd);
    assertTrue("a in delta", delta.has("a"));
    assertTrue("b in delta", delta.has("b"));
    assertTrue("c in delta", delta.has("c"));
    assertTrue("d in delta", delta.has("d"));
    assertTrue(ab.has("a"));
    assertTrue(ab.has("b"));
    assertFalse(ab.has("c"));
    assertFalse(ab.has("d"));
    assertFalse(cd.has("a"));
    assertFalse(cd.has("b"));
    assertTrue(cd.has("c"));
    assertTrue(cd.has("d"));
    assertEquals(null, delta.get("a"));
    assertEquals(null, delta.get("b"));
    assertEquals("c value", delta.get("c"));
    assertEquals("d value", delta.get("d"));
  }

  /**
   * Tests the checkKeyValue iteration method with Each class.
   */
  public void testEach() {
    StateMap ab = createTestMapAB();
    final int[] counters = new int[2];
    ab.each(new StateMap.Each() {
      @Override
      public void apply(String key, String value) {
        if (key.equals("a") && value.equals("a value")) {
          counters[0]++;
        } else if (key.equals("b") && value.equals("b value")) {
          counters[1]++;
        } else {
          fail("Ivalid key-value pair " + key + ":" + value);
        }
      }
    });
    assertEquals(1, counters[0]);
    assertEquals(1, counters[1]);
  }

  /**
   * Tests embedded each loops. Makes sure that the loops have no static
   * dependency.
   */
  public void testEachInEach() {
    StateMap ab = createTestMapAB();
    final int[] counters = new int[4];
    ab.each(new StateMap.Each() {
      @Override
      public void apply(String key, String value) {
        if (key.equals("a") && value.equals("a value")) {
          counters[0]++;
        } else if (key.equals("b") && value.equals("b value")) {
          counters[1]++;
        } else {
          fail("Ivalid key-value pair " + key + ":" + value);
        }
        StateMap cd = createTestMapCD();
        cd.each(new StateMap.Each() {
          @Override
          public void apply(String key, String value) {
            if (key.equals("c") && value.equals("c value")) {
              counters[2]++;
            } else if (key.equals("d") && value.equals("d value")) {
              counters[3]++;
            } else {
              fail("Ivalid key-value pair " + key + ":" + value);
            }
          }
        });
      }
    });
    assertEquals(1, counters[0]);
    assertEquals(1, counters[1]);
    assertEquals(2, counters[2]);
    assertEquals(2, counters[3]);
  }

  /**
   * Tests the checkKeyValue iteration method with CheckKeyValue interface.
   */
  public void testCheckKeyValue() {
    StateMap ab = createTestMapAB();
    final int[] counters = {0, 0};
    boolean result = ab.checkKeyValue(new StateMap.CheckKeyValue() {
      @Override
      public boolean check(String key, String value) {
        if (key.equals("a") && value.equals("a value")) {
          counters[0]++;
          return false;
        } else if (key.equals("b") && value.equals("b value")) {
          fail("The b key should NOT be reached in the loop.");
        } else {
          fail("Ivalid key-value pair " + key + ":" + value);
        }
        return true;
      }
    });
    assertFalse(result);
    assertEquals(1, counters[0]);
    result = ab.checkKeyValue(new StateMap.CheckKeyValue() {
      @Override
      public boolean check(String key, String value) {
        if (key.equals("a") && value.equals("a value")) {
          counters[0]++;
          return true;
        } else if (key.equals("b") && value.equals("b value")) {
          counters[1]++;
          return false;
        } else {
          fail("Ivalid key-value pair " + key + ":" + value);
        }
        return true;
      }
    });
    assertFalse(result);
    assertEquals(2, counters[0]);
    assertEquals(1, counters[1]);
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.gadget.tests";
  }

  /**
   * Creates a test map with a and b keys.
   *
   * @return map with a and b keys.
   */
  private native StateMap createTestMapAB() /*-{
    return {
      ":a": "a value",
      ":b": "b value",
    };
  }-*/;

  /**
   * Creates a test map with c and d keys.
   *
   * @return map with c and d keys.
   */
  private native StateMap createTestMapCD() /*-{
    return {
      ":c": "c value",
      ":d": "d value",
    };
  }-*/;

  /**
   * Creates a test map with b and a keys. The key ordering affects the JS
   * iteration order.
   *
   * @return map with b and a keys.
   */
  private native StateMap createTestMapBA() /*-{
    return {
      ":b": "b value",
      ":a": "a value",
    };
  }-*/;
}
