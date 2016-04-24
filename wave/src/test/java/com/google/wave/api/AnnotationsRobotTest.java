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

/*
 * Test cases for {@link Annotation}.
 */
public class AnnotationsRobotTest extends TestCase {

  public void testAdd() throws Exception {
    Annotations annotations = new Annotations();

    annotations.add("name", "value", 3, 5);
    assertEquals(1, annotations.get("name").size());

    annotations.add("name", "value", 1, 2);
    annotations.add("name", "value", 10, 15);
    assertEquals(3, annotations.get("name").size());

    annotations.add("name", "value", 4, 7);
    assertEquals(3, annotations.get("name").size());
    assertEquals(new Range(3, 7), annotations.get("name").get(2).getRange());

    annotations.add("name", "value2", 6, 7);
    assertEquals(4, annotations.get("name").size());
    assertEquals(new Range(3, 6), annotations.get("name").get(2).getRange());
    assertEquals("value", annotations.get("name").get(2).getValue());
    assertEquals(new Range(6, 7), annotations.get("name").get(3).getRange());
    assertEquals("value2", annotations.get("name").get(3).getValue());
  }

  public void testDelete() throws Exception {
    Annotations annotations = new Annotations();
    annotations.add("name", "value", 1, 2);
    annotations.add("name2", "value", 3, 5);
    annotations.add("name2", "value", 10, 15);

    annotations.delete("name", 2, 3);
    assertEquals(1, annotations.get("name").size());
    annotations.delete("name", 1, 2);
    assertNull(annotations.get("name"));

    annotations.delete("name2", 1, 6);
    assertEquals(1, annotations.get("name2").size());
    annotations.delete("name2", 10, 12);
    assertEquals(1, annotations.get("name2").size());
    assertEquals(new Range(12, 15), annotations.get("name2").get(0).getRange());
  }

  public void testShift() throws Exception {
    Annotations annotations = new Annotations();
    annotations.add("name", "value", 1, 3);
    annotations.add("name2", "value", 3, 5);
    annotations.add("name2", "value", 10, 15);

    annotations.shift(2, 3);
    assertEquals(new Range(1, 6), annotations.get("name").get(0).getRange());
    assertEquals(new Range(6, 8), annotations.get("name2").get(0).getRange());
    assertEquals(new Range(13, 18), annotations.get("name2").get(1).getRange());

    annotations = new Annotations();
    annotations.add("name", "value", 1, 3);
    annotations.add("name", "value", 5, 8);
    annotations.shift(5, -2);
    assertEquals(1, annotations.get("name").size());
    assertEquals(new Range(1, 6), annotations.get("name").get(0).getRange());
  }
}
